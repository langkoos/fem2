package femproto.prepare.parsers;

import com.google.inject.Inject;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.SubsectorData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This reads the provided evacuation to safe node priority list for each subsector.
 *
 * Values are written to an EvacuationSchedule
 */
public class EvacuationToSafeNodeParser {
	private static final Logger log = Logger.getLogger(EvacuationToSafeNodeParser.class);

	private final Network network;
	private final EvacuationSchedule evacuationSchedule;

	//yoyoyo this will become a comma-separated list as an attribute of the subsector shapefile
	@Inject
	public EvacuationToSafeNodeParser(Network network, EvacuationSchedule evacuationSchedule) {
		this.network = network;
		this.evacuationSchedule = evacuationSchedule;
	}


	public void readEvacAndSafeNodes(String fileName) {
		log.info("entering readEvacAndSafeNodes with fileName=" + fileName);

		try (final FileReader reader = new FileReader(fileName)) {

			// construct the csv reader:
			final CsvToBeanBuilder<Record> builder = new CsvToBeanBuilder<>(reader);
			builder.withType(Record.class);
			builder.withSeparator(';');
			final CsvToBean<Record> reader2 = builder.build();

			// go through the records:
			for (Iterator<Record> it = reader2.iterator(); it.hasNext(); ) {
				Record record = it.next();
				log.info(record.toString());
				SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(record.SUBSECTOR);
				subsectorData.setEvacuationNode(getNode(record.EVAC_NODE));
				subsectorData.addSafeNodeAllocation(0,getNode(record.SAFE_NODE1));
				//keep adding safe nodes until no more
				if(record.SAFE_NODE2 != null)
					subsectorData.addSafeNode(getNode(record.SAFE_NODE2));
				if(record.SAFE_NODE3 != null)
					subsectorData.addSafeNode(getNode(record.SAFE_NODE3));
				if(record.SAFE_NODE4 != null)
					subsectorData.addSafeNode(getNode(record.SAFE_NODE4));
				if(record.SAFE_NODE5 != null)
					subsectorData.addSafeNode(getNode(record.SAFE_NODE5));

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}



	public Node getNode(String nodeName) {
		Node node = this.network.getNodes().get(Id.createNodeId(nodeName));
		Gbl.assertNotNull(node);
		return node;
	}

	public final static class Record {
		// needs to be public, otherwise one gets some incomprehensible exception.  kai, nov'17

		@CsvBindByName private String SUBSECTOR;
		@CsvBindByName private String EVAC_NODE;
		@CsvBindByName private String SAFE_NODE1;
		@CsvBindByName private String SAFE_NODE2;
		@CsvBindByName private String SAFE_NODE3;
		@CsvBindByName private String SAFE_NODE4;
		@CsvBindByName private String SAFE_NODE5;

		@Override public String toString() {
			return this.SUBSECTOR
					+ "\t" + this.EVAC_NODE
					+ "\t" + this.SAFE_NODE1
					+ "\t" + this.SAFE_NODE2
					+ "\t" + this.SAFE_NODE3
					+ "\t" + this.SAFE_NODE4
					+ "\t" + this.SAFE_NODE5
					;
		}
	}
}
