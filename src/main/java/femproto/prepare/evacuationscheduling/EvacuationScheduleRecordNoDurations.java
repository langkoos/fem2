package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvBindByName;

public class EvacuationScheduleRecordNoDurations extends EvacuationScheduleRecordNoVehiclesNoDurations {
	// needs to be public otherwise the CsvBind magic does not work. kai, sep'18
	
	@CsvBindByName(required = true)
	private int vehicles;

	public EvacuationScheduleRecordNoDurations(int time, String subsector, String evac_node, String safe_node) {
		super(time, subsector, evac_node, safe_node);
	}

	public EvacuationScheduleRecordNoDurations(int time, String subsector, String evac_node, String safe_node, int vehicles) {
		super(time, subsector, evac_node, safe_node);
		this.vehicles = vehicles;
	}

	//yoyo need default constructor otherwise opencsv throws instantiationexception
	public EvacuationScheduleRecordNoDurations() {
		super();
	}

	public int getVehicles() {
		return vehicles;
	}

	public void setVehicles(int vehicles) {
		this.vehicles = vehicles;
	}
}
