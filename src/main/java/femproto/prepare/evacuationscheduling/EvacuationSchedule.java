package femproto.prepare.evacuationscheduling;

import femproto.prepare.evacuationdata.SafeNodeAllocation;
import femproto.prepare.evacuationdata.SubsectorData;

import java.util.*;

/**
 * The idea with this class is to provide several different mappings to evacuation staging data,
 * allowing evacuation strategies to schedule timings at a higher level than the population
 */
public class EvacuationSchedule {
	public Map<String, SubsectorData> getSubsectorsBySubsectorName() {
		return subsectorsBySubsectorName;
	}

	/**
	 * arguably not the best way to organise these, but for a start, assuming that, for most cases, everybody will evacuate to the same safe node,
	 * organising subsectors by evacuation time will allow for overall scheduling.
	 * And nothing prevents a subsector from appearing twice; then the time also acts as a key to extract the correct evacuation information.
	 */
	private Set<SafeNodeAllocation> subsectorsByEvacuationTime = new TreeSet<>();
	/**
	 * Likely will need to access a subsector's information directly.
	 */
	private Map<String, SubsectorData> subsectorsBySubsectorName = new HashMap<>();

	public Set<SafeNodeAllocation> getSubsectorsByEvacuationTime() {
		return subsectorsByEvacuationTime;
	}

	public void createSchedule() {
		for (SubsectorData subsectorData : subsectorsBySubsectorName.values()) {
			subsectorsByEvacuationTime.addAll(subsectorData.getSafeNodesByTime());
		}

	}

	// this is called if not sure that data has been initialised
	public SubsectorData getOrCreateSubsectorData(String subsector) {
		SubsectorData subsectorData;
		subsectorData = subsectorsBySubsectorName.get(subsector);
		if (subsectorData == null) {
			subsectorData = new SubsectorData(subsector);
			subsectorsBySubsectorName.put(subsectorData.getSubsector(), subsectorData);
		}
		return subsectorData;
	}


}

