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

public class RunFromSchedule {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void runFromSchedule() throws IOException {
//		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
//		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
////		String inputEvacScheduleFile = "test/input/femproto/prepare/evacuationscheduling/simpleEvacuationScheduleV1.csv";
//		String inputEvacScheduleFile = "test/input/femproto/prepare/evacuationscheduling/changedEvacSchedule.csv";

		FEMUtils.setGlobalConfig(FEMGlobalConfig.getDefaultGlobalConfig());
		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(utils.getPackageInputDirectory()+"scenario/input_network.xml");

//		SubsectorShapeFileParser subSectorsToPopulation = new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork());
//		subSectorsToPopulation.readSubSectorsShapeFile(inputshapefile);

		new EvacuationScheduleReader(evacuationSchedule,scenario.getNetwork()).readFile(utils.getPackageInputDirectory()+"scenario/input_evac_plan.csv");

		new EvacuationScheduleToPopulationDepartures(scenario,evacuationSchedule).createPlans();

		// configure stuff

		Config config = scenario.getConfig();
		FEMConfigGroup femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );
		femConfig.setFemRunType(FEMConfigGroup.FEMRunType.justRunInputPlansFile);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

//		FEMUtils.sampleDown( scenario, 0.1);

		new RunMatsim4FloodEvacuation(scenario).run();

	}

}
