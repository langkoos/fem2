package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvBindByName;

public class EvacuationScheduleRecordComplete extends EvacuationScheduleRecordNoDurations {
	// needs to be public otherwise the CsvBind magic does not work. kai, sep'18
	
	@CsvBindByName(required = true)
	private int duration;

	public EvacuationScheduleRecordComplete(int time, String subsector, String evac_node, String safe_node, int vehicles, int duration) {
		super(time, subsector, evac_node, safe_node, vehicles);
		this.duration = duration;
	}

	public EvacuationScheduleRecordComplete() {
		super();
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
}
