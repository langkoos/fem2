package femproto.demand;

import femproto.evacuationstaging.EvacuationStagingUtils;
import femproto.gis.Globals;
import femproto.routing.FEMPreferEmergencyLinksTravelDisutility;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static javafx.scene.input.KeyCode.T;

public class SubSectorsToPopulation {
	private static final Logger log = Logger.getLogger(SubSectorsToPopulation.class) ;


	Scenario scenario;
	private EvacuationStagingUtils evacuationStagingUtils;

	private SubSectorsToPopulation() {
//		log.setLevel(Level.DEBUG);
		this.scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
	}
	
	public static void main(String[] args) throws IOException {
		SubSectorsToPopulation subSectorsToPopulation = new SubSectorsToPopulation();
		subSectorsToPopulation.readNetwork(args[1]);
		subSectorsToPopulation.initializeEvacuationStaging(args[2]);
		subSectorsToPopulation.readSubSectorsShapeFile(args[0]);
		subSectorsToPopulation.writePopulation(args[3]);
		subSectorsToPopulation.writeAttributes(args[4]);
		// TODO if we really want to leave it like this, then put in a more expressive
		// command passing syntax (see bdi-abm-integration project).  kai, feb'18
	}

	private void initializeEvacuationStaging(String arg) {
		 evacuationStagingUtils = new EvacuationStagingUtils(scenario);
		 evacuationStagingUtils.readEvacAndSafeNodes(arg);
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
			

			
			// test that evac nodes are same in both files:
			String evacNodeFromShp = feature.getAttribute("EVAC_NODE").toString();
			String evacNodeFromCorrespondancesFile = evacuationStagingUtils.getEvacNode(subsector) ;
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



			int totalVehicles = (int) (double) feature.getAttribute("Totalvehic");
			for (int i = 0; i < totalVehicles; i++) {
				Person person = pf.createPerson(Id.createPersonId(id++));
				person.getAttributes().putAttribute("SUBSECTOR", subsector);
				int j = 0;
				List<Link> safeLinks = evacuationStagingUtils.getSafeLinks(subsector);
				for (Link safeLink : safeLinks) {

					Plan plan = pf.createPlan();

					Activity startAct = pf.createActivityFromLinkId("evac", startLink.getId());
					startAct.setEndTime(0); // yyyyyy ????
					plan.addActivity(startAct);

					Leg evacLeg = pf.createLeg(TransportMode.car);
					plan.addLeg(evacLeg);

					Activity safe = pf.createActivityFromLinkId("safe_" + j, safeLink.getId());
					plan.addActivity(safe);



					person.addPlan(plan);
				}

				scenario.getPopulation().addPerson(person);
			}
		}
		
	}








	private void writePopulation(String filename) {
		log.info( "entering writePopulation with fileName=" + filename ) ;

		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.write(filename);

	}

	private void writeAttributes(String fileName) {
		// yoyo writing out attributes to a separate file for diagnostics
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("id\tsubsector\n");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			writer.write(person.getId()+"\t"+person.getAttributes().getAttribute("SUBSECTOR").toString()+"\n");
		}
		writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




	
	
	
}
