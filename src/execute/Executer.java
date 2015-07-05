package execute;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.internal.expressions.AndExpression;
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
import de.mdelab.morisia.comparch.Connector;
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
				if (currentNode instanceof Element) {
					Element docElement = (Element) currentNode;
					Element actionNameElement = (Element) docElement
							.getElementsByTagName("actionName").item(0);
					String actionName = actionNameElement.getAttribute("value");
					Element actionValueElement = (Element) docElement
							.getElementsByTagName("actionValue").item(0);
					String actionValue = actionValueElement
							.getAttribute("value");
					if (i == 0) {
						affectedComponent = performFirstAction(actionName,
								actionValue);
					} else {
						performAction(affectedComponent, actionName,
								actionValue);
					}
				}

			}
		}
	}

	private static void performAction(Component affectedComponent, String name,
			String value) {
		switch (name) {
		case "setState":
			System.out.println("Execute Adaption, set ComponentState to "
					+ value + ".");
			setState(affectedComponent, value);
			break;
		case "addToShop":
			Shop shop = findShopByUid(value);
			// Add new Component to shop
			shop.getComponents().add(affectedComponent);
			wireRequiredInterfacesToComponent(shop, affectedComponent);
			// Start connected Component
			setState(affectedComponent, "STARTED");
			connectProvidedInterfacesToOtherComponents(shop, affectedComponent);

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
			ComponentType ct = findAlternativeComponentType(value);
			return instantiateComponentType(ct);
			break;

		case "instantiate and deploy":

			// Find Component Type and create new instance
			EList<ComponentType> types = mRubis.getComponentTypes();
			for (int i = 0; i < types.size(); i++) {
				ComponentType type = types.get(i);
				if (value.equals(type.getUid())) {
					comp = type.instantiate();
					comp.setState(ComponentLifeCycle.DEPLOYED);
				}
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

	private static void setState(Component affectedComponent, String value) {

		switch (value) {
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
			System.err.println("Undefined Value (" + value
					+ ") for action \"setState\". ");
			break;
		}
	}

	private static Shop findShopByUid(String shopUid) {
		EList<Shop> shops = mRubis.getShops();
		Shop shop = null;
		for (int i = 0; i < shops.size(); i++) {
			if (shopUid.equals(shops.get(i).getUid())) {
				shop = shops.get(i);
			}
		}
		return shop;
	}

	/**
	 * Looking for an alternative component type providing the same interface
	 * types, at the same time ignoring the exact same component type
	 * 
	 * @param ct
	 *            the component type to be replaced by an alternative
	 * @return the alternative component type, if one can be found, otherwise
	 *         null
	 */
	private static ComponentType findAlternativeComponentType(ComponentType ct) {
		for (ComponentType possibleAlternative : mRubis.getComponentTypes()) {
			if (isNotTheSameComponentType(ct, possibleAlternative)
					&& hasMatchingInterface(ct, possibleAlternative)) {
				return possibleAlternative;
			}
		}
		return null;
	}

	private static Component instantiateComponentType(ComponentType ct) {
		return ct.instantiate();
	}

	private static void deployComponent(Component comp) {
		comp.setState(ComponentLifeCycle.DEPLOYED);
	}

	private static void wireRequiredInterfacesToComponent(Shop shop,
			Component comp) {
		// Wire connectors
		// Get required interfaces to connect
		EList<RequiredInterface> requiredInterfaces = comp
				.getRequiredInterfaces();

		for (RequiredInterface requiredInterface : requiredInterfaces) {

			String requiredInterfaceUid = requiredInterface.getUid();
			String requiredInterfaceString = requiredInterfaceUid.split(Pattern
					.quote("_"))[1];
			System.out
					.println("Required Interface: " + requiredInterfaceString);

			// find matching interfaces
			EList<Component> components = shop.getComponents();
			for (Component currentComp : components) {

				if (requiredInterface.getConnector() == null) {
					EList<ProvidedInterface> interfaces = currentComp
							.getProvidedInterfaces();
					for (ProvidedInterface provInterface : interfaces) {
						String providedInterfaceUid = provInterface.getUid();
						String providedInterface = providedInterfaceUid
								.split(Pattern.quote("_"))[1];

						// TODO only connect to started components
						// interfaces
						if (providedInterface.equals(requiredInterfaceString)
								&& currentComp.getState().equals(
										ComponentLifeCycle.STARTED)) {
							System.out
									.println("Found matching Provided Interface: "
											+ providedInterface);
							Connector connector = ComparchFactory.eINSTANCE
									.createConnector();
							connector.setSource(requiredInterface);
							connector.setTarget(provInterface);
							requiredInterface.setConnector(connector);
						}
					}
				}
			}
		}
	}

	private static void connectProvidedInterfacesToOtherComponents(Shop shop,
			Component comp) {
		// TODO find connectors with target null in other components and
		// connect new provided interfaces if suitable
		EList<ProvidedInterface> providedInterfaces = comp
				.getProvidedInterfaces();

		for (ProvidedInterface providedInterface : providedInterfaces) {

			String providedInterfaceUid = providedInterface.getUid();

			String providedInterfaceString = providedInterfaceUid.split(Pattern
					.quote("_"))[1];
			System.out
					.println("Provided Interface: " + providedInterfaceString);

			// find matching required interfaces
			EList<Component> components = shop.getComponents();
			for (Component currentComp : components) {

				EList<RequiredInterface> interfaces = currentComp
						.getRequiredInterfaces();
				for (RequiredInterface reqInterface : interfaces) {

					// only rewire if needed

					// This condition is wrong! - Even if the required interface
					// has a connector and a target it needs to be rewired,
					// because its pointing to a removed
					// component!!!
					// if (reqInterface.getConnector() != null
					// && reqInterface.getConnector().getTarget() == null) {
					String requiredInterfaceUid = reqInterface.getUid();
					String requiredInterface = requiredInterfaceUid
							.split(Pattern.quote("_"))[1];

					if (requiredInterface.equals(providedInterfaceString)
							&& currentComp.getState().equals(
									ComponentLifeCycle.STARTED)) {
						System.out
								.println("Found matching Required Interface: "
										+ requiredInterface);

						Connector connector = ComparchFactory.eINSTANCE
								.createConnector();
						connector.setSource(reqInterface);
						connector.setTarget(providedInterface);

						// Remove obsolete connectors
						reqInterface.setConnector(connector);
					}
				}
			}
		}
		// }
	}

	private static boolean isNotTheSameComponentType(ComponentType type1,
			ComponentType type2) {
		return !type1.getUid().equals(type2.getUid());
	}

	private static boolean hasMatchingInterface(ComponentType type1,
			ComponentType type2) {
		return (type1.getProvidedInterfaceTypes().containsAll(type2
				.getProvidedInterfaceTypes()));
	}
}
