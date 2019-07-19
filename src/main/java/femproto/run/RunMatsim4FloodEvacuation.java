/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package femproto.run;

import com.google.inject.Inject;
import femproto.RerouteLastSelected;
import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.*;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.IntegralApproach;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.PrepareForSimImpl;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static femproto.run.FEMPreferEmergencyLinksTravelDisutility.isEvacLink;
import static org.matsim.core.network.NetworkUtils.getEuclideanDistance;

/**
 * @author nagel
 */
public class RunMatsim4FloodEvacuation {
	// this is now deliberately programmed in a way that it does not give out the controler.  So
	// at this point nobody can grab the controler and do her/his own "controler.run()".  This may
	// be too restrictive, but for the time being this is what it is.  kai, jul'18

	private static final Logger log = Logger.getLogger(RunMatsim4FloodEvacuation.class);

	private static final String EVACUATION_SCHEDULE_FOR_VERIFICATION = "optimized_evacuationSchedule.csv";
	private static final String KEEP_LAST_REROUTE = "KeepLastReRoute";

	private boolean hasLoadedConfig = false;
	private boolean hasPreparedConfig = false;
	private boolean hasPreparedScenario = false;
	private boolean hasPreparedControler = false;

	private Config config;
	private FEMConfigGroup femConfig;
	private Scenario scenario;
	private Controler controler;

	RunMatsim4FloodEvacuation() {
		// catch log entries early (before the output directory is there):
		OutputDirectoryLogging.catchLogEntries();
	}

	/**
	 * Assume that you have constructed a scenario elsewhere and just want to run
	 *
	 * @param scenario input scenario
	 */
	public RunMatsim4FloodEvacuation(Scenario scenario) {
		// catch log entries early (before the output directory is there):
		OutputDirectoryLogging.catchLogEntries();
		this.scenario = scenario;
		this.config = scenario.getConfig();
		femConfig = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		hasLoadedConfig = true;
		prepareConfig();
		FEMUtils.sampleDown(scenario, femConfig.getSampleSize());
		hasPreparedScenario = true;

	}

	/**
	 * In case a config file is prepared programmatically
	 *
	 * @param config input config
	 */
	public RunMatsim4FloodEvacuation(Config config) {
		// catch log entries early (before the output directory is there):
		OutputDirectoryLogging.catchLogEntries();
		this.config = config;
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);

		FEMUtils.setGlobalConfig(globalConfig);
		// (this is so that the global config is also available before we have injection.  E.g. for all the upstream data preparation.)

		hasLoadedConfig = true;
	}

	Config loadConfig(final String[] args) {
		if (args == null || args.length == 0 || args[0] == "") {
			config = ConfigUtils.loadConfig("data/config.xml");
		} else {
			log.info("found an argument, thus loading config from file ...");
			config = ConfigUtils.loadConfig(args[0]);
		}

		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);

		FEMUtils.setGlobalConfig(globalConfig);
		hasLoadedConfig = true;

		return config;
	}

	void prepareConfig() {
		// division into loadConfig and prepareConfig is necessary since some configuration depends
		// on (FEM)config switches, and thus external configuration-in-code needs to be done before
		// prepareConfig.   kai, jul'18

		if (!hasLoadedConfig) {
			loadConfig(null);
		}

		femConfig = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);

		// --- controler config group:
		final int lastIteration = config.controler().getLastIteration();
		//  should come from config file so user can change it.
		// agree - fouriep nov 18

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra);
		// landmarks algorithm does not work when network is disconnected.  kai, aug'18
		// I think this can be marked resolved as we can only consider safe nodes assigned to a subsector - pieter jan 19

		// --- strategies:
		switch (femConfig.getFemOptimizationType()) {
			/* not limiting the numebr of plans that get created
			 kai's approach keeps the shortest patn and only relies on the scoring function */
			default:
				config.strategy().setMaxAgentPlanMemorySize(10);
				break;
		}

		// --- routing:
		{
			Set<String> set = new HashSet<>();
			set.add(TransportMode.car);
			config.plansCalcRoute().setNetworkModes(set);
			config.qsim().setMainModes(set);
		}
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		// --- qsim:
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStuckTime(10);

//		config.qsim().setEndTime(600 * 3600);
		// not setting anything just means that the simulation means until everybody is safe or aborted. kai, apr'18

		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		// means that vehicles in driveways will squeeze into the congested traffic.  Otherwise they are
		// not picked up by the decongestion approach.  kai, aug'18

		// --- scoring:
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.7);
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("evac");
			params.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(params);
		}
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("safe");
			params.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(params);
		}
		config.planCalcScore().setWriteExperiencedPlans(true);

		// --- fem: are we running from source, plans or from evacuation schedule

		log.warn("runType=" + femConfig.getFemRunType());
		log.warn("optimizationType=" + femConfig.getFemOptimizationType());

		switch (femConfig.getFemOptimizationType()) {
			case optimizeLikeNICTA: {
				config.strategy().clearStrategySettings();

				for (LeaderOrFollower leaderOrFollower : LeaderOrFollower.values()) {
					{
						StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
						strategySettings.setSubpopulation(leaderOrFollower.name());
						strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected);
						strategySettings.setWeight(7);
						config.strategy().addStrategySettings(strategySettings);
					}

					{
						StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
						strategySettings.setSubpopulation(leaderOrFollower.name());
						strategySettings.setStrategyName(KEEP_LAST_REROUTE);
						strategySettings.setWeight(3);
						strategySettings.setDisableAfter((int) (0.7 * config.controler().getLastIteration()));
						config.strategy().addStrategySettings(strategySettings);
					}

				}


				configureDecongestion(config);
				break;
			}
			case userEquilibriumDecongestion: {
				config.strategy().clearStrategySettings();

				for (LeaderOrFollower leaderOrFollower : LeaderOrFollower.values()) {
					{
						StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
						strategySettings.setSubpopulation(leaderOrFollower.name());
						strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta);
						strategySettings.setWeight(8);
						config.strategy().addStrategySettings(strategySettings);
					}

					{
						StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
						strategySettings.setSubpopulation(leaderOrFollower.name());
						strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
						strategySettings.setWeight(2);
						strategySettings.setDisableAfter((int) (0.8 * config.controler().getLastIteration()));
						config.strategy().addStrategySettings(strategySettings);
					}

				}


				configureDecongestion(config);
				break;
			}
			case followTheLeader: {
				config.strategy().clearStrategySettings();

				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setSubpopulation(LeaderOrFollower.LEADER.name());
					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta);
					strategySettings.setWeight(0.7);
					config.strategy().addStrategySettings(strategySettings);
				}

				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setSubpopulation(LeaderOrFollower.LEADER.name());
					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
					strategySettings.setWeight(0.2);
					strategySettings.setDisableAfter((int) (0.6 * config.controler().getLastIteration()));
					config.strategy().addStrategySettings(strategySettings);
				}

				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setSubpopulation(LeaderOrFollower.FOLLOWER.name());
					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected);
					strategySettings.setWeight(1);
					config.strategy().addStrategySettings(strategySettings);
				}
				//yoyoyo this approach will require a terminationcriterion that checks for a good score and terminates, so it needs to always have events at hand (I guess)
				config.controler().setWriteEventsInterval(1);

				configureDecongestion(config);
				break;
			}

			case optimizeSafeNodesBySubsector: {
				config.strategy().clearStrategySettings();
				for (LeaderOrFollower leaderOrFollower : LeaderOrFollower.values()) {
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setSubpopulation(leaderOrFollower.name());
					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected);
					strategySettings.setWeight(1);
					config.strategy().addStrategySettings(strategySettings);
				}
				// (here, all strategy selection is done in a separate controler listener.  kai, jul'18)
				configureDecongestion(config);
			}
			break;

			case optimizeSafeNodesByPerson: {
				for (LeaderOrFollower leaderOrFollower : LeaderOrFollower.values()) {

					{
						StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
						strategySettings.setSubpopulation(leaderOrFollower.name());
						strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.BestScore);
						strategySettings.setWeight(1);
						config.strategy().addStrategySettings(strategySettings);
					}
					{
						StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
						strategySettings.setSubpopulation(leaderOrFollower.name());
						strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectRandom);
						strategySettings.setWeight(0.1);
						strategySettings.setDisableAfter((int) (0.9 * lastIteration)); // (50 iterations was not enough) (not innovative!)
						config.strategy().addStrategySettings(strategySettings);
					}
				}
				configureDecongestion(config);
			}
			break;

			case none:
				config.controler().setLastIteration(0);
				// I don't think we need strategies since we would run only the zeroth iteration.  kai, jul'18
				break;
			default:
				throw new RuntimeException(Gbl.NOT_IMPLEMENTED);
		}

		MatsimRandom.reset();
		// need this so that the sampling is stable.  not sure why it is unstable without this.  kai, sep'18

		// ---
		hasPreparedConfig = true;
	}

	Scenario prepareScenario() {
		if (!hasPreparedConfig) {
			prepareConfig();
		}

		// === add overriding config material if there is something in that file:
		// yoyo we've had some gripes from D61 on this; can we make it optional - pieter jan 19
//		ConfigUtils.loadConfig(config, ConfigGroup.getInputFileURL(config.getContext(), "overridingConfig.xml"));
//			log.warn("loaded overriding config settings from overrirdingConfig.xml");
		// The main reason for providing this is that we are setting config switches in our run script.  Now if you don't like some of them, but you only have the built jar,
		// then you are out of options if there is no additional insertion point _after_ we set them.  If data61 rather does not want this kind of flexibility, I have no
		// issue with that.  kai, feb'19


		// === prepare scenario === :


		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);


		switch (femConfig.getFemRunType()) {
			case runFromSource: {
				scenario = ScenarioUtils.createMutableScenario(config);
				NetworkConverter networkConverter = new NetworkConverter(scenario);
				networkConverter.run();

				EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
				FEMUtils.setEvacuationSchedule(evacuationSchedule);
				URL subsectorURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfig.getInputSubsectorsShapefile());
				new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile(subsectorURL);

				HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
				URL hydrographURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfig.getHydrographShapeFile());
				hydrographParser.parseHydrographShapefile(hydrographURL);

				URL hydrographDataURL = IOUtils.newUrl(scenario.getConfig().getContext(), femConfig.getHydrographData());
				hydrographParser.readHydrographData(hydrographDataURL, 54961);

				if (config.network().isTimeVariantNetwork()) {
					List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork());
					config.network().setChangeEventsInputFile("input_change_events.xml.gz");
					NetworkUtils.setNetworkChangeEvents(scenario.getNetwork(), networkChangeEvents);
				}

				new EvacuationScheduleFromHydrographData(scenario.getNetwork(), evacuationSchedule, hydrographParser).createEvacuationSchedule();

				EvacuationScheduleToPopulationDepartures populationDepartures = new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule);
				populationDepartures.createPlansForAllSafeNodes();

				break;
			}

			case justRunInputPlansFile: {
				scenario = ScenarioUtils.loadScenario(config);
				//yoyoyo it seems person attributes do no propagate to population.getPersonAttributes(), only to person.getAttributes()
				for (Person person : scenario.getPopulation().getPersons().values()) {
					for (Map.Entry<String, Object> stringObjectEntry : person.getAttributes().getAsMap().entrySet()) {

						scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), stringObjectEntry.getKey(), stringObjectEntry.getValue());
					}

				}

				break;
			}
			case runFromEvacuationSchedule: {
				/*yoyoyo I am making the most radical changes here, assuming that verification runs from an evacuation schedule are set up by a graphical interface of sorts,
				 and that such infrastructure will point to all the relevant files, rather than making assumptions about where the output from a run resides.
				 Purpose-built classes like RunFromSource can perform such tricks.
				 */
				EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
				FEMUtils.setEvacuationSchedule(evacuationSchedule);
				scenario = ScenarioUtils.createMutableScenario(config);

				new MatsimNetworkReader(scenario.getNetwork()).readFile(IOUtils.newUrl(scenario.getConfig().getContext(), config.network().getInputFile()).getFile());

				loadNetworkChangeEvents();

				new EvacuationScheduleReader(evacuationSchedule, scenario.getNetwork()).readFile(IOUtils.newUrl(scenario.getConfig().getContext(), femConfig.getEvacuationScheduleFile()).getFile());

				new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule).createPlans();
				break;
			}

		}

		switch (femConfig.getFemEvacuationTimeAdjustment()) {
			case takeTimesFromInput:
				FEMUtils.haveOneAgentStartOneSecondEarlierThanEverybodyElse(scenario);
				break;
			case allDepartAtMidnight:
				FEMUtils.moveFirstActivityEndTimesTowardsZero(scenario);
				break;
			default:
				throw new RuntimeException(Gbl.NOT_IMPLEMENTED);
		}


		//  reduce to sample for debugging:
		FEMUtils.sampleDown(scenario, femConfig.getSampleSize());

		//  decide how to do this for UI. kai, jul'18
		//  try running validation run always on 100%.  Does not work because 1% has subsectors with no departures, thus no safe node.
		// would need to have sub-sector-based stratified sampling. done - pieter, jan 19

//		config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "/" + femConfig.getFemRunType().name());
		// yyyy this needs to be _after_ the evac schedule was read in the verification run since otherwise I can't
		// find the directory.  Probably do in some other way. kai, sep'18
		// yoyo proposing to break away from this approach as stated earlier: this class should only run simulations. other classes shoud decide what to do wtth simulation results

		// ---
		hasPreparedScenario = true;
		return scenario;
	}


	private void prepareControler(AbstractModule... overridingModules) {
		if (!hasPreparedScenario) {
			prepareScenario();
		}

		controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				// analysis:
				this.addControlerListenerBinding().to(KaiAnalysisListener.class);
				this.addControlerListenerBinding().to(OutputEvents2TravelDiaries.class);


				// routing:
				switch (femConfig.getFemRoutingMode()) {
					case preferEvacuationLinks:
						final String routingMode = TransportMode.car;
						// (the "routingMode" can be different from the "mode".  useful if, say, different cars should follow different routing
						// algorithms, but still executed as "car" on the network.  Ask me if this might be useful for this project.  kai, feb'18)

						// register this routing mode:
						addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode));

						// define how the travel time is computed:
						// (all commented out, so using default)

//						addTravelTimeBinding(routingMode).to(FreeSpeedTravelTime.class);

						// congested travel time:
//						bind(WithinDayTravelTime.class).in(Singleton.class);
//						addEventHandlerBinding().to(WithinDayTravelTime.class);
//						addMobsimListenerBinding().to(WithinDayTravelTime.class);
//						addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;

						// define how the travel disutility is computed:
//						TravelDisutilityFactory delegateFactory = new OnlyTimeDependentTravelDisutilityFactory();

//						TravelDisutilityFactory delegateFactory = new TollTimeDistanceTravelDisutilityFactory() ;
						// NOT using the toll based travel disutility, since we are routing without toll, on the
						// empty network, before the iterations start, and then never again.  kai, jul'18
						//  this changes because of new requirements pieter nov'18

						// if we need to run the other optmisation approaches, we need to re-route and so Kai proposed this approach of being able to switch between the two
//						if (femConfig.getSampleSize() < 1) {
//							addControlerListenerBinding().toInstance(new PCUEquivalentSetter(femConfig.getSampleSize(), scenario));
//						}
						switch (femConfig.getFemOptimizationType()) {
							case followTheLeader:
							case optimizeLikeNICTA:
							case optimizeSafeNodesByPerson:
							case optimizeSafeNodesBySubsector:
							case userEquilibriumDecongestion:
								bind(TollTimeDistanceTravelDisutilityFactory.class);
								addTravelDisutilityFactoryBinding(routingMode).to(
										FEMPreferEmergencyLinksTravelDisutility.Factory.class
								);
								break;
							default:
								addTravelDisutilityFactoryBinding(routingMode).toInstance(
										new FEMPreferEmergencyLinksTravelDisutility.Factory(scenario.getNetwork(), new OnlyTimeDependentTravelDisutilityFactory())
								);
								break;

						}

						break;
					default:
						throw new RuntimeException("not implemented");
				}

				// scoring such that routes on SES links are strongly preferred
				this.bindScoringFunctionFactory().to(NonEvacLinkPenalizingScoringFunctionFactory.class);
				// yy (this is mostly necessary since the "to-all-safe-nodes" initial router also accepts short
				// non-SES links (e.g. ferry links), and if they are not strongly penalized in the iterations, the simulation
				// will use them.  kai, aug'18)

				// calculating all routes at initialization (assuming that they are sufficiently defined by the evac
				// network).  kai, may'18
				switch (femConfig.getFemOptimizationType()) {
					case optimizeSafeNodesByPerson:
					case optimizeSafeNodesBySubsector:
						this.bind(PrepareForSimImpl.class);
						this.bind(PrepareForSim.class).to(FEMPrepareForSim.class);
				}
				this.addControlerListenerBinding().toInstance(new ShutdownListener() {
					@Inject
					private ExperiencedPlansService eps;
					@Inject
					private Network network;
					@Inject
					private Population population;
					@Inject
					private OutputDirectoryHierarchy outDirs;

					@Override
					public void notifyShutdown(final ShutdownEvent event) {
						if (event.isUnexpected()) {
							return;
						}
						//yoyoyo the vehicles from the original evacuationschedule needs to be stored and written out with the optimized schedule
						final EvacuationScheduleFromExperiencedPlans converter = new EvacuationScheduleFromExperiencedPlans(population, network);
						converter.parseExperiencedPlans(eps.getExperiencedPlans(), network);
						final EvacuationSchedule schedule = converter.createEvacuationSchedule();
						EvacuationScheduleWriter writer = new EvacuationScheduleWriter(schedule);
						writer.writeEvacuationScheduleRecordComplete(outDirs.getOutputFilename(Controler.OUTPUT_PREFIX + "output_evacuationSchedule.csv"));
						switch (femConfig.getFemOptimizationType()) {
							case none:
								break;
							default:
								//update with the network routes used, and reset duration and vehicle counts to original values
								EvacuationSchedule origSchedule = FEMUtils.getEvacuationSchedule();
								Map<String, NetworkRoute> subectorRoutes = new HashMap<>();
								for (Person person : population.getPersons().values()) {
									if (person.getAttributes().getAttribute("subpopulation").toString().equals(LeaderOrFollower.LEADER.name())) {
										Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
										subectorRoutes.put(FEMUtils.getSubsectorName(person), (NetworkRoute) leg.getRoute());
									}
								}

								for (SafeNodeAllocation safeNodeAllocation : schedule.getSubsectorsByEvacuationTime()) {
									String subsector = safeNodeAllocation.getContainer().getSubsector();
									int vehicleCount = origSchedule.getSubsectorDataMap().get(subsector).getVehicleCount();
									safeNodeAllocation.setVehicles(vehicleCount);
									safeNodeAllocation.setEndTime(safeNodeAllocation.getStartTime() + 3600 * vehicleCount / FEMUtils.getGlobalConfig().getEvacuationRate());
									safeNodeAllocation.setNetworkRoute(subectorRoutes.get(subsector));
								}

								writer.writeEvacuationScheduleRecordCompleteWithRoutes(config.controler().getOutputDirectory() + "/" + EVACUATION_SCHEDULE_FOR_VERIFICATION);
								break;
						}
					}
				});
				// Notes:

				// Some ground rules:

				// * we do not overwrite material written in earlier stage

				// * we write both output_evacuationSchedule and optimized_evacuationSchedule.  The former is meant as input
				// to analysis, representing the executed run's results.  The latter is meant to be manually modified,
				// and is input for the verification run.

				// * if the verification run wants automatic access to the optimized_evacuationSchedule, it needs to be
				// directly in the "output" directory.  In contrast, the output dir of the verification run could be in
				// "outputDir + runType".  This would also have the consequence that a new optimization run would remove the
				// previous verification run.  This implies that a scenario construction run should remove the previous
				// optimization run.

				// In first cut, assumption was to have departure times as input to optimization, and safe nodes as output.

				// So the verification run would have subsector, dpTime, safeNode as input.

				// Could alternatively also optimize departure times.
			}
		});

		// yyyy should have the infrastructure that is not needed for justRun only enabled for the other runs.  kai, jul'18

		switch (femConfig.getFemOptimizationType()) {
			case followTheLeader: {
				controler.addOverridingModule(new DecongestionModule(scenario));
				// needs an additional listener
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						addControlerListenerBinding().to(SelectedPlanFromSubsectorLeadAgents.class);
					}
				});
				break;
			}
			case optimizeSafeNodesBySubsector:
				controler.addOverridingModule(new DecongestionModule(scenario));
				// toll-dependent routing would have to be added elsewhere, but is not used here since all routes are
				// computed in prepareForSim. kai, jul'18
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						this.addControlerListenerBinding().to(SelectOneBestSafeNodePerSubsector.class);
					}
				});
				break;
			case optimizeSafeNodesByPerson:
			case userEquilibriumDecongestion:
				controler.addOverridingModule(new DecongestionModule(scenario));
				// toll-dependent routing would have to be added elsewhere, but is not used here since all routes are
				// computed in prepareForSim. kai, jul'18
				break;
			case optimizeLikeNICTA:
				controler.addOverridingModule(new DecongestionModule(scenario));
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
//						this.addControlerListenerBinding().to( SelectedPlanFromSubsectorLeadAgents.class );

						//these methods didn't seem to work,
						// working towards decongestion rerouting - getting jumpy results when used in conjunction with the one best safe node per subsector strategy and enforcing the rule of everybody on the same route per subsector
						this.addControlerListenerBinding().to(SelectOneBestRoutePerSubsector.class);
						this.addControlerListenerBinding().to(SelectOneBestSafeNodePerSubsector.class);
						addPlanStrategyBinding(KEEP_LAST_REROUTE).toProvider(RerouteLastSelected.class);
					}
				});
				break;
			case none:
				break;
			default:
				throw new RuntimeException(Gbl.NOT_IMPLEMENTED);
		}

		// adding the overriding modules from external callers:
		for (AbstractModule overridingModule : overridingModules) {
			controler.addOverridingModule(overridingModule);
		}

		// ---
		hasPreparedControler = true;
	}

	private static void configureDecongestion(final Config config) {
		DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule(config, DecongestionConfigGroup.class);
		decongestionSettings.setEnableDecongestionPricing(true);
		decongestionSettings.setToleratedAverageDelaySec(30.);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
		decongestionSettings.setUpdatePriceInterval(1);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);

		decongestionSettings.setDecongestionApproach(DecongestionApproach.PID);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setKp(0.5);

//		decongestionSettings.setDecongestionApproach( DecongestionConfigGroup.DecongestionApproach.BangBang );
//		decongestionSettings.setInitialToll(20.);
//		decongestionSettings.setTollAdjustment(20.);

		// The BangBang approach does NOT work well for evacuation.  The PID approach does. kai, jul'18

		decongestionSettings.setIntegralApproach(IntegralApproach.UnusedHeadway);
		decongestionSettings.setIntegralApproachUnusedHeadwayFactor(10.0);
		decongestionSettings.setIntegralApproachAverageAlpha(0.0);

		decongestionSettings.setWriteOutputIteration(100);
	}


	private void loadNetworkChangeEvents() {
		if ((this.config.network().getChangeEventsInputFile() != null) && this.config.network().isTimeVariantNetwork()) {
			log.info("loading network change events from " + this.config.network().getChangeEventsInputFileUrl(this.config.getContext()).getFile());
			Network network = this.scenario.getNetwork();
			List<NetworkChangeEvent> changeEvents = new ArrayList<>();
			NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network, changeEvents);
			parser.parse(this.config.network().getChangeEventsInputFileUrl(config.getContext()));
			NetworkUtils.setNetworkChangeEvents(network, changeEvents);
		}
	}

	public static void main(String[] args) {
		// The GUI executes the "main" method, with the config file name as the only argument.

		final RunMatsim4FloodEvacuation evac = new RunMatsim4FloodEvacuation();

		evac.loadConfig(args);
		// (yy this type of short script would point towards having the args in the constructor. kai, jul'18)

		evac.run();

	}

	void run() {
		if (!hasPreparedControler) {
			prepareControler();
		}

		controler.run();

		// need to do this fairly late since otherwise the directory is wiped out again when the controler gets going. kai, apr'18
		final String filename = controler.getConfig().controler().getOutputDirectory() + "/output_linkAttribs.txt.gz";
		log.info("will write link attributes to " + filename);

		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			writer.write("id\t" + FEMUtils.getGlobalConfig().getAttribEvacMarker());
			writer.newLine();
			for (Link link : controler.getScenario().getNetwork().getLinks().values()) {
				writer.write(link.getId().toString() + "\t");
				writer.write(Boolean.toString((boolean) link.getAttributes().getAttribute(FEMUtils.getGlobalConfig().getAttribEvacMarker())));
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	// yoyo this could be an issue if
	private class PCUEquivalentSetter implements BeforeMobsimListener {
		private final double pcuEquivalent;
		private final Scenario scenario;

		public PCUEquivalentSetter(double v, Scenario scenario) {
			this.pcuEquivalent = v;
			this.scenario = scenario;
		}


		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			scenario.getVehicles().getVehicleTypes().values().iterator().next().setPcuEquivalents(pcuEquivalent);
			log.warn("Setting PCU equiv to " + pcuEquivalent);
			// yyyyyy you say it came from me ... but why are we _both_ sampling down the population _and_ reducing the pcu? kai, mar'19
		}
	}
}

