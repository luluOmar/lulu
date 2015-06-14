package plan;

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

public final class Planer {

	private static final String BASE_NODE = "plannedAdaption";

	public static List<String> planAdaption(List<String> xmlAnalyzedEvents)
			throws ParserConfigurationException, SAXException, IOException {

		// Prepare XML document for adaption strategy
		List<String> xmlPlannedAdaptions = new LinkedList<String>();
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// Get document out of analyzed events XML
		List<Document> analyzedEvents = new LinkedList<Document>();
		for (String xmlString : xmlAnalyzedEvents) {
			analyzedEvents.add(XmlParser.createDocByString(xmlString));
		}

		// Plan adaption strategy for critical failures
		for (Document event : analyzedEvents) {

			String cfType = XmlParser
					.getElementsValue(event, "cfType", "value");

			if (cfType.equals("CF-1")) {

				System.out.println("Plan adaption for CF-1 in "
						+ XmlParser.getElementsValue(event, "uid", "value"));

				Document xml = docBuilder.newDocument();
				Element baseElement = xml.createElement(BASE_NODE);
				xml.appendChild(baseElement);

				Element notifierShop = xml.createElement("shop");
				XmlBuilder.addAttribute(xml, notifierShop, "value",
						XmlParser.getElementsValue(event, "shop", "value"));
				baseElement.appendChild(notifierShop);

				Element notifierUid = xml.createElement("uid");
				XmlBuilder.addAttribute(xml, notifierUid, "value",
						XmlParser.getElementsValue(event, "uid", "value"));
				baseElement.appendChild(notifierUid);

				// Add action for adaption strategy
				Element actionElement = xml.createElement("action");
				baseElement.appendChild(actionElement);
				
				Element actionName = xml.createElement("actionName");
				XmlBuilder.addAttribute(xml, actionName, "value", "setState");
				actionElement.appendChild(actionName);
				
				Element actionValue = xml.createElement("actionValue");
				XmlBuilder.addAttribute(xml, actionValue, "value", "UNDEPLOYED");
				actionElement.appendChild(actionValue);
				
				actionElement = xml.createElement("action");
				baseElement.appendChild(actionElement);
				
				actionName = xml.createElement("actionName");
				XmlBuilder.addAttribute(xml, actionName, "value", "setState");
				actionElement.appendChild(actionName);
				
				actionValue = xml.createElement("actionValue");
				XmlBuilder.addAttribute(xml, actionValue, "value", "DEPLOYED");
				actionElement.appendChild(actionValue);
				
				actionElement = xml.createElement("action");
				baseElement.appendChild(actionElement);
				
				actionName = xml.createElement("actionName");
				XmlBuilder.addAttribute(xml, actionName, "value", "setState");
				actionElement.appendChild(actionName);
				
				actionValue = xml.createElement("actionValue");
				XmlBuilder.addAttribute(xml, actionValue, "value", "STARTED");
				actionElement.appendChild(actionValue);
				
//				actionNode = xml.createElement("setState");
//				XmlBuilder.addAttribute(xml, actionNode, "value", "DEPLOYED");
//				actionElement.appendChild(actionNode);
//				
//				actionNode = xml.createElement("setState");
//				XmlBuilder.addAttribute(xml, actionNode, "value", "START");
//				actionElement.appendChild(actionNode);

				// Create String representation of XML document
				try {
					xmlPlannedAdaptions.add(XmlBuilder.prettyPrint(xml));
				} catch (Exception e) {
					System.err.println("Unable to add xml-Event to List.");
					e.printStackTrace();
				}
			}
		}
		return xmlPlannedAdaptions;
	}
}
