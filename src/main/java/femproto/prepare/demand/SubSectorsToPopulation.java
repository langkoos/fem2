package femproto.prepare.demand;

import femproto.globals.FEMAttributes;
import femproto.globals.Gis;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SubSectorsToPopulation {
	private static final Logger log = Logger.getLogger(SubSectorsToPopulation.class) ;
	
	Scenario scenario;
	
	private SubSectorsToPopulation() {
//		log.setLevel(Level.DEBUG);
		this.scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
	}
	
	public static void main(String[] args) throws IOException {
		SubSectorsToPopulation subSectorsToPopulation = new SubSectorsToPopulation();
		subSectorsToPopulation.readNetwork(args[1]);
		subSectorsToPopulation.readEvacAndSafeNodes(args[2]);
		subSectorsToPopulation.readSubSectorsShapeFile(args[0]);
		subSectorsToPopulation.writePopulation(args[3]);
		subSectorsToPopulation.writeEvac2SafeNodeShapefile(args[4]);
		// TODO if we really want to leave it like this, then put in a more expressive
		// command passing syntax (see bdi-abm-integration project).  kai, feb'18
	}
	
	private void readNetwork(String fileName) {
		log.info( "entering readNetwork with fileName=" + fileName ) ;
		new MatsimNetworkReader(scenario.getNetwork()).readFile(fileName);
	}
	
	private void readSubSectorsShapeFile(String fileName) throws IOException {
		log.info( "entering readSubSectorsShapeFile with fileName=" + fileName ) ;
		
		// population factory:
		PopulationFactory pf = scenario.getPopulation().getFactory();
		
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
		
		// coordinate transformation:
		String wkt = IOUtils.getBufferedReader(fileName.replaceAll("shp$", "prj")).readLine().toString();
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(wkt, Gis.EPSG28356);
		
		//iterate through features and generate pax by subsector
		Iterator<SimpleFeature> iterator = features.iterator();
		long id=0L;
		while (iterator.hasNext()){
			SimpleFeature feature = iterator.next();
			
			// get subsector name from shp file
			String subsector = feature.getAttribute("Subsector").toString();
			
			// get corresponding safe node record:
			Record record = subsectorToEvacAndSafeNodes.get(subsector);;
			Gbl.assertNotNull(record);
			
			// test that evac nodes are same in both files:
			String evacNodeFromShp = feature.getAttribute("EVAC_NODE").toString();
			String evacNodeFromCorrespondancesFile = record.EVAC_NODE.toString() ;
			if ( ! ( evacNodeFromShp.equals(evacNodeFromCorrespondancesFile) ) ) {
				final String msg = "evacNodes in shape file and in correspondances file not same: evacNodeFromShp="
						+ evacNodeFromShp + "; evacNodeFromCorrespondances=" + evacNodeFromCorrespondancesFile ;
				log.error(msg) ;
//				throw new RuntimeException(msg) ;
			}
			
			Link startLink = null;
			{
				// find evacNode in network file:
				Node node = this.scenario.getNetwork().getNodes().get(Id.createNodeId(evacNodeFromCorrespondancesFile));
				if ( node==null ) {
					String msg = "did not find evacNode in matsim network file; evacNodeFromCorrespondancesFile=" + evacNodeFromCorrespondancesFile ;
					log.warn(msg) ;
					throw new RuntimeException(msg) ;
				}
				
				// find some incoming link:
				// yyyyyy??
				double maxCap = Double.NEGATIVE_INFINITY;
				for (Link link : node.getInLinks().values()) {
					if ( link.getAllowedModes().contains( TransportMode.car) && link.getCapacity() > maxCap) {
						maxCap = link.getCapacity();
						startLink = link;
					}
				}
			}
			List<Link> safeLinks = new ArrayList<>() ;
			{
				// find safeNode in network file:
				// yoyoyo?? currently we only use the ifrst of the safe nodes,
				// upodate 2018.3.7 access to these are cordoned off in order as the previous nodes become inaccesssible
				String defaultSafeNode = record.SAFE_NODE1;
				Link defaultLink = getLinkFromSafeNode(defaultSafeNode);
				if(defaultLink != null)
					safeLinks.add(defaultLink);
				if(record.SAFE_NODE2 != null){
					defaultLink = getLinkFromSafeNode(record.SAFE_NODE2);
					if(defaultLink != null)
						safeLinks.add(defaultLink);
				}
				if(record.SAFE_NODE3 != null){
					defaultLink = getLinkFromSafeNode(record.SAFE_NODE3);
					if(defaultLink != null)
						safeLinks.add(defaultLink);
				}
				if(record.SAFE_NODE4 != null){
					defaultLink = getLinkFromSafeNode(record.SAFE_NODE4);
					if(defaultLink != null)
						safeLinks.add(defaultLink);
				}
				if(record.SAFE_NODE5 != null){
					defaultLink = getLinkFromSafeNode(record.SAFE_NODE5);
					if(defaultLink != null)
						safeLinks.add(defaultLink);
				}

			}


			int totalVehicles = (int) (double) feature.getAttribute("Totalvehic");
			for (int i = 0; i < totalVehicles; i++) {
				Person person = pf.createPerson(Id.createPersonId(id++));
				person.getAttributes().putAttribute(FEMAttributes.SUBSECTOR, subsector);
				for (Link safeLink : safeLinks) {


					Plan plan = pf.createPlan();

					Activity startAct = pf.createActivityFromLinkId("evac", startLink.getId());
					startAct.setEndTime(0); // yyyyyy ????
					plan.addActivity(startAct);

					Leg evacLeg = pf.createLeg(TransportMode.car);
					plan.addLeg(evacLeg);

					Activity safe = pf.createActivityFromLinkId("safe", safeLink.getId());
					plan.addActivity(safe);

					person.getAttributes().putAttribute(FEMAttributes.SAFE_NODE_1, record.SAFE_NODE1);
					if (record.SAFE_NODE2 != null)
						person.getAttributes().putAttribute(FEMAttributes.SAFE_NODE_2, record.SAFE_NODE2);
					if (record.SAFE_NODE3 != null)
						person.getAttributes().putAttribute(FEMAttributes.SAFE_NODE_3, record.SAFE_NODE3);
					if (record.SAFE_NODE4 != null)
						person.getAttributes().putAttribute(FEMAttributes.SAFE_NODE_4, record.SAFE_NODE4);
					if (record.SAFE_NODE5 != null)
						person.getAttributes().putAttribute(FEMAttributes.SAFE_NODE_5, record.SAFE_NODE5);

					person.addPlan(plan);
				}

				scenario.getPopulation().addPerson(person);
			}
		}
		
	}

	private Link getLinkFromSafeNode(String defaultSafeNode) {
		Link endLink = null;
		Node node = this.scenario.getNetwork().getNodes().get(Id.createNodeId(defaultSafeNode));
		Gbl.assertNotNull(node);

		// find some outgoing link:
		// yyyyyy??
		double maxCap = Double.NEGATIVE_INFINITY;
		for (Link link : node.getOutLinks().values()) {
			if (link.getAllowedModes().contains( TransportMode.car) && link.getCapacity() > maxCap) {
				maxCap = link.getCapacity();
				endLink = link;
			}
		}
		return endLink;
	}

	Map<String,Record> subsectorToEvacAndSafeNodes = new LinkedHashMap<>() ;
	
	private void readEvacAndSafeNodes(String fileName) {
		log.info( "entering readEvacAndSafeNodes with fileName=" + fileName ) ;

		try (final FileReader reader = new FileReader(fileName)) {
			
			// construct the csv reader:
			final CsvToBeanBuilder<Record> builder = new CsvToBeanBuilder<>(reader);
			builder.withType(Record.class);
			builder.withSeparator(';');
			final CsvToBean<Record> reader2 = builder.build();
			
			// go through the records:
			for (Iterator<Record> it = reader2.iterator(); it.hasNext(); ) {
				Record record = it.next();
				log.info( record.toString() ) ;
				subsectorToEvacAndSafeNodes.put(record.SUBSECTOR.toString(), record);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * This looks at the mapping of evac to safe nodes, then creates a small disconnected network from the mapping,
	 * calls Links2ESRIShape to render it to a shapefile.
	 * @param fileName
	 */
	public void writeEvac2SafeNodeShapefile(String fileName){
		Network network = NetworkUtils.createNetwork();
		for (Map.Entry<String, Record> stringRecordEntry : subsectorToEvacAndSafeNodes.entrySet()) {
			Node fromNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().EVAC_NODE));
			if ( fromNode==null ) {
				String msg = "did not find evacNode in matsim network file; node id  = "+ stringRecordEntry.getValue().EVAC_NODE;
				log.warn(msg) ;
				throw new RuntimeException(msg) ;
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

			if(stringRecordEntry.getValue().SAFE_NODE2 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE2));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE2"), fromNode, toNode);
				network.addLink(link);
			}
			if(stringRecordEntry.getValue().SAFE_NODE3 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE3));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE3"), fromNode, toNode);
				network.addLink(link);
			}
			if(stringRecordEntry.getValue().SAFE_NODE4 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE4));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE4"), fromNode, toNode);
				network.addLink(link);
			}

			if(stringRecordEntry.getValue().SAFE_NODE5 != null) {
				toNode = this.scenario.getNetwork().getNodes().get(Id.createNodeId(stringRecordEntry.getValue().SAFE_NODE5));
				Gbl.assertNotNull(toNode);
//				toNodeAlt = network.getFactory().createNode(toNode.getId(), toNode.getCoord());
				network.addNode(toNode);
				link = network.getFactory().createLink(Id.createLinkId(stringRecordEntry.getKey() + "_SAFE_NODE5"), fromNode, toNode);
				network.addLink(link);
			}
		}
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, Gis.EPSG28356);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		CoordinateReferenceSystem crs = MGC.getCRS(Gis.EPSG28356);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,fileName, builder).write();
	}
	
	private void writePopulation(String filename) {
		log.info( "entering writePopulation with fileName=" + filename ) ;

		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.write(filename);
	}
	public final static class Record {
		// needs to be public, otherwise one gets some incomprehensible exception.  kai, nov'17
		
		@CsvBindByName private String SUBSECTOR;
		@CsvBindByName private String EVAC_NODE;
		@CsvBindByName private String SAFE_NODE1;
		@CsvBindByName private String SAFE_NODE2;
		@CsvBindByName private String SAFE_NODE3;
		@CsvBindByName private String SAFE_NODE4;
		@CsvBindByName private String SAFE_NODE5;

		@Override public String toString() {
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