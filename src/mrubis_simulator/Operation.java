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
import de.mdelab.morisia.comparch.impl.ComparchPackageImpl;
import de.mdelab.morisia.comparch.impl.ComponentImpl;
import de.mdelab.mrubis.simulator.MRubisSimulator;
import de.mdelab.mrubis.simulator.MRubisSimulatorException;
import de.mdelab.mrubis.simulator.MRubisSimulatorFactory;

public class Operation {

	public static void main(String[] args) {

		Queue<Notification> notifications = new LinkedList<Notification>();

		boolean withSelfHealing = true;
		boolean withSelfOptimization = true;

		Architecture mRubis = loadCompArchModel();

		EContentAdapter adapter = new EContentAdapter() {
			public void notifyChanged(Notification notification) {
				super.notifyChanged(notification);
				System.out.println(notification.getNewStringValue());
				notifications.add(notification);
			}
		};
		mRubis.eAdapters().add(adapter);

		MRubisSimulator simulator = MRubisSimulatorFactory.instance
				.createSimulator(mRubis, withSelfHealing, withSelfOptimization);

		while (!simulator.simulationCompleted()) {
			try {
				// let the simulator validate the adaptation and the model and
				// then
				// inject critcal failures or performance issues into the model
				simulator.validate();
			} catch (MRubisSimulatorException rse) {
				break;
			}
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
		}
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
