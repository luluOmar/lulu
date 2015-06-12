package util;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlBuilder {
	
	public static void addAttribute(Document doc, Element e, String attributeName, Object value) {
		Attr att = doc.createAttribute(attributeName);
		if(value != null) {
			att.setValue(value.toString());			
		}
		e.setAttributeNode(att);
	}
	
	public static final String prettyPrint(Document xml) throws Exception {
 	    Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(xml), new StreamResult(out));
        return out.toString();
    }

}
