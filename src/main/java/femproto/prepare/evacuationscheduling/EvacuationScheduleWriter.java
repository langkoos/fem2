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

	public void writeEvacuationScheduleRecordNoVehiclesNoDurations(String fileName)  {
		// I moved the IO exception handling close to where the exceptions occur.  This is the matsim standard.  kai, sep'18

		try ( Writer writer = Files.newBufferedWriter( Paths.get( fileName ) ) ) {
			
			StatefulBeanToCsv<EvacuationScheduleRecordNoVehiclesNoDurations> beanToCsv = new StatefulBeanToCsvBuilder<EvacuationScheduleRecordNoVehiclesNoDurations>( writer ).withQuotechar( '"' ).build();
			
			List<EvacuationScheduleRecordNoVehiclesNoDurations> records = new ArrayList<>();
			
			for ( SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime() ) {
				int time = (int) (double) safeNodeAllocation.getStartTime();
				SubsectorData subsectorData = safeNodeAllocation.getContainer();
				records.add( new EvacuationScheduleRecordNoVehiclesNoDurations( time, subsectorData.getSubsector(), subsectorData.getEvacuationNode().getId().toString(), subsectorData.getSafeNodeForTime( time ).getId().toString() ) );
				
			}
			beanToCsv.write( records );
		} catch ( IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e ) {
			throw new RuntimeException( e ) ;
		}
	}

	public void writeEvacuationScheduleRecordNoVehicles(String fileName)  {
		// I moved the IO exception handling close to where the exceptions occur.  This is the matsim standard.  kai, sep'18
		
		try ( Writer writer = Files.newBufferedWriter( Paths.get( fileName ) ) ) {
			
			StatefulBeanToCsv<EvacuationScheduleRecordNoVehicles> beanToCsv = new StatefulBeanToCsvBuilder<EvacuationScheduleRecordNoVehicles>( writer ).withQuotechar( '"' ).build();
			
			List<EvacuationScheduleRecordNoVehicles> records = new ArrayList<>();
			
			for ( SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime() ) {
				int time = (int) (double) safeNodeAllocation.getStartTime();
				SubsectorData subsectorData = safeNodeAllocation.getContainer();
				records.add( new EvacuationScheduleRecordNoVehicles(
						time,
						subsectorData.getSubsector(),
						subsectorData.getEvacuationNode().getId().toString(),
						subsectorData.getSafeNodeForTime( time ).getId().toString(),
						(int) safeNodeAllocation.getDuration()
				) );
				
			}
			beanToCsv.write( records );
		} catch ( CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e ) {
			throw new RuntimeException(e) ;
		}
	}

	public void writeEvacuationScheduleRecordNoDurations(String fileName)  {
		// I moved the IO exception handling close to where the exceptions occur.  This is the matsim standard.  kai, sep'18
		
		try ( Writer writer = Files.newBufferedWriter( Paths.get( fileName ) ) ) {
			
			StatefulBeanToCsv<EvacuationScheduleRecordNoDurations> beanToCsv = new StatefulBeanToCsvBuilder<EvacuationScheduleRecordNoDurations>( writer ).withQuotechar( '"' ).build();
			
			List<EvacuationScheduleRecordNoDurations> records = new ArrayList<>();
			
			for ( SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime() ) {
				int time = (int) (double) safeNodeAllocation.getStartTime();
				SubsectorData subsectorData = safeNodeAllocation.getContainer();
				records.add( new EvacuationScheduleRecordNoDurations(
						time,
						subsectorData.getSubsector(),
						subsectorData.getEvacuationNode().getId().toString(),
						subsectorData.getSafeNodeForTime( time ).getId().toString(),
						safeNodeAllocation.getVehicles()
				) );
				
			}
			beanToCsv.write( records );
		} catch ( IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e ) {
			throw new RuntimeException(e) ;
		}
	}

	public void writeEvacuationScheduleRecordComplete(String fileName)  {
		// I moved the IO exception handling close to where the exceptions occur.  This is the matsim standard.  kai, sep'18

		try ( Writer writer = Files.newBufferedWriter( Paths.get( fileName ) ) ) {
			
			StatefulBeanToCsv<EvacuationScheduleRecordComplete> beanToCsv = new StatefulBeanToCsvBuilder<EvacuationScheduleRecordComplete>( writer ).withQuotechar( '"' ).build();
			
			List<EvacuationScheduleRecordComplete> records = new ArrayList<>();
			
			for ( SafeNodeAllocation safeNodeAllocation : evacuationSchedule.getSubsectorsByEvacuationTime() ) {
				int time = (int) (double) safeNodeAllocation.getStartTime();
				SubsectorData subsectorData = safeNodeAllocation.getContainer();
				records.add( new EvacuationScheduleRecordComplete(
						time,
						subsectorData.getSubsector(),
						subsectorData.getEvacuationNode().getId().toString(),
						subsectorData.getSafeNodeForTime( time ).getId().toString(),
						safeNodeAllocation.getVehicles(),
						(int) safeNodeAllocation.getDuration()
				) );
				
			}
			beanToCsv.write( records );
		} catch ( IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e ) {
			throw new RuntimeException(e) ;
		}
	}
}
