package femproto.prepare.evacuationdata;

import org.matsim.api.core.v01.network.Node;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SubsectorData {
	private final String subsector;
	private  Node evacuationNode;
	private  List<Node> safeNodesByDecreasingPriority;
	private int vehicleCount;
	/**
	 * Allow for the possibility that, at some point during the evacuation, people from an evacuation node need to be
	 * routed to different safe node.
	 */
	TreeMap<Double,Node> safeNodesByTime;
	/**
	 * If the shortest (or otherwise routed) path is stored for each subsector, would be possible to create timed shapefile of paths
	 * for diagnostics in e.g. Tableau
	 */
	Map<Node,Path> shortestPathsToSafeNodes;

	public SubsectorData(String subsector, Node evacuationNode, List<Node> safeNodesByDecreasingPriority) {
		this.subsector = subsector;
		this.evacuationNode = evacuationNode;
		this.safeNodesByDecreasingPriority = safeNodesByDecreasingPriority;
		safeNodesByTime = new TreeMap<>();
		// add only the first safe node to the timing map
		safeNodesByTime.put(0.0,safeNodesByDecreasingPriority.get(0));
	}

	public SubsectorData(String subsector) {
		this.subsector = subsector;
	}

	public String getSubsector() {
		return subsector;
	}

	public Node getEvacuationNode() {
		return evacuationNode;
	}

	public List<Node> getSafeNodesByDecreasingPriority() {
		return safeNodesByDecreasingPriority;
	}

	public TreeMap<Double, Node> getSafeNodesByTime() {
		return safeNodesByTime;
	}

	public Map<Node, Path> getShortestPathsToSafeNodes() {
		return shortestPathsToSafeNodes;
	}

	Node getSafeNodeForTime(double time){
		return safeNodesByTime.ceilingEntry(time).getValue();
	}

	public int getVehicleCount() {
		return vehicleCount;
	}

	public void setVehicleCount(int vehicleCount) {
		this.vehicleCount = vehicleCount;
	}
}
