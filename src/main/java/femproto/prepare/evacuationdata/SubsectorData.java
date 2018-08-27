package femproto.prepare.evacuationdata;

import femproto.globals.FEMAttributes;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

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
		safeNodesByTime.add(new SafeNodeAllocation(startTime, endTime, node, vehicles));
		if(!safeNodesByDecreasingPriority.contains(node))
			addSafeNode(node);
	}

	public void addSafeNodeAllocation(double startTime, Node node, int vehicles) {
		safeNodesByTime.add(new SafeNodeAllocation(startTime, node, vehicles));
		if(!safeNodesByDecreasingPriority.contains(node))
			addSafeNode(node);
	}

	public void addSafeNodeAllocation(double startTime, Node node) {
		safeNodesByTime.add(new SafeNodeAllocation(startTime, node));
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
	 * Keeps track of nodes by time, and allows potential for varying rates of evacuation and number of vehicles evacuated to safe node, and simultaneous evacuation of allocation of vehicles to multiple safe nodes.
	 */
	public class SafeNodeAllocation implements Comparable<SafeNodeAllocation> {
		public final double startTime;
		public final double endTime;
		public final Node node;
		public final int vehicles;

		SafeNodeAllocation(double startTime, Node node, int vehicles) {
			this.startTime = startTime;
			this.endTime = startTime + 3600 * vehicles / FEMAttributes.EVAC_FLOWRATE; //expected end time of evacuating these vehicles
			this.node = node;
			this.vehicles = vehicles;
		}

		SafeNodeAllocation(double startTime, double endTime, Node node, int vehicles) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.node = node;
			this.vehicles = vehicles;
		}

		/**
		 * If vehicles are not specified, it is set to -1, which means that the subsector will keep evacuating at governing rate until <tt>endTime</tt>
		 *
		 * @param startTime
		 * @param endTime
		 * @param node
		 */
		SafeNodeAllocation(double startTime, double endTime, Node node) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.node = node;
			this.vehicles = -1;
		}

		/**
		 * If <tt>vehicles</tt> and <tt>endTime</tt> are not specified, subsector will keep evacuating at governing rate until next {@link SafeNodeAllocation}.
		 *
		 * @param startTime
		 * @param node
		 */
		SafeNodeAllocation(double startTime, Node node) {
			this.startTime = startTime;
			this.endTime = Double.POSITIVE_INFINITY;
			this.node = node;
			this.vehicles = -1;
		}

		@Override
		public int compareTo(SafeNodeAllocation o) {
			if (this.startTime == o.startTime && this.node == o.node)
				return 0;
			if (this.startTime <= o.startTime)
				return -1;
			else
				return 1;
		}
	}
}
