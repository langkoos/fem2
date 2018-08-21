package femproto.prepare.evacuationstaging;

import femproto.prepare.evacuationdata.EvacuationSchedule;
import femproto.prepare.evacuationdata.EvacuationToSafeNodeMapping;
import femproto.prepare.evacuationdata.SubsectorData;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Creates a simple evacuation schedule where everybody departs at the same time.
 */
public class SimpleEvacuationScheduleCreator {
	private final EvacuationToSafeNodeMapping nodeMapping;
	private final Scenario scenario;

	public EvacuationSchedule getEvacuationSchedule() {
		return evacuationSchedule;
	}

	private EvacuationSchedule evacuationSchedule;

	public SimpleEvacuationScheduleCreator(EvacuationToSafeNodeMapping nodeMapping) {
		this.nodeMapping = nodeMapping;
		evacuationSchedule = new EvacuationSchedule();
		scenario = nodeMapping.getScenario();
	}

	public void run() {
		Map<String, EvacuationToSafeNodeMapping.Record> evacAndSafeNodes = nodeMapping.getSubsectorToEvacAndSafeNodes();
		for (String subsector : evacAndSafeNodes.keySet()) {
			SubsectorData subsectorData = new SubsectorData(subsector, nodeMapping.getEvacNode(subsector), nodeMapping.getSafeNodesByDecreasingPriority(subsector));
			evacuationSchedule.addSubsectorData(subsectorData);
		}
	}

	public void writeScheduleCSV(String fileName) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		writer.write("time,subsector,evac_node,safe_node\n");
		for (Map.Entry<Double, SubsectorData> subsectorDataEntry : evacuationSchedule.getSubsectorsByEvacuationTime().entrySet()) {
			double time = subsectorDataEntry.getKey();
			SubsectorData subsectorData = subsectorDataEntry.getValue();
			writer.write(String.format("%f,%s,%s,%s\n",time,subsectorData.getSubsector(),subsectorData.getEvacuationNode().getId().toString(),subsectorData.getSafeNodesByTime().firstEntry().getValue().getId().toString()));

		}
		writer.close();
	}


}
