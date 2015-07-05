/**
 * 
 */
package execute;

import java.util.Arrays;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import analyze.Analyzer.CFType;
import analyze.Analyzer.CriticalFailure;
import analyze.Analyzer.CriticalFailureBuilder;
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.impl.ComparchPackageImpl;
import de.mdelab.morisia.comparch.impl.ComponentTypeImpl;

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
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
				"comparch", new XMIResourceFactoryImpl());
		// create a resource set
		ResourceSet resourceSet = new ResourceSetImpl();
		// Load the resource
		Resource resource = resourceSet.getResource(
				URI.createURI("mRUBiS.comparch"), true);
		return (Architecture) resource.getContents().get(0);
	}

	@Before
	public void initArchModel() {
		testArch = loadCompArchModel();
	}

	@Test
	public void executerShouldRedeployAlternativeWhenReceivingCf4Plan()
			throws Exception {
		CriticalFailure cf4 = new CriticalFailureBuilder()
				.setCfType(CFType.CF4).setComponentUid("123")
				.setComponentTypeUid("123").setShopUid("123").build();
		Executer.executeAdaption(testArch, Arrays.asList(cf4.getFailureEvent()));

		for (ComponentType it : testArch.getComponentTypes()) {
			if (it.getUid()
					.equals("ComponentType_External Authentication Service_au8Ko-KWEeS1mZswSnXh1g")) {
				Assert.assertTrue(it.getInstances().size() == 1);
			}
		}
	}

}
