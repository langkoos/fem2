package femproto.prepare.network;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.google.inject.Inject;
import femproto.globals.FEMGlobalConfig;
import femproto.globals.Gis;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;


/**
 * @author sergio
 */
public class NetworkConverter {
	private final FEMGlobalConfig globalConfig;
	private static final Logger log = Logger.getLogger( NetworkConverter.class ) ;
	
	private static final double MIN_DISTANCE = 5.0;

	// yoyoyo need consistency in the labelling of attributes so they are the same as in the EMME shapefile. Probably a global parameter in FEMAttributes
	public static final String EVACUATION_LINK = "evacSES";
	public static final String DESCRIPTION = "T_DES";

	private Scenario scenario;

	public NetworkConverter(String nodesFile, String linksFile) {
		scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		globalConfig = FEMGlobalConfig.getGlobalConfig();
	}

	public NetworkConverter(String nodesFile, String linksFile, String globalConfigFile) {
		scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		globalConfig = FEMGlobalConfig.getGlobalConfig(globalConfigFile);
	}


	private void parseNodes(String fileName) throws IOException, FactoryException {
		File dataFile = new File(fileName) ;
		log.info( "Will attempt to convert nodes from " + dataFile.getAbsolutePath() ) ;
		Gbl.assertIf( dataFile.exists() );
	
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName );
		String wkt = IOUtils.getBufferedReader(fileName.replaceAll("shp$","prj")).readLine().toString() ;
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(wkt, Gis.EPSG28356);

		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		for (SimpleFeature feature : features) {
			Coordinate lonlat = ((Geometry) feature.getDefaultGeometry()).getCoordinates()[0];
			Coord coord = new Coord(lonlat.x, lonlat.y);
			coord = transformation.transform(coord);
			Node node = networkFactory.createNode(Id.createNodeId((long) Double.parseDouble(feature.getAttribute("ID").toString())), coord);
			try {
				scenario.getNetwork().addNode(node);
			}catch (IllegalArgumentException e){
				String message = "Duplicate node id " + feature.getAttribute("ID").toString();
				log.error(message);
				throw new RuntimeException(message);
			}
			// yoyo after discussion with DP, decided that we will not use randdom incoming link to evac node, but rather loop link
			// in case random link leg start interferes with traffic
			if((Integer)feature.getAttribute("EVAC_SES") == 1) {
				Link link = networkFactory.createLink(Id.createLinkId(node.getId().toString()), node, node);
				link.setLength(1);
				link.setNumberOfLanes(1);
				link.setFreespeed(17);
				// yoyoyo this should be set on a per-link basis
				link.setCapacity(globalConfig.getEvacuationRate());
				HashSet<String> modes = new HashSet<>();
				modes.add(TransportMode.car);
				link.setAllowedModes(modes);
				link.getAttributes().putAttribute(EVACUATION_LINK, true);
				link.getAttributes().putAttribute(DESCRIPTION, "dummy");
				scenario.getNetwork().addLink(link);
			}

		}
	}

	private void parseLinks(String fileName) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		Map<Id<Node>, ? extends Node> nodes = scenario.getNetwork().getNodes();

		for (SimpleFeature feature : features) {
			// yoyo client will refactor node ids to be integer not floating point
			Id<Node> fromNodeId = Id.createNodeId((long) Double.parseDouble(feature.getAttribute("INODE").toString()));
			Id<Node> toNodeId = Id.createNodeId((long) Double.parseDouble(feature.getAttribute("JNODE").toString()));
			// yoyo needs more explicit and case specific exception handling
			try {
				Node fromNode = nodes.get(fromNodeId);
				Node toNode = nodes.get(toNodeId);
				Link link = networkFactory.createLink(Id.createLinkId(feature.getAttribute("ID").toString()), fromNode, toNode);
				// yo if euclidean distance is substantially different then raise error
				link.setLength(Double.parseDouble(feature.getAttribute("LENGTH").toString()) * 1000 );
				link.setNumberOfLanes(Double.parseDouble(feature.getAttribute("LANES").toString()));
				link.setFreespeed(Double.parseDouble(feature.getAttribute("SPEED").toString())/3.6);
				link.setCapacity(Double.parseDouble(feature.getAttribute("CAP_SES").toString())*60);
				HashSet<String> modes = new HashSet<>();
				String emmeModes = feature.getAttribute("MODES").toString().toLowerCase();
				for (char modeChar : emmeModes.toCharArray()){
					switch (modeChar){
						case 'w' : modes.add(TransportMode.walk);break;
						case 'c' : modes.add(TransportMode.car);break;
						case 'b' : modes.add("bus");break;
						case 'r' : modes.add("rail");break;
//						case 'y' : modes.add("y");break; // what is y mode? //yoyo our 2016 files dont have this so removing it; raise error in future for weird stuff
						default:
							String message = String.format("Unknown mode \"%s\" specified for link %s ",modeChar,link.getId().toString());
							log.error(message);
							throw new RuntimeException(message);
					}
				}
				link.setAllowedModes(modes);
				boolean evacSES = feature.getAttribute("EVAC_SES").toString().trim().equals("1");
				link.getAttributes().putAttribute(EVACUATION_LINK, evacSES);
				link.getAttributes().putAttribute(DESCRIPTION, feature.getAttribute(DESCRIPTION).toString());
				scenario.getNetwork().addLink(link);
			}catch (IllegalArgumentException ie){
				System.err.println("Duplicate node id "+ feature.getAttribute("ID").toString());
			}catch (NullPointerException npe){

			}
		}
	}

	private void writeNetwork(String fileName){
		new NetworkWriter(scenario.getNetwork()).write(fileName + ".xml.gz");
//		new Links2ESRIShape(scenario.getNetwork(),fileName + ".shp", Gis.EPSG28356).write();
		// yyyy yoyo original input network is given in emme format.  we write shp as a service, but modifying it there will not have an effect onto the simulation.  is this the workflow that we want?  kai, aug'18
		// The emme files come as shapefiles, so this is a different set of shapefiles to be able to compare.
		// But that output shapefile produces different columns.  So better not write it.
		// yoyo D61 has matsim network parsing capability so rendering output shapefile is redundant. pieter oct '18

	}

	public static void main(String[] args) throws IOException, FactoryException {
		NetworkConverter nwc = new NetworkConverter(args[0], args[1]);
		nwc.parseNodes(args[0]);
		System.out.println("Nodes parsed.");
		nwc.parseLinks(args[1]);
		System.out.println("Links parsed.");
		nwc.writeNetwork(args[2]);
	}

}

