package femproto.prepare.parsers;

import com.google.inject.Inject;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import femproto.globals.FEMGlobalConfig;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class EvacuationToSafeNodeMapping {
	private static final Logger log = Logger.getLogger(EvacuationToSafeNodeMapping.class);

	public Scenario getScenario() {
		return scenario;
	}

	private final Scenario scenario;

	public Map<String, Record> getSubsectorToEvacAndSafeNodes() {
		return subsectorToEvacAndSafeNodes;
	}

	private Map<String, Record> subsectorToEvacAndSafeNodes = new LinkedHashMap<>();

	public EvacuationToSafeNodeMapping(Scenario scenario) {
		this.scenario = scenario;
	}

	@Inject
	FEMGlobalConfig globalConfig;


	/**
	 * This looks at the mapping of evac to safe nodes, then creates a small disconnected network from the mapping,
	 * calls Links2ESRIShape to render it to a shapefile.
	 *
	 * @param fileName
	 */
	public void writeEvac2SafeNodeShapefile(String fileName) {
		Network network = NetworkUtils.createNetwork();
		for (Map.Entry<String, Record> stringRecordEntry : subsectorToEvacAndSafeNodes.entrySet()) {
			Node fromNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().EVAC_NODE));
			if (fromNode == null) {
				String msg = "did not find evacNode in matsim network file; node id  = " + stringRecordEntry.getValue().EVAC_NODE;
				log.warn(msg);
				throw new RuntimeException(msg);
			}
			//creates a new node in the small network
//			Node fromNodeAlt = network.getFactory().createNode(fromNode.getId(), fromNode.getCoord());
			network.addNode(fromNode);
			Node toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE1));
			Gbl.assertNotNull(toNode);
//			Node toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
			network.addNode(toNode);
			Link link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE1"), fromNode, toNode);
			network.addLink(link);

			if (stringRecordEntry.getValue().SAFE_NODE2 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE2));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE2"), fromNode, toNode);
				network.addLink(link);
			}
			if (stringRecordEntry.getValue().SAFE_NODE3 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE3));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE3"), fromNode, toNode);
				network.addLink(link);
			}
			if (stringRecordEntry.getValue().SAFE_NODE4 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE4));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE4"), fromNode, toNode);
				network.addLink(link);
			}

			if (stringRecordEntry.getValue().SAFE_NODE5 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE5));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE5"), fromNode, toNode);
				network.addLink(link);
			}
		}
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, globalConfig.getCrsEPSG28356());
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		CoordinateReferenceSystem crs = MGC.getCRS( globalConfig.getCrsEPSG28356());
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network, fileName, builder).write();
	}

	public void writeSubsectorAttributes(String fileName) {
		// yoyo writing out attributes to a separate file for diagnostics
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("subsector\tevacnode\tsafenode\ttraveltime\n");
			for (String subsector : subsectorToEvacAndSafeNodes.keySet()) {
				writer.write(subsector + "\t" +
						subsectorToEvacAndSafeNodes.get(subsector).EVAC_NODE + "\t" +
						subsectorToEvacAndSafeNodes.get(subsector).SAFE_NODE1 + "\t"/*+
						subSectorToPriorityNodeTime.get(subsector) +"\n"
				 */
				);
			}
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readEvacAndSafeNodes(String fileName) {
		log.info("entering readEvacAndSafeNodes with fileName=" + fileName);

		try (final FileReader reader = new FileReader(fileName)) {

			// construct the csv reader:
			final CsvToBeanBuilder<Record> builder = new CsvToBeanBuilder<>(reader);
			builder.withType(Record.class);
			builder.withSeparator(';');
			final CsvToBean<Record> reader2 = builder.build();

			// go through the records:
			for (Iterator<Record> it = reader2.iterator(); it.hasNext(); ) {
				Record record = it.next();
				log.info(record.toString());
				subsectorToEvacAndSafeNodes.put(record.SUBSECTOR.toString(), record);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	public List<Link> getSafeLinks(String subsector) {
		// get corresponding safe node record:
		Record record = subsectorToEvacAndSafeNodes.get(subsector);
		Gbl.assertNotNull(record);
		List<Link> safeLinks = new ArrayList<>();
		// find safeNode in network file:
		// yoyoyo?? currently we only use the ifrst of the safe nodes,
		// upodate 2018.3.7 access to these are cordoned off in order as the previous nodes become inaccesssible
		String defaultSafeNode = record.SAFE_NODE1;
		Link defaultLink = FEMUtils.getLinkFromSafeNode(defaultSafeNode, scenario );
		if (defaultLink != null)
			safeLinks.add(defaultLink);
		if (record.SAFE_NODE2 != null) {
			defaultLink = FEMUtils.getLinkFromSafeNode(record.SAFE_NODE2, scenario );
			if (defaultLink != null)
				safeLinks.add(defaultLink);
		}
		if (record.SAFE_NODE3 != null) {
			defaultLink = FEMUtils.getLinkFromSafeNode(record.SAFE_NODE3, scenario );
			if (defaultLink != null)
				safeLinks.add(defaultLink);
		}
		if (record.SAFE_NODE4 != null) {
			defaultLink = FEMUtils.getLinkFromSafeNode(record.SAFE_NODE4, scenario );
			if (defaultLink != null)
				safeLinks.add(defaultLink);
		}
		if (record.SAFE_NODE5 != null) {
			defaultLink = FEMUtils.getLinkFromSafeNode(record.SAFE_NODE5, scenario );
			if (defaultLink != null)
				safeLinks.add(defaultLink);
		}
		return safeLinks;
	}
	public List<Node> getSafeNodesByDecreasingPriority(String subsector){
		Record record = subsectorToEvacAndSafeNodes.get(subsector);
		Gbl.assertNotNull(record);
		List<Node> safeNodes = new ArrayList<>();
		safeNodes.add(this.scenario.getNetwork().getNodes().get(Id.createNodeId(record.SAFE_NODE1)));
		if (record.SAFE_NODE2 != null) {
			safeNodes.add(this.scenario.getNetwork().getNodes().get(Id.createNodeId(record.SAFE_NODE2)));
		}
		if (record.SAFE_NODE3 != null) {
			safeNodes.add(this.scenario.getNetwork().getNodes().get(Id.createNodeId(record.SAFE_NODE3)));
		}
		if (record.SAFE_NODE4 != null) {
			safeNodes.add(this.scenario.getNetwork().getNodes().get(Id.createNodeId(record.SAFE_NODE4)));
		}
		if (record.SAFE_NODE5 != null) {
			safeNodes.add(this.scenario.getNetwork().getNodes().get(Id.createNodeId(record.SAFE_NODE5)));
		}
		return safeNodes;
	}
	
	public Node getEvacNode(String subsector) {
		Node node = this.scenario.getNetwork().getNodes().get(Id.createNodeId(subsectorToEvacAndSafeNodes.get(subsector).EVAC_NODE));
		Gbl.assertNotNull(node);
		return node;
	}

	public final static class Record {
		// needs to be public, otherwise one gets some incomprehensible exception.  kai, nov'17

		@CsvBindByName
		private String SUBSECTOR;
		@CsvBindByName
		private String EVAC_NODE;
		@CsvBindByName
		private String SAFE_NODE1;
		@CsvBindByName
		private String SAFE_NODE2;
		@CsvBindByName
		private String SAFE_NODE3;
		@CsvBindByName
		private String SAFE_NODE4;
		@CsvBindByName
		private String SAFE_NODE5;

		@Override
		public String toString() {
			return this.SUBSECTOR
					+ "\t" + this.EVAC_NODE
					+ "\t" + this.SAFE_NODE1
					+ "\t" + this.SAFE_NODE2
					+ "\t" + this.SAFE_NODE3
					+ "\t" + this.SAFE_NODE4
					+ "\t" + this.SAFE_NODE5
					;
		}
	}
}
