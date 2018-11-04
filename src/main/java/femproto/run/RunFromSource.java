package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleFromHydrographData;
import femproto.prepare.evacuationscheduling.EvacuationScheduleToPopulationDepartures;
import femproto.prepare.evacuationscheduling.EvacuationScheduleWriter;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.util.List;

public class RunFromSource {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		new File(config.controler().getOutputDirectory()).mkdirs();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		//yoyo this needs to be marked as an essential step for the moment until injection works
		FEMUtils.setGlobalConfig(globalConfig);
//		ConfigUtils.writeMinimalConfig(config,"scenarios/FEM2TestDataOctober18/2016/config_2016.xml");
		NetworkConverter networkConverter = new NetworkConverter(femConfigGroup.getInputNetworkNodesShapefile(), femConfigGroup.getInputNetworkLinksShapefile(), scenario);
		networkConverter.run();
		networkConverter.writeNetwork(config.controler().getOutputDirectory() + "/input_network.xml.gz");
		config.network().setInputFile(config.controler().getOutputDirectory() + "/input_network.xml.gz");

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile(femConfigGroup.getInputSubsectorsShapefile());

		HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
		hydrographParser.parseHydrographShapefile(femConfigGroup.getHydrographShapeFile());
		hydrographParser.readHydrographData(femConfigGroup.getHydrographData());
		hydrographParser.hydrographToViaXY(config.controler().getOutputDirectory() + "/hydrograph_XY_time.txt");
		hydrographParser.hydrographToViaLinkAttributesFromLinkData(config.controler().getOutputDirectory() + "/hydrograph_linkID_time.txt", scenario.getNetwork());
		List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork(), config.controler().getOutputDirectory() + "/input_change_events.xml.gz");
		config.network().setChangeEventsInputFile(config.controler().getOutputDirectory() + "/input_change_events.xml.gz");
		NetworkChangeEventsParser eventsParser = new NetworkChangeEventsParser(scenario.getNetwork(), networkChangeEvents);

		new EvacuationScheduleFromHydrographData(scenario.getNetwork(), evacuationSchedule, hydrographParser).createEvacuationSchedule();

		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordComplete(config.controler().getOutputDirectory() + "/input_evac_plan.csv");

		new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule).writePopulation(config.controler().getOutputDirectory() + "/input_population.xml.gz");
		config.plans().setInputFile(config.controler().getOutputDirectory() + "/input_population.xml.gz");

		config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "/output");

		new RunMatsim4FloodEvacuation(scenario).run();
	}
}
