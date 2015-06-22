package util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParser {

	public static Document createDocByString(String xmlString)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder db = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(xmlString));
		return db.parse(is);
	}

	public static String getElementsValue(Document xml, String element,
			String attributeName) {
		String uid = null;
		NodeList elementNodes = xml.getElementsByTagName(element);
		if (elementNodes != null) {
			Element node = (Element) elementNodes.item(0);
			uid = node.getAttribute(attributeName);
		}
		return uid;
	}

}
