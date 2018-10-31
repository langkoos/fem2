package femproto.prepare.network;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * FEM V1 files have the nodes x,y coords as lon lat.
 * <p>
 * Via can't handle this. need to project them into a projected CRS
 */
public class FEMV1NetworkConverter {
	public static void main(String[] args) throws FactoryException {
		Network networkIn = NetworkUtils.createNetwork();
		String EPSG28356 = "PROJCS[\"GDA94 / MGA zone 56\", GEOGCS[\"GDA94\", DATUM[\"Geocentric Datum of Australia 1994\", SPHEROID[\"GRS 1980\", 6378137.0, 298.257222101, AUTHORITY[\"EPSG\",\"7019\"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY[\"EPSG\",\"6283\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4283\"]], PROJECTION[\"Transverse_Mercator\", AUTHORITY[\"EPSG\",\"9807\"]], PARAMETER[\"central_meridian\", 153.0], PARAMETER[\"latitude_of_origin\", 0.0], PARAMETER[\"scale_factor\", 0.9996], PARAMETER[\"false_easting\", 500000.0], PARAMETER[\"false_northing\", 10000000.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH], AUTHORITY[\"EPSG\",\"28356\"]]";
		CoordinateReferenceSystem projCRS = CRS.parseWKT(EPSG28356);
		String EPSG4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
		CoordinateReferenceSystem worldCRS = CRS.parseWKT(EPSG4326);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(EPSG4326, EPSG28356);
		new MatsimNetworkReader(networkIn).readFile(args[0]);
		for (Node node : networkIn.getNodes().values()) {
			Coord lonlat = node.getCoord();
			Coord xy = transformation.transform(lonlat);
			node.setCoord(xy);
		}
		new NetworkWriter(networkIn).write(args[1]);
	}
}
