package femproto.prepare.parsers;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import femproto.globals.Gis;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.parsers.HydrographPoint.HydrographPointData;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class HydrographParser {

	Logger log = Logger.getLogger(HydrographParser.class);

	public HydrographParser(Network network, EvacuationSchedule evacuationSchedule) {
		this.network = network;
		this.evacuationSchedule = evacuationSchedule;
	}

	public Map<String, HydrographPoint> getHydrographPointMap() {
		return hydrographPointMap;
	}

	private Map<String, HydrographPoint> hydrographPointMap;

	public Map<String, HydrographPoint> getConsolidatedHydrographPointMap() {
		return consolidatedHydrographPointMap;
	}

	private Map<String, HydrographPoint> consolidatedHydrographPointMap;

	private final Network network;

	private final EvacuationSchedule evacuationSchedule;


	/**
	 * This initialises the data structure and populates it with the subsectors and/or link ids that this point may affect,
	 * as well as its altitude.
	 * <p>
	 * If a point has link ids associated with it, these are recorded, otherwise the subsector's centroid connectors are recorded and
	 * network change events will be generated for these later on.
	 *
	 * @param shapefile
	 */
	public void parseHydrographShapefile(String shapefile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapefile);
		CoordinateTransformation transformation = null;
		// coordinate transformation:
		String wkt = null;
		try {
			wkt = IOUtils.getBufferedReader(shapefile.replaceAll("shp$", "prj")).readLine().toString();
			transformation = TransformationFactory.getCoordinateTransformation(wkt, Gis.EPSG28356);
		} catch (IOException e) {
			log.warn("The shapefile doesn't have a .prj file; continuing, but no guarantees on projection.");
		}

		hydrographPointMap = new HashMap<>();

		//iterate through features and generate hydrograph data points
		Iterator<SimpleFeature> iterator = features.iterator();
		long id = 0L;
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();

			String pointID = feature.getAttribute(FEMUtils.getGlobalConfig().getAttribHydrographPointId()).toString();
			String linkIDs = feature.getAttribute(FEMUtils.getGlobalConfig().getAttribHydrographLinkIds()).toString();
			String subsector = feature.getAttribute(FEMUtils.getGlobalConfig().getAttribSubsector()).toString();
			final Double ALT_AHD = Double.valueOf(feature.getAttribute(FEMUtils.getGlobalConfig().getAttribHydrographSelectedAltAHD()).toString());
			Coordinate lonlat = ((Geometry) feature.getDefaultGeometry()).getCoordinates()[0];
			Coord coord = new Coord(lonlat.x, lonlat.y);
			if (transformation != null)
				coord = transformation.transform(coord);
			HydrographPoint hydrographPoint = new HydrographPoint(pointID, ALT_AHD, coord);
			hydrographPoint.setSubSector(subsector);
			hydrographPointMap.put(pointID, hydrographPoint);
			//yoyo for hydrogrpah points with no link id associated with them, this will use the centroid connector instead
			if (linkIDs.equals("") && !subsector.equals("")) {
				Node evacuationNode = evacuationSchedule.getOrCreateSubsectorData(subsector).getEvacuationNode();
				if (evacuationNode == null) {
					String message = "The evacuation schedule has not been properly initialised for hydrograph parsing. No evacuation node for subsector " + subsector;
					log.error(message);
					throw new RuntimeException(message);
				}
				Set<Id<Link>> evacLinkIds = new HashSet<>();
				evacLinkIds.addAll(evacuationNode.getOutLinks().keySet());
				evacLinkIds.addAll(evacuationNode.getInLinks().keySet());
				for (Id<Link> evacLinkId : evacLinkIds) {
					hydrographPoint.addLinkId(evacLinkId.toString());
				}

			} else
				hydrographPoint.addLinkIds(linkIDs.split(","));
		}
	}

	/**
	 * This reads the  time series from the hydrograph file, and populates the relevant point data structures.
	 *
	 * <b>It  also finds the minimum time and subtracts that from all time values, so simulations can start at zero hours.</b>
	 *
	 * @param fileName
	 */
	public void readHydrographData(String fileName) {
		BufferedReader reader = IOUtils.getBufferedReader(fileName);
		List<List<Double>> entries = new ArrayList<>();
		String[] header;
		try {
			header = reader.readLine().split(",");
			for (String s : header) {
				entries.add(new ArrayList<>());
			}

			String line = reader.readLine();
			while (line != null) {
				String[] lineArray = line.split(",");
				for (int i = 0; i < header.length; i++) {
					entries.get(i).add(Double.valueOf(lineArray[i].trim()));

				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Something went wrong while reading the hydrograph data");
		}


		//find minimum time value
		//normalise all
		// yoyoyo note that Peter said the 3rd row of the hydrograph is actually time 0
		double minTime = entries.get(0).get(1) * 3600;
		for (int i = 1; i < header.length; i++) {
			HydrographPoint hydrographPoint = hydrographPointMap.get(header[i]);
			if (hydrographPoint != null) {
				for (int j = 1; j < entries.get(i).size(); j++) {
					// yoyoyo it might be better top not have BUFFER_TIME in here and only use in the routing of agents, not in generating network change events
					hydrographPoint.addTimeSeriesData(entries.get(0).get(j) * 3600 - minTime, entries.get(i).get(j));
				}
				hydrographPoint.calculateFloodTimeFromData();
			}
		}

		removeHydrographPointsWithNoData();

		consolidateHydrographPointsByLink();

	}

	private void removeHydrographPointsWithNoData() {
		Set<String> badkeys = new HashSet<>();
		badkeys.addAll(hydrographPointMap.keySet());

		for (Map.Entry<String, HydrographPoint> pointEntry : hydrographPointMap.entrySet()) {
			if (pointEntry.getValue().inHydrograph())
				badkeys.remove(pointEntry.getKey());
		}

		for (String badkey : badkeys) {
			hydrographPointMap.remove(badkey);
			log.warn(String.format("Removed hydrograph point id %s from consideration as it has no hydrograph data associated with it.", badkey));
		}
	}

	/**
	 * For now this consolidates hyrograph data for many points associated with a subsector into a single one, taking the minimum of the recprded flood times as its value.
	 * David et al will remove excess points so that ultimately only a single point is associated with a subsector, which will make this method redundant.
	 */
	private void consolidateHydrographPointsByLink() {
		consolidatedHydrographPointMap = new HashMap<>();
		for (HydrographPoint hydrographPoint : hydrographPointMap.values()) {
			for (String linkId : hydrographPoint.getLinkIds()) {
				HydrographPoint linkHydroPoint = consolidatedHydrographPointMap.get(linkId);
				if (linkHydroPoint == null) {
					linkHydroPoint = new HydrographPoint(linkId, hydrographPoint.getALT_AHD(), hydrographPoint.coord);
					linkHydroPoint.setSubSector(hydrographPoint.getSubSector());
					linkHydroPoint.addLinkId(linkId);
					linkHydroPoint.setFloodTime(hydrographPoint.getFloodTime());
					consolidatedHydrographPointMap.put(linkId, linkHydroPoint);
				} else {
					// yoyo set the flood time to the minimum of the existing and new data point
					double currentFloodTime = linkHydroPoint.getFloodTime();
					double newFloodTime = hydrographPoint.getFloodTime();
					if (currentFloodTime > 0 && newFloodTime > 0)
						linkHydroPoint.setFloodTime(Math.min(currentFloodTime, newFloodTime));
					else
						linkHydroPoint.setFloodTime(Math.max(currentFloodTime, newFloodTime));

				}
			}
		}
	}

	public void hydrographToViaXY(String fileName) {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("ID\tX\tY\ttime\tflooded\n");

			for (HydrographPoint point : hydrographPointMap.values()) {
				List<HydrographPointData> pointData = point.getData();
				for (HydrographPointData pointDatum : pointData) {
					writer.write(String.format("%s\t%f\t%f\t%f\t%d\n", point.pointId, point.coord.getX(), point.coord.getY(), pointDatum.getTime(), pointDatum.getLevel_ahd() - point.ALT_AHD > 0 ? 1 : 0));
				}

			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Something went wrong writing the XY plotfile.");
			throw new RuntimeException();
		}

	}

	public void hydrographToViaLinkAttributesFromPointData(String fileName, Network network) {
		Set<Id<Link>> ids = new TreeSet<>();
		ids.addAll(network.getLinks().keySet());
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("ID\ttime\tflooded\n");

			for (Id<Link> id : ids) {
				writer.write(String.format("%s\t%f\t%d\n", id, 0f, 0));
			}

			for (HydrographPoint point : hydrographPointMap.values()) {
				if (!point.mappedToNetworkLink())
					continue;
				for (String linkId : point.getLinkIds()) {
					List<HydrographPointData> pointData = point.getData();
					for (HydrographPointData pointDatum : pointData) {
						writer.write(String.format("%s\t%f\t%d\n", linkId, pointDatum.getTime(),
								pointDatum.getLevel_ahd() - point.getALT_AHD() > 0 ? 1 : 0));
					}
				}
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
			String message = "Something went wrong writing the Via attributes file.";
			log.error(message);
			throw new RuntimeException(message);
		}

	}


	public void hydrographToViaLinkAttributesFromLinkData(String fileName, Network network) {
		Set<Id<Link>> ids = new TreeSet<>();
		ids.addAll(network.getLinks().keySet());
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("ID\ttime\tflooded\n");
			for (Id<Link> id : ids) {
				writer.write(String.format("%s\t%f\t%d\n", id, 0f, 0));
			}

			for (HydrographPoint point : consolidatedHydrographPointMap.values()) {
				if (!point.mappedToNetworkLink())
					continue;

				for (String linkId : point.getLinkIds()) {
					writer.write(String.format("%s\t%f\t%d\n", linkId, point.getFloodTime(), 1));

				}
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
			String message = "Something went wrong writing the Via attributes file.";
			log.error(message);
			throw new RuntimeException(message);
		}

	}


	public void networkChangeEventsFromHydrographData(Network network, String outputFileName) {
		Set<NetworkChangeEvent> networkChangeEvents = new HashSet<>();
		for (HydrographPoint point : hydrographPointMap.values()) {
			if (point.getFloodTime() < 0)
				continue;
			if (point.mappedToNetworkLink()) {
				for (String linkId : point.getLinkIds()) {
					Link link = network.getLinks().get(Id.createLinkId(linkId));
					if (link != null) {
						NetworkChangeEvent changeEvent = new NetworkChangeEvent(point.getFloodTime());
						NetworkChangeEvent.ChangeValue flowChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 0.0000);
						changeEvent.setFlowCapacityChange(flowChange);
						NetworkChangeEvent.ChangeValue speedChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 0.0000);
						changeEvent.setFreespeedChange(speedChange);
						changeEvent.addLink(link);
						networkChangeEvents.add(changeEvent);
					}
				}
			} else {

			}
		}
		new NetworkChangeEventsWriter().write(outputFileName, networkChangeEvents);
	}


	public List<NetworkChangeEvent> networkChangeEventsFromConsolidatedHydrographFloodTimes(Network network, String outputFileName) {
		List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
		double maxTime = Double.NEGATIVE_INFINITY;
		for (HydrographPoint point : consolidatedHydrographPointMap.values()) {
			if (point.getFloodTime() < 0)
				continue;
			if (point.mappedToNetworkLink()) {
				for (String linkId : point.getLinkIds()) {
					Link link = network.getLinks().get(Id.createLinkId(linkId));
					if (link != null) {
						NetworkChangeEvent changeEvent = new NetworkChangeEvent(point.getFloodTime());
						NetworkChangeEvent.ChangeValue flowChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 0.0);
						changeEvent.setFlowCapacityChange(flowChange);
						NetworkChangeEvent.ChangeValue speedChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, link.getLength() / 86400.0);
						changeEvent.setFreespeedChange(speedChange);
						changeEvent.addLink(link);
						networkChangeEvents.add(changeEvent);
						maxTime = Math.max(maxTime, changeEvent.getStartTime());
					}
				}
			}
		}
		//reset the capcity of dead links a long time after the last one has died so that agents make it to destinations,
		// with a bad score.
		List<NetworkChangeEvent> networkChangeEvents1 = new ArrayList<>();
		networkChangeEvents1.addAll(networkChangeEvents);
		for (NetworkChangeEvent capacityReductionEvent : networkChangeEvents1) {
			NetworkChangeEvent capacityResetEvent = new NetworkChangeEvent(maxTime + 86400);
			NetworkChangeEvent.ChangeValue flowChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 6000);
			capacityResetEvent.setFlowCapacityChange(flowChange);
			NetworkChangeEvent.ChangeValue speedChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 16.7);
			capacityResetEvent.setFreespeedChange(speedChange);
			capacityResetEvent.addLinks(capacityReductionEvent.getLinks());
			networkChangeEvents.add(capacityResetEvent);
		}

		new NetworkChangeEventsWriter().write(outputFileName, networkChangeEvents);
		return networkChangeEvents;
	}


}
