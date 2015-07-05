package execute;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
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
		LinkedList<Component> affectedFilters = new LinkedList<Component>();
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
		switch (name) {
		case "setState":
			System.out.println("Execute Adaption, set ComponentState to "
					+ value + ".");
			setState(affectedComponent, value);
			break;
		case "addToShop":
			EList<Shop> shops = mRubis.getShops();
			Shop shop = null;
			for (int i = 0; i < shops.size(); i++) {
				if (value.equals(shops.get(i).getUid())) {
					shop = shops.get(i);
				}
			}

			// Add new Component to shop
			shop.getComponents().add(affectedComponent);

			// Wire connectors
			// Get required interfaces to connect
			EList<RequiredInterface> requiredInterfaces = affectedComponent
					.getRequiredInterfaces();

			for (RequiredInterface requiredInterface : requiredInterfaces) {

				String requiredInterfaceUid = requiredInterface.getUid();

				String requiredInterfaceString = requiredInterfaceUid
						.split(Pattern.quote("_"))[1];
				System.out.println("Required Interface: "
						+ requiredInterfaceString);

				// find matching interfaces
				EList<Component> components = shop.getComponents();
				for (Component currentComp : components) {

					if (requiredInterface.getConnector() == null) {
						EList<ProvidedInterface> interfaces = currentComp
								.getProvidedInterfaces();
						for (ProvidedInterface provInterface : interfaces) {
							String providedInterfaceUid = provInterface
									.getUid();
							String providedInterface = providedInterfaceUid
									.split(Pattern.quote("_"))[1];

							// TODO only connect to started components
							// interfaces
							if (providedInterface
									.equals(requiredInterfaceString)) {
								System.out
										.println("Found matching Provided Interface: "
												+ providedInterface);
								Connector connector = ComparchFactory.eINSTANCE
										.createConnector();
								connector.setSource(requiredInterface);
								connector.setTarget(provInterface);
							}
						}
					}
				}
			}

			// TODO find connectors with target null in other components and
			// connect new provided interfaces if suitable
			EList<ProvidedInterface> providedInterfaces = affectedComponent
					.getProvidedInterfaces();

			for (ProvidedInterface providedInterface : providedInterfaces) {

				String providedInterfaceUid = providedInterface.getUid();

				String providedInterfaceString = providedInterfaceUid
						.split(Pattern.quote("_"))[1];
				System.out.println("Provided Interface: "
						+ providedInterfaceString);

				// find matching required interfaces
				EList<Component> components = shop.getComponents();
				for (Component currentComp : components) {

					EList<RequiredInterface> interfaces = currentComp
							.getRequiredInterfaces();
					for (RequiredInterface reqInterface : interfaces) {

						// only rewire if needed
						if (reqInterface.getConnector() != null
								&& reqInterface.getConnector().getTarget() == null) {
							String requiredInterfaceUid = reqInterface.getUid();
							String requiredInterface = requiredInterfaceUid
									.split(Pattern.quote("_"))[1];

							// TODO only connect to started components
							// interfaces
							if (requiredInterface
									.equals(providedInterfaceString)) {
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
			}

			// Start connected Component
			setState(affectedComponent, "STARTED");
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
			ComponentType neededCt = null;
			ComponentType alternateCt = null;
			for (ComponentType ct : mRubis.getComponentTypes()) {
				if (ct.getUid().equals(value)) {
					neededCt = ct;
					break;
				}
			}
			EList<InterfaceType> iTypes = neededCt.getRequiredInterfaceTypes();
			iTypes.addAll(neededCt.getProvidedInterfaceTypes());

			for (ComponentType ct : mRubis.getComponentTypes()) {
				if (ct.getUid() != value) {
					EList<InterfaceType> iTypes2 = ct
							.getProvidedInterfaceTypes();
					iTypes2.addAll(ct.getRequiredInterfaceTypes());
					if (iTypes.containsAll(iTypes2)) {
						alternateCt = ct;
					}
				}
			}

			// TODO if no alternative is found jump to redeployment (AS-3) of
			// the old component (including stopping, removal, etc. of old
			// component)

			// create the alternative component instead of the old one
			comp = alternateCt.instantiate();

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
}
