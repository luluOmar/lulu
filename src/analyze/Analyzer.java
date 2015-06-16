package analyze;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.ecore.xmi.impl.XMLHandler;
import org.eclipse.emf.ecore.xmi.impl.XMLHelperImpl;
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

		
		xmlAnalyzedEvents = new LinkedList<String>();
		docBuilder = docFactory.newDocumentBuilder();

		receivedEvents = new LinkedList<Document>();
		for (String xmlString : inputStrings) {
			receivedEvents.add(XmlParser.createDocByString(xmlString));
		}
		//Check for CF-1
		for (Document event : receivedEvents) {
			// Detect CF-1
			// A Component has crashed, its state has been set to NOT SUPPORTED
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			if (newValue.equals("NOT_SUPPORTED")) {
				createCF1(event);
			}
		}
		
		//Check for CF-2
		for (Document event : receivedEvents) {
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			if(newValue.contains("failure no ")){
				//parse failure count
				String failureNoString = newValue.substring(newValue.indexOf("failure no ") + 11, newValue.lastIndexOf(")"));
				int failureNo = Integer.parseInt(failureNoString);
				if(failureNo == 6) {
					createCF2(event);
				}
			}
		}
		
		//Check for CF-3
		for (Document event : receivedEvents) {
//		for(int i = 0; i < receivedEvents.size(); i++) {
			String componentID = ""; 
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			String oldValueString = XmlParser.getElementsValue(event, "oldValue", "value");
			String notifierString = XmlParser.getElementsValue(event, "notifier", "value");

			//Find destroyed component
			
			if(newValue.isEmpty() && oldValueString.contains("ShopImpl") && notifierString.contains("ComponentImpl")) {
				componentID = XmlParser.getElementsValue(event, "uid", "value");
			}
			
			if(componentID.isEmpty() == false) {
				//Find shop for component
				String shopId = getUid("uid: " + componentID, "ShopImpl");
				
				//Find ComponentType
				String componentTypeId = getUid("uid: " + componentID, "ComponentTypeImpl");			
				
				//TODO find more relevant information for CF-3
				createCF3(componentID, shopId, componentTypeId);
			}
			
		}
			
		return xmlAnalyzedEvents;

	}
	
	private static void createCF3(String componentId, String shopId, String componentTypeId) {
		System.out.println("Detected CF-3 in " + componentId);
		// Add detected failure to events for planning
		Document xml = docBuilder.newDocument();
		Element baseElement = xml.createElement(BASE_NODE);
		xml.appendChild(baseElement);

		Element newValueNode = xml.createElement("cfType");
		XmlBuilder.addAttribute(xml, newValueNode, "value", "CF-3");
		baseElement.appendChild(newValueNode);

//		Element notifierShop = xml.createElement("shop");
//		XmlBuilder.addAttribute(xml, notifierShop, "value",
//				XmlParser.getElementsValue(event, "shop", "value"));
//		baseElement.appendChild(notifierShop);

		Element shop = xml.createElement("shop");
		XmlBuilder.addAttribute(xml, shop, "value", shopId);
		baseElement.appendChild(shop);
		
		Element componentType = xml.createElement("ComponentType");
		XmlBuilder.addAttribute(xml, componentType, "value", componentTypeId);
		baseElement.appendChild(componentType);
		
		// Create String representation of XML document
		try {
			xmlAnalyzedEvents.add(XmlBuilder.prettyPrint(xml));
			System.out.println(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			System.err.println("Unable to add xml-Event to List.");
			e.printStackTrace();
		}
	}
	
	private static String getUid(String oldValue, String notifierValue) {
		String uid = "";
		for(Document event : receivedEvents) {
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			String oldValueString = XmlParser.getElementsValue(event, "oldValue", "value");
			String notifierString = XmlParser.getElementsValue(event, "notifier", "value");

			//Find destroyed component
			
			if(newValue.isEmpty() && oldValueString.contains(oldValue) && notifierString.contains(notifierValue)) {
				uid = XmlParser.getElementsValue(event, "uid", "value");
//				receivedEvents.remove(event);
				return uid;
			}
		}
		return uid;
	}
	
	private static void createCF1(Document event) {
		System.out.println("Detected CF-1 in "
				+ XmlParser.getElementsValue(event, "uid", "value"));
		// Add detected failure to events for planning
		Document xml = docBuilder.newDocument();
		Element baseElement = xml.createElement(BASE_NODE);
		xml.appendChild(baseElement);

		Element newValueNode = xml.createElement("cfType");
		XmlBuilder.addAttribute(xml, newValueNode, "value", "CF-1");
		baseElement.appendChild(newValueNode);

//		Element notifierShop = xml.createElement("shop");
//		XmlBuilder.addAttribute(xml, notifierShop, "value",
//				XmlParser.getElementsValue(event, "shop", "value"));
//		baseElement.appendChild(notifierShop);

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
	
	private static void createCF3(Document event) {
		Document xml = docBuilder.newDocument();
		Element baseElement = xml.createElement(BASE_NODE);
		xml.appendChild(baseElement);
		
		Element cfNode = xml.createElement("cfType");
		XmlBuilder.addAttribute(xml, cfNode, "value", "CF-3");
		baseElement.appendChild(cfNode);
	
		Element notifierUid = xml.createElement("uid");
		XmlBuilder.addAttribute(xml, notifierUid, "value",
				XmlParser.getElementsValue(event, "uid", "value"));
		baseElement.appendChild(notifierUid);
		
		Element oldValue = xml.createElement("oldValue");
		XmlBuilder.addAttribute(xml, oldValue, "value",
				XmlParser.getElementsValue(event, "oldValue", "value"));
		baseElement.appendChild(oldValue);
		
		Element symptom = xml.createElement("symptom");
		String oldValueString = XmlParser.getElementsValue(event, "oldValue", "value");
		String notifierString = XmlParser.getElementsValue(event, "notifier", "value");
		String symptomString = "";
		if(oldValueString.contains("ComponentTypeImpl") && notifierString.contains("ComponentImpl")) {
			symptomString = "ComponentType";
		} else if(oldValueString.contains("ShopImpl") && notifierString.contains("ComponentImpl")) {
			symptomString = "Shop";
		} else if(oldValueString.contains("ProvidedInterfaceImpl") && notifierString.contains("ConnectorImpl")) {
			symptomString = "ProvidedInterface";
//		} else if(oldValueString.contains("ProvidedInterfaceImpl") && notifierString.contains("ConnectorImpl")) {
//			symptomString = "Provided Interface";
		}
		XmlBuilder.addAttribute(xml, symptom, "value", symptomString);
		baseElement.appendChild(symptom);
		
		try {
			xmlAnalyzedEvents.add(XmlBuilder.prettyPrint(xml));
			System.out.println(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			System.err.println("Unable to add xml-Event to List.");
			e.printStackTrace();
		}
	}
	
	private static void createCF2(Document event) {
		Document xml = docBuilder.newDocument();
		Element baseElement = xml.createElement(BASE_NODE);
		xml.appendChild(baseElement);

		Element newValueNode = xml.createElement("cfType");
		XmlBuilder.addAttribute(xml, newValueNode, "value", "CF-2");
		baseElement.appendChild(newValueNode);

		Element notifierUid = xml.createElement("uid");
		XmlBuilder.addAttribute(xml, notifierUid, "value",
				XmlParser.getElementsValue(event, "uid", "value"));
		baseElement.appendChild(notifierUid);
		try {
			xmlAnalyzedEvents.add(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			System.err.println("Unable to add xml-Event to List.");
			e.printStackTrace();
		}
	}
}
