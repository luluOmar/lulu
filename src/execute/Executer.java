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

import util.ArchitectureHelper;
import util.XmlParser;
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.ComparchFactory;
import de.mdelab.morisia.comparch.Component;
import de.mdelab.morisia.comparch.ComponentLifeCycle;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.Connector;
import de.mdelab.morisia.comparch.ProvidedInterface;
import de.mdelab.morisia.comparch.RequiredInterface;
import de.mdelab.morisia.comparch.Shop;

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
		LinkedList<Component> affectedFilters = new LinkedList<Component>();


		for (Document adaption : plannedAdaption) {

			// Execute adaption strategy, take action!
			NodeList elementNodes = adaption.getElementsByTagName("action");

			for (int i = 0; i < elementNodes.getLength(); i++) {
				Node currentNode = elementNodes.item(i);
				if (currentNode instanceof Element) {
					Element docElement = (Element) currentNode;
					Element actionNameElement = (Element) docElement
							.getElementsByTagName("actionName").item(0);
					String actionName = actionNameElement.getAttribute("value");
					Element actionValueElement = (Element) docElement
							.getElementsByTagName("actionValue").item(0);
					String actionValue = actionValueElement
							.getAttribute("value");
					if (actionName.equals("findFilter")) {
						findFilter(affectedFilters, actionValue);
					} else if (i == 0) {
						affectedComponent = performFirstAction(actionName,
								actionValue);
					} else if (actionName.equals("move filter in pipe")) {
						moveFilter(affectedFilters, actionValue);
					} else {
						performAction(affectedComponent, actionName,
								actionValue);
					}
				}

			}
		}
	}


	private static void moveFilter(LinkedList<Component> affectedFilters,
			String filterUid) {

		Shop shop = affectedFilters.get(0).getShop();
		EList<Component> allComponents = shop.getComponents();
		Component changedFilter = affectedFilters.get(0);
		Component beforeFilter = affectedFilters.get(1);
		Component afterFilter = affectedFilters.get(2);

		// Remove connectors at changed filter's old position
		ProvidedInterface oldBefore = null;
		RequiredInterface oldAfter = null;
		// Find filter before changed filter in pipe
		for (Component currentComponent : allComponents) {
			for (ProvidedInterface pI : currentComponent
					.getProvidedInterfaces()) {
				// Find before old position
				if (pI.getConnectors().size() != 0) {
					if (pI.getConnectors().get(0).getSource() != null)
						if (changedFilter.getUid().equals(
								pI.getConnectors().get(0).getSource()
										.getComponent().getUid())) {
							oldBefore = pI;
						}
				}
			}

			// Find successor of changed filter
			for (RequiredInterface rI : currentComponent
					.getRequiredInterfaces()) {
				// Find before old position
				if (rI.getConnector() != null) {
					if (changedFilter.getUid().equals(
							rI.getConnector().getTarget().getComponent()
									.getUid())) {
						oldAfter = rI;
					}
				}

			}
		}
		// Wire old neighbors with new connector
		if (oldAfter != null && oldBefore != null) {
			Connector connector = ComparchFactory.eINSTANCE.createConnector();
			connector.setSource(oldAfter);
			connector.setTarget(oldBefore);
			oldAfter.setConnector(connector);
		} else {
			System.out.println("Old neighbors not found");
		}

		// Cut wire at new position

		// Rewire filter at new position

		affectedFilters.clear();
	}

	private static void findFilter(LinkedList<Component> affectedFilters,
			String filterUid) {
		switch (filterUid) {
		case "front":
			affectedFilters.add(null);
			break;
		case "back":
			affectedFilters.add(null);
			break;
		default:
			EList<Shop> shops = mRubis.getShops();
			for (Shop shop : shops) {
				EList<Component> components = shop.getComponents();
				for (Component currentComp : components) {
					if (currentComp.getUid().equals(filterUid)) {
						affectedFilters.add(currentComp);
						System.out.println("found Component");
					}
				}
			}
			break;
		}
	}

	private static void performAction(Component affectedComponent, String name,
			String value) {
    	Shop shop;
		switch (name) {
		case "setState":
			System.out.println("Execute Adaption, set ComponentState to " + value + ".");
			affectedComponent.setState(ComponentLifeCycle.valueOf(value));
			break;
		case "addToShop":
			shop = ArchitectureHelper.findShopByUid(mRubis, value);
			// Add new Component to shop
			shop.getComponents().add(affectedComponent);
			ArchitectureHelper.wireRequiredInterfacesToComponent(shop, affectedComponent);
			// Start connected Component
			affectedComponent.setState(ComponentLifeCycle.STARTED);
			ArchitectureHelper.connectProvidedInterfacesToOtherComponents(shop, affectedComponent);

			break;
		case "destroyComponent":
			shop = affectedComponent.getShop();
			Component toBeRemovedComponent = ArchitectureHelper.lookupShopComponentByUid(shop, value);
			ArchitectureHelper.removeComponentFromShop(toBeRemovedComponent);
			break;
		default:
			System.err.println("No Action for \"" + name + "\" defined.");
			break;
		}
	}

	private static Component performFirstAction(String name, String value) {
		Component comp = null;
		switch (name) {

		case "lookup alternative components":
			Component c = ArchitectureHelper.lookupComponentInShops(mRubis, value);
			ComponentType originalCt = c.getType();
			ComponentType alternativeCt = ArchitectureHelper.findAlternativeComponentType(mRubis, originalCt);
			ComponentType replacement;

			if (alternativeCt != null) {
				replacement = alternativeCt;
			} else {
				replacement = originalCt;
				ArchitectureHelper.removeComponentFromShop(c);
			}
			comp = replacement.instantiate();

			break;
		case "instantiate and deploy":

			// Find Component Type and create new instance
			ComponentType ct = ArchitectureHelper.lookupComponentTypeByUid(mRubis, value);
			if (ct != null) {
				comp = ct.instantiate();
				comp.setState(ComponentLifeCycle.DEPLOYED);
			}

			break;
		case "findComponent":
			EList<Shop> shops = mRubis.getShops();
			for (Shop shop : shops) {
				EList<Component> components = shop.getComponents();
				for (Component currentComp : components) {
					if (currentComp.getUid().equals(value)) {
						comp = currentComp;

						System.out.println("found Component");
					}
					EList<ProvidedInterface> interfaces = currentComp
							.getProvidedInterfaces();
					for (ProvidedInterface provInterface : interfaces) {
						if (provInterface.getUid().equals(value)) {
							comp = provInterface.getComponent();
						}
					}
				}
			}
		}
		return comp;
	}
}
