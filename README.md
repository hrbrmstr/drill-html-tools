# drill-html-tools

Apache Drill UDFs for retrieving and working with HTML text

Based on the [`jsoup`](https://github.com/jhy/jsoup/) library.

NOTE: This is definitely a WIP.

## UDFs

- `soup_read_html(url-string, timeout-ms)`: **This UDF _requires_ network reachability to the intended URL target**. Given a URL and a connection timeout (in milliseconds), this UDF will fetch the contents. Older Java installations may not have modern certificate stores available which can cause errors in resource retrieval.
- `soup_html_to_plaintext(html-string)`: Given a string of HTML, return the "text" version of it (i.e. no tags and all text content from all text nodes).
- `soup_select_text(html-string, css-selector-string [, include-child-node-text-boolean])`: The first parameter is the HTML text. The second parameter is a CSS selector for target node(s). The (optional) third parameter is a boolean indicating whether the text of all child nodes should be included with the results (default is `true` if not provided). The HTML will be parsed and if any elements are found matching the CSS selector their text &mdash; including all child text of the third parameter if `true` &mdash; will be returned in a list.
- `soup_select_attr(html-string, css-selector-string, attribute-key-string)`: The first parameter is the HTML text. The second parameter is a CSS selector for target node(s). The third parameter is the name of a node attribute (e.g. for `<a href="xyz">`, the selector would be `'a'` and the attribute key would be `'href'` to retrieve `"xyz"`). The HTML will be parsed and if any elements are found matching the CSS selector they will be scanned to see if they contain the specified attribute key; if any attribute keys were found they will be returned in a list.

## Building

Retrieve the dependencies and build the UDF:

```
make deps
make udf
```

To automatically install it locally, ensure `DRILL_HOME` is set (the `Makefile` has a default of `/usr/local/drill`) and:

```
make install
```

Assuming you're running in standalone mode, you can then do:

```
make restart
```

You can manually copy:

- `deps/jsoup-1.11.3.jar`
- `target/drill-html-tools-1.0.jar`
- `target/drill-html-tools-1.0-sources.jar`

(after a successful build) to your `$DRILL_HOME/jars/3rdparty` directory and manually restart Drill as well.


## Examples

### Basic usage

The query:

```
SELECT
    url,
    substring(contents, 1, 100) AS snippet,
    soup_select_text(contents, 'h1,h2,h3,h4') AS postTitles
FROM (
    SELECT
        url,
        soup_read_html(url, 5000) AS contents
    FROM
        (SELECT 
           'https://community.apache.org/' AS url
         FROM (VALUES((1))))
    )
```

Output:

```
$ drill-conf
apache drill 1.14.0-SNAPSHOT
"a little sql for your nosql"
0: jdbc:drill:> !set outputFormat vertical
0: jdbc:drill:> SELECT
. . . . . . . >     url,
. . . . . . . >     substring(contents, 1, 100) AS snippet,
. . . . . . . >     soup_select_text(contents, 'h1,h2,h3,h4') AS postTitles
. . . . . . . > FROM (
. . . . . . . >     SELECT
. . . . . . . >         url,
. . . . . . . >         soup_read_html(url, 5000) AS contents
. . . . . . . >     FROM
. . . . . . . >         (SELECT
. . . . . . . >            'https://community.apache.org/' AS url
. . . . . . . >          FROM (VALUES((1))))
. . . . . . . >     );
url         https://community.apache.org/
snippet     <!doctype html>
<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    con
postTitles  ["The Apache Software Foundation","Community > Code","Apache Community Development!","New to Apache","Events & Mentoring","The Apache Way","Newcomers to Apache","Event Organizers and Speakers","The Foundation","Open Source Contributors","Pilot Mentoring Programme with India ICFOSS","How Apache works","How To Ask Questions","Google Summer of Code program (GSoC)","FAQ & Code of Conduct"]

1 row selected (0.316 seconds)
```

### A more complex example

The query:

```
SELECT
  a.url AS url,
  substr(a.doc, 1, 100) AS origDoc,
  substr(soup_html_to_plaintext(a.doc), 1, 100) AS docTxt,
  soup_select_text(a.doc, 'title')[0] AS title,
  soup_select_attr(a.doc, 'a', 'href') AS links,
  soup_select_attr(a.doc, 'img', 'src') AS imgSrc,
  soup_select_attr(a.doc, 'script', 'src') AS scriptSrc
FROM (
    SELECT 
       url,
       soup_read_html(url, 5000) doc
    FROM 
      (SELECT 
           'https://community.apache.org/' AS url
         FROM (VALUES((1))))
) a
WHERE doc IS NOT NULL
```

Output:

```
0: jdbc:drill:> SELECT
. . . . . . . >   a.url AS url,
. . . . . . . >   substr(a.doc, 1, 100) AS origDoc,
. . . . . . . >   substr(soup_html_to_plaintext(a.doc), 1, 100) AS docTxt,
. . . . . . . >   soup_select_text(a.doc, 'title')[0] AS title,
. . . . . . . >   soup_select_attr(a.doc, 'a', 'href') AS links,
. . . . . . . >   soup_select_attr(a.doc, 'img', 'src') AS imgSrc,
. . . . . . . >   soup_select_attr(a.doc, 'script', 'src') AS scriptSrc
. . . . . . . > FROM (
. . . . . . . >     SELECT
. . . . . . . >        url,
. . . . . . . >        soup_read_html(url, 5000) doc
. . . . . . . >     FROM
. . . . . . . >       (SELECT
. . . . . . . >            'https://community.apache.org/' AS url
. . . . . . . >          FROM (VALUES((1))))
. . . . . . . > ) a
. . . . . . . > WHERE doc IS NOT NULL;
url        https://community.apache.org/
origDoc    <!doctype html>
<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    con
docTxt     Apache Community Development - Apache Community Development - Welcome Home About What We Do Frequent
title      Apache Community Development - Apache Community Development - Welcome
links      ["/","#","/about/","/newbiefaq.html","/projectIndependence.html","/apache-way/apache-project-maturity-model.html","/contactpoints.html","/comdevboardresolution.html","https://whimsy.apache.org/board/minutes/Community_Development.html","/links.html","https://issues.apache.org/jira/projects/COMDEV","#","/newcomers/","/gettingStarted/101.html","/contributors/","http://www.apache.org/dev/","/gsoc.html","/mentee-ranking-process.html","/mentoring/experiences.html","/guide-to-being-a-mentor.html","/use-the-comdev-issue-tracker-for-gsoc-tasks.html","/gsoc-admin-tasks.html","#","/newcommitter.html","/committers/","/committers/consensusBuilding.html","/committers/lazyConsensus.html","/committers/decisionMaking.html","/committers/voting.html","/committers/funding-disclaimer.html","#","/mentoringprogramme.html","/mentorprogrammeformaleducation.html","/mentorprogrammeapplication.html","/guide-to-being-a-mentor.html","/mentor-request-mail.html","#","http://apachecon.com/","/calendars/","http://www.apache.org/events/meetups.html","http://www.apache.org/foundation/marks/events","/speakers/","/speakers/speakers.html","/speakers/slides.html","#","https://www.facebook.com/ApacheSoftwareFoundation","http://twitter.com/ApacheCommunity","/lists.html","/","https://www.apache.org/events/current-event.html","newcomers/index.html","https://projects.apache.org/projects.html?category","http://www.apache.org/free/","speakers/index.html","http://www.apache.org/foundation/marks/events","events/small-events.html","calendars/index.html","http://www.apache.org/foundation/","http://www.apache.org/foundation/governance/","http://www.apache.org/foundation/board/calendar.html","http://www.apache.org/free/","http://www.apache.org/foundation/governance/","http://www.apache.org/foundation/contributing.html","contributors/index.html","links.html","committers/index.html","http://www.apache.org/dev/","mentoringprogramme-icfoss-pilot.html","apache-way/apache-project-maturity-model.html","http://www.apache.org/foundation/how-it-works.html","https://lists.apache.org/","http://community.apache.org/contributors/etiquette","https://lists.apache.org/list.html?dev@community.apache.org:lte=3M:","gsoc.html","mentoring/experiences.html","https://www.apache.org/foundation/policies/conduct","newbiefaq.html","https://lists.apache.org/list.html?dev@community.apache.org:lte=3M:","http://www.apache.org/licenses/LICENSE-2.0","https://www.apache.org/foundation/marks/list/"]
imgSrc     ["https://www.apache.org/events/current-event-125x125.png"]
scriptSrc  ["https://helpwanted.apache.org/widget.js","/js/jquery-1.9.1.min.js","/js/bootstrap.min.js"]

1 row selected (0.731 seconds)
```

