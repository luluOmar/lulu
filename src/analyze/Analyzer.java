package analyze;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import util.XmlBuilder;
import util.XmlParser;

public final class Analyzer {

	private static LinkedList<Document> receivedEvents = new LinkedList<Document>();
	private static LinkedList<String> xmlAnalyzedEvents = new LinkedList<String>();
	private static final String BASE_NODE = "analyzedEvent";
	private static DocumentBuilderFactory docFactory = DocumentBuilderFactory
			.newInstance();
	private static DocumentBuilder docBuilder;

	public static List<String> activate(List<String> inputStrings)
			throws ParserConfigurationException, SAXException, IOException {

		docBuilder = docFactory.newDocumentBuilder();

		receivedEvents = new LinkedList<Document>();
		for (String xmlString : inputStrings) {
			receivedEvents.add(XmlParser.createDocByString(xmlString));
		}

		for (Document event : receivedEvents) {
			// Detect CF-1
			// A Component has crashed, its state has been set to NOT SUPPORTED
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			if (newValue.equals("NOT_SUPPORTED")) {
				System.out.println("Detected CF-1 in "
						+ XmlParser.getElementsValue(event, "uid", "value"));
				// Add detected failure to events for planning
				Document xml = docBuilder.newDocument();
				Element baseElement = xml.createElement(BASE_NODE);
				xml.appendChild(baseElement);

				Element newValueNode = xml.createElement("cfType");
				XmlBuilder.addAttribute(xml, newValueNode, "value", "CF-1");
				baseElement.appendChild(newValueNode);

				Element notifierShop = xml.createElement("shop");
				XmlBuilder.addAttribute(xml, notifierShop, "value",
						XmlParser.getElementsValue(event, "shop", "value"));
				baseElement.appendChild(notifierShop);

				Element notifierUid = xml.createElement("uid");
				XmlBuilder.addAttribute(xml, notifierUid, "value",
						XmlParser.getElementsValue(event, "uid", "value"));
				baseElement.appendChild(notifierUid);
				
				// Create String representation of XML document
				try {
					xmlAnalyzedEvents.add(XmlBuilder.prettyPrint(xml));
				} catch (Exception e) {
					System.err.println("Unable to add xml-Event to List.");
					e.printStackTrace();
				}
			}
		}

		return xmlAnalyzedEvents;

	}
}
