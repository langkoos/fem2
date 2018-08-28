package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvBindByName;

public class EvacuationScheduleRecordV3 extends EvacuationScheduleRecordV2 {
	@CsvBindByName(required = true)
	private int duration;

	public EvacuationScheduleRecordV3(int time, String subsector, String evac_node, String safe_node, int vehicles, int duration) {
		super(time, subsector, evac_node, safe_node, vehicles);
		this.duration = duration;
	}

	public EvacuationScheduleRecordV3() {
		super();
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
}
