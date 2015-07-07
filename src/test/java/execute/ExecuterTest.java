/**
 * 
 */
package execute;

import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.Component;
import de.mdelab.morisia.comparch.ComponentLifeCycle;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.Shop;
import de.mdelab.morisia.comparch.impl.ComparchPackageImpl;
import util.ArchitectureHelper;
import util.XmlBuilder;

/**
 * @author lucieomar
 *
 */
public class ExecuterTest {

	private Architecture testArch;

	private static Architecture loadCompArchModel() {
		// Initialize the metamodel
		ComparchPackageImpl.init();
		// Register the XMI resource factory for the .comparch extension
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("comparch", new XMIResourceFactoryImpl());
		// create a resource set
		ResourceSet resourceSet = new ResourceSetImpl();
		// Load the resource
		Resource resource = resourceSet.getResource(URI.createURI("mRUBiS.comparch"), true);
		return (Architecture) resource.getContents().get(0);
	}

	@Before
	public void initArchModel() {
		testArch = loadCompArchModel();
	}

	@Test
	public void executerShouldRedeployAlternativeWhenReceivingCf4Plan() throws Exception {
		Executer.executeAdaption(testArch, Arrays.asList(XmlBuilder.prettyPrint(buildCF4Plan())));

		Shop shop = ArchitectureHelper.findShopByUid(testArch, "mRUBiS #1__avAcEOKWEeS1mZswSnXh1g");
		ComponentType replacementType = ArchitectureHelper.lookupComponentTypeByUid(testArch,
				"ComponentType_External Authentication Service_au8Ko-KWEeS1mZswSnXh1g");

		Component replacedComponent = null;
		for (Component c : shop.getComponents()) {
			if (c.getType().equals(replacementType) && c.getState() == ComponentLifeCycle.STARTED) {
				replacedComponent = c;
			}
		}

		System.out.println("Replaced component is: " + replacedComponent);
		Assert.assertNotNull("Replaced component may not be null", replacedComponent);
	}

	// TODO: Plan + executer
	private static Document buildCF4Plan() throws ParserConfigurationException {
		Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element baseElem = xml.createElement("plannedAdaption");
		xml.appendChild(baseElem);

		Element actionElement = xml.createElement("action");

		baseElem.appendChild(actionElement);

		Element actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "lookup alternative components");
		actionElement.appendChild(actionName);
		Element actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", "Component_Authentication Service_avPFm-KWEeS1mZswSnXh1g");
		actionElement.appendChild(actionValue);

		actionElement = xml.createElement("action");
		baseElem.appendChild(actionElement);
		actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "setState");
		actionElement.appendChild(actionName);
		actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", "DEPLOYED");
		actionElement.appendChild(actionValue);

		actionElement = xml.createElement("action");
		baseElem.appendChild(actionElement);
		actionName = xml.createElement("actionName");
		XmlBuilder.addAttribute(xml, actionName, "value", "addToShop");
		actionElement.appendChild(actionName);
		actionValue = xml.createElement("actionValue");
		XmlBuilder.addAttribute(xml, actionValue, "value", "mRUBiS #1__avAcEOKWEeS1mZswSnXh1g");
		actionElement.appendChild(actionValue);

		// TODO add more actions
		try {
			System.out.println(XmlBuilder.prettyPrint(xml));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xml;
	}

}
