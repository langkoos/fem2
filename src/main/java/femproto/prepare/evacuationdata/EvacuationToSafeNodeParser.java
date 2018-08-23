package femproto.prepare.evacuationdata;

import com.google.inject.Inject;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import femproto.globals.Gis;
import femproto.run.FEMPreferEmergencyLinksTravelDisutility;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static femproto.prepare.network.NetworkConverter.EVACUATION_LINK;

/**
 * This reads the provided evacuation to safe node priority list for each subsector.
 *
 * Values are written to an EvacuationSchedule
 */
public class EvacuationToSafeNodeParser {
	private static final Logger log = Logger.getLogger(EvacuationToSafeNodeParser.class);

	private final Network network;
	private final EvacuationSchedule evacuationSchedule;

	@Inject
	EvacuationToSafeNodeParser(Network network, EvacuationSchedule evacuationSchedule) {
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

		@CsvBindByName
		private String SUBSECTOR;
		@CsvBindByName
		private String EVAC_NODE;
		@CsvBindByName
		private String SAFE_NODE1;
		@CsvBindByName
		private String SAFE_NODE2;
		@CsvBindByName
		private String SAFE_NODE3;
		@CsvBindByName
		private String SAFE_NODE4;
		@CsvBindByName
		private String SAFE_NODE5;

		@Override
		public String toString() {
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
