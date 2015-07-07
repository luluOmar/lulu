package util;

import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;

import de.mdelab.morisia.comparch.ArchitecturalElement;
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.ComparchFactory;
import de.mdelab.morisia.comparch.Component;
import de.mdelab.morisia.comparch.ComponentLifeCycle;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.Connector;
import de.mdelab.morisia.comparch.ProvidedInterface;
import de.mdelab.morisia.comparch.RequiredInterface;
import de.mdelab.morisia.comparch.Shop;

public class ArchitectureHelper {

	public static void removeComponentFromShop(Component component) {
		if (component.getState() == ComponentLifeCycle.STARTED) {
			component.setState(ComponentLifeCycle.DEPLOYED);
		}
		component.setState(ComponentLifeCycle.UNDEPLOYED);
		Shop shop = component.getShop();
		shop.getComponents().remove(component);
	}

	/**
	 * 
	 * @param architecture
	 * @param componentUid
	 * @return
	 */
	public static Component lookupComponentInShops(Architecture architecture, String componentUid) {
		Component c;
		for (Shop shop : architecture.getShops()) {
			c = lookupShopComponentByUid(shop, componentUid);
			if (c != null) {
				return c;
			}
		}
		return null;
	}

	public static Component lookupShopComponentByUid(Shop shop, String uid) {
		return lookupArchitecturalElementByUid(shop.getComponents(), uid);
	}

	public static ComponentType lookupComponentTypeByUid(Architecture architecture, String uid) {
		return lookupArchitecturalElementByUid(architecture.getComponentTypes(), uid);
	}

	public static Shop findShopByUid(Architecture architecture, String shopUid) {
		return lookupArchitecturalElementByUid(architecture.getShops(), shopUid);
	}

	private static <T extends ArchitecturalElement> T lookupArchitecturalElementByUid(EList<T> elements, String uid) {
		for (T elem : elements) {
			if (elem.getUid().equals(uid)) {
				return elem;
			}
		}
		return null;
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
	public static ComponentType findAlternativeComponentType(Architecture architecture, ComponentType ct) {
		for (ComponentType possibleAlternative : architecture.getComponentTypes()) {
			if (isNotTheSameComponentType(ct, possibleAlternative) && hasMatchingInterface(ct, possibleAlternative)) {
				return possibleAlternative;
			}
		}
		return null;
	}

	private static boolean isNotTheSameComponentType(ComponentType type1, ComponentType type2) {
		return !type1.getUid().equals(type2.getUid());
	}

	private static boolean hasMatchingInterface(ComponentType type1, ComponentType type2) {
		return (type1.getProvidedInterfaceTypes().containsAll(type2.getProvidedInterfaceTypes()));
	}

	public static void wireRequiredInterfacesToComponent(Shop shop, Component comp) {
		// Wire connectors
		// Get required interfaces to connect
		EList<RequiredInterface> requiredInterfaces = comp.getRequiredInterfaces();

		for (RequiredInterface requiredInterface : requiredInterfaces) {

			String requiredInterfaceUid = requiredInterface.getUid();
			String requiredInterfaceString = requiredInterfaceUid.split(Pattern.quote("_"))[1];
			System.out.println("Required Interface: " + requiredInterfaceString);

			// find matching interfaces
			EList<Component> components = shop.getComponents();
			for (Component currentComp : components) {

				if (requiredInterface.getConnector() == null) {
					EList<ProvidedInterface> interfaces = currentComp.getProvidedInterfaces();
					for (ProvidedInterface provInterface : interfaces) {
						String providedInterfaceUid = provInterface.getUid();
						String providedInterface = providedInterfaceUid.split(Pattern.quote("_"))[1];

						// TODO only connect to started components
						// interfaces
						if (providedInterface.equals(requiredInterfaceString)
								&& currentComp.getState().equals(ComponentLifeCycle.STARTED)) {
							System.out.println("Found matching Provided Interface: " + providedInterface);
							Connector connector = ComparchFactory.eINSTANCE.createConnector();
							connector.setSource(requiredInterface);
							connector.setTarget(provInterface);
							requiredInterface.setConnector(connector);
						}
					}
				}
			}
		}
	}

	public static void connectProvidedInterfacesToOtherComponents(Shop shop, Component comp) {
		// TODO find connectors with target null in other components and
		// connect new provided interfaces if suitable
		EList<ProvidedInterface> providedInterfaces = comp.getProvidedInterfaces();

		for (ProvidedInterface providedInterface : providedInterfaces) {

			String providedInterfaceUid = providedInterface.getUid();

			String providedInterfaceString = providedInterfaceUid.split(Pattern.quote("_"))[1];
			System.out.println("Provided Interface: " + providedInterfaceString);

			// find matching required interfaces
			EList<Component> components = shop.getComponents();
			for (Component currentComp : components) {

				EList<RequiredInterface> interfaces = currentComp.getRequiredInterfaces();
				for (RequiredInterface reqInterface : interfaces) {

					// only rewire if needed

					// This condition is wrong! - Even if the required interface
					// has a connector and a target it needs to be rewired,
					// because its pointing to a removed
					// component!!!
					// if (reqInterface.getConnector() != null
					// && reqInterface.getConnector().getTarget() == null) {
					String requiredInterfaceUid = reqInterface.getUid();
					String requiredInterface = requiredInterfaceUid.split(Pattern.quote("_"))[1];

					if (requiredInterface.equals(providedInterfaceString)
							&& currentComp.getState().equals(ComponentLifeCycle.STARTED)) {
						System.out.println("Found matching Required Interface: " + requiredInterface);

						Connector connector = ComparchFactory.eINSTANCE.createConnector();
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

}
