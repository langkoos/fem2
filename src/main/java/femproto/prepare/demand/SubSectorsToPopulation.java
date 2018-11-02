package femproto.prepare.demand;

import com.google.inject.Inject;
import femproto.globals.FEMGlobalConfig;
import femproto.prepare.parsers.EvacuationToSafeNodeMapping;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;


public class SubSectorsToPopulation {
	private static final Logger log = Logger.getLogger(SubSectorsToPopulation.class) ;
	private final FEMGlobalConfig globalConfig;
	private final Scenario scenario;
	private EvacuationToSafeNodeMapping evacuationToSafeNodeMapping;

	private SubSectorsToPopulation() {
//		log.setLevel(Level.DEBUG);
		this.scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		this.globalConfig = FEMGlobalConfig.getGlobalConfig();
	}

	private SubSectorsToPopulation(String config) {
//		log.setLevel(Level.DEBUG);
		this.scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		this.globalConfig = FEMGlobalConfig.getGlobalConfig(config);
	}


	
	public static void main(String[] args) throws IOException {
		SubSectorsToPopulation subSectorsToPopulation = new SubSectorsToPopulation();
		subSectorsToPopulation.readNetwork(args[1]);
		subSectorsToPopulation.initializeEvacuationStaging(args[2]);
		subSectorsToPopulation.readSubSectorsShapeFile(args[0]);
		subSectorsToPopulation.writePopulation(args[3]);
		subSectorsToPopulation.writeAttributes(args[4]);
		// do we need to write the attributes, or is this now (also) in the population?  kai, aug'18
		// I put this here for joining results in external packages like tableau. Via also doens't read attributes from population yet - pieter

		//  if we really want to leave it like this, then put in a more expressive command passing syntax (see bdi-abm-integration project).  kai, feb'18
		// resolved as we don't allow this level of control - pieter, sep'18
	}

	private void initializeEvacuationStaging(String arg) {
		 evacuationToSafeNodeMapping = new EvacuationToSafeNodeMapping(scenario);
		 evacuationToSafeNodeMapping.readEvacAndSafeNodes(arg);
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
		
		//iterate through features and generate pax by subsector
		Iterator<SimpleFeature> iterator = features.iterator();
		long personCnt=0L;
		boolean linkErrors = false;
		while (iterator.hasNext()){
			SimpleFeature feature = iterator.next();
			
			// get subsector name from shp file
			String subsector = feature.getAttribute("Subsector").toString();
			

			
			// test that evac nodes are same in both files:
			String evacNodeFromShp = feature.getAttribute("EVAC_NODE").toString();
			String evacNodeFromCorrespondancesFile = evacuationToSafeNodeMapping.getEvacNode(subsector).getId().toString() ;
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
				// yoyo I think it's better to check for an evac link, preferably the one that gives the shortest path to evac node.
				// this means, though, that we might need to assign a different link for each possible evac node, for each plan in the agent's memory
				for (Link link : node.getInLinks().values()) {
					// the comment above says "incoming", the code does "outgoing".  Spatially, the link entry points is closer to the node for
					// incoming links.  kai, sep'18
					// resolved: DP accepted request for using incoming links so agents travel the full centroid connector distance.
					if ( link.getAllowedModes().contains( TransportMode.car) && (boolean)link.getAttributes().getAttribute(globalConfig.getattribEvacMarker())) {
						startLink = link;
					}
				}
				boolean badOutlink = true;
				for (Link link : node.getOutLinks().values()){
					if ( link.getAllowedModes().contains( TransportMode.car) && (boolean)link.getAttributes().getAttribute(globalConfig.getattribEvacMarker())) {
						badOutlink = false;
					}
				}
				if (startLink == null || badOutlink){
					String msg = String.format("There seems to be no incoming/outgoing car mode EVAC link for evac node %s, subsector %s. The link IDs to check are as follows:",evacNodeFromCorrespondancesFile,subsector);
					log.error(msg); ;
					double maxCap = Double.NEGATIVE_INFINITY;
					msg = "INCOMING links: ";
					log.error(msg);
					for (Link link : node.getInLinks().values()) {
						msg = String.format("%s: modes = %12s , evac link = %s",link.getId(),link.getAllowedModes(),link.getAttributes().getAttribute(globalConfig.getattribEvacMarker()).toString());
						log.error(msg);
					}
					msg = "OUTGOING links: ";
					log.error(msg);
					for (Link link : node.getOutLinks().values()) {
						msg = String.format("%s: modes = %12s , evac link = %s",link.getId(),link.getAllowedModes(),link.getAttributes().getAttribute(globalConfig.getattribEvacMarker()).toString());
						log.error(msg);
					}
					linkErrors = true;
					continue;
				}
			}



			int totalVehicles = (int) (double) feature.getAttribute("Totalvehic");
			for (int i = 0; i < totalVehicles; i++) {
				Person person = pf.createPerson(Id.createPersonId(personCnt++));
				setSubsectorName( subsector, person );
				List<Link> safeLinks = evacuationToSafeNodeMapping.getSafeLinks(subsector);
				for (Link safeLink : safeLinks) {


					Plan plan = pf.createPlan();

					Activity startAct = pf.createActivityFromLinkId("evac", startLink.getId());
					startAct.setEndTime(0); // yyyyyy ????
					plan.addActivity(startAct);

					Leg evacLeg = pf.createLeg(TransportMode.car);
					plan.addLeg(evacLeg);

					Activity safe = pf.createActivityFromLinkId("safe" , safeLink.getId());
					plan.addActivity(safe);



					person.addPlan(plan);
				}

				scenario.getPopulation().addPerson(person);
			}
		}

		if(linkErrors)
			throw new RuntimeException("Review list of link Errors for fixing");
		
	}

	private void setSubsectorName(String subsector, Person person) {
			person.getAttributes().putAttribute(globalConfig.getAttribSubsector(), subsector);
	}


	private void writePopulation(String filename) {
		log.info( "entering writePopulation with fileName=" + filename ) ;

		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.write(filename);

	}

	private void writeAttributes(String fileName) {
		// yoyo writing out attributes to a separate file for diagnostics
		try ( BufferedWriter writer = IOUtils.getBufferedWriter( fileName ) ) {
			writer.write( "id\tsubsector\n" );
			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				writer.write( person.getId() + "\t" + person.getAttributes().getAttribute( globalConfig.getAttribSubsector()) + "\n" );
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
	
}
