package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleReader;
import femproto.prepare.evacuationscheduling.EvacuationScheduleToPopulationDepartures;
import femproto.prepare.network.NetworkConverter;
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

public class RunFromSource {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void runFromSchedule() throws IOException {
		Config config = ConfigUtils.loadConfig("scenarios/FEM2TestDataOctober18/2016/config_2016.xml");
		Scenario scenario = ScenarioUtils.createScenario(config);
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
//		ConfigUtils.writeMinimalConfig(config,"scenarios/FEM2TestDataOctober18/2016/config_2016.xml");
		NetworkConverter networkConverter = new NetworkConverter(femConfigGroup.getInputNetworkNodesShapefile(), femConfigGroup.getInputNetworkLinksShapefile(), scenario);
		networkConverter.runConversion();
		networkConverter.writeNetwork("network.xml.gz");


		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
//		new SubsectorShapeFileParser()
	}

}
