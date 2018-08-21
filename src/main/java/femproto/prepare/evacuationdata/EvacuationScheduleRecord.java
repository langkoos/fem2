package femproto.prepare.evacuationdata;

import com.opencsv.bean.CsvBindByName;

public final class EvacuationScheduleRecord {
		@CsvBindByName
		public String time;
		@CsvBindByName
		public String subsector;
		@CsvBindByName
		public String evac_node;
		@CsvBindByName
		public String safe_node;

		@Override
		public String toString() {
			return this.time
					+ "\t" + this.subsector
					+ "\t" + this.evac_node
					+ "\t" + this.safe_node
					;
		}
}
