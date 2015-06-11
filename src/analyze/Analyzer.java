package analyze;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import util.XmlParser;

public class Analyzer {

	private LinkedList<Document> receivedEvents = new LinkedList<Document>();
	private LinkedList<String> analyzedEvents = new LinkedList<String>();
	
	public List<String> activate(List<String> inputStrings) throws ParserConfigurationException, SAXException, IOException {

		receivedEvents = new LinkedList<Document>(); //TODO is it right to init first?
		for(String xmlString : inputStrings) {
			receivedEvents.add(XmlParser.createDocByString(xmlString));
		}
		for(Document event : receivedEvents) {
			System.out.println("Uid: " + XmlParser.getElementsValue(event, "uid", "value"));
		}
		
		//TODO analyze received events
		return analyzedEvents;
	}
}
