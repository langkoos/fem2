package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.*;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// yoyo this is the entry point for D61, so keeping it
public class RunFromSource {
	public static void main(String[] args) {
		standardFullSizeOptimization(args);
	}

	public static void optimizationThenVerification(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		config.network().setTimeVariantNetwork(true);
		String outputDirectory = config.controler().getOutputDirectory();
		new File(outputDirectory).mkdirs();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		femConfigGroup.setSampleSize(0.1);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);

		// yoyo this needs to be marked as an essential step for the moment until injection works
		FEMUtils.setGlobalConfig(globalConfig);
//		ConfigUtils.writeMinimalConfig(config,"scenarios/FEM2TestDataOctober18/2016/config_2016.xml");
		NetworkConverter networkConverter = new NetworkConverter(scenario);
		networkConverter.run();
		networkConverter.writeNetwork(outputDirectory + "/input_network.xml");
		config.network().setInputFile("input_network.xml");

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		FEMUtils.setEvacuationSchedule(evacuationSchedule);
		URL subsectorURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getInputSubsectorsShapefile());
		new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile(subsectorURL);

		HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
		URL hydrographURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getHydrographShapeFile());
		hydrographParser.parseHydrographShapefile(hydrographURL);
		// yoyoyo this forces the first network change event to be synchronised wioth the 2016 NICTA reference results
		URL hydrographDataURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfigGroup.getHydrographData());
		hydrographParser.readHydrographData(hydrographDataURL, 54961);
		hydrographParser.hydrographToViaXY(outputDirectory + "/hydrograph_XY_time.txt");
		hydrographParser.hydrographToViaLinkAttributesFromLinkData(outputDirectory + "/hydrograph_linkID_time.txt");

		List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork());
		new NetworkChangeEventsWriter().write(outputDirectory + "/input_change_events.xml.gz", networkChangeEvents);
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

		config.controler().setOutputDirectory(outputDirectory + "/optimization");

		new RunMatsim4FloodEvacuation(scenario).run();


		Set<Id<Person>> pax = new HashSet<>();
		pax.addAll(scenario.getPopulation().getPersons().keySet());
		for (Id<Person> personId : pax) {
			scenario.getPopulation().removePerson(personId);
		}
		evacuationSchedule = new EvacuationSchedule();
		new EvacuationScheduleReader(evacuationSchedule, scenario.getNetwork()).readFile(IOUtils.newUrl(scenario.getConfig().getContext(), outputDirectory + "/optimization/optimized_evacuationSchedule.csv").getFile());

		new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule).createPlans();

		femConfigGroup.setFemRunType(FEMConfigGroup.FEMRunType.justRunInputPlansFile);
		femConfigGroup.setFemOptimizationType(FEMConfigGroup.FEMOptimizationType.none);
		femConfigGroup.setSampleSize(1.0);
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(1.0);
		config.controler().setOutputDirectory(outputDirectory + "/output");
		config.strategy().clearStrategySettings();
		new ConfigWriter(config, ConfigWriter.Verbosity.minimal).write(outputDirectory + "/config.xml");

		new RunMatsim4FloodEvacuation(scenario).run();
	}

	public static void standardFullSizeOptimization(String[] args) {
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
		NetworkConverter networkConverter = new NetworkConverter(scenario);
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

		List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork());
		new NetworkChangeEventsWriter().write(outputDirectory + "/input_change_events.xml.gz", networkChangeEvents);
		config.network().setChangeEventsInputFile("input_change_events.xml.gz");
		NetworkUtils.setNetworkChangeEvents(scenario.getNetwork(), networkChangeEvents);

		new EvacuationScheduleFromHydrographData(scenario.getNetwork(), evacuationSchedule, hydrographParser).createEvacuationSchedule();

		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordComplete(outputDirectory + "/input_evac_plan.csv");
		FEMUtils.setEvacuationSchedule(evacuationSchedule);

		EvacuationScheduleToPopulationDepartures populationDepartures = new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule);
//		populationDepartures.createPlans();
		populationDepartures.createPlansForAllSafeNodes();
		populationDepartures.writePopulation(outputDirectory + "/input_population.xml.gz");
		populationDepartures.writeAttributes(outputDirectory + "/input_population_attrs.txt");
		config.plans().setInputFile("input_population.xml.gz");

		config.controler().setOutputDirectory(outputDirectory + "/output");
		new ConfigWriter(config, ConfigWriter.Verbosity.minimal).write(outputDirectory + "/config.xml");


		new RunMatsim4FloodEvacuation(scenario).run();

	}
}
