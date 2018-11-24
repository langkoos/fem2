package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.*;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RunFromSourceTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void runFromSource() throws IOException {
		Config config = ConfigUtils.loadConfig("scenarios/FEM2TestDataOctober18/config_2016.xml");
		new File(config.controler().getOutputDirectory()).mkdirs();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		//yoyo this needs to be marked as an essential step for the moment until injection works
		FEMUtils.setGlobalConfig(globalConfig);
//		ConfigUtils.writeMinimalConfig(config,"scenarios/FEM2TestDataOctober18/2016/config_2016.xml");
		NetworkConverter networkConverter = new NetworkConverter(femConfigGroup.getInputNetworkNodesShapefile(), femConfigGroup.getInputNetworkLinksShapefile(), scenario);
		networkConverter.run();
		networkConverter.writeNetwork(config.controler().getOutputDirectory()+"/input_network.xml.gz");
		config.network().setInputFile(config.controler().getOutputDirectory()+"/input_network.xml.gz");

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile(femConfigGroup.getInputSubsectorsShapefile());

		HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
		hydrographParser.parseHydrographShapefile(femConfigGroup.getHydrographShapeFile());
		hydrographParser.readHydrographData(femConfigGroup.getHydrographData(), 36000);
		hydrographParser.hydrographToViaXY(config.controler().getOutputDirectory()+"/hydrograph_XY_time.txt");
		hydrographParser.hydrographToViaLinkAttributesFromLinkData(config.controler().getOutputDirectory()+"/hydrograph_linkID_time.txt",scenario.getNetwork());
		List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork(), config.controler().getOutputDirectory() + "/input_change_events.xml.gz");
		config.network().setChangeEventsInputFile(config.controler().getOutputDirectory()+"/input_change_events.xml.gz");
		NetworkUtils.setNetworkChangeEvents(scenario.getNetwork(),networkChangeEvents);

		new EvacuationScheduleFromHydrographData(scenario.getNetwork(), evacuationSchedule, hydrographParser).createEvacuationSchedule();

		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordComplete(config.controler().getOutputDirectory()+"/input_evac_plan.csv");

		new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule).writePopulation(config.controler().getOutputDirectory()+"/input_population.xml.gz");
		config.plans().setInputFile(utils.getOutputDirectory()+"input_population.xml.gz");

		config.controler().setOutputDirectory(config.controler().getOutputDirectory()+"/output");

		new RunMatsim4FloodEvacuation(scenario).run();



	}

}
