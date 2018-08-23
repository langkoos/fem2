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
		for (EvacuationSchedule.TimedSubSectorDataReference subsectorDataEntry : evacuationSchedule.getSubsectorsByEvacuationTime()) {
			int time = (int) (double) subsectorDataEntry.time;
			SubsectorData subsectorData = subsectorDataEntry.data;
			writer.write(String.format("%d,%s,%s,%s\n", time, subsectorData.getSubsector(), subsectorData.getEvacuationNode().getId().toString(), subsectorData.getSafeNodeForTime(time).getId().toString()));

		}
		writer.close();
	}
}
