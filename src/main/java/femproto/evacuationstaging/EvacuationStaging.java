package femproto.evacuationstaging;

import java.util.Map;
import java.util.TreeMap;

/**
 * The idea with this class is to provide several different mappings to evacuation staging data,
 * allowing evacuation strategies to schedule timings at a higher level than the population
 */
public class EvacuationStaging {
	/**
	 * arguably not the best way to organise these, but for a start, assuming that, for most cases, everybody will evacuate to the same safe node,
	 * organising subsectors by evacuation time will allow for overall scheduling.
	 * And nothing prevents a subsector from appearing twice; then the time also acts as a key to extract the correct evacuation information.
	 */
	TreeMap<Double, SubsectorData> subsectorsByEvacuationTime;
	/**
	 * Likely will need to access a subsector's information directly.
	 */
	Map<String, SubsectorData> subsectorsBySubsectorName;
}

