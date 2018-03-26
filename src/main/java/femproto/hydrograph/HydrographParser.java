package femproto.hydrograph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import femproto.gis.Globals;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class HydrographParser {

	public Map<String, HydrographPoint> getHydrographPointMap() {
		return hydrographPointMap;
	}

	private Map<String, HydrographPoint> hydrographPointMap;

	public void hydroPointsShapefile2HydrographPointMap(String shapefile, Network network) {

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapefile);
		CoordinateTransformation transformation = null;
		// coordinate transformation:
		String wkt = null;
		try {
			wkt = IOUtils.getBufferedReader(shapefile.replaceAll("shp$", "prj")).readLine().toString();
			transformation = TransformationFactory.getCoordinateTransformation(wkt, Globals.EPSG28356);
		} catch (IOException e) {
			System.err.println("The shapefile doesn't have a .prj file; continuing, but no guarantees on projection.");
		}

		hydrographPointMap = new HashMap<>();

		//iterate through features and generate pax by subsector
		Iterator<SimpleFeature> iterator = features.iterator();
		long id = 0L;
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();
			String pointID = feature.getAttribute("ID").toString();
			String linkIDs = feature.getAttribute("links_ids").toString();
			final Double ALT_AHD = Double.valueOf(feature.getAttribute("ALT_AHD").toString());
			Coordinate lonlat = ((Geometry) feature.getDefaultGeometry()).getCoordinates()[0];
			Coord coord = new Coord(lonlat.x, lonlat.y);
			if (transformation != null)
				coord = transformation.transform(coord);
			HydrographPoint hydrographPoint = new HydrographPoint(pointID, ALT_AHD, coord);
			hydrographPointMap.put(pointID, hydrographPoint);
			if (linkIDs.equals(""))
				continue;
			else
				hydrographPoint.setLinkIds(linkIDs.split(","));
		}
	}

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
		for (int i = 1; i < header.length; i++) {
			HydrographPoint hydrographPoint = hydrographPointMap.get(header[i]);
			if(hydrographPoint != null){
				for (int j = 0; j < entries.get(i).size(); j++){
					hydrographPoint.addTimeSeriesData(entries.get(0).get(j),entries.get(i).get(j));
				}
			}
		}

	}

	public void removeHydrographPointsWithNoData(){
		Set<String> badkeys = new HashSet<>();
		badkeys.addAll(hydrographPointMap.keySet());

		for (Map.Entry<String, HydrographPoint> pointEntry : hydrographPointMap.entrySet()) {
			if(pointEntry.getValue().inHydrograph())
				badkeys.remove(pointEntry.getKey());
		}

		for (String badkey : badkeys) {
			hydrographPointMap.remove(badkey);
		}


	}

}
