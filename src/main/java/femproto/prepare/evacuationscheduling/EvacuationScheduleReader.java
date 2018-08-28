package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import femproto.prepare.evacuationdata.SubsectorData;
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
			final CsvToBean<EvacuationScheduleRecordV3> reader2 = new CsvToBeanBuilder<EvacuationScheduleRecordV3>(reader).withType(EvacuationScheduleRecordV3.class).build();

			for (Iterator<EvacuationScheduleRecordV3> it = reader2.iterator(); it.hasNext(); ) {
				EvacuationScheduleRecordV3 record = it.next();
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
			final CsvToBean<EvacuationScheduleRecordV2> reader2 = new CsvToBeanBuilder<EvacuationScheduleRecordV2>(reader).withType(EvacuationScheduleRecordV2.class).build();

			for (Iterator<EvacuationScheduleRecordV2> it = reader2.iterator(); it.hasNext(); ) {
				EvacuationScheduleRecordV2 record = it.next();
				SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(record.getSubsector());
				subsectorData.setEvacuationNode(getNode(record.getEvac_node()));
				subsectorData.addSafeNodeAllocation(record.getTime(), getNode(record.getSafe_node()),record.getVehicles());
			}
			return;
		}catch (Exception e){
			log.warn(fileName + " is not a version 2 EvacuationSchedule. trying v1.");
		}

		try (final FileReader reader = new FileReader(fileName)) {
			final CsvToBean<EvacuationScheduleRecordV1> reader2 = new CsvToBeanBuilder<EvacuationScheduleRecordV1>(reader).withType(EvacuationScheduleRecordV1.class).build();

			// go through the records:
			for (Iterator<EvacuationScheduleRecordV1> it = reader2.iterator(); it.hasNext(); ) {
				EvacuationScheduleRecordV1 record = it.next();
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
