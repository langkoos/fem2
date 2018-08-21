package femproto.prepare.evacuationdata;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SubsectorData {
	private static Logger log = Logger.getLogger(SubsectorData.class);
	private final String subsector;

	public void setEvacuationNode(String evacuationNode) {
		if(evacuationNode != null && !evacuationNode.equals(this.evacuationNode)){
			log.warn("Subsector "+subsector+" has evacuation node already set to a different value. Overwriting.");
		}
		this.evacuationNode = evacuationNode;
	}

	private String evacuationNode;
	private List<String> safeNodesByDecreasingPriority;
	private int vehicleCount;
	/**
	 * Allow for the possibility that, at some point during the evacuation, people from an evacuation node need to be
	 * routed to different safe node.
	 */
	TreeMap<Double,String> safeNodesByTime;


	public SubsectorData(String subsector, String evacuationNode, List<String> safeNodesByDecreasingPriority) {
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

	public String getEvacuationNode() {
		return evacuationNode;
	}

	public List<String> getSafeNodesByDecreasingPriority() {
		return safeNodesByDecreasingPriority;
	}

	public TreeMap<Double, String> getSafeNodesByTime() {
		return safeNodesByTime;
	}

	public String getSafeNodeForTime(double time){
		return safeNodesByTime.ceilingEntry(time).getValue();
	}

	public int getVehicleCount() {
		return vehicleCount;
	}

	public void setVehicleCount(int vehicleCount) {
		this.vehicleCount = vehicleCount;
	}
}
