package femproto.prepare.evacuationdata;

import org.matsim.core.gbl.MatsimRandom;

import java.util.*;

/**
 * The idea with this class is to provide several different mappings to evacuation staging data,
 * allowing evacuation strategies to schedule timings at a higher level than the population
 */
public class EvacuationSchedule {
	/**
	 * arguably not the best way to organise these, but for a start, assuming that, for most cases, everybody will evacuate to the same safe node,
	 * organising subsectors by evacuation time will allow for overall scheduling.
	 * And nothing prevents a subsector from appearing twice; then the time also acts as a key to extract the correct evacuation information.
	 */
	private Set<TimedSubSectorDataReference> subsectorsByEvacuationTime = new TreeSet<>();
	/**
	 * Likely will need to access a subsector's information directly.
	 */
	private Map<String, SubsectorData> subsectorsBySubsectorName = new HashMap<>();

	public Set<TimedSubSectorDataReference> getSubsectorsByEvacuationTime() {
		return subsectorsByEvacuationTime;
	}

	public void createSchedule() {
		for (SubsectorData subsectorData : subsectorsBySubsectorName.values()) {
			for (SubsectorData.SafeNodeAllocation safeNodeAllocation : subsectorData.getSafeNodesByTime()) {
				subsectorsByEvacuationTime.add(new TimedSubSectorDataReference(safeNodeAllocation.startTime, subsectorData));
			}
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

	class TimedSubSectorDataReference implements Comparable<TimedSubSectorDataReference> {

		public final double time;
		public final SubsectorData data;

		private TimedSubSectorDataReference(double time, SubsectorData data) {
			this.time = time;
			this.data = data;
		}

		@Override
		public int compareTo(TimedSubSectorDataReference o) {
			if (o.time == this.time && o.data == this.data)
				return 0;
			if (o.time == this.time)
				return this.data.getSubsector().compareTo(o.data.getSubsector());
			if (this.time < o.time)
				return -1;
			else
				return 1;
		}
	}
}

