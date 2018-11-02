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
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleFromExperiencedPlans;
import femproto.prepare.evacuationscheduling.EvacuationScheduleReader;
import femproto.prepare.evacuationscheduling.EvacuationScheduleWriter;
import femproto.prepare.evacuationscheduling.SafeNodeAllocation;
import femproto.prepare.evacuationscheduling.SubsectorData;
import femproto.prepare.network.NetworkConverter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
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
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static femproto.run.FEMPreferEmergencyLinksTravelDisutility.isEvacLink;
import static org.matsim.core.network.NetworkUtils.getEuclideanDistance;

/**
 * @author nagel
 *
 */
public class RunMatsim4FloodEvacuation {
	// this is now deliberately programmed in a way that it does not give out the controler.  So
	// at this point nobody can grab the controler and do her/his own "controler.run()".  This may
	// be too restrictive, but for the time being this is what it is.  kai, jul'18

	private static final Logger log = Logger.getLogger( RunMatsim4FloodEvacuation.class );

	private static final String EVACUATION_SCHEDULE_FOR_VERIFICATION = "optimized_evacuationSchedule.csv";
	private static final String KEEP_LAST_REROUTE = "KeepLastReRoute";

	private boolean hasLoadedConfig = false ;
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	private Config config ;
	private FEMConfigGroup femConfig ;
	private Scenario scenario ;
	private Controler controler ;

	RunMatsim4FloodEvacuation() {
		// catch log entries early (before the output directory is there):
		OutputDirectoryLogging.catchLogEntries();
	}

	/**
	 * Assume that you have constructred a scenario elsewhere and just want to run
	 * @param scenario
	 */
	public RunMatsim4FloodEvacuation(Scenario scenario) {
		// catch log entries early (before the output directory is there):
		OutputDirectoryLogging.catchLogEntries();
		this.scenario = scenario;
		this.config = scenario.getConfig();
		femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );
		hasLoadedConfig = true;
		prepareConfig();
		FEMUtils.sampleDown(scenario,femConfig.getSampleSize());
		hasPreparedScenario = true;

	}

	Config loadConfig( final String[] args ) {
		if ( args == null || args.length == 0 || args[0] == "" ) {

//			config = ConfigUtils.loadConfig( "scenarios/fem2016_v20180307/00config-just-run-plans-file.xml" );
//			config = ConfigUtils.loadConfig( "scenarios/fem2016_v20180307/00config.xml" );
			config = ConfigUtils.loadConfig( "scenarios/00sandbox/00config.xml" );

//			config = ConfigUtils.createConfig() ;
//			config.network().setInputFile( "test/output/femproto/gis/NetworkConverterTest/testMain/netconvert.xml.gz");
//			config.plans().setInputFile("plans_from_hn_evacuationmodel_PL2016_V12subsectorsVehic2016.xml.gz");

//			config = ConfigUtils.loadConfig( "workspace-csiro/proj1/wsconfig-for-matsim-v10.xml" ) ;
//			config = ConfigUtils.loadConfig( "scenarios/hawkesbury-from-bdi-project-2018-01-16/00config-just-run-plans-file.xml" ) ;


		} else {
			log.info( "found an argument, thus loading config from file ..." );
			config = ConfigUtils.loadConfig( args[0] );
		}

		// ---

		hasLoadedConfig = true ;
		FEMGlobalConfig globalConfig = ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		//yoyo this needs to be marked as an essential step for the moment until injection works
		try {
			FEMUtils.setGlobalConfig(globalConfig);
		}catch (Exception e){

		}
		return config ;
	}

	void prepareConfig() {
		// division into loadConfig and prepareConfig is necessary since some configuration depends
		// on (FEM)config switches, and thus external configuration-in-code needs to be done before
		// prepareConfig.   kai, jul'18

		if ( !hasLoadedConfig ) {
			loadConfig( null ) ;
		}

		femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );

		// --- controler config group:
		final int lastIteration = config.controler().getLastIteration();
		//  should come from config file so user can change it.
		// agree - fouriep nov 18

//		config.controler().setLastIteration( lastIteration );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setRoutingAlgorithmType( ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra );
		// yy landmarks algorithm does not work when network is disconnected.  kai, aug'18

		// --- strategies:
		switch( femConfig.getFemRunType() ) {
			//yoyo will need some planremovalselector that will maintain diversity of destinations; setting memory to a relatively large value for now, and using BestScore plan selection
			case runFromSource:
				config.strategy().setMaxAgentPlanMemorySize(10);
			default:
				config.strategy().setMaxAgentPlanMemorySize(0);
				break;
		}

		// --- routing:
		{
			Set<String> set = new HashSet<>();
			set.add( TransportMode.car );
			config.plansCalcRoute().setNetworkModes( set );
			config.qsim().setMainModes( set );
		}
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		// --- qsim:
		config.qsim().setRemoveStuckVehicles( false );
		config.qsim().setStuckTime( 10 );

//		config.qsim().setEndTime(600 * 3600);
		// not setting anything just means that the simulation means until everybody is safe or aborted. kai, apr'18

		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
		// means that vehicles in driveways will squeeze into the congested traffic.  Otherwise they are
		// not picked up by the decongestion approach.  kai, aug'18

		// --- scoring:
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA( 0.7 );
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams( "evac" );
			params.setScoringThisActivityAtAll( false );
			config.planCalcScore().addActivityParams( params );
		}
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams( "safe" );
			params.setScoringThisActivityAtAll( false );
			config.planCalcScore().addActivityParams( params );
		}
		config.planCalcScore().setWriteExperiencedPlans( true );

		// --- fem:

		log.warn( "runType=" + femConfig.getFemRunType() ) ;

		switch( femConfig.getFemRunType() ) {
			case runFromSource:
				config.strategy().clearStrategySettings();
				{
					StrategyConfigGroup.StrategySettings strategySettingsReroute = new StrategyConfigGroup.StrategySettings();

					strategySettingsReroute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
					strategySettingsReroute.setSubpopulation(LeaderOrFollower.LEADER.name());
					strategySettingsReroute.setWeight(0.2);
					strategySettingsReroute.setDisableAfter((int) (0.8*config.controler().getLastIteration()));
					config.strategy().addStrategySettings(strategySettingsReroute);
				}
				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setSubpopulation(LeaderOrFollower.LEADER.name());
					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.BestScore);
					strategySettings.setWeight(0.001);
					config.strategy().addStrategySettings(strategySettings);
				}
				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setSubpopulation(LeaderOrFollower.LEADER.name());
					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
					strategySettings.setWeight(0.8);
					strategySettings.setDisableAfter((int) (0.8*config.controler().getLastIteration()));
					config.strategy().addStrategySettings(strategySettings);
				}
				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setSubpopulation(LeaderOrFollower.FOLLOWER.name());
					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected);
					strategySettings.setWeight(1);
					config.strategy().addStrategySettings(strategySettings);
				}
				// (here, all strategy selection is done in a separate controler listener.  kai, jul'18)
				configureDecongestion( config );
				break;
			case optimizeSafeNodesBySubsector: {
				config.strategy().clearStrategySettings();
				StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
				strategySettings.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected );
				strategySettings.setWeight( 1 );
				config.strategy().addStrategySettings( strategySettings );
				// (here, all strategy selection is done in a separate controler listener.  kai, jul'18)
				configureDecongestion( config );
			}
			break;
			case optimizeSafeNodesByPerson: {
//				{
//					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
//					strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
//					strategySettings.setWeight(1);
//					strategySettings.setDisableAfter( (int) (0.9*lastIteration) ); // (50 iterations was not enough)
//					config.strategy().addStrategySettings(strategySettings);
//				}
				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.BestScore );
					strategySettings.setWeight( 1 );
					config.strategy().addStrategySettings( strategySettings );
				}
				{
					StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
					strategySettings.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.SelectRandom );
					strategySettings.setWeight( 0.1 );
					strategySettings.setDisableAfter( (int) ( 0.9 * lastIteration ) ); // (50 iterations was not enough) (not innovative!)
					config.strategy().addStrategySettings( strategySettings );
				}
				configureDecongestion( config );
			}
			break;
			case justRunInputPlansFile:

			case runFromEvacuationSchedule:
				config.controler().setLastIteration( 0 );
				// I don't think we need strategies since we would run only the zeroth iteration.  kai, jul'18
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
		}

		MatsimRandom.reset();
		// need this stable so that the sampling is stable.  not sure why it is unstable without this.  kai, sep'18

		// ---

		hasPreparedConfig = true ;
	}

	Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig(  ) ;
		}

		// === add overriding config material if there is something in that file:
		ConfigUtils.loadConfig( config, ConfigGroup.getInputFileURL( config.getContext(), "overridingConfig.xml" ) );

		// === prepare scenario === :

		log.info("here10") ;

		scenario = ScenarioUtils.loadScenario( config );

		log.info("here20") ;

		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );

//		new NetworkCleaner().run(scenario.getNetwork());
		// yyyyyy fem2016 network seems to have unconnected pieces.

		// yyyyyy reduce to sample for debugging:
		FEMUtils.sampleDown( scenario, femConfig.getSampleSize());
		// yyyy decide how to do this for UI. kai, jul'18
		// yyyy try running validation run always on 100%.  Does not work because 1% has subsectors with no departures, thus no safe node.
		// woud need to have sub-sector-based stratified sampling.


		switch ( femConfig.getFemRunType() ) {
			case runFromSource:
			case justRunInputPlansFile:
				break;
			case runFromEvacuationSchedule:
				log.info("here30") ;

				final String fileName = config.controler().getOutputDirectory() + "/" + FEMConfigGroup.FEMRunType.optimizeSafeNodesBySubsector + "/" + EVACUATION_SCHEDULE_FOR_VERIFICATION ;
				final EvacuationSchedule evacSched = new EvacuationSchedule() ;
				new EvacuationScheduleReader( evacSched, scenario.getNetwork() ).readFile( fileName );

				log.info("here40") ;

				log.info("here50") ;

				for ( Person person : scenario.getPopulation().getPersons().values() ) {
					// remove unselected plans:
					person.getPlans().removeIf( plan -> !person.getSelectedPlan().equals( plan ) );
					// get subsector info:
					final SubsectorData data = evacSched.getOrCreateSubsectorData( FEMUtils.getSubsectorName( person ) );
					final Set<SafeNodeAllocation> set = data.getSafeNodesByTime();
					if ( set.size() > 1 ) {
						throw new RuntimeException( "more than one safe node for a subsector.  cannot deal with this in verification run. Aborting ...") ;
					}
					if ( set.size()==0 ) {
						log.info( "subsectorName=" + FEMUtils.getSubsectorName( person ) ) ;
						log.info( "set=" + set ) ;
						throw new RuntimeException("somehow, there is no safe node for the subsector.  cannot deal with this in verification run.  Aborting ...");
					}
					final SafeNodeAllocation alloc = set.iterator().next(); // there should now be only one!

					final List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();

					// set evacuation time:
					final Activity initialAct = (Activity) planElements.get(0) ;
					initialAct.setEndTime( alloc.getStartTime() );
					// (start time is indeed the departure time)

					// remove route so that it will be re-computed:
					final Leg evacLeg = (Leg) planElements.get( 1 );
					evacLeg.setRoute( null );

					// set safe node:
					final Activity safeAct = (Activity) planElements.get(2) ;
					safeAct.setLinkId( FEMUtils.getLinkFromSafeNode( alloc.getNode().getId().toString(), scenario ).getId()  );
					// yy arrrggghhh ... kai, sep'18

				}
				break;

			case optimizeSafeNodesByPerson:
			case optimizeSafeNodesBySubsector:
				FEMUtils.giveAllSafeNodesToAllAgents( scenario );
				// yyyy will we get valid initial mappings?  kai, jul'18
				break;
		}

		switch( femConfig.getFemEvacuationTimeAdjustment() ) {
			case takeTimesFromInput:
				FEMUtils.haveOneAgentStartOneSecondEarlierThanEverybodyElse( scenario );
				break;
			case allDepartAtMidnight:
				FEMUtils.moveFirstActivityEndTimesTowardsZero( scenario );
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
		}

		//		preparationsForRmitHawkesburyScenario();

		config.controler().setOutputDirectory( config.controler().getOutputDirectory() + "/" + femConfig.getFemRunType().name() );
		// yyyy this needs to be _after_ the evac schedule was read in the verification run since otherwise I can't
		// find the directory.  Probably do in some other way. kai, sep'18

		// ---
		hasPreparedScenario = true ;
		return scenario;
	}

	void prepareControler( AbstractModule... overridingModules ) {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}

		controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {

				// analysis:
				this.addControlerListenerBinding().to( KaiAnalysisListener.class );
				this.addControlerListenerBinding().to( OutputEvents2TravelDiaries.class );

				// routing:
				switch ( femConfig.getFemRoutingMode() ) {
					case preferEvacuationLinks:
						final String routingMode = TransportMode.car;
						// (the "routingMode" can be different from the "mode".  useful if, say, different cars should follow different routing
						// algorithms, but still executed as "car" on the network.  Ask me if this might be useful for this project.  kai, feb'18)

						// register this routing mode:
						addRoutingModuleBinding( routingMode ).toProvider( new NetworkRoutingProvider( TransportMode.car, routingMode ) );

						// define how the travel time is computed:
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
						// yoyo this changes because of new requirements pieter nov'18
						bind(TollTimeDistanceTravelDisutilityFactory.class);
						addTravelDisutilityFactoryBinding( routingMode ).to(
								FEMPreferEmergencyLinksTravelDisutility.Factory.class
						);

						break;
					default:
						throw new RuntimeException( "not implemented" );
				}

				// scoring such that routes on SES links are strongly preferred
				this.bindScoringFunctionFactory().to( NonEvacLinkPenalizingScoringFunctionFactory.class );
				// yy (this is mostly necessary since the "to-all-safe-nodes" initial router also accepts short
				// non-SES links (e.g. ferry links), and if they are not strongly penalized in the iterations, the simulation
				// will use them.  kai, aug'18)

				// calculating all routes at initialization (assuming that they are sufficiently defined by the evac
				// network).  kai, may'18
				this.bind( PrepareForSimImpl.class );
				this.bind( PrepareForSim.class ).to(FEMPrepareForSim.class);
				this.addControlerListenerBinding().toInstance( new ShutdownListener() {
					@Inject private ExperiencedPlansService eps ;
					@Inject private Network network ;
					@Inject private Population population ;
					@Inject private OutputDirectoryHierarchy outDirs ;
					@Override public void notifyShutdown( final ShutdownEvent event ) {
						if ( event.isUnexpected() ) {
							return ;
						}
						final EvacuationScheduleFromExperiencedPlans converter = new EvacuationScheduleFromExperiencedPlans(population,network);
						converter.parseExperiencedPlans( eps.getExperiencedPlans(), network ) ;
						final EvacuationSchedule schedule = converter.createEvacuationSchedule();
						EvacuationScheduleWriter writer = new EvacuationScheduleWriter( schedule ) ;
						writer.writeEvacuationScheduleRecordComplete( outDirs.getOutputFilename( Controler.OUTPUT_PREFIX + "evacuationSchedule.csv" ) );
						switch ( ConfigUtils.addOrGetModule( config, FEMConfigGroup.class ).getFemRunType() ) {
							case justRunInputPlansFile:
							case runFromEvacuationSchedule:
								break;

							case runFromSource:
							case optimizeSafeNodesByPerson:
							case optimizeSafeNodesBySubsector:
								writer.writeEvacuationScheduleRecordNoVehiclesNoDurations( config.controler().getOutputDirectory() + "/" + EVACUATION_SCHEDULE_FOR_VERIFICATION ) ;
								break;
						}
					}
				} );
				// Notes:

				// Some ground rules:

				// * we do not overwrite material written in earlier stage

				// * we write both output_evacuationSchedule and optimized_evacuationSchedule.  The former is meant as input
				// to analysis.  The latter is meant to be manually modified, and is input for the verification run.

				// * if the verification run wants automatic access to the optimized_evacuationSchedule, it needs to be
				// directly in the "output" directory.  In contrast, the output dir of the verification run could be in
				// "outputDir + runType".  This would also have the consequence that a new optimization run would remove the
				// previous verification run.  This implies that a scenario construction run should remove the previous
				// optimization run.

				// In first cut, assumption was to have departure times as input to optimization, and safe nodes as output.

				// So the verification run would have subsector, dpTime, safeNode as input.

				// Could alternatively also optimize departure times.
			}
		} );

		// yyyy should have the infrastructure that is not needed for justRun only enabled for the other runs.  kai, jul'18

		switch( femConfig.getFemRunType() ) {
			case optimizeSafeNodesBySubsector:
				controler.addOverridingModule( new DecongestionModule( scenario ) );
				// toll-dependent routing would have to be added elsewhere, but is not used here since all routes are
				// computed in prepareForSim. kai, jul'18
				controler.addOverridingModule( new AbstractModule() {
					@Override public void install() {
						this.addControlerListenerBinding().to( SelectOneBestSafeNodePerSubsector.class );
					}
				} );
				break ;
			case optimizeSafeNodesByPerson:
				controler.addOverridingModule( new DecongestionModule( scenario ) );
				// toll-dependent routing would have to be added elsewhere, but is not used here since all routes are
				// computed in prepareForSim. kai, jul'18
				break;
			case justRunInputPlansFile:
			case runFromSource:
				controler.addOverridingModule( new DecongestionModule( scenario ) );
				controler.addOverridingModule( new AbstractModule() {
					@Override public void install() {
						this.addControlerListenerBinding().to( SelectedPlanFromSubsectorLeadAgents.class );

						//these methods didn't seem to work,
						// working towards decongestion rerouting - getting jumpy results when used in conjunction with the one best safe node per subsector strategy and enforcing the rule of everybody on the same route per subsector
//						this.addControlerListenerBinding().to( SelectOneBestRoutePerSubsector.class );
//						addPlanStrategyBinding(KEEP_LAST_REROUTE).toProvider(RerouteLastSelected.class);
					}
				} );
				break;
			case runFromEvacuationSchedule:
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
		}

		// adding the overriding modules from external callers:
		for ( AbstractModule overridingModule : overridingModules ) {
			controler.addOverridingModule( overridingModule );
		}

		// ---
		hasPreparedControler = true ;
	}

	private static void configureDecongestion( final Config config ) {
		DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule( config, DecongestionConfigGroup.class );
		decongestionSettings.setEnableDecongestionPricing( true );
		decongestionSettings.setToleratedAverageDelaySec( 30. );
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment( 1.0 );
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment( 0.0 );
		decongestionSettings.setUpdatePriceInterval( 1 );
		decongestionSettings.setMsa( false );
		decongestionSettings.setTollBlendFactor( 1.0 );

		decongestionSettings.setDecongestionApproach( DecongestionApproach.PID );
		decongestionSettings.setKd( 0.0 );
		decongestionSettings.setKi( 0.0 );
		decongestionSettings.setKp( 0.5 );

//		decongestionSettings.setDecongestionApproach( DecongestionConfigGroup.DecongestionApproach.BangBang );
//		decongestionSettings.setInitialToll(20.);
//		decongestionSettings.setTollAdjustment(20.);

		// The BangBang approach does NOT work well for evacuation.  The PID approach does. kai, jul'18

		decongestionSettings.setIntegralApproach( IntegralApproach.UnusedHeadway );
		decongestionSettings.setIntegralApproachUnusedHeadwayFactor( 10.0 );
		decongestionSettings.setIntegralApproachAverageAlpha( 0.0 );

		decongestionSettings.setWriteOutputIteration( 100 );
	}

	public static void main( String[] args ) {
		// The GUI executes the "main" method, with the config file name as the only argument.

		final RunMatsim4FloodEvacuation evac = new RunMatsim4FloodEvacuation();

		evac.loadConfig( args ) ;
		// (yy this type of short script would point towards having the args in the constructor. kai, jul'18)

		evac.run() ;

	}

	void run() {
		if ( !hasPreparedControler ) {
			prepareControler(  );
		}

		controler.run();

		// need to do this fairly late since otherwise the directory is wiped out again when the controler gets going. kai, apr'18
		final String filename = controler.getConfig().controler().getOutputDirectory() + "/output_linkAttribs.txt.gz";
		log.info( "will write link attributes to " + filename );

		try ( BufferedWriter writer = IOUtils.getBufferedWriter( filename ) ) {
			writer.write( "id\t" + NetworkConverter.EVACUATION_LINK );
			writer.newLine();
			for ( Link link : controler.getScenario().getNetwork().getLinks().values() ) {
				writer.write( link.getId().toString() + "\t" );
				writer.write( Boolean.toString( (boolean) link.getAttributes().getAttribute( NetworkConverter.EVACUATION_LINK ) ) );
				writer.newLine();
			}
			writer.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}


	}

}

class NonevacLinksPenalizerV2 implements SumScoringFunction.ArbitraryEventScoring {
		// the difference of this one to V1 is that it is heavily penalized to leave the evac network and
		// then re-enter it again.  penalizing things like shortcutting through ferry links, or through
		// centroid connectors.  kai, jul'18

		private final TravelDisutility travelDisutility;
		private final Person person;
		private final Network network;
		private double score = 0.;
		private Link prevLink = null;
		private boolean hasBeenOnEvacNetwork = false;
		private boolean hasLeftEvacNetworkAfterHavingBeenOnIt = false;

		NonevacLinksPenalizerV2( TravelDisutility travelDisutility, Person person, Network network ) {
			this.travelDisutility = travelDisutility;
			this.person = person;
			this.network = network;
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void handleEvent( Event event ) {
			if ( event instanceof LinkEnterEvent ) {
				// (by the framework, only link events where the person is involved (as driver or passenger) end up here!)

				Link link = network.getLinks().get( ( (LinkEnterEvent) event ).getLinkId() );
				score -= ( (FEMPreferEmergencyLinksTravelDisutility) travelDisutility ).getAdditionalLinkTravelDisutility( link, event.getTime(), person, null );

				if ( isEvacLink( link ) ) {
					hasBeenOnEvacNetwork = true;
				}
				if ( hasBeenOnEvacNetwork && !isEvacLink( link ) ) {
					hasLeftEvacNetworkAfterHavingBeenOnIt = true;
				}
				if ( hasLeftEvacNetworkAfterHavingBeenOnIt ) {
					if ( !isEvacLink( prevLink ) && isEvacLink( link ) ) {
						// (means has re-entered evac network for second time; this is what we penalize)
						score -= 100000.;
					}
				}

				prevLink = link;
			}
		}
}

class NonevacLinksPenalizerV1 implements SumScoringFunction.ArbitraryEventScoring {
		private final TravelDisutility travelDisutility;
		private final Person person;
		private final Network network;
		private double score = 0.;

		NonevacLinksPenalizerV1( TravelDisutility travelDisutility, Person person, Network network ) {
			this.travelDisutility = travelDisutility;
			this.person = person;
			this.network = network;
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void handleEvent( Event event ) {
			if ( event instanceof LinkEnterEvent ) {
				// (by the framework, only link events where the person is involved (as driver or passenger) end up here!)

				Link link = network.getLinks().get( ( (LinkEnterEvent) event ).getLinkId() );
				score -= ( (FEMPreferEmergencyLinksTravelDisutility) travelDisutility ).getAdditionalLinkTravelDisutility( link, event.getTime(), person, null );
			}
		}
}

class NonEvacLinkPenalizingScoringFunctionFactory implements ScoringFunctionFactory {
		@Inject
		private ScoringParametersForPerson params;
		@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;
		@Inject private Map<String, TravelTime> travelTimes ;
		@Inject private Network network ;

		@Override public ScoringFunction createNewScoringFunction( Person person) {

			final ScoringParameters parameters = params.getScoringParameters( person );

			TravelTime travelTime = travelTimes.get( TransportMode.car) ;
			TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car).createTravelDisutility(travelTime) ;

			SumScoringFunction sumScoringFunction = new SumScoringFunction();
			sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring( parameters ));
			sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring( parameters , network));
			sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
			sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
			sumScoringFunction.addScoringFunction(new NonevacLinksPenalizerV2( travelDisutility, person, network) );
			return sumScoringFunction;
		}
}

