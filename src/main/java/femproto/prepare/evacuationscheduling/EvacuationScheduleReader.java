package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;

import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

/**
 * reads an evacuation schedule file and updates the evacuation data
 */
public class EvacuationScheduleReader {
	private final EvacuationSchedule evacuationSchedule;
	private final Network network;
	Logger log = Logger.getLogger(EvacuationScheduleReader.class);

	public EvacuationScheduleReader(EvacuationSchedule evacuationSchedule, Network network) {
		this.evacuationSchedule = evacuationSchedule;
		this.network = network;
	}

	/**
	 * This will look at the header of the csv file, and try and pick an appropriate builder.
	 * @param fileName
	 * @throws IOException
	 */
	public void readFile(String fileName) throws IOException {

		try (final FileReader reader = new FileReader(fileName)) {

			// construct the csv reader:
			final CsvToBean<EvacuationScheduleRecordComplete> reader2 = new CsvToBeanBuilder<EvacuationScheduleRecordComplete>(reader).withType(EvacuationScheduleRecordComplete.class).build();

			for (Iterator<EvacuationScheduleRecordComplete> it = reader2.iterator(); it.hasNext(); ) {
				EvacuationScheduleRecordComplete record = it.next();
				SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(record.getSubsector());
				subsectorData.setEvacuationNode(getNode(record.getEvac_node()));
				subsectorData.addSafeNodeAllocation(record.getTime(), record.getTime() + record.getDuration(), getNode(record.getSafe_node()), record.getVehicles());
			}
			return;
		} catch (Exception e){
			log.warn(fileName + " is not a version 3 EvacuationSchedule. trying v2.");
		}

		try (final FileReader reader = new FileReader(fileName)) {


			// construct the csv reader:
			final CsvToBean<EvacuationScheduleRecordNoDurations> reader2 = new CsvToBeanBuilder<EvacuationScheduleRecordNoDurations>(reader).withType(EvacuationScheduleRecordNoDurations.class).build();

			for (Iterator<EvacuationScheduleRecordNoDurations> it = reader2.iterator(); it.hasNext(); ) {
				EvacuationScheduleRecordNoDurations record = it.next();
				SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(record.getSubsector());
				subsectorData.setEvacuationNode(getNode(record.getEvac_node()));
				subsectorData.addSafeNodeAllocation(record.getTime(), getNode(record.getSafe_node()),record.getVehicles());
			}
			return;
		}catch (Exception e){
			log.warn(fileName + " is not a version 2 EvacuationSchedule. trying v1.");
		}

		try (final FileReader reader = new FileReader(fileName)) {
			final CsvToBean<EvacuationScheduleRecordNoVehiclesNoDurations> reader2 = new CsvToBeanBuilder<EvacuationScheduleRecordNoVehiclesNoDurations>(reader).withType(EvacuationScheduleRecordNoVehiclesNoDurations.class).build();

			// go through the records:
			for (Iterator<EvacuationScheduleRecordNoVehiclesNoDurations> it = reader2.iterator(); it.hasNext(); ) {
				EvacuationScheduleRecordNoVehiclesNoDurations record = it.next();
				SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(record.getSubsector());
				subsectorData.setEvacuationNode(getNode(record.getEvac_node()));
				subsectorData.addSafeNodeAllocation(record.getTime(), getNode(record.getSafe_node()));
			}
			return;

		}
	}

	public Node getNode(String nodeName) {
		Node node = this.network.getNodes().get(Id.createNodeId(nodeName));
		Gbl.assertNotNull(node);
		return node;
	}
}
