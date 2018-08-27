package femproto.prepare.evacuationdata;

import femproto.globals.FEMAttributes;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.*;

public class SubsectorData {
	private static Logger log = Logger.getLogger(SubsectorData.class);
	private final String subsector;
	/**
	 * Allow for the possibility that, at some point during the evacuation, people from an evacuation node need to be
	 * routed to different safe node.
	 */

	private TreeSet<SafeNodeAllocation> safeNodesByTime = new TreeSet<>();
	private Node evacuationNode;
	private LinkedHashSet<Node> safeNodesByDecreasingPriority = new LinkedHashSet<>(); //maintain insertion order
	private int vehicleCount;

	public SubsectorData(String subsector) {
		this.subsector = subsector;
	}

	public void clearSafeNodesByTime() {
		safeNodesByTime.clear();
	}

	public void addSafeNode(Node node) {
		safeNodesByDecreasingPriority.add(node);
	}

	public void addSafeNodeAllocation(double startTime, double endTime, Node node, int vehicles) {
		safeNodesByTime.add(new SafeNodeAllocation(startTime, endTime, node, vehicles, this));
		if(!safeNodesByDecreasingPriority.contains(node))
			addSafeNode(node);
	}

	public void addSafeNodeAllocation(double startTime, Node node) {
		safeNodesByTime.add(new SafeNodeAllocation(startTime, node,  this));
		if(!safeNodesByDecreasingPriority.contains(node))
			addSafeNode(node);
	}

	public void addSafeNodeAllocation(double startTime, Node node, int vehicles) {
		safeNodesByTime.add(new SafeNodeAllocation(startTime, node, vehicles, this));
		if(!safeNodesByDecreasingPriority.contains(node))
			addSafeNode(node);
	}

	public void addSafeNodeAllocation(double startTime, double endTime, Node node) {
		safeNodesByTime.add(new SafeNodeAllocation(startTime, endTime, node, this));
		if(!safeNodesByDecreasingPriority.contains(node))
			addSafeNode(node);
	}

	public String getSubsector() {
		return subsector;
	}

	public Node getEvacuationNode() {
		return evacuationNode;
	}

	public void setEvacuationNode(Node evacuationNode) {
		if (this.evacuationNode != null && !evacuationNode.equals(this.evacuationNode)) {
			log.warn("Subsector " + subsector + " has evacuation node already set to a different value. Overwriting.");
		}
		this.evacuationNode = evacuationNode;
	}

	public Set<Node> getSafeNodesByDecreasingPriority() {
		return safeNodesByDecreasingPriority;
	}

	public Set<SafeNodeAllocation> getSafeNodesByTime() {
		return safeNodesByTime;
	}

	/**
	 * @param time
	 * @return the first safe node, or the last safe node with time <= time.
	 */
	public Node getSafeNodeForTime(double time) {
		Node node = null;
		for (SafeNodeAllocation safeNodeAllocation : safeNodesByTime) {
			if (safeNodeAllocation.startTime <= time)
				node = safeNodeAllocation.node;
		}
		if (node == null)
			node = safeNodesByTime.first().node;

		return node;

	}

	public int getVehicleCount() {
		return vehicleCount;
	}

	public void setVehicleCount(int vehicleCount) {
		this.vehicleCount = vehicleCount;
	}

	/**
	 * When a schedule has been completely parsed, not all {@link SafeNodeAllocation} objects will have a vehicle count, end time assigned.
	 * We need all these to be pre-specified, otherwise destinations need to be changed while the simulation is running, which we don't want to do.
	 *
	 * This will go through the list and complete with expected values where information is missing, and make the schedule explicit.
	 *
	 * IMPORTANT: assume departure rate specified in {@link femproto.globals.FEMAttributes}, unless <tt>vehicles</tt> and <tt>endTime</tt> have both been set explicitly.
	 * If only one {@link SafeNodeAllocation} exists and it has fewer vehicles assigned than the subsector total, tough cookies, but raise a warning.
	 */
	public void completeAllocations(){

		LinkedList<SafeNodeAllocation> noVehicles = new LinkedList<>();
		LinkedList<SafeNodeAllocation> noDurations = new LinkedList<>();

		int numberOfSafeNodes = safeNodesByTime.size();
		int vehiclesAllocated = 0;
		double sumTimeWeights = 0;


		for (SafeNodeAllocation safeNodeAllocation : safeNodesByTime) {
			vehiclesAllocated += safeNodeAllocation.vehicles > 0 ? safeNodeAllocation.vehicles : 0;
			sumTimeWeights += safeNodeAllocation.endTime > safeNodeAllocation.startTime ? safeNodeAllocation.endTime - safeNodeAllocation.startTime : 0;

			if(safeNodeAllocation.vehicles < 0)
				noVehicles.add(safeNodeAllocation);
			if(safeNodeAllocation.endTime < safeNodeAllocation.startTime)
				noDurations.add(safeNodeAllocation);

		}

		int remainingVehicles = vehicleCount - vehiclesAllocated;
		if(remainingVehicles < 0  ) {
			log.warn("Subsector " + subsector + " has more vehicles evacuating from it than what was specified in the shapefile.");
			return;
		}

		//fill in expected durations
		if(remainingVehicles == 0){
			if(noDurations.size()>0){
				for (SafeNodeAllocation noDuration : noDurations) {
					//just allocate acording to evac rate
					noDuration.endTime = noDuration.startTime + noDuration.vehicles * 3600 / FEMAttributes.EVAC_FLOWRATE;
				}
			}
			return;
		}

		// split the remaining vehicles equally between allocations
		if(noVehicles.size()>0 ){
			for (SafeNodeAllocation nodeAllocation : noVehicles) {
				nodeAllocation.vehicles = remainingVehicles/noVehicles.size();
				remainingVehicles -= nodeAllocation.vehicles;
			}
			if(remainingVehicles > 0)
				noVehicles.getLast().vehicles += remainingVehicles;
			// run the whole thing again to do the rest of rebalancing
			completeAllocations();

		}




	}


}
