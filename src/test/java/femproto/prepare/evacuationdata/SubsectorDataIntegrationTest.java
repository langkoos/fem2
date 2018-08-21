package femproto.prepare.evacuationdata;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.IOException;

import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.compare;

public class SubsectorDataIntegrationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public void testWriteOnly() throws IOException {
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		String inputEvactoSafeNode = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/2016_subsectors_safe_node_mapping.txt";

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		SubsectorShapeFileParser subSectorsToPopulation = new SubsectorShapeFileParser(evacuationSchedule, network);
		try {
			subSectorsToPopulation.readSubSectorsShapeFile(inputshapefile);
		} catch (IOException e) {
			throw new RuntimeException("Input shapefile not found, or some other IO error");
		}

		EvacuationToSafeNodeParser parser = new EvacuationToSafeNodeParser(network,evacuationSchedule);
		parser.readEvacAndSafeNodes(inputEvactoSafeNode);

		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleCSV(utils.getOutputDirectory()+"simpleEvacuationSchedule.csv");
	}

	@Test
	public void testReadWrite() throws IOException {
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		String inputEvacScheduleFile = "test/input/femproto/prepare/evacuationdata/simpleEvacuationSchedule.csv";

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		SubsectorShapeFileParser subSectorsToPopulation = new SubsectorShapeFileParser(evacuationSchedule, network);
		try {
			subSectorsToPopulation.readSubSectorsShapeFile(inputshapefile);
		} catch (IOException e) {
			throw new RuntimeException("Input shapefile not found, or some other IO error");
		}

		new EvacuationScheduleReader(evacuationSchedule,network).readFile(inputEvacScheduleFile);

		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleCSV(utils.getOutputDirectory()+"simpleEvacuationScheduleCopy.csv");
	}
}