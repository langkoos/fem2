package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvBindByName;


/**
 * this basic version of an evac schedule does not have number of vehicles allocated, nor end time.
 */
public class EvacuationScheduleRecordNoVehiclesNoDurations {
	// needs to be public otherwise the CsvBind magic does not work. kai, sep'18

	@CsvBindByName(required = true)
	private  int time;
	@CsvBindByName(required = true)
	private  String subsector;
	@CsvBindByName(required = true)
	private  String evac_node;
	@CsvBindByName(required = true)
	private  String safe_node;

	public EvacuationScheduleRecordNoVehiclesNoDurations(int time, String subsector, String evac_node, String safe_node) {
		this.time = time;
		this.subsector = subsector;
		this.evac_node = evac_node;
		this.safe_node = safe_node;
	}
	//yoyo need default constructor otherwise opencsv throws instantiationexception
	public EvacuationScheduleRecordNoVehiclesNoDurations() {
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getSubsector() {
		return subsector;
	}

	public void setSubsector(String subsector) {
		this.subsector = subsector;
	}

	public String getEvac_node() {
		return evac_node;
	}

	public void setEvac_node(String evac_node) {
		this.evac_node = evac_node;
	}

	public String getSafe_node() {
		return safe_node;
	}

	public void setSafe_node(String safe_node) {
		this.safe_node = safe_node;
	}

	@Override
	public String toString() {
		return this.time
				+ "\t" + this.subsector
				+ "\t" + this.evac_node
				+ "\t" + this.safe_node
				;
	}
}
