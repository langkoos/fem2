package femproto.hydrograph;

import femproto.gis.Globals;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.*;

public class HydrographShape2NetworkMappingUtil {
	public static Map<String, String[]> hydroPoints2LinkMap(String shapefile, Network network) {

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapefile);

		// coordinate transformation:
		String wkt = null;
		try {
			wkt = IOUtils.getBufferedReader(shapefile.replaceAll("shp$", "prj")).readLine().toString();
			CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(wkt, Globals.EPSG28356);
		} catch (IOException e) {
			System.err.println("The shapefile doesn't have a .prj file; continuing, but no guarantees on projection.");
		}

		Map<String, String[]> output = new HashMap<>();

		//iterate through features and generate pax by subsector
		Iterator<SimpleFeature> iterator = features.iterator();
		long id = 0L;
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();
			String pointID = feature.getAttribute("ID").toString();
			String linkIDs = feature.getAttribute("links_ids").toString();
			if(linkIDs == null)
				continue;
			output.put(pointID,linkIDs.split(","));
		}

		return output;
	}
}
