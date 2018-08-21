package femproto.prepare.evacuationdata;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;

import java.util.*;

public class SubsectorData {
	private static Logger log = Logger.getLogger(SubsectorData.class);
	private final String subsector;

	public void setEvacuationNode(Node evacuationNode) {
		if(this.evacuationNode != null && !evacuationNode.equals(this.evacuationNode)){
			log.warn("Subsector "+subsector+" has evacuation node already set to a different value. Overwriting.");
		}
		this.evacuationNode = evacuationNode;
	}

	private Node evacuationNode;
	private Set<Node> safeNodesByDecreasingPriority = new LinkedHashSet<>(); //maintain insertion order
	private int vehicleCount;
	/**
	 * Allow for the possibility that, at some point during the evacuation, people from an evacuation node need to be
	 * routed to different safe node.
	 */
	TreeMap<Double,Node> safeNodesByTime = new TreeMap<>();

	public void clearSafeNodesByTime(){
		safeNodesByTime.clear();
	}

	public SubsectorData(String subsector) {
		this.subsector = subsector;
	}

	public void addSafeNode(Node node){
		safeNodesByDecreasingPriority.add(node);
		// TODO probably not the best way to initialise... pieter aug'18
		if (safeNodesByTime.size()==0)
			safeNodesByTime.put(0.0, node);
	}

	public void addSafeNodeForTime(Node node, double time){
		safeNodesByTime.put(time, node);
	}

	public String getSubsector() {
		return subsector;
	}

	public Node getEvacuationNode() {
		return evacuationNode;
	}

	public Set<Node> getSafeNodesByDecreasingPriority() {
		return safeNodesByDecreasingPriority;
	}

	public TreeMap<Double, Node> getSafeNodesByTime() {
		return safeNodesByTime;
	}

	public Node getSafeNodeForTime(double time){
		return safeNodesByTime.floorEntry(time).getValue();
	}

	public int getVehicleCount() {
		return vehicleCount;
	}

	public void setVehicleCount(int vehicleCount) {
		this.vehicleCount = vehicleCount;
	}
}
