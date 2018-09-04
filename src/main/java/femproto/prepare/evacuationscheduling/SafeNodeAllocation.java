package femproto.prepare.evacuationscheduling;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;

/**
 * Keeps track of nodes by time, and allows potential for varying rates of evacuation and number of vehicles evacuated to safe node, and simultaneous evacuation of allocation of vehicles to multiple safe nodes.
 */
public class SafeNodeAllocation implements Comparable<SafeNodeAllocation> {
	private final double startTime;
	private final Node node;
	private final SubsectorData container;
	Logger log = Logger.getLogger(SafeNodeAllocation.class);
	private double endTime = Double.NEGATIVE_INFINITY;
	private int vehicles = -Integer.MAX_VALUE;

	SafeNodeAllocation(double startTime, Node node, int vehicles, SubsectorData container) {
		this.startTime = startTime;
		this.container = container;
		this.node = node;
		this.vehicles = vehicles;
	}
	SafeNodeAllocation(double startTime, Node node, SubsectorData container) {
		this.startTime = startTime;
		this.container = container;
		this.node = node;
	}

	SafeNodeAllocation(double startTime, double endTime, Node node, int vehicles, SubsectorData container) {
		this.startTime = startTime;
		if (endTime > startTime)
			this.endTime = endTime;
		else {
			log.warn("Invalid end time for SafeNodeAllocation for Subsector " + container.getSubsector() + ". Overriding.");
		}
		this.node = node;
		this.vehicles = vehicles;
		this.container = container;
	}

	/**
	 * If vehicles are not specified, it is set to -1, which means that the allocation will be done later, weighted by time window size.
	 *
	 * @param startTime
	 * @param endTime
	 * @param node
	 * @param container
	 */
	SafeNodeAllocation(double startTime, double endTime, Node node, SubsectorData container) {
		this.startTime = startTime;
		if (endTime > startTime)
			this.endTime = endTime;
		else {
			log.warn("Invalid end time for SafeNodeAllocation for Subsector " + container.getSubsector() + ". Overriding.");
		}
		this.node = node;
		this.container = container;
	}

	public SubsectorData getContainer() {
		return container;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public Node getNode() {
		return node;
	}

	public int getVehicles() {
		return vehicles;
	}

	public void setVehicles(int vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	public int compareTo(SafeNodeAllocation o) {
		if (this.startTime == o.startTime && this.container == o.container)
			return 0;
		if (this.startTime == o.startTime)
			return this.container.getSubsector().compareTo(o.container.getSubsector());
		if (this.startTime < o.startTime)
			return -1;
		else
			return 1;
	}

	public double getDuration() {
		if(startTime > endTime) {
			log.warn("Invalid end time for SafeNodeAllocation for Subsector " + container.getSubsector() + ". Overriding with one hour duration.");
			return 3600;
		}else {
			return endTime - startTime;
		}
	}
}
