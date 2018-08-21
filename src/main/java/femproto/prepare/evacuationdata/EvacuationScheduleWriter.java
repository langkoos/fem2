package femproto.prepare.evacuationdata;

import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

public class EvacuationScheduleWriter {

	private final EvacuationSchedule evacuationSchedule;

	public EvacuationScheduleWriter(EvacuationSchedule evacuationSchedule) {
		this.evacuationSchedule = evacuationSchedule;
		evacuationSchedule.createSchedule();
	}

	public void writeScheduleCSV(String fileName) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		writer.write("time,subsector,evac_node,safe_node\n");
		for (Map.Entry<Double, SubsectorData> subsectorDataEntry : evacuationSchedule.getSubsectorsByEvacuationTime().entrySet()) {
			double time = subsectorDataEntry.getKey();
			SubsectorData subsectorData = subsectorDataEntry.getValue();
			writer.write(String.format("%f,%s,%s,%s\n", time, subsectorData.getSubsector(), subsectorData.getEvacuationNode().getId().toString(), subsectorData.getSafeNodeForTime(time).getId().toString()));

		}
		writer.close();
	}
}
