package femproto.evacuationstaging;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SubsectorData {
	String subsector;
	Id<Node> evacuationNode;
	List<Id<Node>> safeNodesByDecreasingPriority;
	/**
	 * Allow for the possibility that, at some point during the evacuation, people from an evacuation node need to be
	 * routed to different safe node.
	 */
	TreeMap<Double,Id<Node>> safeNodesByTime;
	/**
	 * If the shortest (or otherwise routed) path is stored for each subsector, would be possible to create timed shapefile of paths
	 * for diagnostics in e.g. Tableau
	 */
	Map<Id<Node>,Path> shortestPathsToSafeNodes;

	Id<Node> getSafeNodeForTime(double time){
		return safeNodesByTime.ceilingEntry(time).getValue();
	}
}
