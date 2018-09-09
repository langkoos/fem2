package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvBindByName;

public class EvacuationScheduleRecordNoVehicles extends EvacuationScheduleRecordNoVehiclesNoDurations {
	// needs to be public otherwise the CsvBind magic does not work. kai, sep'18

	@CsvBindByName(required = true)
	private int duration;

	public EvacuationScheduleRecordNoVehicles(int time, String subsector, String evac_node, String safe_node) {
		super(time, subsector, evac_node, safe_node);
	}

	public EvacuationScheduleRecordNoVehicles(int time, String subsector, String evac_node, String safe_node, int duration) {
		super(time, subsector, evac_node, safe_node);
		this.duration = duration;
	}

	//yoyo need default constructor otherwise opencsv throws instantiationexception
	public EvacuationScheduleRecordNoVehicles() {
		super();
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
}
