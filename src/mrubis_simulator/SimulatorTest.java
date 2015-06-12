package mrubis_simulator;

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.ComponentLifeCycle;
import de.mdelab.morisia.comparch.MonitoredProperty;
import de.mdelab.morisia.comparch.impl.ComparchPackageImpl;
import de.mdelab.morisia.comparch.impl.ComponentImpl;
import de.mdelab.mrubis.simulator.MRubisSimulatorFactory;
import de.mdelab.mrubis.simulator.MRubisSimulatorTest;

public class SimulatorTest {

	public static void main(String[] args) {

		Queue<Notification> notifications = new LinkedList<Notification>();

		// flags inidcating which capabilities the simulator should support
		boolean withSelfHealing = true;
		boolean withSelfOptimization = false;
		// TODO load the CompArch model
		Architecture mRubis = loadCompArchModel();

		EContentAdapter adapter = new EContentAdapter() {
			public void notifyChanged(Notification notification) {
				super.notifyChanged(notification);
				// TODO check in Analyze if event has to be handled
				System.out.println(notification.getNewStringValue());
				notifications.add(notification);
				// while ( !notifications.isEmpty() ) {
				// Notification notification = notifications.poll();
				// ComponentImpl notifyingComponent = (ComponentImpl)
				// notification.getNotifier();
				// notifyingComponent.setState(ComponentLifeCycle.STARTED);
				// }
			}
		};
		mRubis.eAdapters().add(adapter);

		// obtain an instance of the simulator
		MRubisSimulatorTest simulatorTest = MRubisSimulatorFactory.instance
				.createSimulatorTest(mRubis, withSelfHealing,
						withSelfOptimization);
		// analyze the model
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 1. Run of the Feedback Loop ===================
		// inject a failure (CF-1)
		simulatorTest.changeComponentState();
		// TODO run your feedback loop ...
		while (!notifications.isEmpty()) {
			Notification notification = notifications.poll();
			if (notification.getNewStringValue() == "NOT_SUPPORTED") {
				System.out.println("Notification queued");
				ComponentImpl notifyingComponent = (ComponentImpl) notification
						.getNotifier();
				notifyingComponent.setState(ComponentLifeCycle.UNDEPLOYED);
				notifyingComponent.setState(ComponentLifeCycle.DEPLOYED);
				notifyingComponent.setState(ComponentLifeCycle.STARTED);					
			} else {
				System.out.println("Notification NOT queued");
			}
		}
		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 2. Run of the Feedback Loop ===================
		// inject a failure (CF-2)
		// simulatorTest.attachFailures(10);
		// TODO run your feedback loop ...
		// analyze the model
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 3. Run of the Feedback Loop ===================
		// inject a failure (CF-3)
		// simulatorTest.removeComponent();
		// TODO run your feedback loop ...
		// analyze the model
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 4. Run of the Feedback Loop ===================
		// inject a performance issue (PI-1)
		// simulatorTest.changeFilterCharacteristics();
		// TODO run your feedback loop ...
		// analyze the model
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 5. Run of the Feedback Loop ===================

		// inject a performance issue (PI-2)
		// simulatorTest.changeResponseTime(1200);
		// TODO run your feedback loop ...
		// analyze the model
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());
		// =================== 6. Run of the Feedback Loop ===================
		// inject a performance issue (PI-3)
		// simulatorTest.changeResponseTime(800);
		// TODO run your feedback loop ...
		// analyze the model
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());
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
		Resource resource = resourceSet.getResource(
				URI.createURI("mRUBiS.comparch"), true);
		return (Architecture) resource.getContents().get(0);
	}

}
