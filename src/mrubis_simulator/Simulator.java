package mrubis_simulator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.impl.ComparchPackageImpl;
import de.mdelab.mrubis.simulator.MRubisSimulatorFactory;
import de.mdelab.mrubis.simulator.MRubisSimulatorTest;

public class Simulator {
	
	public static void main(String[] args) {
		
		// flags inidcating which capabilities the simulator should support
		boolean withSelfHealing = true;
		boolean withSelfOptimization = true;
		// TODO load the CompArch model
		Architecture mRubis = loadCompArchModel();
		// obtain an instance of the simulator
		MRubisSimulatorTest simulatorTest = MRubisSimulatorFactory.instance
		.createSimulatorTest(mRubis, withSelfHealing, withSelfOptimization);
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 1. Run of the Feedback Loop ===================
		// inject a failure (CF-1)
		simulatorTest.changeComponentState();
		// TODO run your feedback loop ...
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 2. Run of the Feedback Loop ===================
		// inject a failure (CF-2)
		simulatorTest.attachFailures(10);
		// TODO run your feedback loop ...
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 3. Run of the Feedback Loop ===================
		// inject a failure (CF-3)
		simulatorTest.removeComponent();
		// TODO run your feedback loop ...
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 4. Run of the Feedback Loop ===================
		// inject a performance issue (PI-1)
		simulatorTest.changeFilterCharacteristics();
		// TODO run your feedback loop ...
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 5. Run of the Feedback Loop ===================
		
		// inject a performance issue (PI-2)
		simulatorTest.changeResponseTime(1200);
		// TODO run your feedback loop ...
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 6. Run of the Feedback Loop ===================
		// inject a performance issue (PI-3)
		simulatorTest.changeResponseTime(800);
		// TODO run your feedback loop ...
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
	}
	
	private static Architecture loadCompArchModel() {
		// Initialize the metamodel
		ComparchPackageImpl.init();
		// Register the XMI resource factory for the .comparch extension
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
		"comparch", new XMIResourceFactoryImpl());
		// create a resource set
		ResourceSet resourceSet = new ResourceSetImpl();
		// Load the resource
		Resource resource = resourceSet.getResource(URI.createURI("mRUBiS.comparch"), true);
		return (Architecture) resource.getContents().get(0);
	}
	
}
