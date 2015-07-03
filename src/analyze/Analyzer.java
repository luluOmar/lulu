package analyze;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mrubis_simulator.Queue;

import org.eclipse.emf.ecore.resource.impl.ArchiveURIHandlerImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.XmlBuilder;
import util.XmlParser;

public final class Analyzer {
	//failure types
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
	
	public enum PIType {
		NONE("No Pi"), PI1("PI-1"), PI2("PI-2"), PI3("PI-3");
		
		private String name;
		
		private PIType(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}

		public static PIType byName(String value) {
			for (PIType type : PIType.values()) {
				if (type.name.equalsIgnoreCase(value))
					return type;
			}
			throw new IllegalArgumentException("Unknown performance issue type name!");
		}
	}
	
	private static final class ItemManagementService {
		public float getAvgRespTime() {
			return avgRespTime;
		}

		public float getTotalTime() {
			return totalTime;
		}
		
		public float getInvocationCount() {
			return invocationCount;
		}
		
		private float totalTime;
		private float invocationCount;
		private float avgRespTime;
		
		public ItemManagementService(float totalTime, float invocationCount) {
			this.totalTime = totalTime;
			this.invocationCount = invocationCount;
			this.avgRespTime = totalTime/invocationCount;
		}
	}
	
	private static final class ItemFilter {
		
		public float getRate() {
			return rate;
		}

		public float getTime() {
			return time;
		}

		private String uid;
		private float slope;
		private float rate;
		private float time;
		private boolean skipped;
		
		public ItemFilter(String uid, float slope, float rate, float time, String status) {
			this.uid = uid;
			this.slope = slope;
			this.rate = rate;
			this.time = time;
			if(status.equals("STARTED")) {
				this.skipped = false;
			} else {
				this.skipped = true;
			}
		}
		
		public String getUid() {
			return uid;
		}
		
		public float getSlope() {
			return slope;
		}
	}
	
	private static final class PerformanceIssue {
		
		public static final PerformanceIssue NONE = new PerformanceIssue(
				PIType.NONE, null);
		
		private final PIType type;
		
		private final String piEvent;
		
		public PerformanceIssue(PIType type , String piEvent) {
			this.type = type;
			this.piEvent = piEvent;
		}
		//schon als fehler klassifiziert?
		public boolean isClassifiedAsPi() {
			return !PIType.NONE.equals(type);
		}
	}
	
	private static final class CriticalFailure {

		public static final CriticalFailure NONE = new CriticalFailure(
				CFType.NONE, null);

		private final CFType type;
		//for planer
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
		//schon als fehler klassifizier?
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
	//key is uid, value is bitmask of last three loops (001,011,010,100,111,etc.)
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
		//TODO delete piFailures which are also CF failures
		Map<String, Integer> piFailures = new HashMap<String, Integer>();

		for (Document event : receivedEvents) {
			CriticalFailure criticalFailure = classifyEvent(event, piFailures);
			if (criticalFailure != CriticalFailure.NONE) {
				xmlAnalyzedEvents.add(criticalFailure.getFailureEvent());
			}
		}
		
		//TODO delete already classified events
		for (Document event : receivedEvents) {
			PerformanceIssue pi = classifyPerformanceEvent(event);
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
	
	private static PerformanceIssue classifyPerformanceEvent(Document event) throws Exception {
		String uid = XmlParser.getElementsValue(event, "uid", "value");
		String shopUid = XmlParser.getElementsValue(event, "shop", "value");
		PerformanceIssue pI = PerformanceIssue.NONE;
		ItemManagementService ims = null;
		//Read Item management service
		NodeList imsList = event.getElementsByTagName("ItemManagementService");
		for(int i = 0; i < imsList.getLength(); i++) {
			float totalTime = Float.valueOf(((Element)imsList.item(i)).getAttribute("totalTime"));
			float invocationCount = Float.valueOf(((Element)imsList.item(i)).getAttribute("invocationCount"));
			ims = new ItemManagementService(totalTime, invocationCount);
		}
		
		//Read item filter
		NodeList itemFilterList = event.getElementsByTagName("itemFilter");
		LinkedList<ItemFilter> pipeFilter = new LinkedList<Analyzer.ItemFilter>();
		ArrayList<ItemFilter> skippedFilter = new ArrayList<ItemFilter>();
		for(int i = 0; i < itemFilterList.getLength(); i++) {
			Element iF = (Element)itemFilterList.item(i);
			String id = iF.getAttribute("uid");
			String status = iF.getAttribute("status");
			float slope = Float.valueOf(iF.getAttribute("slope"));
			float rate = Float.valueOf(iF.getAttribute("rate"));
			float time = Float.valueOf(iF.getAttribute("time"));
			if(status.equals("STARTED")) {
				pipeFilter.add(new ItemFilter(id, slope, rate, time, status));				
			} else {
				skippedFilter.add(new ItemFilter(id, slope, rate, time, status));
			}
		}
		pI = checkForPi1(pI, pipeFilter, uid, shopUid);
		pI = checkForPi2(pI, uid, ims, pipeFilter, shopUid);
		pI = checkForPi3(pI, uid, ims, pipeFilter, shopUid);
		return pI;
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
	
	private static PerformanceIssue createPI(PIType piType, String uid, 
			String shopUid, LinkedList<ItemFilter> itemFilter, float avgResponseTime) throws Exception {
		return new PerformanceIssueBuilder().setPiType(piType).setComponentUid(uid)
				.setShopUid(shopUid).setItemFilter(itemFilter).setAverageResponseTime(avgResponseTime).build();
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
				//component uid needed in case of CF2
				uid = XmlParser.getElementsValue(event, "component", "value");
			System.out.println("Check for cf4:" + event + " with uid:" + uid);
			String runMatches = "001";
			if (failureCount.containsKey(uid)) {
				runMatches = failureCount.get(uid).substring(0, 2).concat("1");
				System.err.println("Run matches: " + runMatches);
				if (runMatches.equals("111")) {
					//remove from map
					failureCount.remove(uid);
					return createCF(CFType.CF4, uid, componentTypeUid, shopUid);
				}
			}
			//set new failure string
			failureCount.put(uid, runMatches);
		}
		return cf;
	}
	
	private static PerformanceIssue checkForPi1(PerformanceIssue pi, 
			LinkedList<ItemFilter> itemFilter, String uid, String shopUid) throws Exception {
		if (pi.isClassifiedAsPi()) {
			return pi;
		}
		for(int i = 1; i < itemFilter.size(); i++) {
			float slope1 = itemFilter.get(i - 1).getSlope();
			float slope2 = itemFilter.get(i).getSlope();
			if(slope1 < slope2) {
				return createPI(PIType.PI1, uid, shopUid, itemFilter, -1);
			}
		}
		return pi;
	}
	
	private static PerformanceIssue checkForPi2(PerformanceIssue pi, String uid, 
			ItemManagementService ims, LinkedList<ItemFilter> itemFilter, String shopUid) throws Exception {
		if(pi.isClassifiedAsPi()) {
			return pi;
		}
		if(ims != null) {
			float avgResponseTime = ims.getAvgRespTime();
			if(avgResponseTime > 1150) {
				pi = createPI(PIType.PI2, uid, shopUid, itemFilter, avgResponseTime);
			}
		}
		return pi;
	}

	private static PerformanceIssue checkForPi3(PerformanceIssue pi, String uid, 
			ItemManagementService ims, LinkedList<ItemFilter> itemFilter, String shopUid) throws Exception {
		if(pi.isClassifiedAsPi()) {
			return pi;
		}
		if(ims != null) {
			float avgResponseTime = ims.getAvgRespTime();
			if(avgResponseTime < 850) { //TODO check pipe-length: no PI-3 if the pipe has length 10
				pi = createPI(PIType.PI3, uid, shopUid, itemFilter, avgResponseTime);
			}
		}
		return pi;
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
	
	private static class PerformanceIssueBuilder {

		private PIType piType;
		private LinkedList<ItemFilter> itemFilter;
		private String componentUid;
		private String shopUid;
		private float avgResponseTime;

		private PerformanceIssueBuilder setPiType(PIType piType) {
			this.piType = piType;
			return this;
		}

		public PerformanceIssueBuilder setComponentUid(String uid) {
			this.componentUid = uid;
			return this;
		}

		public PerformanceIssueBuilder setShopUid(String shopUid) {
			this.shopUid = shopUid;
			return this;
		}
		
		public PerformanceIssueBuilder setItemFilter(LinkedList<ItemFilter> itemFilter) {
			this.itemFilter = itemFilter;
			return this;
		}
		
		public PerformanceIssueBuilder setAverageResponseTime(float avgResponseTime) {
			this.avgResponseTime = avgResponseTime;
			return this;
		}

		private PerformanceIssue build() throws Exception {
			Document xml = docBuilder.newDocument();
			Element baseElement = xml.createElement(BASE_NODE);
			xml.appendChild(baseElement);
			baseElement.appendChild(addUid(xml));
			if (piType != null) {
				baseElement.appendChild(addPiType(xml));
			}
			if (shopUid != null) {
				baseElement.appendChild(addShopUid(xml));
			}
			if (itemFilter != null) {
				for(ItemFilter iF : itemFilter) {
					baseElement.appendChild(addItemFilter(xml, iF));
				}
			}
			if (avgResponseTime > -1) {
				baseElement.appendChild(addAvgResponseTime(xml));
			}
			PerformanceIssue pi = new PerformanceIssue(piType,
					XmlBuilder.prettyPrint(xml));
			return pi;
		}

		private Node addShopUid(Document xml) {
			Element shop = xml.createElement("shop");
			XmlBuilder.addAttribute(xml, shop, "value", shopUid);
			return shop;
		}
		
		private Node addItemFilter(Document xml, ItemFilter iF) {
			Element itemFilter = xml.createElement("itemFilter");
			XmlBuilder.addAttribute(xml, itemFilter, "uid", iF.getUid());
			XmlBuilder.addAttribute(xml, itemFilter, "slope", iF.getSlope());
			XmlBuilder.addAttribute(xml, itemFilter, "time", iF.getTime());
			XmlBuilder.addAttribute(xml, itemFilter, "rate", iF.getRate());
			return itemFilter;
		}

		private Element addPiType(Document xml) {
			Element newValueNode = xml.createElement("piType");
			XmlBuilder.addAttribute(xml, newValueNode, "value",
					piType.toString());
			return newValueNode;
		}

		private Element addUid(Document xml) {
			Element notifierUid = xml.createElement("uid");
			XmlBuilder.addAttribute(xml, notifierUid, "value", componentUid);
			return notifierUid;
		}
		
		private Element addAvgResponseTime(Document xml) {
			Element avgRespElement = xml.createElement("avgResponseTime");
			XmlBuilder.addAttribute(xml, avgRespElement, "avgResponseTime", avgResponseTime);
			return avgRespElement;
		}
	}
}
