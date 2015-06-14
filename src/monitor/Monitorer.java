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
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.impl.ComponentImpl;

public final class Monitorer {
	
	private static Architecture model;
	private static Queue notifications;
	private static LinkedList<String> xmlMonitoringEvents = new LinkedList<String>();
	private static final String BASE_NODE = "monitoringEvent";
	private static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder docBuilder;
	
	public static List<String> monitorModel(Queue notifications) throws ParserConfigurationException {
		Monitorer.notifications = notifications;
		docBuilder = docFactory.newDocumentBuilder();
		return checkNotifications();
	}
	
	public static LinkedList<String> checkNotifications() {
		
		//TODO is this right? Delete old events before a new run?
		xmlMonitoringEvents = new LinkedList<String>();
		
		// Loop: build for each notification one monitoringEvent and add it 
		// (as string) to xmlMonitoringEvents
		for(int i = 0; i < notifications.getSize(0); i++) {
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
			
//			ArchitecturalElement notifierSource = (ArchitecturalElement)notification.getNotifier();
			ComponentImpl notifyingComponent = (ComponentImpl) notification
					.getNotifier();
//			baseElement.appendChild(notifierSource);
			
			Element notifierShop = xml.createElement("shop");
			XmlBuilder.addAttribute(xml, notifierShop, "value", notifyingComponent.getShop().getName());
			notifierElement.appendChild(notifierShop);
			
			Element notifierUid = xml.createElement("uid");
			XmlBuilder.addAttribute(xml, notifierUid, "value", notifyingComponent.getUid());
			notifierElement.appendChild(notifierUid);
			
			Element eventTypeElement = xml.createElement("eventType");
			XmlBuilder.addAttribute(xml, eventTypeElement, "value", notification.getEventType());
			baseElement.appendChild(eventTypeElement);

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
	
}
