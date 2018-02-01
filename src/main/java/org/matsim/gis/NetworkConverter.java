package org.matsim.gis;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


/**
 * @author sergio
 */
public class NetworkConverter {
	private static final Logger log = Logger.getLogger( NetworkConverter.class ) ;
	
    private static final double MIN_DISTANCE = 5.0;

    Scenario scenario;
    CoordinateTransformation transformation;

    public NetworkConverter(String nodesFile, String linksFile) {
        scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, Globals.EPSG3308);

    }


    private void parseNodes(String fileName) {
    	File dataFile = new File(fileName) ;
		log.info( "will try to read from " + dataFile.getAbsolutePath() ) ;
		Gbl.assertIf( dataFile.exists() );
	
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
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
                link.setLength(Double.parseDouble(feature.getAttribute("LENGTH").toString()));
                link.setNumberOfLanes(Double.parseDouble(feature.getAttribute("LANES").toString()));
                link.setFreespeed(Double.parseDouble(feature.getAttribute("SPEED").toString())/3.6);
                link.setCapacity(Double.parseDouble(feature.getAttribute("CAP_EMME").toString()));
                HashSet<String> modes = new HashSet<>();
                modes.add(feature.getAttribute("MODES").toString());
                link.setAllowedModes(modes);
                scenario.getNetwork().addLink(link);
            }catch (IllegalArgumentException ie){
                System.err.println("Duplicate node id "+ feature.getAttribute("ID").toString());
            }catch (NullPointerException npe){

            }
        }
    }

    private void writeNetwork(String fileName){
        new NetworkWriter(scenario.getNetwork()).write(fileName + ".xml.gz");
        new Links2ESRIShape(scenario.getNetwork(),fileName + ".shp",Globals.EPSG3308).write();
    }

    public static void main(String[] args) throws FileNotFoundException {
        NetworkConverter nwc = new NetworkConverter(args[0], args[1]);
        nwc.parseNodes(args[0]);
        System.out.println("Nodes parsed.");
        nwc.parseLinks(args[1]);
        System.out.println("Links parsed.");
        nwc.writeNetwork(args[2]);
    }

}

