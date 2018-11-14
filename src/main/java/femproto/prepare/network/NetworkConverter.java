package femproto.prepare.network;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import femproto.globals.Gis;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
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

import static femproto.run.FEMUtils.*;


/**
 * @author sergio
 */
public class NetworkConverter {
	private static final Logger log = Logger.getLogger(NetworkConverter.class);

	private static final double MIN_DISTANCE = 5.0;

	// yoyoyo need consistency in the labelling of attributes so they are the same as in the EMME shapefile. Proabably a global parameter in FEMATtributes
	public static final String EVACUATION_LINK = getGlobalConfig().getAttribEvacMarker();
	public static final String DESCRIPTION = getGlobalConfig().getAttribDescr();
	private final String nodesFile;
	private final String linksFile;

	private Scenario scenario;

	public NetworkConverter(String nodesFile, String linksFile, Scenario scenario) {
		this.nodesFile = nodesFile;
		this.linksFile = linksFile;
		this.scenario = scenario;
	}

	public NetworkConverter(String nodesFile, String linksFile) {
		this.nodesFile = nodesFile;
		this.linksFile = linksFile;
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	public Network run() {
		this.parseNodes();
		this.parseLinks();
		return scenario.getNetwork();
	}

	private void parseNodes() {
		File dataFile = new File(nodesFile);
		log.info("Will attempt to convert nodes from " + dataFile.getAbsolutePath());
		Gbl.assertIf(dataFile.exists());

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(nodesFile);
		String wkt = null;
		try {
			wkt = IOUtils.getBufferedReader(nodesFile.replaceAll("shp$", "prj")).readLine().toString();
		} catch (IOException e) {
			String message = "There is an error loading the nodes projection information. Cannot convert to default projection.";
			log.error(message);
			throw new RuntimeException(e.getCause());
		}
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(wkt, Gis.EPSG28356);

		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		boolean warned = false;
		for (SimpleFeature feature : features) {
			Coordinate lonlat = ((Geometry) feature.getDefaultGeometry()).getCoordinates()[0];
			Coord coord = new Coord(lonlat.x, lonlat.y);
			coord = transformation.transform(coord);
			Node node = networkFactory.createNode(Id.createNodeId((long) Double.parseDouble(feature.getAttribute("ID").toString())), coord);
			try {
				scenario.getNetwork().addNode(node);
			} catch (IllegalArgumentException e) {
				String message = "Duplicate node id " + feature.getAttribute("ID").toString();
				log.error(message);
				throw new RuntimeException(message);
			}
			// yoyo after discussion with DP, decided that we will not use randdom incoming link to evac node, but rather loop link
			// in case random link leg start interferes with traffic
			int evacValue;
			String attribEvacMarker = getGlobalConfig().getAttribEvacMarker();

			try {
				evacValue = (int) feature.getAttribute(attribEvacMarker);
			} catch (ClassCastException e) {
				try {
					evacValue = (int) (long) feature.getAttribute(attribEvacMarker);
					if (!warned) {
						String message = String.format("Column %s in nodes shapefile is a big integer or long value. Converting it but expecting a small integer for consistency.", attribEvacMarker);
						log.warn(message);
						warned = true;
					}
				} catch (ClassCastException e2) {
					try {
						evacValue = (int) (double) feature.getAttribute(attribEvacMarker);
						if (!warned) {
							String message = String.format("Column %s in nodes shapefile is a big integer or long value. Converting it but expecting a small integer for consistency.", attribEvacMarker);
							log.warn(message);
							warned = true;
						}
					} catch (ClassCastException e3) {
						String message = String.format("Cannot convert %s in nodes shapefile to the appropriate data type (0-1 integer). Aborting.", attribEvacMarker);
						log.error(message);
						throw new RuntimeException(message);
					}
				}
			}
			if (evacValue == 1) {
				Link link = networkFactory.createLink(Id.createLinkId(node.getId().toString()), node, node);
				link.setLength(1);
				link.setNumberOfLanes(1);
				link.setFreespeed(17);
				link.setCapacity(getGlobalConfig().getEvacuationRate());
				HashSet<String> modes = new HashSet<>();
				modes.add(TransportMode.car);
				link.setAllowedModes(modes);
				link.getAttributes().putAttribute(EVACUATION_LINK, true);
				link.getAttributes().putAttribute(DESCRIPTION, "dummy");
				scenario.getNetwork().addLink(link);
			}

		}
	}


	private void parseLinks() {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(linksFile);
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		Map<Id<Node>, ? extends Node> nodes = scenario.getNetwork().getNodes();

		for (SimpleFeature feature : features) {
			// yoyo client will refactor node ids to be integer not floating point
			long fromNodeString = (long) Double.parseDouble(feature.getAttribute(getGlobalConfig().getAttribNetworkLinksINode()).toString());
			Id<Node> fromNodeId = Id.createNodeId(fromNodeString);
			long toNodeString = (long) Double.parseDouble(feature.getAttribute(getGlobalConfig().getAttribNetworkLinksJNode()).toString());
			Id<Node> toNodeId = Id.createNodeId(toNodeString);
			// yoyo needs more explicit and case specific exception handling
			String linkId = feature.getAttribute("ID").toString();
			Node fromNode = nodes.get(fromNodeId);
			Node toNode = nodes.get(toNodeId);
			if (fromNode == null || toNode == null) {
				log.warn(String.format("Link ID %s has missing from/to node, no link generated, can lead to disconnected network issues down the line...", linkId));
				continue;
			}
			try {
				Link link = networkFactory.createLink(Id.createLinkId(linkId), fromNode, toNode);
				// yo if euclidean distance is substantially different then raise error
				double length = Double.parseDouble(feature.getAttribute(getGlobalConfig().getAttribNetworkLinksLength()).toString());
				if(length <= 0.001){
					String message = String.format("Link %s has length of only %f km, will likely cause Dijkstra to lock up",linkId,length);
					log.error(message);
					throw new RuntimeException(message);
				}

				link.setLength(length * 1000);
				link.setNumberOfLanes(Double.parseDouble(feature.getAttribute(getGlobalConfig().getAttribNetworkLinksLanes()).toString()));
				link.setFreespeed(Double.parseDouble(feature.getAttribute(getGlobalConfig().getAttribNetworkLinksSpeed()).toString()) / 3.6);
				link.setCapacity(Double.parseDouble(feature.getAttribute(getGlobalConfig().getAttribNetworkLinksCapSES()).toString()) * 60);
				HashSet<String> modes = new HashSet<>();
				String emmeModes = feature.getAttribute(getGlobalConfig().getAttribNetworkLinksModes()).toString().toLowerCase();
				for (char modeChar : emmeModes.toCharArray()) {
					switch (modeChar) {
						case 'w':
							modes.add(TransportMode.walk);
							break;
						case 'c':
							modes.add(TransportMode.car);
							break;
						case 'b':
							modes.add("bus");
							break;
						case 'r':
							modes.add("rail");
							break;
//						case 'y' : modes.add("y");break; // what is y mode? //yoyo our 2016 files dont have this so removing it; raise error in future for weird stuff
						default:
							String message = String.format("Unknown mode \"%s\" specified for link %s ", modeChar, link.getId().toString());
							log.error(message);
							throw new RuntimeException(message);
					}
				}
				// yoyo a cheat to identify evac links in via without having to load object attributes
				boolean evacSES = feature.getAttribute(getGlobalConfig().getAttribEvacMarker()).toString().trim().equals("1");
				if (evacSES) {
					modes.add("evac");
					if (!modes.contains(TransportMode.car)) {
						log.warn(String.format("Link %s has no car mode assigned to it in the shapefile, but is marked as an evacuation link. May cause routing issues down the line...", linkId));
					}
				}
				link.setAllowedModes(modes);
				link.getAttributes().putAttribute(EVACUATION_LINK, evacSES);
				link.getAttributes().putAttribute(DESCRIPTION, feature.getAttribute(DESCRIPTION).toString());
				scenario.getNetwork().addLink(link);
			} catch (IllegalArgumentException ie) {
				System.err.println("Duplicate node id " + linkId);
			} catch (NullPointerException e) {
				log.warn(String.format("Link ID %s has missing attributes , generating an impassible link (marked \"BADLINK\" in network file), can lead to disconnected network issues down the line...", linkId));
				Link link = networkFactory.createLink(Id.createLinkId(linkId), fromNode, toNode);
				NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
				link.setNumberOfLanes(1);
				link.setFreespeed(0.0001);
				link.setCapacity(0);
				HashSet<String> modes = new HashSet<>();
				modes.add(TransportMode.car);
				link.setAllowedModes(modes);
				link.getAttributes().putAttribute(EVACUATION_LINK, false);
				link.getAttributes().putAttribute(DESCRIPTION, "BADLINK");
				scenario.getNetwork().addLink(link);
			}
		}

		// check that the evacuation links have an evac link in opposite direction (if link exists)
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if ((boolean) link.getAttributes().getAttribute(EVACUATION_LINK)) {
				Link linkInOppositeDirection = NetworkUtils.findLinkInOppositeDirection(link);
				if (linkInOppositeDirection != null) {
					if (!(boolean) linkInOppositeDirection.getAttributes().getAttribute(EVACUATION_LINK)) {
						log.warn(String.format("Link %s is not marked for evacuation but the link in opposite direction is. this can cause routing issues (infinite loop)...", linkInOppositeDirection.getId().toString()));

					}
				}
			}
		}

	}


	public void writeNetwork(String fileName) {
		String fileNameNoXML = fileName.split(".xml")[0];
		log.info("Writing before and after NetworkCleaner versions of the network. Check for missing nodes and links if there are issues down the line... ");
		new NetworkWriter(scenario.getNetwork()).write(fileName);
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(fileName);
//		Set<Id<Link>> linkIds = new HashSet<>();
//		linkIds.addAll(network.getLinks().keySet());
//		for (Id<Link> linkId : linkIds) {
//			if (!(boolean)network.getLinks().get(linkId).getAttributes().getAttribute(getGlobalConfig().getAttribEvacMarker()))
//				network.removeLink(linkId);
//		}


		NetworkUtils.runNetworkCleaner(network);
		new NetworkWriter(network).write(fileNameNoXML + "_clean.xml");
//		new Links2ESRIShape(scenario.getNetwork(),fileName + ".shp", Gis.EPSG28356).write();
		// yyyy yoyo original input network is given in emme format.  we write shp as a service, but modifying it there will not have an effect onto the simulation.  is this the workflow that we want?  kai, aug'18
		// The emme files come as shapefiles, so this is a different set of shapefiles to be able to compare.
		// But that output shapefile produces different columns.  So better not write it.
		// yoyo D61 has matsim network parsing capability so rendering output shapefile is redundant. pieter oct '18

	}


	public static void main(String[] args) throws IOException, FactoryException {
		NetworkConverter nwc = new NetworkConverter(args[0], args[1]);
		nwc.run();
		nwc.writeNetwork(args[2]);
	}

}

