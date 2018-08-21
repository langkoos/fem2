package femproto.prepare.evacuationdata;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
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

	public EvacuationScheduleReader(EvacuationSchedule evacuationSchedule, Network network) {
		this.evacuationSchedule = evacuationSchedule;
		this.network = network;
	}

	public void readFile(String fileName) throws IOException {

		try (final FileReader reader = new FileReader(fileName)) {

			// construct the csv reader:
			final CsvToBeanBuilder<EvacuationScheduleRecord> builder = new CsvToBeanBuilder<>(reader);
			builder.withType(EvacuationScheduleRecord.class);
			builder.withSeparator(',');
			final CsvToBean<EvacuationScheduleRecord> reader2 = builder.build();

			// go through the records:
			for (Iterator<EvacuationScheduleRecord> it = reader2.iterator(); it.hasNext(); ) {
				EvacuationScheduleRecord record = it.next();
				SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(record.subsector);
				subsectorData.setEvacuationNode(getNode(record.evac_node));
				subsectorData.addSafeNodeForTime(getNode(record.safe_node), Double.parseDouble(record.time));
			}
		}
	}

	public Node getNode(String nodeName) {
		Node node = this.network.getNodes().get(Id.createNodeId(nodeName));
		Gbl.assertNotNull(node);
		return node;
	}
}
