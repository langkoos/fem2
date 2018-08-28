package femproto.prepare.evacuationdata;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleReader;
import femproto.prepare.evacuationscheduling.EvacuationScheduleToPopulationDepartures;
import femproto.prepare.evacuationscheduling.EvacuationScheduleWriter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

public class SubsectorDataIntegrationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public void testWriteOnly() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
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

		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleV1(utils.getOutputDirectory()+"simpleEvacuationScheduleV1.csv");

		evacuationSchedule.createSchedule();
		evacuationSchedule.completeAllocations();
		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleV2(utils.getOutputDirectory()+"simpleEvacuationScheduleV2.csv");
		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleV3(utils.getOutputDirectory()+"simpleEvacuationScheduleV3.csv");




	}

	@Test
	public void testReadWriteV1() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		String inputEvacScheduleFile = "test/input/femproto/prepare/evacuationdata/simpleEvacuationScheduleV1.csv";
		String outputFile = utils.getOutputDirectory() + "simpleEvacuationScheduleCopyV1.csv";

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

		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleV1(outputFile);


		long input = CRCChecksum.getCRCFromFile(inputEvacScheduleFile);
		long output = CRCChecksum.getCRCFromFile(outputFile);
		Assert.assertEquals(input,output);
	}

	@Test
	public void testReadWriteV2() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		String inputEvacScheduleFile = "test/input/femproto/prepare/evacuationdata/simpleEvacuationScheduleV2.csv";
		String outputFile = utils.getOutputDirectory() + "simpleEvacuationScheduleCopyV2.csv";

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

		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleV2(outputFile);


		long input = CRCChecksum.getCRCFromFile(inputEvacScheduleFile);
		long output = CRCChecksum.getCRCFromFile(outputFile);
		Assert.assertEquals(input,output);
	}

	@Test
	public void testReadWriteV3() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		String inputEvacScheduleFile = "test/input/femproto/prepare/evacuationdata/simpleEvacuationScheduleV3.csv";
		String outputFile = utils.getOutputDirectory() + "simpleEvacuationScheduleCopyV3.csv";

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

		new EvacuationScheduleWriter(evacuationSchedule).writeScheduleV3(outputFile);


		long input = CRCChecksum.getCRCFromFile(inputEvacScheduleFile);
		long output = CRCChecksum.getCRCFromFile(outputFile);
		Assert.assertEquals(input,output);
	}


	@Test
	public void testSimplePopulationDepartures() {
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		String inputEvactoSafeNode = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/2016_subsectors_safe_node_mapping.txt";

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		SubsectorShapeFileParser subSectorsToPopulation = new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork());
		try {
			subSectorsToPopulation.readSubSectorsShapeFile(inputshapefile);
		} catch (IOException e) {
			throw new RuntimeException("Input shapefile not found, or some other IO error");
		}

		EvacuationToSafeNodeParser parser = new EvacuationToSafeNodeParser(scenario.getNetwork(),evacuationSchedule);
		parser.readEvacAndSafeNodes(inputEvactoSafeNode);

		new EvacuationScheduleToPopulationDepartures(scenario,evacuationSchedule).writePopulation(utils.getOutputDirectory()+"simpledepartures_pop.xml.gz");
	}

}