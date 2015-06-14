package execute;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.EList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.XmlParser;
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.Component;
import de.mdelab.morisia.comparch.ComponentLifeCycle;
import de.mdelab.morisia.comparch.ProvidedInterface;
import de.mdelab.morisia.comparch.Shop;

public final class Executer {

	public static void executeAdaption(Architecture mRubis,
			List<String> xmlPlannedAdaption)
			throws ParserConfigurationException, SAXException, IOException {

		// Get document out of analyzed events XML
		List<Document> plannedAdaption = new LinkedList<Document>();
		for (String xmlString : xmlPlannedAdaption) {
			plannedAdaption.add(XmlParser.createDocByString(xmlString));
		}
		
		Component affectedComponent = null;
		EList<Shop> shops = mRubis.getShops();
		
		for (Document adaption : plannedAdaption) {
			String uid = XmlParser.getElementsValue(adaption, "uid", "value");
			for(Shop shop : shops) {
				EList<Component> components = shop.getComponents();
				for(Component comp : components) {
					if(comp.getUid().equals(uid)) {
						affectedComponent = comp;
					}
					EList<ProvidedInterface> interfaces = comp.getProvidedInterfaces();
					for(ProvidedInterface provInterface : interfaces) {
						if(provInterface.getUid().equals(uid)) {
							affectedComponent = provInterface.getComponent();
						}
					}
				}
			}	

			// Execute adaption strategy, take action!
			NodeList elementNodes = adaption.getElementsByTagName("action");
			for (int i = 0; i < elementNodes.getLength(); i++) {
				Node currentNode = elementNodes.item(i);
								
				if(currentNode instanceof Element) {
					Element docElement = (Element)currentNode;
					Element actionNameElement = (Element)docElement.getElementsByTagName("actionName").item(0);
					String actionName = actionNameElement.getAttribute("value");
					Element actionValueElement = (Element)docElement.getElementsByTagName("actionValue").item(0);
					String actionValue = actionValueElement.getAttribute("value");
					performAction(affectedComponent, actionName, actionValue);
				}
				
			}
		}
	}
	
	private static void performAction(Component affectedComponent, String name, String value) {
		switch(name) {
			case "setState":
				System.out.println("Execute Adaption, set ComponentState to " + value + ".");
				setState(affectedComponent, value);
				break;
			default:
				System.err.println("No Action for \""+ name + "\" defined.");
				break;
		}
	}
	
	private static void setState(Component affectedComponent, String value) {
		
		switch(value) {
			case "UNDEPLOYED":
				affectedComponent.setState(ComponentLifeCycle.UNDEPLOYED);
				break;
			case "DEPLOYED":
				affectedComponent.setState(ComponentLifeCycle.DEPLOYED);
				break;
			case "STARTED":
				affectedComponent.setState(ComponentLifeCycle.STARTED);
				break;
			default:
				System.err.println("Undefined Value ("+ value + ") for action \"setState\". ");
				break;
		}
	}
}
