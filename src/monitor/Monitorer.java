package monitor;

import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mrubis_simulator.Queue;

import org.eclipse.emf.common.notify.Notification;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import util.XmlBuilder;
import de.mdelab.morisia.comparch.ArchitecturalElement;
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.Component;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.Failure;
import de.mdelab.morisia.comparch.ProvidedInterface;
import de.mdelab.morisia.comparch.Shop;

public final class Monitorer {

	// private static Architecture model;
	private static Queue notifications;
	private static LinkedList<String> xmlMonitoringEvents = new LinkedList<String>();
	private static final String BASE_NODE = "monitoringEvent";
	private static DocumentBuilderFactory docFactory = DocumentBuilderFactory
			.newInstance();
	private static DocumentBuilder docBuilder;

	public static List<String> monitorModel(Queue notifications,
			Architecture mRubis) throws ParserConfigurationException {
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
			XmlBuilder.addAttribute(xml, newValueNode, "value",
					notification.getNewValue());
			baseElement.appendChild(newValueNode);

			Element oldValueElement = xml.createElement("oldValue");
			XmlBuilder.addAttribute(xml, oldValueElement, "value",
					notification.getOldValue());
			baseElement.appendChild(oldValueElement);

			Element notifierElement = xml.createElement("notifier");
			XmlBuilder.addAttribute(xml, notifierElement, "value",
					notification.getNotifier());
			baseElement.appendChild(notifierElement);

			ArchitecturalElement notifierSource = (ArchitecturalElement) notification
					.getNotifier();

			Component c = null;

			Class<?> notifierCls = notifierSource.getClass();

			if (Component.class.isAssignableFrom(notifierCls)) {
				c = (Component) notifierSource;
			} else if (ProvidedInterface.class.isAssignableFrom(notifierCls)) {
				ProvidedInterface pi = (ProvidedInterface) notifierSource;
				c = pi.getComponent();
			} else if (Failure.class.isAssignableFrom(notifierCls)) {
				Failure fa = (Failure) notifierSource;
				if (fa.getInterface() != null) {
					c = fa.getInterface().getComponent();
				}
			}

			if (c != null) {
				Element component = xml.createElement("component");
				XmlBuilder.addAttribute(xml, component, "value", c.getUid());
				ComponentType ct = c.getType();
				if (ct != null) {
					XmlBuilder
							.addAttribute(xml, component, "type", ct.getUid());
					notifierElement.appendChild(component);
				}

				Shop s = c.getShop();
				if (s != null) {
					Element shop = xml.createElement("shop");
					XmlBuilder.addAttribute(xml, shop, "value", s.getUid());
					notifierElement.appendChild(shop);
				}
			}

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
			XmlBuilder.addAttribute(xml, notifierUid, "value",
					notifierSource.getUid());
			notifierElement.appendChild(notifierUid);

			Element eventTypeElement = xml.createElement("eventType");
			XmlBuilder.addAttribute(xml, eventTypeElement, "value",
					notification.getEventType());
			baseElement.appendChild(eventTypeElement);

			// create String representation
			String newValue = notification.getNewStringValue();
			try {
				xmlMonitoringEvents.add(XmlBuilder.prettyPrint(xml));
				if (newValue != null
						&& (newValue.equals("NOT_SUPPORTED") || newValue
								.contains("FailureImpl")))
					System.out.println(XmlBuilder.prettyPrint(xml));
			} catch (Exception e) {
				System.err.println("Unable to add xml-Event to List.");
				e.printStackTrace();
			}
		}
		return xmlMonitoringEvents;
	}
}
