package hrbrmstr.drill.udf;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.IntHolder;

//import org.jsoup.HttpStatusException;
//import org.jsoup.Jsoup;
//import java.net.MalformedURLException;

import javax.inject.Inject;

public class SoupGetHTMLFunctions {
  
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SoupGetHTMLFunctions.class);
  
  private SoupGetHTMLFunctions() {} 
  
  /*
  * Fetch HTML from a web site (requires network reachability)
  */
  
  @SuppressWarnings( "deprecation" )
  @FunctionTemplate(
    names = { "soup_read_html" },
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupGetHTMLTimeout implements DrillSimpleFunc {
    
    @Param VarCharHolder input;
    @Param(constant = true) IntHolder timeout;
    @Output VarCharHolder out;
    
    @Inject DrillBuf buffer;
    
    public void setup() {}
    
    public void eval() {
      
      String url_string = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
        input.start, input.end, input.buffer
      );
      
      try {
        
        if (url_string.indexOf("http") != 0) url_string = "http://" + url_string;
        
        org.jsoup.Connection con = org.jsoup.Jsoup.connect(url_string);
        
        con.userAgent("drill-html-tools/1.0.0; https://github.com/hrbrmstr/drill-html-tools");
        con.method(org.jsoup.Connection.Method.GET);
        con.validateTLSCertificates(false);
        con.timeout(timeout.value);
        
        org.jsoup.Connection.Response res = con.execute();
        org.jsoup.nodes.Document doc = res.parse();
        
        String outHtml = doc.outerHtml();
        
        if (outHtml == null) outHtml = "";
        
        byte[] outBytes = outHtml.getBytes();
        
        out.start = 0;
        out.end = outBytes.length;
        out.buffer = buffer = buffer.reallocIfNeeded(out.end );
        out.buffer.setBytes(0, outBytes, 0, out.end);
        
      } catch(Exception e) {
      }
      
    }
    
  }
  
  @SuppressWarnings( "deprecation" )
  @FunctionTemplate(
    names = { "soup_read_html" },
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupGetHTMLTimeoutWithMetadata implements DrillSimpleFunc {
    
    @Param VarCharHolder input;
    @Param(constant = true) IntHolder timeout;
    @Param(constant = true) BitHolder include_metadata;
    
    @Output BaseWriter.ComplexWriter out;
    
    @Inject DrillBuf buffer;
    
    public void setup() {}
    
    public void eval() {
      
      String url_string = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
         input.start, input.end, input.buffer
      );
      
      org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter mw = out.rootAsMap();
      
      try {
        
        org.apache.drill.exec.expr.holders.VarCharHolder row = new org.apache.drill.exec.expr.holders.VarCharHolder();
        
        if (url_string.indexOf("http") != 0) url_string = "http://" + url_string;
        
        org.jsoup.Connection con = org.jsoup.Jsoup.connect(url_string);
        
        con.userAgent("drill-html-tools/1.0.0; https://github.com/hrbrmstr/drill-html-tools");
        con.method(org.jsoup.Connection.Method.GET);
        con.validateTLSCertificates(false);
        con.timeout(timeout.value);
        
        org.jsoup.Connection.Response res = con.execute();
        
        mw.start();
        
        if (include_metadata.value == 1) {
          
          // get response status code
          mw.integer("status_code").writeInt(res.statusCode());
          
          // get response status message
          String msg = res.statusMessage();
          if (msg == null) msg = "";
          
          byte[] msgBytes = msg.getBytes();
          buffer.reallocIfNeeded(msgBytes.length); 
          buffer.setBytes(0, msgBytes);
          row.start = 0; 
          row.end = msgBytes.length; 
          row.buffer = buffer;
          mw.varChar("status_message").write(row);
          
          // get the response content type
          String cType = res.contentType();
          if (cType == null) cType = "";
          
          byte[] ctypeBytes = cType.getBytes();
          buffer.reallocIfNeeded(ctypeBytes.length); 
          buffer.setBytes(0, ctypeBytes);
          row.start = 0; 
          row.end = ctypeBytes.length; 
          row.buffer = buffer;
          mw.varChar("content_type").write(row);
          
          java.util.Map<String, String> hdrs = res.headers();
          
          org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter lw = mw.list("headers");
          lw.startList();
          
          for(java.util.Map.Entry<String,String> entry : hdrs.entrySet()) {
            
            String key = entry.getKey().toString();
            String val = entry.getValue().toString();
            
            if (!((key == null) || (val == null))) {
              
              org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter hw = lw.map();
              org.apache.drill.exec.expr.holders.VarCharHolder hrow = new org.apache.drill.exec.expr.holders.VarCharHolder();
              
              hw.start();
              
              byte[] keyBytes = key.getBytes();
              buffer.reallocIfNeeded(keyBytes.length); 
              buffer.setBytes(0, keyBytes);
              hrow.start = 0; 
              hrow.end = keyBytes.length; 
              hrow.buffer = buffer;
              hw.varChar("key").write(hrow);
              
              byte[] valBytes = val.getBytes();
              buffer.reallocIfNeeded(valBytes.length); 
              buffer.setBytes(0, valBytes);
              hrow.start = 0; 
              hrow.end = valBytes.length; 
              hrow.buffer = buffer;
              hw.varChar("value").write(hrow);
              
              hw.end();
              
            }
            
          }
          
          lw.endList();
          
        }
        //parse the document
        org.jsoup.nodes.Document doc = res.parse();
        
        // get HTML content
        String outHtml = doc.outerHtml();        
        if (outHtml == null) outHtml = "";
        
        byte[] htmlBytes = outHtml.getBytes();
        buffer.reallocIfNeeded(htmlBytes.length); 
        buffer.setBytes(0, htmlBytes);
        row.start = 0; 
        row.end = htmlBytes.length; 
        row.buffer = buffer;
        mw.varChar("html").write(row);
        
        mw.end();
        
      } catch(Exception e) {
      }
      
    }
    
  }
  
  @FunctionTemplate(
    names = { "soup_html_to_plaintext" },
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupHTMLToPlainText implements DrillSimpleFunc {
    
    @Param VarCharHolder input;
    @Output VarCharHolder out;
    
    @Inject DrillBuf buffer;
    
    public void setup() {}
    
    public void eval() {
      
      String content = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
        input.start, input.end, input.buffer
      );
      
      try {
        
        String txt = null;
        
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);
        
        if (doc != null) txt = doc.text();
        if (txt == null) txt = "";
        
        byte[] outBytes = txt.getBytes();
        
        out.start = 0;
        out.end = outBytes.length;
        out.buffer = buffer = buffer.reallocIfNeeded(out.end);
        out.buffer.setBytes(0, outBytes, 0, out.end);
        
      } catch(java.lang.Exception  e) {
      }
      
    }
    
  }
  
}