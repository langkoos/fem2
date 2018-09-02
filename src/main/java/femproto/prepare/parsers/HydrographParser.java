package femproto.prepare.parsers;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import femproto.globals.FEMAttributes;
import femproto.globals.Gis;
import femproto.prepare.parsers.HydrographPoint.HydrographPointData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.MatsimNetworkReader;
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

	public Map<String, HydrographPoint> getHydrographPointMap() {
		return hydrographPointMap;
	}

	private Map<String, HydrographPoint> hydrographPointMap;

	/**
	 * This initialises the data structure and populates it with the subsectors and/or link ids that this point may affect,
	 * as well as its altitude.
	 * @param shapefile
	 * @param network
	 */
	public void hydroPointsShapefile2HydrographPointMap(String shapefile, Network network) {

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

			String pointID = feature.getAttribute(FEMAttributes.HYDROGRAPH_POINT_ID_FIELD).toString();
			String linkIDs = feature.getAttribute(FEMAttributes.HYDROGRAPH_LINK_IDS).toString();
			String subsector = feature.getAttribute(FEMAttributes.HYDROGRAPH_SUBSECTOR).toString();
			final Double ALT_AHD = Double.valueOf(feature.getAttribute(FEMAttributes.HYDROGRAPH_ALT_AHD).toString());
			Coordinate lonlat = ((Geometry) feature.getDefaultGeometry()).getCoordinates()[0];
			Coord coord = new Coord(lonlat.x, lonlat.y);
			if (transformation != null)
				coord = transformation.transform(coord);
			HydrographPoint hydrographPoint = new HydrographPoint(pointID, ALT_AHD, coord);
			hydrographPoint.setSubSector(subsector);
			hydrographPointMap.put(pointID, hydrographPoint);
			if (linkIDs.equals(""))
				continue;
			else
				hydrographPoint.setLinkIds(linkIDs.split(","));
		}
	}

	/**
	 * This reads the  time series from the hydrograph file, and populates the relevant point data structures.
	 *
	 * <b>It  also finds the minimum time and subtracts that from all time values, so simulations can start at zero hours.</b>
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



		double minTime = entries.get(0).get(0) * 3600;
		for (int i = 1; i < header.length; i++) {
			HydrographPoint hydrographPoint = hydrographPointMap.get(header[i]);
			if (hydrographPoint != null) {
				for (int j = 0; j < entries.get(i).size(); j++) {
					hydrographPoint.addTimeSeriesData(entries.get(0).get(j) * 3600 - minTime, entries.get(i).get(j));
				}
			}
		}
		//find minimum time value
		//normalise all

	}

	public void removeHydrographPointsWithNoData() {
		Set<String> badkeys = new HashSet<>();
		badkeys.addAll(hydrographPointMap.keySet());

		for (Map.Entry<String, HydrographPoint> pointEntry : hydrographPointMap.entrySet()) {
			if (pointEntry.getValue().inHydrograph())
				badkeys.remove(pointEntry.getKey());
		}

		for (String badkey : badkeys) {
			hydrographPointMap.remove(badkey);
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

	public void hydrographToViaLinkAttributes(String fileName, Network network) {
		Set<Id<Link>> ids = new HashSet<>();
		ids.addAll(network.getLinks().keySet());
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("ID\ttime\tflooded\n");

			for (HydrographPoint point : hydrographPointMap.values()) {
				if (!point.mappedToNetworkLink())
					continue;
				List<HydrographPointData> pointData = point.getData();
				for (HydrographPointData pointDatum : pointData) {
					for (String linkId : point.getLinkIds()) {
						ids.remove(Id.createLinkId(linkId));
						writer.write(String.format("%s\t%f\t%d\n", linkId, pointDatum.getTime(), pointDatum.getLevel_ahd() - point.getALT_AHD() > 0 ? 1 : 0));
					}
				}
			}

			for (Id<Link> id : ids) {
				writer.write(String.format("%s\t%f\t%d\n", id,  0f , 0));
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Something went wrong writing the links plotfile.");
			throw new RuntimeException();
		}

	}

	public void setHydroFloodTimes() {
		for (HydrographPoint point : hydrographPointMap.values()) {
			double floodtime = -1;
			for (HydrographPointData pointDatum : point.getData()) {
				if (pointDatum.getLevel_ahd() - point.getALT_AHD() > 0) {
					floodtime = pointDatum.getTime();
					System.out.println("flooding subsector "+point.getSubSector()+" starts flooding at " +  pointDatum.getTime());
					break;
				}
			}
			point.setFloodTime(floodtime);
		}
	}

	public void networkChangeEventsFromHydrographData(Network network, String outputFileName) {
		Set<NetworkChangeEvent> networkChangeEvents = new HashSet<>();
		for (HydrographPoint point : hydrographPointMap.values()) {
			if (!point.mappedToNetworkLink())
				continue;
			if (point.getFloodTime() < 0)
				continue;
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
		}
		new NetworkChangeEventsWriter().write(outputFileName, networkChangeEvents);
	}


}
