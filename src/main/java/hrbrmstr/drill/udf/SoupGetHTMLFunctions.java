package hrbrmstr.drill.udf;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import java.net.MalformedURLException;

import javax.inject.Inject;

public class SoupGetHTMLFunctions {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SoupGetHTMLFunctions.class);

  private SoupGetHTMLFunctions() {} 

  /*
   * Fetch HTML from a web site (requires network reachability)
   */
  
  @FunctionTemplate(
    names = { "soup_read_html" },
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupGetHTMLTimeout implements DrillSimpleFunc {
    
    @Param NullableVarCharHolder input;
    @Param IntHolder timeout;
    @Output VarCharHolder out;

    @Inject DrillBuf buffer;

    public void setup() {}
    
    public void eval() {

      String url_string = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
          input.start, input.end, input.buffer
      );

      byte[] outBytes = null;

      try {

        org.jsoup.Connection con = org.jsoup.Jsoup.connect(url_string);

        con.userAgent("drill-html-tools/1.0.0; https://github.com/hrbrmstr/drill-html-tools");
        con.method(org.jsoup.Connection.Method.GET);
        con.validateTLSCertificates(false);
        con.timeout(timeout.value);

        org.jsoup.Connection.Response res = con.execute();
        org.jsoup.nodes.Document doc = res.parse();

        outBytes = doc.outerHtml().getBytes();
  
      } catch(java.net.MalformedURLException e ) { // handle malformed URLs gracefully
        outBytes = ("SOUP_ERROR: " + e.getMessage()).getBytes();
      } catch(org.jsoup.HttpStatusException e) { // response was not OK
        outBytes = ("SOUP_ERROR: " + e.getMessage()).getBytes();
      } catch(org.jsoup.UnsupportedMimeTypeException e) { // unsupported MIME type
        outBytes = ("SOUP_ERROR: " + e.getMessage()).getBytes();
      } catch(java.net.SocketTimeoutException e) { // connection timeout
        outBytes = ("SOUP_ERROR: " + e.getMessage()).getBytes();
      } catch(java.io.IOException  e) { // general connection or read errors
        outBytes = ("SOUP_ERROR: " + e.getMessage()).getBytes();
      }

      out.start = 0;
      out.end = (outBytes == null) ? 0 : outBytes.length;
      out.buffer = buffer = buffer.reallocIfNeeded(out.end );
      out.buffer.setBytes(0, outBytes, 0, out.end);

    }
    
  }

  
  @FunctionTemplate(
    names = { "soup_html_to_plaintext" },
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupHTMLToPlainText implements DrillSimpleFunc {
    
    @Param NullableVarCharHolder input;
    @Output VarCharHolder out;

    @Inject DrillBuf buffer;

    public void setup() {}
    
    public void eval() {

      String content = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
          input.start, input.end, input.buffer
      );

      byte[] outBytes = null;

      try {
        if (content != null) {
          org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);
          if (doc != null) {
            String txt = doc.text();
            if (txt != null) outBytes = txt.getBytes();
          }
        }
  
      } catch(java.lang.Exception  e) { // general connection or read errors
        outBytes = ("SOUP_ERROR: " + e.getMessage()).getBytes();
      }

      out.start = 0;
      out.end = (outBytes == null) ? 0 : outBytes.length;
      out.buffer = buffer = buffer.reallocIfNeeded(out.end );
      out.buffer.setBytes(0, outBytes, 0, out.end);

    }
    
  }


}