package monitor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mrubis_simulator.Queue;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import util.XmlBuilder;
import de.mdelab.morisia.comparch.ArchitecturalElement;
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.Component;
import de.mdelab.morisia.comparch.ComponentLifeCycle;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.Failure;
import de.mdelab.morisia.comparch.MonitoredProperty;
import de.mdelab.morisia.comparch.PerformanceStats;
import de.mdelab.morisia.comparch.ProvidedInterface;
import de.mdelab.morisia.comparch.Shop;
import de.mdelab.morisia.comparch.impl.PerformanceStatsImpl;

public final class Monitorer {

	// private static Architecture model;
	private static Queue notifications;
	private static LinkedList<String> xmlMonitoringEvents = new LinkedList<String>();
	private static final String BASE_NODE = "monitoringEvent";
	private static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder docBuilder;

	public static List<String> monitorModel(Queue notifications, Architecture mRubis)
			throws ParserConfigurationException {
		Monitorer.notifications = notifications;
		docBuilder = docFactory.newDocumentBuilder();
		return checkNotifications(mRubis);
	}

	public static LinkedList<String> checkNotifications(Architecture mRubis) {

		// TODO is this right? Delete old events before a new run?
		xmlMonitoringEvents = new LinkedList<String>();

		// Loop: build for each notification one monitoringEvent and add it
		// (as string) to xmlMonitoringEvents
		for (int i = 0; i < notifications.getSize(0); i++) {
			Notification notification = notifications.get(0, i);

			Document xml = docBuilder.newDocument();
			Element baseElement = xml.createElement(BASE_NODE);
			xml.appendChild(baseElement);

			Element newValueNode = xml.createElement("newValue");
			XmlBuilder.addAttribute(xml, newValueNode, "value", notification.getNewValue());
			baseElement.appendChild(newValueNode);

			Element oldValueElement = xml.createElement("oldValue");
			XmlBuilder.addAttribute(xml, oldValueElement, "value", notification.getOldValue());
			baseElement.appendChild(oldValueElement);

			Element notifierElement = xml.createElement("notifier");
			XmlBuilder.addAttribute(xml, notifierElement, "value", notification.getNotifier());
			baseElement.appendChild(notifierElement);

			ArchitecturalElement notifierSource = (ArchitecturalElement) notification.getNotifier();

			Component c = null;
			float invocationCount = -1;
			float totalTime = -1;
			Class<?> notifierCls = notifierSource.getClass();
			// pruefen, ob notifierSouce eine componente ist
			if (Component.class.isAssignableFrom(notifierCls)) {
				c = (Component) notifierSource;
				// aus provided interface die komponente holen
			} else if (ProvidedInterface.class.isAssignableFrom(notifierCls)) {
				ProvidedInterface pi = (ProvidedInterface) notifierSource;
				c = pi.getComponent();
				// vom failure das interface holen, vom interfache componente
				// holen
			} else if (Failure.class.isAssignableFrom(notifierCls)) {
				Failure fa = (Failure) notifierSource;
				if (fa.getInterface() != null) {
					c = fa.getInterface().getComponent();
				}
			} else if (MonitoredProperty.class.isAssignableFrom(notifierCls)) {
				MonitoredProperty mp = (MonitoredProperty) notifierSource;
				c = mp.getComponent();
			} else if (PerformanceStatsImpl.class.isAssignableFrom(notifierCls)) {
				PerformanceStatsImpl psi = (PerformanceStatsImpl) notifierSource;
				c = psi.getInterface().getComponent();
				if (psi.getMethod().getSignature().equals("getPersonalizedItems(java.lang.String,java.lang.String)")) {
					invocationCount = psi.getInvocationCount();
					totalTime = psi.getTotalTime();
				}
			}

			ArrayList<ProvidedInterface> pipeFilter = new ArrayList<ProvidedInterface>();
			ArrayList<Component> skippedFilter = new ArrayList<Component>();
			// add additional information is possible
			if (c != null) {
				// type?
				Element component = xml.createElement("component");
				XmlBuilder.addAttribute(xml, component, "value", c.getUid());
				ComponentType ct = c.getType();
				if (ct != null) {
					XmlBuilder.addAttribute(xml, component, "type", ct.getUid());
					notifierElement.appendChild(component);
				}
				// shop?
				Shop s = c.getShop();
				if (s != null) {
					Element shop = xml.createElement("shop");
					XmlBuilder.addAttribute(xml, shop, "value", s.getUid());
					notifierElement.appendChild(shop);

					// ItemFilter-List
					EList<Component> allComponents = s.getComponents();
					for (Component currentComponent : allComponents) {
						// System.out.println("Type: " +
						// currentComponent.getType().getName());
						for (ProvidedInterface pI : currentComponent.getProvidedInterfaces()) {
							if (pI.getType().getFullyQualifiedName().equals("de.hpi.sam.rubis.filter.ItemFilter")) {
								if (currentComponent.getState().equals(ComponentLifeCycle.STARTED)) {
									pipeFilter.add(pI);
								} else {
									skippedFilter.add(currentComponent);
								}
							}
						}
					}
				}
				// total time / invocation count
				if (totalTime > -1 && invocationCount > -1) {
					Element ims = xml.createElement("ItemManagementService");
					XmlBuilder.addAttribute(xml, ims, "totalTime", totalTime);
					XmlBuilder.addAttribute(xml, ims, "invocationCount", invocationCount);
					notifierElement.appendChild(ims);
				}

			}

			LinkedList<Component> itemFilterList = new LinkedList<>();
//			sortFilter(pipeFilter);

			// System.out.println("Matching component: " + c + " for: "
			// + notifierSource + " notification - " + notification);

			// ComponentImpl notifyingComponent = (ComponentImpl) notification
			// .getNotifier();
			// baseElement.appendChild(notifierSource);

			// Element notifierShop = xml.createElement("shop");
			// XmlBuilder.addAttribute(xml, notifierShop, "value",
			// notifyingComponent.getShop().getName());
			// notifierElement.appendChild(notifierShop);

			Element notifierUid = xml.createElement("uid");
			// XmlBuilder.addAttribute(xml, notifierUid, "value",
			// notifyingComponent.getUid());
			XmlBuilder.addAttribute(xml, notifierUid, "value", notifierSource.getUid());
			notifierElement.appendChild(notifierUid);

			Element eventTypeElement = xml.createElement("eventType");
			XmlBuilder.addAttribute(xml, eventTypeElement, "value", notification.getEventType());
			baseElement.appendChild(eventTypeElement);

			for (Component currentComp : itemFilterList) {
				Element itemFilterElement = xml.createElement("itemFilter");
				XmlBuilder.addAttribute(xml, itemFilterElement, "uid", currentComp.getUid());
				EList<MonitoredProperty> monProperties = currentComp.getMonitoredProperties();
				float sR = 0;
				float lC = 0;
				for (MonitoredProperty mP : monProperties) {
					if (mP.toString().contains("name: selection-rate")) {
						sR = Float.valueOf(mP.getValue());
					} else if (mP.toString().contains("name: local-computation-time")) {
						lC = Float.valueOf(mP.getValue());
					}
				}
				XmlBuilder.addAttribute(xml, itemFilterElement, "time", lC);
				XmlBuilder.addAttribute(xml, itemFilterElement, "rate", sR);
				XmlBuilder.addAttribute(xml, itemFilterElement, "slope", sR / lC);
				XmlBuilder.addAttribute(xml, itemFilterElement, "status", "STARTED");
				baseElement.appendChild(itemFilterElement);
			}

			for (Component currentComp : skippedFilter) {
				Element itemFilterElement = xml.createElement("itemFilter");
				XmlBuilder.addAttribute(xml, itemFilterElement, "uid", currentComp.getUid());
				EList<MonitoredProperty> monProperties = currentComp.getMonitoredProperties();
				float sR = 0;
				float lC = 0;
				for (MonitoredProperty mP : monProperties) {
					if (mP.toString().contains("name: selection-rate")) {
						sR = Float.valueOf(mP.getValue());
					} else if (mP.toString().contains("name: local-computation-time")) {
						lC = Float.valueOf(mP.getValue());
					}
				}
				XmlBuilder.addAttribute(xml, itemFilterElement, "time", lC);
				XmlBuilder.addAttribute(xml, itemFilterElement, "rate", sR);
				XmlBuilder.addAttribute(xml, itemFilterElement, "slope", sR / lC);
				XmlBuilder.addAttribute(xml, itemFilterElement, "status", "UNDEPLOYED");
				baseElement.appendChild(itemFilterElement);
			}
			// create String representation
			try {
				xmlMonitoringEvents.add(XmlBuilder.prettyPrint(xml));
			} catch (Exception e) {
				System.err.println("Unable to add xml-Event to List.");
				e.printStackTrace();
			}
		}

		return xmlMonitoringEvents;
	}

	private static LinkedList<Component> sortFilter(ArrayList<ProvidedInterface> allFilter) {
		// find first
		ProvidedInterface currentPI = null;
		LinkedList<Component> sortedFilter = new LinkedList<Component>();
		if (allFilter.size() > 0) {
			for (ProvidedInterface pI : allFilter) {
				if (pI.getConnectors() != null && pI.getConnectors().size() > 0) {
					if (pI.getConnectors().get(0).getSource().getComponent().getUid().contains("Query Service")) {
						currentPI = pI;
					}
				}
			}
			// sort
			boolean hasNext = false;
			do {
				hasNext = false;
				if (currentPI != null) {
					sortedFilter.add(currentPI.getComponent());
					String currentUid = currentPI.getComponent().getUid();
					for (ProvidedInterface prov : allFilter) {
						if ((prov.getConnectors() != null && prov.getConnectors().size() > 0)
								&& currentUid.equals(prov.getConnectors().get(0).getSource().getComponent().getUid())) {
							currentPI = prov;
							hasNext = true;
						}
					}
				}
			} while (currentPI != null && hasNext != false);
		}

		return sortedFilter;
	}
}
