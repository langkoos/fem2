package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvBindByName;

public class EvacuationScheduleRecordV2 extends EvacuationScheduleRecordV1 {
	@CsvBindByName(required = true)
	private int vehicles;

	public EvacuationScheduleRecordV2(int time, String subsector, String evac_node, String safe_node) {
		super(time, subsector, evac_node, safe_node);
	}

	public EvacuationScheduleRecordV2(int time, String subsector, String evac_node, String safe_node, int vehicles) {
		super(time, subsector, evac_node, safe_node);
		this.vehicles = vehicles;
	}

	//yoyo need default constructor otherwise opencsv throws instantiationexception
	public EvacuationScheduleRecordV2() {
		super();
	}

	public int getVehicles() {
		return vehicles;
	}

	public void setVehicles(int vehicles) {
		this.vehicles = vehicles;
	}
}
