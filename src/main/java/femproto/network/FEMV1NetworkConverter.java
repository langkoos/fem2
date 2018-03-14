package femproto.network;

import femproto.gis.Globals;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * FEM V1 files have the nodes x,y coords as lon lat.
 * <p>
 * Via can't handle this. need to project them into a projected CRS
 */
public class FEMV1NetworkConverter {
	public static void main(String[] args) {
		Network networkIn = NetworkUtils.createNetwork();
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, Globals.EPSG28356);
		new MatsimNetworkReader(networkIn).readFile(args[0]);
		for (Node node : networkIn.getNodes().values()) {
			Coord lonlat = node.getCoord();
			Coord xy = transformation.transform(lonlat);
			node.setCoord(xy);
		}
		new NetworkWriter(networkIn).write(args[1]);
	}
}
