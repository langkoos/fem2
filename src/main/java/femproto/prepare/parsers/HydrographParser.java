package femproto.prepare.parsers;

import femproto.globals.Gis;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.parsers.HydrographPoint.HydrographPointData;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import java.net.URL;
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


	public void parseHydrographShapefile(String shapefile) {
		parseHydrographShapefile(IOUtils.newUrl(null,shapefile));
	}

	/**
	 * This initialises the data structure and populates it with the subsectors and/or link ids that this point may affect,
	 * as well as its altitude.
	 * <p>
	 * If a point has link ids associated with it, these are recorded, otherwise the subsector's centroid connectors are recorded and
	 * network change events will be generated for these later on.
	 *
	 * @param shapefile
	 */
	public void parseHydrographShapefile(URL shapefile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapefile.getPath());
		CoordinateTransformation transformation = null;
		// coordinate transformation:
		String wkt = null;
		try {
			wkt = IOUtils.getBufferedReader(shapefile.getPath().replaceAll("shp$", "prj")).readLine().toString();
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
			// for hydrogrpah points with no link id associated with them, this will use the centroid connector instead
			/* yoyo UPDATE: after emailing to David (22 Nov), it appears that powering evacuations by centroid connector links might not be apporpiate,
			 as other agents might have to pass through the subsector, and now links have been closed.
			 when they provide updated data, where subsector evacuations will happen from a single link that wont be passed through
			 by other agents, then this wont be an issue. for now, I will still attempt to poer it by killing the loop link at the evac node..
			 */
//			if (linkIDs.equals("") && !subsector.equals("")) {
//				Node evacuationNode = evacuationSchedule.getOrCreateSubsectorData(subsector).getEvacuationNode();
//				if (evacuationNode == null) {
//					String message = "The evacuation schedule has not been properly initialised for hydrograph parsing. No evacuation node for subsector " + subsector;
//					log.error(message);
//					throw new RuntimeException(message);
//				}
//				Set<Id<Link>> evacLinkIds = new HashSet<>();
//				evacLinkIds.addAll(evacuationNode.getOutLinks().keySet());
//				evacLinkIds.addAll(evacuationNode.getInLinks().keySet());
//				for (Id<Link> evacLinkId : evacLinkIds) {
//					hydrographPoint.addLinkId(evacLinkId.toString());
//				}
//				for (Link link : evacuationNode.getOutLinks().values()) {
//					if (link.getFromNode().equals(link.getToNode()))
//						hydrographPoint.addLinkId(link.getId().toString());
//
//				}
//
//
//			} else
			hydrographPoint.addLinkIds(linkIDs.split(","));
		}
	}


	public void readHydrographData(String fileName, int offsetTime) {
		readHydrographData(IOUtils.newUrl(null,fileName),offsetTime);
	}
	/**
	 * This reads the  time series from the hydrograph file, and populates the relevant point data structures.
	 *
	 * <b>It  also finds the minimum time and subtracts that from all time values, so simulations can start at zero hours.</b>
	 *
	 * @param fileName
	 */
	public void readHydrographData(URL fileName, int offsetTime) {
		List<List<Double>> columns = new ArrayList<>();
		String[] header;
		try ( BufferedReader reader = IOUtils.getBufferedReader(fileName.getPath()) ) {
			header = reader.readLine().split(",");
			for ( int ii = 0 ; ii < header.length ; ii++ ) {
				columns.add(new ArrayList<>()); // for each header we now have a list
			}

			String line = reader.readLine();
			while (line != null) {
				String[] lineArray = line.split(",");
				for (int ii = 0; ii < header.length; ii++) {
					columns.get(ii).add(Double.valueOf(lineArray[ii].trim()));
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Something went wrong while reading the hydrograph data");
		}


		//find minimum time value
		//normalise all
		//  note that Peter said the 3rd row of the hydrograph is actually time 0
		double minTime = columns.get(0).get(2) * 3600;
		for (int i = 1; i < header.length; i++) {
			HydrographPoint hydrographPoint = hydrographPointMap.get(header[i]);
			if (hydrographPoint != null) {
				for (int j = 2; j < columns.get(i).size(); j++) {
					//  it might be better top not have BUFFER_TIME in here and only use in the routing of agents, not in generating network change events
					hydrographPoint.addTimeSeriesData(columns.get(0).get(j) * 3600 - minTime + offsetTime, columns.get(i).get(j));
				}
				hydrographPoint.calculateFloodTimeFromData();
			}
		}


		removeHydrographPointsWithNoDataOrThatNeverFlood();

		consolidateHydrographPointsByLink();

	}


	private void removeHydrographPointsWithNoDataOrThatNeverFlood() {
		Set<String> badkeys = new HashSet<>();
		badkeys.addAll(hydrographPointMap.keySet());

		for (Map.Entry<String, HydrographPoint> pointEntry : hydrographPointMap.entrySet()) {
			if (pointEntry.getValue().inHydrograph() && pointEntry.getValue().getFloodTime() > 0)
				badkeys.remove(pointEntry.getKey());
		}

		for (String badkey : badkeys) {
			hydrographPointMap.remove(badkey);
			log.warn(String.format("Removed hydrograph point id %s from consideration as it has no hydrograph data associated with it.", badkey));
		}
		log.info("Done removing bad hydrograph points...");
	}

	/**
	 * For now this consolidates hyrograph data for many points associated with a link into a single one, taking the minimum of the recprded flood times as its value, except when its associated with a
	 * subsector loop link, indicating that its only for raising road access flooding (subsector flooding, not link flooding).
	 * For raising road access flooding, we have to take the maximum flood time
	 * David et al will remove excess points so that ultimately only a single point is associated with a subsector, which will make this method redundant.
	 */
	private void consolidateHydrographPointsByLink() {
		log.info("Consolidating hydrograph data by link and subsector (it is possible to have multiple associations on both so need to sort out which have priority)");
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
					if (currentFloodTime > 0 && newFloodTime > 0) {
						linkHydroPoint.setFloodTime(Math.min(currentFloodTime, newFloodTime));
					} else {
						linkHydroPoint.setFloodTime(Math.max(currentFloodTime, newFloodTime));
					}

				}
			}
			if (!hydrographPoint.getSubSector().equals("")) {
				String subsectorId = hydrographPoint.getSubSector();
				HydrographPoint subsectorHydroPoint = consolidatedHydrographPointMap.get(subsectorId);
				if (subsectorHydroPoint == null) {
					subsectorHydroPoint = new HydrographPoint(subsectorId, hydrographPoint.getALT_AHD(), hydrographPoint.coord);
					subsectorHydroPoint.setSubSector(hydrographPoint.getSubSector());
					subsectorHydroPoint.addLinkId(subsectorId);
					subsectorHydroPoint.setFloodTime(hydrographPoint.getFloodTime());
					consolidatedHydrographPointMap.put(subsectorId, subsectorHydroPoint);
				} else {
					// yoyo set the flood time to the MAXIMUM of the existing and new data point
					double currentFloodTime = subsectorHydroPoint.getFloodTime();
					double newFloodTime = hydrographPoint.getFloodTime();

					subsectorHydroPoint.setFloodTime(Math.max(currentFloodTime, newFloodTime));

				}
			}
		}
		log.info("Done consolidating hydrograph data by link and subsector");
	}

	public void hydrographToViaXY(String fileName) {
		log.info("Writing hydrograph data to a human-readable file that shows the transitions at each point.");
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
		log.info("Done writing hydrograph data to XY file.");
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


	public void hydrographToViaLinkAttributesFromLinkData(String fileName) {
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


	public List<NetworkChangeEvent> networkChangeEventsFromConsolidatedHydrographFloodTimes(Network network) {
		log.info("Generating network change events from hydrograph data...");
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
						//yoyo i cant set the link speed to absolutely zero because then agents get permanently trapped when they enter the link, and never reach the end.
						NetworkChangeEvent.ChangeValue speedChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, link.getLength() / 86400);
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

		log.info("DONE Generating network change events from hydrograph data.");
		return networkChangeEvents;
	}


}
