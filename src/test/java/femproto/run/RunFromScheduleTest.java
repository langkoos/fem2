package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleReader;
import femproto.prepare.evacuationscheduling.EvacuationScheduleToPopulationDepartures;
import femproto.prepare.parsers.EvacuationToSafeNodeParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

public class RunFromScheduleTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test()  {


		Config config = ConfigUtils.loadConfig(utils.getPackageInputDirectory()+"scenario/config_runFromSchedule.xml");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		new RunMatsim4FloodEvacuation(config).run();

	}

}
