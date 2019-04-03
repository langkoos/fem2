package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.*;
import femproto.prepare.parsers.EvacuationToSafeNodeParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.IOException;
import java.util.Iterator;

import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.compare;

public class RunFromScheduleTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test()  {


		String utilsPackageInputDir = utils.getPackageInputDirectory();
		String utilsOutputDir = utils.getOutputDirectory();
		Config config = ConfigUtils.loadConfig(utilsPackageInputDir +"scenario/config_runFromSchedule.xml");
		config.controler().setOutputDirectory(utilsOutputDir);
		new RunMatsim4FloodEvacuation(config).run();


		Network network = NetworkUtils.readNetwork(utilsPackageInputDir + "scenario/input_network.xml");
		EvacuationSchedule expectedSchedule = new EvacuationSchedule();
		new EvacuationScheduleReader(expectedSchedule, network).readFile(utilsPackageInputDir + "scenario/input_evac_plan.csv");
		EvacuationSchedule actualSchedule = new EvacuationSchedule();
		new EvacuationScheduleReader(actualSchedule, network).readFile(utilsOutputDir + "output_output_evacuationSchedule.csv");

		Iterator<SafeNodeAllocation> expectedIterator = expectedSchedule.getSubsectorsByEvacuationTime().iterator();

		while (expectedIterator.hasNext()) {
			SafeNodeAllocation expected = expectedIterator.next();
			SubsectorData actualData = actualSchedule.getSubsectorDataMap().get(expected.getContainer().getSubsector());
			SafeNodeAllocation actual = actualData.getSafeNodesByTime().iterator().next();
			Assert.assertEquals(expected.getStartTime(), actual.getStartTime(), 1.0);
			Assert.assertEquals(expected.getStartTime(), actual.getEndTime(), 1.0);
			Assert.assertEquals(expected.getVehicles(), actual.getVehicles());
		}

	}

}
