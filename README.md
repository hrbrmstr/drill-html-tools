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

### Multiple sites

From a CSV of a recent Alexa Top 500:

```
0: jdbc:drill:> SELECT
. . . . . . . >   `rank`, site
. . . . . . . > FROM dfs.d.`/top500.csvh`
. . . . . . . > LIMIT 30
. . . . . . . > ;
+-------+------------------+
| rank  |       site       |
+-------+------------------+
| 1     | google.com       |
| 2     | youtube.com      |
| 3     | facebook.com     |
| 4     | baidu.com        |
| 5     | wikipedia.org    |
| 6     | yahoo.com        |
| 7     | qq.com           |
| 8     | taobao.com       |
| 9     | twitter.com      |
| 10    | amazon.com       |
| 11    | tmall.com        |
| 12    | instagram.com    |
| 13    | google.co.in     |
| 14    | sohu.com         |
| 15    | live.com         |
| 16    | vk.com           |
| 17    | jd.com           |
| 18    | reddit.com       |
| 19    | sina.com.cn      |
| 20    | weibo.com        |
| 21    | google.co.jp     |
| 22    | 360.cn           |
| 23    | login.tmall.com  |
| 24    | blogspot.com     |
| 25    | google.co.uk     |
| 26    | linkedin.com     |
| 27    | yandex.ru        |
| 28    | netflix.com      |
| 29    | google.ru        |
| 30    | google.com.br    |
+-------+------------------+
```

We can grab the title, text, HTML source (snippet) and converted plaintext (snippet):

```
0:jdbc:drill:> !set outputformat vertical
0:jdbc:drill:> SELECT
. . . . . . . >   site,
. . . . . . . >   title,. . . . . . . >   substring(contents, 1, 100) AS html_contents,
. . . . . . . >   substring(text_contents, 1, 100) AS text_contents. . . . . . . > FROM. . . . . . . >   (SELECT
. . . . . . . >      site,. . . . . . . >      contents,
. . . . . . . >      soup_html_to_plaintext(contents) AS text_contents,
. . . . . . . >      soup_select_text(contents, 'title')[0] AS title
. . . . . . . >    FROM. . . . . . . >      (SELECT
. . . . . . . >         site,. . . . . . . >         soup_read_html(site, 1000) AS contents
. . . . . . . >       FROM dfs.d.`/top500.csvh`
. . . . . . . >       LIMIT 30
. . . . . . . >      ). . . . . . . >   )
. . . . . . . > WHERE NOT title = ''. . . . . . . > ;
site           google.com
title          Google
html_contents  <!doctype html><html itemscope itemtype="http://schema.org/WebPage" lang="en">
 <head>  <meta conttext_contents  Google Search Images Maps Play YouTube News Gmail Drive More » Web History | Settings | Sign in Adva

site           wikipedia.org
title          Wikipedia
html_contents  <!doctype html>
<html lang="mul" class="no-js">
 <head>
  <meta charset="utf-8">
  <title>Wikipedi
text_contents  Wikipedia Wikipedia The Free Encyclopedia English 5 686 000+ articles 日本語 1 113 000+ 記事 Español 1 43

site           yahoo.com
title          Yahoo
html_contents  <!doctype html>
<html id="atomic" lang="en-US" class="atomic my3columns  l-out Pos-r https fp fp-v2
text_contents  Yahoo Home Mail Tumblr Entertainment Lifestyle Mobile View More Politics Answers Groups Music Tech S

site           qq.com
title          腾讯首页
html_contents  <!doctype html>
<html lang="zh-CN">
 <head>
  <meta content="text/html; charset=gb2312" http-equiv=
text_contents  腾讯首页 WWWQQCOM 网页 搜狗 [退出] QQ邮箱： 未读邮件 漂流瓶 群邮件 文件夹QQ空间： 我的动态 好友动态 我的参与 新闻 图片 军事 财经 证券 理财 视频 热剧 综艺 体育 N

site           taobao.com
title          淘宝网(淘寶網) 美国站
html_contents  <!doctype html>
<html>
 <head>
  <meta name="keywords" content="淘宝,淘寶,淘寶網,掏寶,掏保,網上購物,集運,淘宝全球,taobao
text_contents  淘宝网(淘寶網) 美国站

      {"headerData":[{"__data_default":{"length":1,"url":"//tmall-rmc.alicdn.co

site           twitter.com
title          Twitter. It's what's happening.
html_contents  <!doctype html>
<html lang="en" data-scribe-reduced-action-queue="true">
 <head>
  <meta charset="u
text_contents  Twitter. It's what's happening. We've detected that JavaScript is disabled in your browser. Would yo

site           google.co.in
title          Google
html_contents  <!doctype html>
<html itemscope itemtype="http://schema.org/WebPage" lang="en">
 <head>
  <meta cont
text_contents  Google Search Images Maps Play YouTube News Gmail Drive More » Web History | Settings | Sign in Adva

site           live.com
title          Outlook.com - Microsoft free personal email
html_contents  <!doctype html>
<html>
 <head>
  <meta http-equiv="X-UA-Compatible" content="IE=Edge">
  <meta htt
text_contents  Outlook.com - Microsoft free personal email JavaScript is required to sign in.

site           vk.com
title          VK mobile version
html_contents  <!--?xml version="1.0" encoding="utf-8"?--><!DOCTYPE html PUBLIC "-//WAPFORUM//DTD XHTML Mobile 1.0/
text_contents  VK mobile version Install the VK mobile app Stay in touch on the go with VK mobile. Forgot password?

site           jd.com
title          京东(JD.COM)-正品低价、品质保障、配送及时、轻松购物！
html_contents  <!doctype html>
<html lang="zh-CN">
 <head>
  <meta charset="UTF-8">
  <title>京东(JD.COM)-正品低价、品质保障
text_contents  京东(JD.COM)-正品低价、品质保障、配送及时、轻松购物！ 你好，请登录 免费注册 我的订单 我的京东  ◇ 京东会员 企业采购 客户服务  ◇ 网站导航  ◇ 手机京东 京东 京东,多快好

site           google.co.jp
title          Google
html_contents  <!doctype html>
<html itemscope itemtype="http://schema.org/WebPage" lang="en">
 <head>
  <meta cont
text_contents  Google Search Images Maps Play YouTube News Gmail Drive More » Web History | Settings | Sign in Adva

site           360.cn
title          360官网 - 360安全软件 - 360智能硬件 - 360智能家居 - 360企业服务
html_contents  <!doctype html>
<!--[if lt IE 7 ]><html class=ie6 lang="zh-cn"><![endif]-->
<!--[if IE 7 ]><html cla
text_contents  360官网 - 360安全软件 - 360智能硬件 - 360智能家居 - 360企业服务 网页 360搜索 网页 新闻 问答 视频 图片 中文版 Global 360官网 电脑软件 手机软件 视频

site           blogspot.com
title          Blogger.com - Create a unique and beautiful blog. It’s easy and free.
html_contents  <!doctype html>
<html class="no-js" lang="en">
 <head>
  <title>Blogger.com - Create a unique and b
text_contents  Blogger.com - Create a unique and beautiful blog. It’s easy and free. Skip to content Sign in Create

site           google.co.uk
title          Google
html_contents  <!doctype html>
<html itemscope itemtype="http://schema.org/WebPage" lang="en">
 <head>
  <meta cont
text_contents  Google Search Images Maps Play YouTube News Gmail Drive More » Web History | Settings | Sign in Adva

site           linkedin.com
title          LinkedIn: Log In or Sign Up
html_contents  <!doctype html>
<!--[if lt IE 7]> <html lang="en" class="ie ie6 lte9 lte8 lte7 os-other"> <![endif]-
text_contents  LinkedIn: Log In or Sign Up
 Email Password Forgot password? Trying to sign in? Someone's already us

site           yandex.ru
title          Яндекс
html_contents  <!doctype html>
<html class="i-ua_js_no i-ua_css_standart i-ua_browser_unknown i-ua_browser_desktop
text_contents  Яндекс Огаста Настройка Поставить тему Изменить город Настройки портала Почта Завести почту Войти в

site           netflix.com
title          Netflix - Watch TV Shows Online, Watch Movies Online
html_contents  <!doctype html>
<html>
 <head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
text_contents  Netflix - Watch TV Shows Online, Watch Movies Online Netflix Sign In See what’s next. WATCH ANYWHERE

site           google.ru
title          Google
html_contents  <!doctype html>
<html itemscope itemtype="http://schema.org/WebPage" lang="en">
 <head>
  <meta cont
text_contents  Google Search Images Maps Play YouTube News Gmail Drive More » Web History | Settings | Sign in Adva

site           google.com.br
title          Google
html_contents  <!doctype html>
<html itemscope itemtype="http://schema.org/WebPage" lang="en">
 <head>
  <meta cont
text_contents  Google Search Images Maps Play YouTube News Gmail Drive More » Web History | Settings | Sign in Adva

19 rows selected (30.147 seconds)
```

