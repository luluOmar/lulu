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
import de.mdelab.morisia.comparch.ComparchFactory;
import de.mdelab.morisia.comparch.Component;
import de.mdelab.morisia.comparch.ComponentLifeCycle;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.InterfaceType;
import de.mdelab.morisia.comparch.ProvidedInterface;
import de.mdelab.morisia.comparch.RequiredInterface;
import de.mdelab.morisia.comparch.Shop;
import de.mdelab.morisia.comparch.impl.ProvidedInterfaceImpl;

public final class Executer {
	
	private static Architecture mRubis;

	public static void executeAdaption(Architecture mRubis,
			List<String> xmlPlannedAdaption)
			throws ParserConfigurationException, SAXException, IOException {
		
		Executer.mRubis = mRubis;
		
		// Get document out of analyzed events XML
		List<Document> plannedAdaption = new LinkedList<Document>();
		for (String xmlString : xmlPlannedAdaption) {
			plannedAdaption.add(XmlParser.createDocByString(xmlString));
		}
		
		
		Component affectedComponent = null;
		EList<Shop> shops = mRubis.getShops();
		
		for (Document adaption : plannedAdaption) {
				
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
					if(i == 0) {
						affectedComponent = performFirstAction(actionName, actionValue);
					} else {
						performAction(affectedComponent, actionName, actionValue);
					}
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
			case "addToShop":
				EList<Shop> shops = mRubis.getShops();
				for(int i = 0; i < shops.size(); i++) {
					if(value.equals(shops.get(i).getUid())) {
						shops.get(i).getComponents().add(affectedComponent);
					}
				}
				break;
			default:
				System.err.println("No Action for \""+ name + "\" defined.");
				break;
		}
	}
	
	private static Component performFirstAction(String name, String value) {
		Component comp = null;
		switch(name) {
			case "instantiate and deploy":
				EList<ComponentType> types = mRubis.getComponentTypes();
				for(int i = 0; i < types.size(); i++) {
					ComponentType type = types.get(i);
					if(value.equals(type.getUid())) {
						comp = type.instantiate();
						comp.setState(ComponentLifeCycle.DEPLOYED);						
					}
				}
				
				break;
			case "findComponent":
				EList<Shop> shops = mRubis.getShops();
				for(Shop shop : shops) {
					EList<Component> components = shop.getComponents();
					for(Component currentComp : components) {
						if(currentComp.getUid().equals(value)) {
							comp = currentComp;
						}
						EList<ProvidedInterface> interfaces = currentComp.getProvidedInterfaces();
						for(ProvidedInterface provInterface : interfaces) {
							if(provInterface.getUid().equals(value)) {
								comp = provInterface.getComponent();
							}
						}
					}
				}	
		}
		return comp;
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
