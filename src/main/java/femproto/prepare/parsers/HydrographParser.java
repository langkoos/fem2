package femproto.prepare.parsers;

import femproto.globals.FEMGlobalConfig;
import femproto.globals.Gis;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.SubsectorData;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographPoint.HydrographPointData;
import femproto.run.FEMConfigGroup;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;
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


	private Map<Integer, HydrographPoint> hydrographPointMap;

	public Map<String, HydrographPoint> getConsolidatedHydrographPointMap() {
		return consolidatedHydrographPointMap;
	}

	private Map<String, HydrographPoint> consolidatedHydrographPointMap;

	private final Network network;

	private final EvacuationSchedule evacuationSchedule;

	@Deprecated
	public void parseHydrographShapefile(String shapefile) {
		parseHydrographShapefile(IOUtils.newUrl(null, shapefile));
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
	@Deprecated
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

			int pointID = Integer.parseInt(feature.getAttribute(FEMUtils.getGlobalConfig().getAttribHydrographPointId()).toString());
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
		readHydrographData(IOUtils.newUrl(null, fileName), offsetTime);
	}

	/**
	 * This reads the  time series from the hydrograph file, and populates the relevant point data structures.
	 *
	 * <b>It  also finds the minimum time and subtracts that from all time values, so simulations can start at zero hours.</b>
	 *
	 * @param fileName
	 */
	public void readHydrographData(URL fileName, int offsetTime) {

		createHydrographPointsFromNetworkLinksAndSubsectors();

		List<List<Double>> columns = new ArrayList<>();
		String[] header;
		try (BufferedReader reader = IOUtils.getBufferedReader(fileName.getPath())) {
			header = reader.readLine().split(",");
			for (int ii = 0; ii < header.length; ii++) {
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
			HydrographPoint hydrographPoint = hydrographPointMap.get(Integer.parseInt(header[i]));
			if (hydrographPoint != null) {
				for (int j = 2; j < columns.get(i).size(); j++) {
					//  it might be better top not have BUFFER_TIME in here and only use in the routing of agents, not in generating network change events
					hydrographPoint.addTimeSeriesData(columns.get(0).get(j) * 3600 - minTime + offsetTime, columns.get(i).get(j));
				}
				hydrographPoint.calculateFloodTimeFromData();
			}
		}


		removeHydrographPointsWithNoDataOrThatNeverFlood();

		//yoyo perhaps not best way to maintain backward compatability
		if (consolidatedHydrographPointMap.size() == 0)
			consolidateHydrographPoints();
		else {
			for (HydrographPoint hydrographPoint : consolidatedHydrographPointMap.values()) {
				hydrographPoint.calculateFloodTimeFromData();
			}

		}

	}


	private void removeHydrographPointsWithNoDataOrThatNeverFlood() {
		Set<Integer> badkeys = new HashSet<>();
		badkeys.addAll(hydrographPointMap.keySet());

		for (Map.Entry<Integer, HydrographPoint> pointEntry : hydrographPointMap.entrySet()) {
			if (pointEntry.getValue().inHydrograph() && pointEntry.getValue().getFloodTime() > 0)
				badkeys.remove(pointEntry.getKey());
		}

		for (int badkey : badkeys) {
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
	private void consolidateHydrographPoints() {
		log.info("Consolidating hydrograph data by link and subsector (it is possible to have multiple associations on both so need to sort out which have priority)");
		consolidatedHydrographPointMap = new HashMap<>();
		for (HydrographPoint hydrographPoint : hydrographPointMap.values()) {
			for (String linkId : hydrographPoint.getLinkIds()) {
				HydrographPoint linkHydroPoint = consolidatedHydrographPointMap.get(linkId);
				if (linkHydroPoint == null) {
					linkHydroPoint = new HydrographPoint(hydrographPoint.pointId, hydrographPoint.getALT_AHD(), hydrographPoint.coord);
					linkHydroPoint.addLinkId(linkId);
					linkHydroPoint.setSubSector("");
					linkHydroPoint.setFloodTime(hydrographPoint.getFloodTime());
					consolidatedHydrographPointMap.put(linkId, linkHydroPoint);
				} else {
					//  set the flood time to the minimum of the existing and new data point
					double currentFloodTime = linkHydroPoint.getFloodTime();
					double newFloodTime = hydrographPoint.getFloodTime();
					if (currentFloodTime > 0 && newFloodTime > 0) {
						if (newFloodTime < currentFloodTime) {
							linkHydroPoint.setFloodTime(newFloodTime);
							linkHydroPoint.pointId = hydrographPoint.pointId;
							linkHydroPoint.ALT_AHD = hydrographPoint.ALT_AHD;
						}
					} else {
						if (newFloodTime > currentFloodTime) {
							linkHydroPoint.setFloodTime(newFloodTime);
							linkHydroPoint.pointId = hydrographPoint.pointId;
							linkHydroPoint.ALT_AHD = hydrographPoint.ALT_AHD;
						}
					}
				}
			}
			if (!hydrographPoint.getSubSector().equals("")) {
				String subsectorId = hydrographPoint.getSubSector();
				HydrographPoint subsectorHydroPoint = consolidatedHydrographPointMap.get(subsectorId);
				if (subsectorHydroPoint == null) {
					subsectorHydroPoint = new HydrographPoint(hydrographPoint.pointId, hydrographPoint.getALT_AHD(), hydrographPoint.coord);
					subsectorHydroPoint.setSubSector(hydrographPoint.getSubSector());
					subsectorHydroPoint.setFloodTime(hydrographPoint.getFloodTime());
					consolidatedHydrographPointMap.put(subsectorId, subsectorHydroPoint);
				} else {
					// yoyo set the flood time to the MAXIMUM of the existing and new data point
					double currentFloodTime = subsectorHydroPoint.getFloodTime();
					double newFloodTime = hydrographPoint.getFloodTime();
					if (newFloodTime > currentFloodTime) {
						subsectorHydroPoint.setFloodTime(newFloodTime);
						subsectorHydroPoint.pointId = hydrographPoint.pointId;
						subsectorHydroPoint.ALT_AHD = hydrographPoint.ALT_AHD;
					}

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

	/**
	 * This method is a helper to produce a lookup table and eliminate this entire class in the end
	 */
	public void writeLink2GaugeLookupTable(String outputFileName) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileName);
		try {
			writer.write("ID\tGAUGE_ID\tALT_AHD\n");
			for (HydrographPoint point : consolidatedHydrographPointMap.values()) {
				if (point.mappedToNetworkLink()) {
					for (String linkId : point.getLinkIds()) {
						writer.write(String.format("%s\t%d\t%f\n", linkId.toString(), point.pointId, point.ALT_AHD));
					}
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is a helper to produce a lookup table of subsectors
	 */
	public void writeSubsector2GaugeLookupTable(String outputFileName) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileName);
		try {
			writer.write("SUBSECTOR\tGAUGE_ID\tALT_AHD\n");
			for (HydrographPoint point : consolidatedHydrographPointMap.values()) {
				if (!(point.getSubSector().equals(""))) {

					writer.write(String.format("%s\t%d\t%f\n", point.getSubSector(), point.pointId, point.ALT_AHD));
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		FEMUtils.setGlobalConfig(globalConfig);
		NetworkConverter networkConverter = new NetworkConverter(scenario);
		networkConverter.run();

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		URL subsectorURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getInputSubsectorsShapefile());
		new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile(subsectorURL);

		HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
		URL hydrographURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getHydrographShapeFile());
		hydrographParser.parseHydrographShapefile(hydrographURL);
		hydrographParser.consolidateHydrographPointsForConversion();

		hydrographParser.writeLink2GaugeLookupTable(args[1]);
		hydrographParser.writeSubsector2GaugeLookupTable(args[2]);


	}

	public void createHydrographPointsFromNetworkLinksAndSubsectors() {
		consolidatedHydrographPointMap = new HashMap<>();
		String altAHD = FEMUtils.getGlobalConfig().getAttribHydrographSelectedAltAHD();
		String gaugeId = FEMUtils.getGlobalConfig().getAttribGaugeId();
		for (Link link : network.getLinks().values()) {
			Object ahd = link.getAttributes().getAttribute(altAHD);
			Object gauge = link.getAttributes().getAttribute(gaugeId);
			if(gauge==null) {
				continue;
			}
			if (hydrographPointMap.get((int) gauge) == null)
				hydrographPointMap.put((int) gauge, new HydrographPoint((int) gauge, -1.0, null));
			if (ahd != null) {
				HydrographPoint linkHydroPoint = new HydrographPoint((int) gauge, (double) ahd, null);
				linkHydroPoint.addLinkId(link.getId().toString());
				linkHydroPoint.setReferencePoint(hydrographPointMap.get((int) gauge));
				linkHydroPoint.setSubSector("");
				consolidatedHydrographPointMap.put(link.getId().toString(), linkHydroPoint);
			}
		}
		for (SubsectorData subsectorData : evacuationSchedule.getSubsectorDataMap().values()) {
			if (subsectorData.getAltAHD() > 0) {
				if (subsectorData.getGaugeId() < 0)
					throw new RuntimeException(String.format("Subsector %s has an ALT_AHD value but no gauge id.", subsectorData.getSubsector()));
				if (hydrographPointMap.get(subsectorData.getGaugeId()) == null)
					hydrographPointMap.put(subsectorData.getGaugeId(), new HydrographPoint(subsectorData.getGaugeId(), -1.0, null));
				HydrographPoint hydrographPoint = new HydrographPoint(subsectorData.getGaugeId(), subsectorData.getAltAHD(), null);
				hydrographPoint.setReferencePoint(hydrographPointMap.get(subsectorData.getGaugeId()));
				hydrographPoint.setSubSector(subsectorData.getSubsector());
				consolidatedHydrographPointMap.put(subsectorData.getSubsector(), hydrographPoint);
			}
		}
	}


	private void consolidateHydrographPointsForConversion() {
		log.info("Consolidating hydrograph data by link and subsector (it is possible to have multiple associations on both so need to sort out which have priority)");
		consolidatedHydrographPointMap = new HashMap<>();
		for (HydrographPoint hydrographPoint : hydrographPointMap.values()) {
			for (String linkId : hydrographPoint.getLinkIds()) {
				HydrographPoint linkHydroPoint = consolidatedHydrographPointMap.get(linkId);
				if (linkHydroPoint == null) {
					linkHydroPoint = new HydrographPoint(hydrographPoint.pointId, hydrographPoint.getALT_AHD(), hydrographPoint.coord);
					linkHydroPoint.addLinkId(linkId);
					linkHydroPoint.setSubSector("");
					consolidatedHydrographPointMap.put(linkId, linkHydroPoint);
				} else {
					//  set the ALT_AHD to the minimum of the existing and new data point
					double current_ALT = linkHydroPoint.ALT_AHD;
					double new_ALT_AHD = hydrographPoint.ALT_AHD;
					if (new_ALT_AHD < current_ALT) {
						linkHydroPoint.setFloodTime(new_ALT_AHD);
						linkHydroPoint.pointId = hydrographPoint.pointId;
						linkHydroPoint.ALT_AHD = hydrographPoint.ALT_AHD;
					}
				}
			}
			if (!hydrographPoint.getSubSector().equals("")) {
				String subsectorId = hydrographPoint.getSubSector();
				HydrographPoint subsectorHydroPoint = consolidatedHydrographPointMap.get(subsectorId);
				if (subsectorHydroPoint == null) {
					subsectorHydroPoint = new HydrographPoint(hydrographPoint.pointId, hydrographPoint.getALT_AHD(), hydrographPoint.coord);
					subsectorHydroPoint.setSubSector(hydrographPoint.getSubSector());
					consolidatedHydrographPointMap.put(subsectorId, subsectorHydroPoint);
				} else {
					// set the ALT_AHD to the maximum of the existing and new data point
					double current_ALT = subsectorHydroPoint.ALT_AHD;
					double new_ALT_AHD = hydrographPoint.ALT_AHD;
					if (new_ALT_AHD > current_ALT) {
						subsectorHydroPoint.setFloodTime(current_ALT);
						subsectorHydroPoint.pointId = hydrographPoint.pointId;
						subsectorHydroPoint.ALT_AHD = hydrographPoint.ALT_AHD;
					}

				}
			}
		}
		log.info("Done consolidating hydrograph data by link and subsector");
	}
}
