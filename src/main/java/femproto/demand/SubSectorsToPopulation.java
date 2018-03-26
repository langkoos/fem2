package femproto.demand;

import femproto.gis.Globals;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;


import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(wkt, Globals.EPSG28356);
		
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
			Link endLink = null ;
			{
				// find safeNode in network file:
				// yoyoyo?? currently we only use the ifrst of the safe nodes,
				// upodate 2018.3.7 access to these are cordoned off in order as the previous nodes become inaccesssible
				String defaultSafeNode = record.SAFE_NODE1;
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
			}
			
			
			
			
			
			int totalVehicles = (int)(double)feature.getAttribute("Totalvehic");
			for (int i = 0; i < totalVehicles; i++) {
				Person person = pf.createPerson(Id.createPersonId(id++));
				person.getAttributes().putAttribute("Subsector",subsector);
				
				Plan plan = pf.createPlan() ;
				
				Activity startAct = pf.createActivityFromLinkId("evac", startLink.getId() );
				startAct.setEndTime(0); // yyyyyy ????
				plan.addActivity(startAct);
				
				Leg evacLeg = pf.createLeg(TransportMode.car) ;
				plan.addLeg(evacLeg);
				
				Activity safe = pf.createActivityFromLinkId("safe", endLink.getId() ) ;
				plan.addActivity(safe);

				person.getAttributes().putAttribute("SAFE_NODE1",record.SAFE_NODE1);
				if(record.SAFE_NODE2 != null)
					person.getAttributes().putAttribute("SAFE_NODE2",record.SAFE_NODE2);
				if(record.SAFE_NODE3 != null)
					person.getAttributes().putAttribute("SAFE_NODE3",record.SAFE_NODE3);
				if(record.SAFE_NODE4 != null)
					person.getAttributes().putAttribute("SAFE_NODE4",record.SAFE_NODE4);
				if(record.SAFE_NODE5 != null)
					person.getAttributes().putAttribute("SAFE_NODE5",record.SAFE_NODE5);

				person.addPlan(plan) ;

				scenario.getPopulation().addPerson(person);
			}
		}
		
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
