package analyze;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mrubis_simulator.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import util.XmlBuilder;
import util.XmlParser;

public final class Analyzer {

	public enum CFType {
		CF1("CF-1"), CF2("CF-2"), CF3("CF-3"), CF4("CF-4");

		private String name;

		private CFType(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public static CFType byName(String value) {
			for (CFType type : CFType.values()) {
				if (type.name.equalsIgnoreCase(value))
					return type;
			}
			throw new IllegalArgumentException("Unknown failure type name!");
		}
	}

	private static LinkedList<Document> receivedEvents = new LinkedList<Document>();
	private static LinkedList<String> xmlAnalyzedEvents = new LinkedList<String>();
	private static final String BASE_NODE = "analyzedEvent";
	private static DocumentBuilderFactory docFactory = DocumentBuilderFactory
			.newInstance();
	private static DocumentBuilder docBuilder;
	private static CriticalFailureBuilder cfBuilder = new CriticalFailureBuilder();

	private static Map<String, String> failureCount = new HashMap<String, String>();

	public static List<String> activate(List<String> inputStrings,
			Queue notificationHistory) throws Exception {

		xmlAnalyzedEvents = new LinkedList<String>();
		docBuilder = docFactory.newDocumentBuilder();

		receivedEvents = new LinkedList<Document>();
		for (String xmlString : inputStrings) {
			receivedEvents.add(XmlParser.createDocByString(xmlString));
		}

		shiftHistory();

		// Check for CF-1
		for (Document event : receivedEvents) {
			// Detect CF-1
			// A Component has crashed, its state has been set to NOT
			// SUPPORTED
			// System.out.println("XML:" + XmlBuilder.prettyPrint(event));
			String criticalFailureEvent = null;
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			String uid = XmlParser.getElementsValue(event, "uid", "value");
			if (newValue.equals("NOT_SUPPORTED")) {
				if (isClassifiedAsCf4(event, uid)) {
					continue;
				}
				criticalFailureEvent = createCF(CFType.CF1, uid);

			} else if (newValue.contains("failure no ")) {
				// parse failure count
				String failureNoString = newValue.substring(
						newValue.indexOf("failure no ") + 11,
						newValue.lastIndexOf(")"));
				int failureNo = Integer.parseInt(failureNoString);
				if (failureNo == 6) {
					if (isClassifiedAsCf4(event, uid)) {
						continue;
					}
					criticalFailureEvent = createCF(CFType.CF2, uid);
				}
			}

			if (criticalFailureEvent != null) {
				xmlAnalyzedEvents.add(criticalFailureEvent);
			}
		}

		// Check for CF-3
		for (Document event : receivedEvents) {
			// for(int i = 0; i < receivedEvents.size(); i++) {
			String componentID = "";
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			String oldValueString = XmlParser.getElementsValue(event,
					"oldValue", "value");
			String notifierString = XmlParser.getElementsValue(event,
					"notifier", "value");

			// Find destroyed component

			if (newValue.isEmpty() && oldValueString.contains("ShopImpl")
					&& notifierString.contains("ComponentImpl")) {
				componentID = XmlParser.getElementsValue(event, "uid", "value");
			}

			if (componentID.isEmpty() == false) {
				// Find shop for component
				String shopId = getUid("uid: " + componentID, "ShopImpl");

				// Find ComponentType
				String componentTypeId = getUid("uid: " + componentID,
						"ComponentTypeImpl");

				// TODO find more relevant information for CF-3
				createCF3(componentID, shopId, componentTypeId);
			}
		}

		return xmlAnalyzedEvents;

	}

	private static void shiftHistory() {
		for (Entry<String, String> e : failureCount.entrySet()) {
			failureCount.put(e.getKey(), e.getValue().substring(1, 3) + "0");
		}
	}

	private static void createCF3(String componentId, String shopId,
			String componentTypeId) {
		System.out.println("Detected CF-3 in " + componentId);
		// Add detected failure to events for planning
		Document xml = docBuilder.newDocument();
		Element baseElement = xml.createElement(BASE_NODE);
		xml.appendChild(baseElement);

		Element newValueNode = xml.createElement("cfType");
		XmlBuilder.addAttribute(xml, newValueNode, "value", "CF-3");
		baseElement.appendChild(newValueNode);

		// Element notifierShop = xml.createElement("shop");
		// XmlBuilder.addAttribute(xml, notifierShop, "value",
		// XmlParser.getElementsValue(event, "shop", "value"));
		// baseElement.appendChild(notifierShop);

		Element shop = xml.createElement("shop");
		XmlBuilder.addAttribute(xml, shop, "value", shopId);
		baseElement.appendChild(shop);

		Element componentType = xml.createElement("ComponentType");
		XmlBuilder.addAttribute(xml, componentType, "value", componentTypeId);
		baseElement.appendChild(componentType);

		// Create String representation of XML document
		try {
			xmlAnalyzedEvents.add(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			System.err.println("Unable to add xml-Event to List.");
			e.printStackTrace();
		}
	}

	private static String getUid(String oldValue, String notifierValue) {
		String uid = "";
		for (Document event : receivedEvents) {
			String newValue = XmlParser.getElementsValue(event, "newValue",
					"value");
			String oldValueString = XmlParser.getElementsValue(event,
					"oldValue", "value");
			String notifierString = XmlParser.getElementsValue(event,
					"notifier", "value");

			// Find destroyed component

			if (newValue.isEmpty() && oldValueString.contains(oldValue)
					&& notifierString.contains(notifierValue)) {
				uid = XmlParser.getElementsValue(event, "uid", "value");
				// receivedEvents.remove(event);
				return uid;
			}
		}
		return uid;
	}

	private static String createCF(CFType cfType, String uid) throws Exception {
		return cfBuilder.setCfType(cfType).setUid(uid).build();
	}

	private static boolean isClassifiedAsCf4(Document event, String uid)
			throws Exception {
		System.out.println("Check for cf4:" + event);
		String runMatches = "001";
		if (failureCount.containsKey(uid)) {
			runMatches = failureCount.get(uid).substring(0, 2).concat("1");
			System.err.println("Run matches: " + runMatches);
			if (runMatches.equals("111")) {
				createCF(CFType.CF4, uid);
				failureCount.remove(uid);
				return true;
			}
		}
		failureCount.put(uid, runMatches);
		return false;
	}

	private static class CriticalFailureBuilder {

		private CFType cfType;
		private String uid;

		private CriticalFailureBuilder setCfType(CFType cfType) {
			this.cfType = cfType;
			return this;
		}

		public CriticalFailureBuilder setUid(String uid) {
			this.uid = uid;
			return this;
		}

		private String build() throws Exception {
			Document xml = docBuilder.newDocument();
			Element baseElement = xml.createElement(BASE_NODE);
			xml.appendChild(baseElement);
			baseElement.appendChild(addUid(xml));
			if (cfType != null) {
				baseElement.appendChild(addCfType(xml));
			}
			reset();
			return XmlBuilder.prettyPrint(xml);
		}

		private Element addCfType(Document xml) {
			Element newValueNode = xml.createElement("cfType");
			XmlBuilder.addAttribute(xml, newValueNode, "value",
					cfType.toString());
			return newValueNode;
		}

		private Element addUid(Document xml) {
			Element notifierUid = xml.createElement("uid");
			XmlBuilder.addAttribute(xml, notifierUid, "value", uid);
			return notifierUid;
		}

		private void reset() {
			this.uid = null;
			this.cfType = null;
		}
	}
}
