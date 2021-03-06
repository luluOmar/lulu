package mrubis_simulator;

import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.parsers.ParserConfigurationException;

import monitor.Monitorer;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.xml.sax.SAXException;

import plan.Planer;
import analyze.Analyzer;
import de.mdelab.morisia.comparch.Architecture;
import de.mdelab.morisia.comparch.ComponentType;
import de.mdelab.morisia.comparch.InterfaceType;
import de.mdelab.morisia.comparch.impl.ComparchPackageImpl;
import de.mdelab.mrubis.simulator.MRubisSimulator;
import de.mdelab.mrubis.simulator.MRubisSimulatorException;
import de.mdelab.mrubis.simulator.MRubisSimulatorFactory;
import de.mdelab.mrubis.simulator.MRubisSimulatorTest;
import execute.Executer;

public class Simulator {

	private Architecture mRubis;
	private Queue queue = new Queue();
	private MRubisSimulatorTest simulatorTest;
	private MRubisSimulator simulator;

	public Simulator() throws ParserConfigurationException {
		this.mRubis = loadCompArchModel();
	}

	private void run(boolean withSelfHealing, boolean withSelfOptimization)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {

		// obtain an instance of the simulator
		simulator = MRubisSimulatorFactory.instance.createSimulator(mRubis,
				withSelfHealing, withSelfOptimization);
		EContentAdapter adapter = new EContentAdapter() {
			public void notifyChanged(Notification notification) {
				super.notifyChanged(notification);
				queue.add(notification);
			}
		};
		mRubis.eAdapters().add(adapter);
		// run the adaptation engine while the simulation is not completed
		while (!simulator.simulationCompleted()) {
			try {
				// let the simulator validate the adaptation and the model and
				// then
				// inject critcal failures or performance issues into the model
				simulator.validate();
				mape();
			} catch (MRubisSimulatorException rse) {
				break;
			}
		}
		System.out.println("Simulator finished.");
	}

	private void simulate(boolean withSelfHealing, boolean withSelfOptimization)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {

		configureLogging();

		// load the CompArch model
		mRubis = loadCompArchModel();
		EContentAdapter adapter = new EContentAdapter() {
			public void notifyChanged(Notification notification) {
				super.notifyChanged(notification);
				queue.add(notification);
//				System.out.println(notification);
			}
		};
		mRubis.eAdapters().add(adapter);

		// obtain an instance of the simulator
		simulatorTest = MRubisSimulatorFactory.instance.createSimulatorTest(
				mRubis, withSelfHealing, withSelfOptimization);
		// analyze the model
		// System.out.println("Initial Check");
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());

//		// =================== 1. Run of the Feedback Loop ===================
//		// inject a failure (CF-1)
//		System.out.println("Inject CF-1");
//		simulatorTest.changeComponentState();
//		// run feedback loop
//		mape();
//
//		// analyze the model after feedback loop
//		System.out.println(simulatorTest.analyzeAdaptationAndModel());
//
//		// // =================== 2. Run of the Feedback Loop
//		// ===================
//		// inject a failure (CF-2)
//		simulatorTest.attachFailures(10);
//		mape();
//		// TODO run your feedback loop ...
//		// analyze the model
//		System.out.println(simulatorTest.analyzeAdaptationAndModel());
//		//
//		// // =================== 3. Run of the Feedback Loop
//		// ===================
//		// inject a failure (CF-3)
//		System.out.println("Inject CF-3");
//		simulatorTest.removeComponent();
//		mape();
//
		// // =================== 4. Run of the Feedback Loop
		// ===================
		// // inject a performance issue (PI-1)
		 simulatorTest.changeFilterCharacteristics();
		 mape();
		 // TODO run your feedback loop ...
		// // analyze the model
		 System.out.println(simulatorTest.analyzeAdaptationAndModel());

		// // =================== 5. Run of the Feedback Loop
		// ===================
		//
//		// // inject a performance issue (PI-2)
//		 simulatorTest.changeResponseTime(1200);
//		 mape();
		// // TODO run your feedback loop ...
		// // analyze the model
		// System.out.println(simulatorTest.analyzeAdaptationAndModel());

//		// // =================== 6. Run of the Feedback Loop
//		// ===================
//		// // inject a performance issue (PI-3)
//		 simulatorTest.changeResponseTime(800);
//		 mape();
//		// TODO run your feedback loop ...
//		// analyze the model
		System.out.println(simulatorTest.analyzeAdaptationAndModel());

	}

	private void mape() throws ParserConfigurationException, SAXException,
			IOException, Exception {
		// simulatorTest.analyzeAdaptationAndModel();
		List<String> monitoredEvents = Monitorer.monitorModel(queue, mRubis);
		List<String> analyzedEvents = Analyzer.activate(monitoredEvents, queue);
		List<String> plannedAdaption = Planer.planAdaption(analyzedEvents);
		Executer.executeAdaption(mRubis, plannedAdaption);
		// simulatorTest.analyzeAdaptationAndModel();
		queue.initNewLoop();
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

	private static void configureLogging() {
		// get the root logger
		Logger root = Logger.getLogger("");
		// suppress the logging output to the console
		root.removeHandler(root.getHandlers()[0]);
		// set the log level
		root.setLevel(Level.INFO);
		try {
			// Log to a file
			FileHandler fileTxt = new FileHandler("Logging.log");
			// create a txt formatter and define how logs are printed
			Formatter formatterTxt = new SimpleFormatter();
			formatterTxt = new java.util.logging.Formatter() {
				@Override
				public String format(LogRecord record) {
					return record.getLevel() + " " + record.getMessage() + "\n";
				}
			};
			fileTxt.setFormatter(formatterTxt);
			root.addHandler(fileTxt);
		} catch (SecurityException e) {
		} catch (IOException e) {
		}
	}

	public static void main(String[] args) throws ParserConfigurationException,
			Exception {
		Simulator simulator = new Simulator();
		try {
//			simulator.simulate(false, true);
			simulator.run(true, false);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
	}

}
