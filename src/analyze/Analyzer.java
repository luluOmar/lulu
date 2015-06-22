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
import org.w3c.dom.Node;

import util.XmlBuilder;
import util.XmlParser;

public final class Analyzer {

	public enum CFType {
		NONE("No Failure"), CF1("CF-1"), CF2("CF-2"), CF3("CF-3"), CF4("CF-4");

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

	private static final class CriticalFailure {

		public static final CriticalFailure NONE = new CriticalFailure(
				CFType.NONE, null);

		private final CFType type;

		private final String failureEvent;

		public CriticalFailure(CFType type, String failureEvent) {
			this.type = type;
			this.failureEvent = failureEvent;
		}

		public CFType getType() {
			return type;
		}

		public String getFailureEvent() {
			return failureEvent;
		}

		public boolean isClassifiedAsCf() {
			return !CFType.NONE.equals(type);
		}

	}

	private static LinkedList<Document> receivedEvents = new LinkedList<Document>();
	private static LinkedList<String> xmlAnalyzedEvents = new LinkedList<String>();
	private static final String BASE_NODE = "analyzedEvent";
	private static DocumentBuilderFactory docFactory = DocumentBuilderFactory
			.newInstance();
	private static DocumentBuilder docBuilder;

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
		Map<String, Integer> piFailures = new HashMap<String, Integer>();

		for (Document event : receivedEvents) {
			CriticalFailure criticalFailure = CriticalFailure.NONE;
			criticalFailure = classifyEvent(event, piFailures);
			if (criticalFailure != CriticalFailure.NONE) {
				xmlAnalyzedEvents.add(criticalFailure.getFailureEvent());
			}
		}

		return xmlAnalyzedEvents;

	}

	private static CriticalFailure classifyEvent(Document event,
			Map<String, Integer> failureMap) throws Exception {
		String value = XmlParser.getElementsValue(event, "newValue", "value");
		String uid = XmlParser.getElementsValue(event, "uid", "value");
		String componentTypeUid = XmlParser.getElementsValue(event,
				"component", "type");
		String shopUid = XmlParser.getElementsValue(event, "shop", "value");

		CriticalFailure cf = CriticalFailure.NONE;
		cf = checkForCf1(event, uid, value, componentTypeUid, shopUid);
		cf = checkForCf2(cf, failureMap, event, uid, value, componentTypeUid,
				shopUid);
		cf = checkForCf3(cf, event, componentTypeUid, shopUid);
		cf = checkForCf4(cf, event, uid, componentTypeUid, shopUid);
		return cf;
	}

	private static void shiftHistory() {
		for (Entry<String, String> e : failureCount.entrySet()) {
			failureCount.put(e.getKey(), e.getValue().substring(1, 3) + "0");
		}
	}

	/**
	 * TODO move this to monitoring, this should be included in the monitoring
	 * events... instead of looked up here.
	 * 
	 * @param oldValue
	 * @param notifierValue
	 * @return
	 */
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

	/**
	 * Creates a critical failure instance with the given type and uid
	 * 
	 * @param cfType
	 * @param uid
	 * @return
	 * @throws Exception
	 */
	private static CriticalFailure createCF(CFType cfType, String uid,
			String componentTypeUid, String shopUid) throws Exception {
		return new CriticalFailureBuilder().setCfType(cfType)
				.setComponentUid(uid).setComponentTypeUid(componentTypeUid)
				.setShopUid(shopUid).build();
	}

	private static CriticalFailure checkForCf1(Document event, String uid,
			String value, String componentTypeUid, String shopUid)
			throws Exception {
		if (value.equals("NOT_SUPPORTED")) {
			return createCF(CFType.CF1, uid, componentTypeUid, shopUid);
		}
		return CriticalFailure.NONE;
	}

	private static CriticalFailure checkForCf2(CriticalFailure cf,
			Map<String, Integer> failureMap, Document event, String uid,
			String value, String componentTypeUid, String shopUid)
			throws Exception {

		if (!cf.isClassifiedAsCf() && value.contains("FailureImpl")) {
			int failureCount = 0;
			if (failureMap.containsKey(uid)) {
				failureCount = failureMap.get(uid);
			}
			failureCount++;
			// does it satisfy cf2 constraints
			if (failureCount == 5) {
				cf = createCF(CFType.CF2, uid, componentTypeUid, shopUid);
			}
			failureMap.put(uid, failureCount);
		}
		return cf;
	}

	private static CriticalFailure checkForCf3(CriticalFailure cf,
			Document event, String componentTypeUid, String shopUid)
			throws Exception {
		// Check for CF-3

		if (cf.isClassifiedAsCf()) {
			return cf;
		}

		String componentID = "";
		String newValue = XmlParser
				.getElementsValue(event, "newValue", "value");
		String oldValueString = XmlParser.getElementsValue(event, "oldValue",
				"value");
		String notifierString = XmlParser.getElementsValue(event, "notifier",
				"value");

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
			System.out.println(componentTypeId + " " + shopId);
			System.out.println(componentTypeUid + " " + shopUid);
			return createCF(CFType.CF3, componentID, componentTypeId, shopId);
		}
		return cf;
	}

	private static CriticalFailure checkForCf4(CriticalFailure cf,
			Document event, String uid, String componentTypeUid, String shopUid)
			throws Exception {

		CFType cfType = cf.getType();

		if (cfType.equals(CFType.CF1) || cfType.equals(CFType.CF2)) {
			if (cfType.equals(CFType.CF2))
				uid = XmlParser.getElementsValue(event, "component", "value");
			System.out.println("Check for cf4:" + event + " with uid:" + uid);
			String runMatches = "001";
			if (failureCount.containsKey(uid)) {
				runMatches = failureCount.get(uid).substring(0, 2).concat("1");
				System.err.println("Run matches: " + runMatches);
				if (runMatches.equals("111")) {
					failureCount.remove(uid);
					cf = createCF(CFType.CF4, uid, componentTypeUid, shopUid);
				}
			}
			failureCount.put(uid, runMatches);
		}
		return cf;
	}

	private static class CriticalFailureBuilder {

		private CFType cfType;
		private String componentUid;
		private String componentTypeUid;
		private String shopUid;

		private CriticalFailureBuilder setCfType(CFType cfType) {
			this.cfType = cfType;
			return this;
		}

		public CriticalFailureBuilder setComponentUid(String uid) {
			this.componentUid = uid;
			return this;
		}

		public CriticalFailureBuilder setComponentTypeUid(
				String componentTypeUid) {
			this.componentTypeUid = componentTypeUid;
			return this;
		}

		public CriticalFailureBuilder setShopUid(String shopUid) {
			this.shopUid = shopUid;
			return this;
		}

		private CriticalFailure build() throws Exception {
			Document xml = docBuilder.newDocument();
			Element baseElement = xml.createElement(BASE_NODE);
			xml.appendChild(baseElement);
			baseElement.appendChild(addUid(xml));
			if (cfType != null) {
				baseElement.appendChild(addCfType(xml));
			}
			if (shopUid != null) {
				baseElement.appendChild(addShopUid(xml));
			}
			if (componentTypeUid != null) {
				baseElement.appendChild(addComponentTypeUid(xml));
			}
			CriticalFailure cf = new CriticalFailure(cfType,
					XmlBuilder.prettyPrint(xml));
			return cf;
		}

		private Node addShopUid(Document xml) {
			Element shop = xml.createElement("shop");
			XmlBuilder.addAttribute(xml, shop, "value", shopUid);
			return shop;
		}

		private Node addComponentTypeUid(Document xml) {
			Element cType = xml.createElement("componentType");
			XmlBuilder.addAttribute(xml, cType, "value", componentTypeUid);
			return cType;
		}

		private Element addCfType(Document xml) {
			Element newValueNode = xml.createElement("cfType");
			XmlBuilder.addAttribute(xml, newValueNode, "value",
					cfType.toString());
			return newValueNode;
		}

		private Element addUid(Document xml) {
			Element notifierUid = xml.createElement("uid");
			XmlBuilder.addAttribute(xml, notifierUid, "value", componentUid);
			return notifierUid;
		}
	}
}
