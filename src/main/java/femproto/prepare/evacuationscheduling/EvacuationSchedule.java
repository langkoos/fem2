package femproto.prepare.evacuationscheduling;

import java.util.*;

/**
 * The idea with this class is to provide several different mappings to evacuation staging data,
 * allowing evacuation strategies to schedule timings at a higher level than the population
 */
public final class EvacuationSchedule {
	/**
	 * arguably not the best way to organise these, but for a start, assuming that, for most cases, everybody will evacuate to the same safe node,
	 * organising subsectors by evacuation time will allow for overall scheduling.
	 * And nothing prevents a subsector from appearing twice; then the time also acts as a key to extract the correct evacuation information.
	 */
	private Set<SafeNodeAllocation> subsectorsByEvacuationTime = new TreeSet<>();
	/**
	 * Likely will need to access a subsector's information directly.
	 */
	private Map<String, SubsectorData> subsectorDataMap = new HashMap<>();

	public void createSchedule() {
		for (SubsectorData subsectorData : subsectorDataMap.values()) {
			subsectorsByEvacuationTime.addAll(subsectorData.getSafeNodesByTime());
		}

	}

	// this is called if not sure that data has been initialised
	public SubsectorData getOrCreateSubsectorData(String subsector) {
		SubsectorData subsectorData;
		subsectorData = subsectorDataMap.get(subsector);
		if (subsectorData == null) {
			subsectorData = new SubsectorData(subsector);
			subsectorDataMap.put(subsectorData.getSubsector(), subsectorData);
		}
		return subsectorData;
	}

	public void completeAllocations(){
		for (SubsectorData subsectorData : subsectorDataMap.values()) {
			subsectorData.completeAllocations();
		}

	}
	
	Map<String, SubsectorData> getSubsectorDataMap() {
		return subsectorDataMap;
	}
	Set<SafeNodeAllocation> getSubsectorsByEvacuationTime() {
		return subsectorsByEvacuationTime;
	}
	
	
	
}

