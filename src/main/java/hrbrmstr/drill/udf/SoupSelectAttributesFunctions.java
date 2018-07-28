package hrbrmstr.drill.udf;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
// import org.jsoup.Jsoup;
// import org.jsoup.helper.Validate;
// import org.jsoup.nodes.Document;
// import org.jsoup.nodes.Element;
// import org.jsoup.select.Elements;

import javax.inject.Inject;

public class SoupSelectAttributesFunctions {
  
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SoupSelectAttributesFunctions.class);
  
  private SoupSelectAttributesFunctions() {} 
  
  /*
  * HTML selection & extraction of attributes
  */
  
  @FunctionTemplate(
  names = { "soup_select_attr" },
  scope = FunctionTemplate.FunctionScope.SIMPLE,
  nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
  public static class SoupSelectAttr implements DrillSimpleFunc {
    
    @Param VarCharHolder input;
    @Param(constant = true) VarCharHolder nodeField;
    @Param(constant = true) VarCharHolder attrField;
    
    @Output BaseWriter.ComplexWriter out;
    
    @Inject DrillBuf buffer;
    
    public void setup() {}
    
    public void eval() {
      
      org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter lw = out.rootAsList();
      
      String htmlInput = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
         input.start, input.end, input.buffer
      );
      
      String nodeValue = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
        nodeField.start, nodeField.end, nodeField.buffer
      );
      
      String attrValue = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
        attrField.start, attrField.end, attrField.buffer
      );
      
      lw.startList();
      
      try {
        
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(htmlInput);
        if (doc != null) {
          org.jsoup.select.Elements els = doc.select(nodeValue);
          if (els != null) {
            for (org.jsoup.nodes.Element el : els) {
              org.jsoup.nodes.Attributes attrs = el.attributes();
              if (attrs != null) {
                String attr = attrs.getIgnoreCase(attrValue);
                if (!((attr == null) || attr == "")) {
                  byte[] outBytes = attr.getBytes();
                  buffer.reallocIfNeeded((outBytes.length > 0) ? outBytes.length : 0); 
                  buffer.setBytes(0, outBytes);
                  lw.varChar().writeVarChar(0, (outBytes.length > 0) ? outBytes.length : 0, buffer); 
                }
              }
            }
          }
        }

      } catch(java.lang.Exception e) {
      }
      
      lw.endList();
      
    }
    
  }
  
}