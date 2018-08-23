package femproto.prepare.evacuationdata;

import femproto.prepare.evacuationdata.SubsectorData;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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
	private TreeMap<Double, SubsectorData> subsectorsByEvacuationTime = new TreeMap<>();
	/**
	 * Likely will need to access a subsector's information directly.
	 */
	private Map<String, SubsectorData> subsectorsBySubsectorName = new HashMap<>();

	public TreeMap<Double, SubsectorData> getSubsectorsByEvacuationTime() {
		return subsectorsByEvacuationTime;
	}

	public void createSchedule() {
		Random random = MatsimRandom.getLocalInstance();
		for (SubsectorData subsectorData : subsectorsBySubsectorName.values()) {
			for (Double time : subsectorData.getSafeNodesByTime().keySet()) {
				//yoyoyo this is a cheat to prevent different subsectors from overwriting each other in the map
				subsectorsByEvacuationTime.put(time + random.nextDouble(), subsectorData);
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


}

