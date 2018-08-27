package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import femproto.prepare.evacuationdata.SafeNodeAllocation;
import femproto.prepare.evacuationdata.SubsectorData;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EvacuationScheduleWriter {

	private final EvacuationSchedule evacuationSchedule;

	public EvacuationScheduleWriter(EvacuationSchedule evacuationSchedule) {
		this.evacuationSchedule = evacuationSchedule;
		evacuationSchedule.createSchedule();
	}

	public void writeScheduleCSV(String fileName) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		Writer writer = Files.newBufferedWriter(Paths.get(fileName));

		StatefulBeanToCsv<EvacuationScheduleRecord> beanToCsv= new StatefulBeanToCsvBuilder<EvacuationScheduleRecord>(writer).withQuotechar('"').build();

		List<EvacuationScheduleRecord> records = new ArrayList<>();

		for (SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime()) {
			int time = (int) (double) safeNodeAllocation.startTime;
			SubsectorData subsectorData = safeNodeAllocation.container;
			records.add(new EvacuationScheduleRecord( time, subsectorData.getSubsector(), subsectorData.getEvacuationNode().getId().toString(), subsectorData.getSafeNodeForTime(time).getId().toString()));

		}
		beanToCsv.write(records);
		writer.close();
	}
}
