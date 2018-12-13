package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleFromHydrographData;
import femproto.prepare.evacuationscheduling.EvacuationScheduleToPopulationDepartures;
import femproto.prepare.evacuationscheduling.EvacuationScheduleWriter;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

public class RunFromSource {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		config.network().setTimeVariantNetwork(true);
		String outputDirectory = config.controler().getOutputDirectory();
		new File(outputDirectory).mkdirs();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		//yoyo this needs to be marked as an essential step for the moment until injection works
		FEMUtils.setGlobalConfig(globalConfig);
//		ConfigUtils.writeMinimalConfig(config,"scenarios/FEM2TestDataOctober18/2016/config_2016.xml");
		NetworkConverter networkConverter = new NetworkConverter( scenario);
		networkConverter.run();
		networkConverter.writeNetwork(outputDirectory + "/input_network.xml");
		config.network().setInputFile("input_network.xml");

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		URL subsectorURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getInputSubsectorsShapefile());
		new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile(subsectorURL);

		HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
		URL hydrographURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getHydrographShapeFile());
		hydrographParser.parseHydrographShapefile(hydrographURL);
		// yoyoyo this forces the first network change evnt to be synchronised wioth the 2016 NICTA reference results
		URL hydrographDataURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getHydrographData());
		hydrographParser.readHydrographData(hydrographDataURL, 54961);
		hydrographParser.hydrographToViaXY(outputDirectory + "/hydrograph_XY_time.txt");
		hydrographParser.hydrographToViaLinkAttributesFromLinkData(outputDirectory + "/hydrograph_linkID_time.txt");

		List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork(), outputDirectory + "/input_change_events.xml.gz");
		config.network().setChangeEventsInputFile("input_change_events.xml.gz");
		NetworkUtils.setNetworkChangeEvents(scenario.getNetwork(), networkChangeEvents);

		new EvacuationScheduleFromHydrographData(scenario.getNetwork(), evacuationSchedule, hydrographParser).createEvacuationSchedule();

		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordComplete(outputDirectory + "/input_evac_plan.csv");

		EvacuationScheduleToPopulationDepartures populationDepartures = new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule);
//		populationDepartures.createPlans();
		populationDepartures.createPlansForAllSafeNodes();
		populationDepartures.writePopulation(outputDirectory + "/input_population.xml.gz");
		populationDepartures.writeAttributes(outputDirectory + "/input_population_attrs.txt");
		config.plans().setInputFile("input_population.xml.gz");

		config.controler().setOutputDirectory(outputDirectory+"/output");
		new ConfigWriter(config, ConfigWriter.Verbosity.minimal).write(outputDirectory + "/config.xml");

		new RunMatsim4FloodEvacuation(scenario).run();

	}
}
