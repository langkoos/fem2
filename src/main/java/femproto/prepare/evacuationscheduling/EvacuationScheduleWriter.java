package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

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

	public void writeScheduleV1(String fileName) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		Writer writer = Files.newBufferedWriter(Paths.get(fileName));

		StatefulBeanToCsv<EvacuationScheduleRecordV1> beanToCsv= new StatefulBeanToCsvBuilder<EvacuationScheduleRecordV1>(writer).withQuotechar('"').build();

		List<EvacuationScheduleRecordV1> records = new ArrayList<>();

		for (SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime()) {
			int time = (int) (double) safeNodeAllocation.getStartTime();
			SubsectorData subsectorData = safeNodeAllocation.getContainer();
			records.add(new EvacuationScheduleRecordV1( time, subsectorData.getSubsector(), subsectorData.getEvacuationNode().getId().toString(), subsectorData.getSafeNodeForTime(time).getId().toString()));

		}
		beanToCsv.write(records);
		writer.close();
	}

	public void writeScheduleV2(String fileName) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		Writer writer = Files.newBufferedWriter(Paths.get(fileName));

		StatefulBeanToCsv<EvacuationScheduleRecordV2> beanToCsv= new StatefulBeanToCsvBuilder<EvacuationScheduleRecordV2>(writer).withQuotechar('"').build();

		List<EvacuationScheduleRecordV2> records = new ArrayList<>();

		for (SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime()) {
			int time = (int) (double) safeNodeAllocation.getStartTime();
			SubsectorData subsectorData = safeNodeAllocation.getContainer();
			records.add(new EvacuationScheduleRecordV2(
					time,
					subsectorData.getSubsector(),
					subsectorData.getEvacuationNode().getId().toString(),
					subsectorData.getSafeNodeForTime(time).getId().toString(),
					safeNodeAllocation.getVehicles()
			));

		}
		beanToCsv.write(records);
		writer.close();
	}

	public void writeScheduleV3(String fileName) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		Writer writer = Files.newBufferedWriter(Paths.get(fileName));

		StatefulBeanToCsv<EvacuationScheduleRecordV3> beanToCsv= new StatefulBeanToCsvBuilder<EvacuationScheduleRecordV3>(writer).withQuotechar('"').build();

		List<EvacuationScheduleRecordV3> records = new ArrayList<>();

		for (SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime()) {
			int time = (int) (double) safeNodeAllocation.getStartTime();
			SubsectorData subsectorData = safeNodeAllocation.getContainer();
			records.add(new EvacuationScheduleRecordV3(
					time,
					subsectorData.getSubsector(),
					subsectorData.getEvacuationNode().getId().toString(),
					subsectorData.getSafeNodeForTime(time).getId().toString(),
					safeNodeAllocation.getVehicles(),
					(int) safeNodeAllocation.getDuration()
			));

		}
		beanToCsv.write(records);
		writer.close();
	}
}
