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

			Document xml = docBuilder.newDocument();
			Element baseElement = xml.createElement(BASE_NODE);
			xml.appendChild(baseElement);

//			Element notifierShop = xml.createElement("shop");
//			XmlBuilder.addAttribute(xml, notifierShop, "value",
//					XmlParser.getElementsValue(event, "shop", "value"));
//			baseElement.appendChild(notifierShop);
			
			if (cfType.equals("CF-1")) {
				
				Element type = xml.createElement("type");
				XmlBuilder.addAttribute(xml, type, "value", cfType);
				baseElement.appendChild(type);			
				
				System.out.println("Plan adaption for CF-1 in "
						+ XmlParser.getElementsValue(event, "uid", "value"));
				// Add action for adaption strategy
				buildCF1Plan(xml, baseElement, XmlParser.getElementsValue(event, "uid", "value"));
			} else if(cfType.equals("CF-2")) {
				
				Element type = xml.createElement("type");
				XmlBuilder.addAttribute(xml, type, "value", cfType);
				baseElement.appendChild(type);			
				
				System.out.println("Plan adaption for CF-2 in "
						+ XmlParser.getElementsValue(event, "uid", "value"));
				// Add action for adaption strategy
				buildCF2Plan(xml, baseElement, 
						XmlParser.getElementsValue(event, "uid", "value"));
			} else if(cfType.equals("CF-3")) {
				Element type = xml.createElement("type");
				XmlBuilder.addAttribute(xml, type, "value", cfType);
				baseElement.appendChild(type);			
				buildCF3Plan(xml, baseElement, event);
			}

			// Create String representation of XML document
			try {
				xmlPlannedAdaptions.add(XmlBuilder.prettyPrint(xml));
			} catch (Exception e) {
				System.err.println("Unable to add xml-Event to List.");
				e.printStackTrace();
			}
		}
		return xmlPlannedAdaptions;
	}
	
	private static Document buildCF1Plan(Document xml, Element baseElement, String uid) {
		
		Element actionElement = xml.createElement("action");
		baseElement.appendChild(actionElement);
		
		Element actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "findComponent");
		actionElement.appendChild(actionName);
		
		Element actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", uid);
		actionElement.appendChild(actionValue);
		
		actionElement = xml.createElement("action");
		baseElement.appendChild(actionElement);
		
		actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "setState");
		actionElement.appendChild(actionName);
		
		actionValue = xml.createElement("actionValue");
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
		
		return xml;
	}

	private static Document buildCF2Plan(Document xml, Element baseElement, String uid) {
		
		Element actionElement = xml.createElement("action");
		baseElement.appendChild(actionElement);
		
		Element actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "findComponent");
		actionElement.appendChild(actionName);
		
		Element actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", uid);
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
		
		return xml;
	}

	private static Document buildCF3Plan(Document xml, Element baseElement, Document event) {
		Element actionElement = xml.createElement("action");
		baseElement.appendChild(actionElement);
		
		Element actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "instantiate and deploy");
		actionElement.appendChild(actionName);
		
		Element actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", 
				XmlParser.getElementsValue(event, "ComponentType", "value"));
		actionElement.appendChild(actionValue);
		
		actionElement = xml.createElement("action");
		baseElement.appendChild(actionElement);
		
		actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "addToShop");
		actionElement.appendChild(actionName);
		
		actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", 
				XmlParser.getElementsValue(event, "shop", "value"));
		actionElement.appendChild(actionValue);
		
		//TODO add more actions
		try {
			System.out.println(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xml;
	}
	
}
