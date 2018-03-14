package femproto.network;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import femproto.gis.Globals;
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
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;


/**
 * @author sergio
 */
public class NetworkConverter {
	private static final Logger log = Logger.getLogger( NetworkConverter.class ) ;
	
	private static final double MIN_DISTANCE = 5.0;

	public static final String EVACUATION_LINK = "evacSES";
	public static final String DESCRIPTION = "T_DES";

	Scenario scenario;

	public NetworkConverter(String nodesFile, String linksFile) {
		scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());

	}


	private void parseNodes(String fileName) throws IOException, FactoryException {
		File dataFile = new File(fileName) ;
		log.info( "will try to read from " + dataFile.getAbsolutePath() ) ;
		Gbl.assertIf( dataFile.exists() );
	
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName );
		String wkt = IOUtils.getBufferedReader(fileName.replaceAll("shp$","prj")).readLine().toString() ;
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(wkt, Globals.EPSG28356);
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		for (SimpleFeature feature : features) {
			Coordinate lonlat = ((Geometry) feature.getDefaultGeometry()).getCoordinates()[0];
			Coord coord = new Coord(lonlat.x, lonlat.y);
			coord = transformation.transform(coord);
			Node node = networkFactory.createNode(Id.createNodeId(Long.parseLong(feature.getAttribute("ID").toString())), coord);
			try {
				scenario.getNetwork().addNode(node);
			}catch (IllegalArgumentException e){
				System.err.println("Duplicate node id "+ feature.getAttribute("ID").toString());
			}
		}
	}
	private void parseLinks(String fileName) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		Map<Id<Node>, ? extends Node> nodes = scenario.getNetwork().getNodes();

		for (SimpleFeature feature : features) {
			Id<Node> fromNodeId = Id.createNodeId(Long.parseLong(feature.getAttribute("INODE").toString()));
			Id<Node> toNodeId = Id.createNodeId(Long.parseLong(feature.getAttribute("JNODE").toString()));
			try {
				Node fromNode = nodes.get(fromNodeId);
				Node toNode = nodes.get(toNodeId);
				Link link = networkFactory.createLink(Id.createLinkId(feature.getAttribute("ID").toString()), fromNode, toNode);
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
						case 'y' : modes.add("y");break; //yyyy what is y mode?
						default:  throw new RuntimeException("No mode specified for link ");
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
		new Links2ESRIShape(scenario.getNetwork(),fileName + ".shp",Globals.EPSG28356).write();
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

