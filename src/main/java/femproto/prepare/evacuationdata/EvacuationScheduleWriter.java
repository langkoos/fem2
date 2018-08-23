package femproto.prepare.evacuationdata;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

		for (EvacuationSchedule.TimedSubSectorDataReference subsectorDataEntry : evacuationSchedule.getSubsectorsByEvacuationTime()) {
			int time = (int) (double) subsectorDataEntry.time;
			SubsectorData subsectorData = subsectorDataEntry.data;
			records.add(new EvacuationScheduleRecord( time, subsectorData.getSubsector(), subsectorData.getEvacuationNode().getId().toString(), subsectorData.getSafeNodeForTime(time).getId().toString()));

		}
		beanToCsv.write(records);
		writer.close();
	}
}
