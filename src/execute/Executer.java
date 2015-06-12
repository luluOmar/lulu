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
import de.mdelab.morisia.comparch.Shop;
import de.mdelab.morisia.comparch.impl.ComponentImpl;

public final class Executer {

	public static void executeAdaption(Architecture mRubis,
			List<String> xmlPlannedAdaption)
			throws ParserConfigurationException, SAXException, IOException {

		EList<Shop> shops = mRubis.getShops();

		// Get document out of analyzed events XML
		List<Document> plannedAdaption = new LinkedList<Document>();
		for (String xmlString : xmlPlannedAdaption) {
			plannedAdaption.add(XmlParser.createDocByString(xmlString));
		}

		for (Document adaption : plannedAdaption) {
			// Select affected shop and component
			Shop affectedShop = null;
			for (Shop shop : shops) {
				if (shop.getName().equals(
						XmlParser.getElementsValue(adaption, "shop", "value"))) {
					affectedShop = shop;
				}
			}
			EList<Component> components = affectedShop.getComponents();
			Component affectedComponent = null;
			for (Component component : components) {
				if (component.getUid().equals(
						XmlParser.getElementsValue(adaption, "uid", "value"))) {
					affectedComponent = component;
				}
			}
			// Execute adaption strategy, take action!
			NodeList elementNodes = adaption.getElementsByTagName("action");
			for (int i = 0; i < elementNodes.getLength(); i++) {
				// TODO get child node for actionName and actionValue
				Node currentNode = elementNodes.item(i);
				// if(currentNode instanceof Element) {
				// Element docElement = (Element)currentNode;
				// System.out.println(docElement.getElementsByTagName("actionName").item(0).getNodeValue());
				// }
			}
			// if (XmlParser.getElementsValue(adaption, "uid", "value")))
			// affectedComponent.setState(ComponentLifeCycle.UNDEPLOYED);

			affectedComponent.setState(ComponentLifeCycle.UNDEPLOYED);
			affectedComponent.setState(ComponentLifeCycle.DEPLOYED);
			affectedComponent.setState(ComponentLifeCycle.STARTED);
		}
	}
}
