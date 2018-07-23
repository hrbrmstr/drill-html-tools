package hrbrmstr.drill.udf;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.inject.Inject;

public class SoupSelectTextFunctions {
  
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SoupSelectTextFunctions.class);
  
  private SoupSelectTextFunctions() {} 
  
  /*
  * HTML selection & text extraction function implementation.
  */
  
  @FunctionTemplate(
    names = { "soup_select_text" },
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupSelectText implements DrillSimpleFunc {
    
    @Param NullableVarCharHolder input;
    @Param(constant = true) VarCharHolder field;
    @Param(constant = true) BitHolder include_children;
    
    @Output BaseWriter.ComplexWriter out;
    
    @Inject DrillBuf buffer;
    
    public void setup() {}
    
    public void eval() {
      
      org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter mw = out.rootAsList();
      
      String html_string = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
        input.start, input.end, input.buffer
      );
      
      String fieldValue = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
         field.start, field.end, field.buffer
      );
      
      mw.startList();
      
      if (html_string != null) {
        
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html_string);
        
        if (doc != null) {
          
          org.jsoup.select.Elements els = doc.select(fieldValue);
          
          if (els != null) {
            
            for (org.jsoup.nodes.Element el : els) {
              
              if (el != null) {
                
                String el_txt = (include_children.value == 1) ? el.text() : el.ownText();
                
                if (el_txt != null) {
                  
                  byte[] outBytes = el_txt.getBytes();
                  
                  buffer.reallocIfNeeded((outBytes.length) > 0 ? outBytes.length : 0); 
                  buffer.setBytes(0, outBytes);
                  
                  mw.varChar().writeVarChar(0, (outBytes.length > 0) ? outBytes.length : 0, buffer); 
                  
                }
                
              }
              
            }
            
          }
          
        }
        
      }
      
      mw.endList();
      
    }
    
  }
  
  // optional boolean parameter not specified to defaults to true
  
  @FunctionTemplate(
    names = { "soup_select_text" },
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupSelectTextInclude implements DrillSimpleFunc {
    
    @Param NullableVarCharHolder input;
    @Param(constant = true) VarCharHolder field;
    
    @Output BaseWriter.ComplexWriter out;
    
    @Inject DrillBuf buffer;
    
    public void setup() {}
    
    public void eval() {
      
      org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter lw = out.rootAsList();
      
      String html_string = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
        input.start, input.end, input.buffer
      );
      
      String fieldValue = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
        field.start, field.end, field.buffer
      );
      
      lw.startList();
      
      org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html_string);
      
      if (doc != null) {
        org.jsoup.select.Elements els = doc.select(fieldValue);
        
        if (els != null) {
          
          for (org.jsoup.nodes.Element el : els) {
            
            if (el != null) {
              
              String el_text = el.text();
              
              if (el_text != null) {
                
                byte[] outBytes = el_text.getBytes() ;
                
                buffer.reallocIfNeeded((outBytes.length > 0) ? outBytes.length : 0); 
                buffer.setBytes(0, outBytes);
                
                lw.varChar().writeVarChar(0, (outBytes.length > 0) ? outBytes.length : 0, buffer); 
                
              }
            }
          }
        }
      }
      
      lw.endList();
      
    }
    
  }
  
}