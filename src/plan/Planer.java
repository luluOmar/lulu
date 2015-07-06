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

import analyze.Analyzer.CFType;
import analyze.Analyzer.PIType;
import util.XmlBuilder;
import util.XmlParser;

public final class Planer {

	private static final String BASE_NODE = "plannedAdaption";

	public static List<String> planAdaption(List<String> xmlAnalyzedEvents)
			throws ParserConfigurationException, SAXException, IOException {

		// Prepare XML document for adaption strategy
		List<String> xmlPlannedAdaptions = new LinkedList<String>();
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// Get document out of analyzed events XML
		List<Document> analyzedEvents = new LinkedList<Document>();
		for (String xmlString : xmlAnalyzedEvents) {
			System.out.println("Planner received event:" + xmlString);
			analyzedEvents.add(XmlParser.createDocByString(xmlString));
		}

		// Plan adaption strategy for critical failures
		for (Document event : analyzedEvents) {

			String cfType = XmlParser.getElementsValue(event, "cfType", "value");
			CFType failureType = CFType.NONE;
			if (cfType != null) {
				failureType = CFType.byName(cfType);
			}

			String piType = XmlParser.getElementsValue(event, "piType", "value");
			PIType performanceIssueType = PIType.NONE;
			if (piType != null) {
				performanceIssueType = PIType.byName(piType);
			}

			Document xml = docBuilder.newDocument();
			Element baseElement = xml.createElement(BASE_NODE);
			xml.appendChild(baseElement);

			// Element notifierShop = xml.createElement("shop");
			// XmlBuilder.addAttribute(xml, notifierShop, "value",
			// XmlParser.getElementsValue(event, "shop", "value"));
			// baseElement.appendChild(notifierShop);
			String typeString = failureType.toString();
			if (piType != null) {
				typeString = performanceIssueType.toString();
			}
			Element type = xml.createElement("type");
			XmlBuilder.addAttribute(xml, type, "value", typeString);
			baseElement.appendChild(type);

			switch (failureType) {
			case CF1:
				System.out.println("Plan adaption for CF-1 in " + XmlParser.getElementsValue(event, "uid", "value"));
				// Add action for adaption strategy
				buildCF1Plan(xml, baseElement, XmlParser.getElementsValue(event, "uid", "value"));
				break;
			case CF2:
				System.out.println("Plan adaption for CF-2 in " + XmlParser.getElementsValue(event, "uid", "value"));
				// Add action for adaption strategy
				buildCF2Plan(xml, baseElement, XmlParser.getElementsValue(event, "uid", "value"));
				break;
			case CF3:

				buildCF3Plan(xml, baseElement, event);
				break;
			case CF4:
				buildCF4Plan(xml, baseElement, event);
				break;
			case NONE:
			default:
				break;
			}

			switch (performanceIssueType) {
			case PI1:
				buildPI1Plan(xml, baseElement, event);
				System.out.println("received a PI-1");
				break;
			case PI2:
				buildPI2Plan(xml, baseElement, event);
				break;
			case PI3:
				buildPI3Plan(xml, baseElement, event);
				break;
			case NONE:
				break;
			default:
				break;
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
		XmlBuilder.addAttribute(xml, actionValue, "value", XmlParser.getElementsValue(event, "componentType", "value"));
		actionElement.appendChild(actionValue);

		actionElement = xml.createElement("action");
		baseElement.appendChild(actionElement);

		actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "addToShop");
		actionElement.appendChild(actionName);

		actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", XmlParser.getElementsValue(event, "shop", "value"));
		actionElement.appendChild(actionValue);

		// TODO add more actions
		try {
			System.out.println(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xml;
	}

	// TODO: Plan + executer
	private static Document buildCF4Plan(Document xml, Element baseElem, Document event) {
		Element actionElement = xml.createElement("action");
		baseElem.appendChild(actionElement);

		Element actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "lookup alternative components");
		actionElement.appendChild(actionName);
		Element actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", XmlParser.getElementsValue(event, "componentType", "value"));
		actionElement.appendChild(actionValue);

		actionElement = xml.createElement("action");
		baseElem.appendChild(actionElement);
		
		actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "setState");
		actionElement.appendChild(actionName);

		actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", "DEPLOYED");
		actionElement.appendChild(actionValue);

		actionElement = xml.createElement("action");
		baseElem.appendChild(actionElement);

		actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "addToShop");
		actionElement.appendChild(actionName);

		actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", XmlParser.getElementsValue(event, "shop", "value"));
		actionElement.appendChild(actionValue);

		// TODO add more actions
		try {
			System.out.println(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xml;
	}

	private static Document buildPI1Plan(Document xml, Element baseElement, Document event) {

		return xml;
	}

	private static Document buildPI3Plan(Document xml, Element baseElement, Document event) {
		// TODO Auto-generated method stub
		return xml;
	}

	private static Document buildPI2Plan(Document xml, Element baseElement, Document event) {
		// TODO Auto-generated method stub
		return xml;
	}
}
