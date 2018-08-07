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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.IntegralApproach;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.PrepareForSimImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
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
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.Facility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
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
	
	Config loadConfig( final String[] args ) {
		if ( args == null || args.length == 0 || args[0] == "" ) {
			
			config = ConfigUtils.loadConfig( "scenarios/fem2016_v20180307/configSmall.xml" );

//			config = ConfigUtils.createConfig() ;
//			config.network().setInputFile( "test/output/femproto/gis/NetworkConverterTest/testMain/netconvert.xml.gz");
//			config.plans().setInputFile("pop.xml.gz");

//			config = ConfigUtils.loadConfig( "workspace-csiro/proj1/wsconfig-for-matsim-v10.xml" ) ;
//			config = ConfigUtils.loadConfig( "scenarios/hawkesbury-from-bdi-project-2018-01-16/configSmall.xml" ) ;
		
		
		} else {
			log.info( "found an argument, thus loading config from file ..." );
			config = ConfigUtils.loadConfig( args[0] );
		}
		
		// ---
		
		hasLoadedConfig = true ;
		return config ;
	}
	
	void prepareConfig() {
		// division into loadConfig and prepareConfig is necessary since some configuration depends
		// on (FEM)config switches, and thus configuration-in-code needs to be done before
		// prepareConfig.  :-(  kai, jul'18
		
		if ( !hasLoadedConfig ) {
			loadConfig( null ) ;
		}
		
		// --- controler config group:
		final int lastIteration = 100;
		config.controler().setLastIteration( lastIteration );
//		config.qsim().setEndTime(3600.);
		
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setRoutingAlgorithmType( ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra );
		
		// --- strategies:
		config.strategy().setMaxAgentPlanMemorySize( 0 );
		
		// --- routing:
		{
			Set<String> set = new HashSet<>();
			set.add( TransportMode.car );
			config.plansCalcRoute().setNetworkModes( set );
			config.qsim().setMainModes( set );
		}
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

//		config.travelTimeCalculator().setMaxTime(72*3600); // congestion observation, also for decongestion
		
		
		// --- qsim:
		
		config.qsim().setRemoveStuckVehicles( true );
		config.qsim().setStuckTime( 86400 );
		
		//		config.qsim().setEndTime(264 * 3600);
		// not setting anything just means that the simulation means until everybody is safe or aborted. kai, apr'18
		
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
		
		femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );
		
		log.warn( "runType=" + femConfig.getFemRunType() ) ;
		
		switch( femConfig.getFemRunType() ) {
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
					strategySettings.setDisableAfter( (int) ( 0.9 * lastIteration ) ); // (50 iterations was not enough)
					config.strategy().addStrategySettings( strategySettings );
				}
				configureDecongestion( config );
			}
			break;
			case justRunInitialPlansFile:
				config.controler().setLastIteration( 0 );
				// I don't think we need strategies since we would run only the zeroth iteration.  kai, jul'18
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
		}
		
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
		
		scenario = ScenarioUtils.loadScenario( config );
		
		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );

//		new NetworkCleaner().run(scenario.getNetwork());
		// yyyyyy fem2016 network seems to have unconnected pieces.
		
		// yyyyyy reduce to sample for debugging:
		FEMUtils.sampleDown( scenario, 0.01 );
		// yyyy decide how to do this for UI. kai, jul'18
		
		FEMUtils.giveAllSafeNodesToAllAgents( scenario );
		// yyyy will we get valid initial mappings?  kai, jul'18
		
		switch( femConfig.getFemEvacuationTimeAdjustment() ) {
			case takeTimesFromInputPlans:
				FEMUtils.haveOneAgentStartOneSecondEarlierThanEverybodyElse( scenario );
				break;
			case allDepartAtMidnight:
				FEMUtils.moveFirstActivityEndTimesTowardsZero( scenario );
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
		}
		
		
		
		
		//		preparationsForRmitHawkesburyScenario();
		
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
				
				this.addControlerListenerBinding().to( KaiAnalysisListener.class );
				this.addControlerListenerBinding().to( OutputEvents2TravelDiaries.class );
				
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
						TravelDisutilityFactory delegateFactory = new OnlyTimeDependentTravelDisutilityFactory();

//						TravelDisutilityFactory delegateFactory = new TollTimeDistanceTravelDisutilityFactory() ;
						// NOT using the toll based travel disutility, since we are routing without toll, on the
						// empty network, before the iterations start, and then never again.  kai, jul'18
						
						addTravelDisutilityFactoryBinding( routingMode ).toInstance(
								new FEMPreferEmergencyLinksTravelDisutility.Factory( scenario.getNetwork(), delegateFactory )
						);
						
						break;
					default:
						throw new RuntimeException( "not implemented" );
				}
			}
		} );
		
		
		// yyyy in the "justRun" mode, do we also assume initial routes?  kai, jul'18
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				
				this.bindScoringFunctionFactory().to( NonEvacLinkPenalizingScoringFunctionFactory.class );
				
				// calculating all routes at initialization (assuming that they are sufficiently defined by the evac
				// network).  kai, may'18
				this.bind( PrepareForSimImpl.class );
				this.bind( PrepareForSim.class ).toInstance( new PrepareForSim() {
					@Inject TripRouter tripRouter;
					@Inject PrepareForSimImpl delegate;
					@Override public void run() {
						log.info( "running local PrepareForSim implementation ..." );
						long exceptionCnt = 0;
						Counter counter = new Counter( "person # " );
						for ( Person person : scenario.getPopulation().getPersons().values() ) {
							counter.incCounter();
							List<Plan> plansToRemove = new ArrayList<>();
							for ( Plan plan : person.getPlans() ) {
								List<Activity> acts = TripStructureUtils.getActivities( plan, tripRouter.getStageActivityTypes() );
								Activity origAct = acts.get( 0 );
								Activity destAct = acts.get( 1 );
								Facility fromFacility = new ActivityWrapperFacility( origAct );
								Facility toFacility = new ActivityWrapperFacility( destAct );
								try {
									List<? extends PlanElement> trip = tripRouter.calcRoute( TransportMode.car, fromFacility, toFacility, origAct.getEndTime(), person );
									TripRouter.insertTrip( plan, origAct, trip, destAct );
								} catch ( Exception ee ) {
//									ee.printStackTrace();
									exceptionCnt++;
									plansToRemove.add( plan );
									
									// Exceptions here are ignored since the network may be disconnected, and so there may be no route from
									// some subsector to some safe node. yyyy maybe rather throw the exception?  kai, jul'18
								}
							}
							for ( Plan planToRemove : plansToRemove ) {
								person.removePlan( planToRemove );
							}
						}
						if ( exceptionCnt > 0 ) {
							log.warn( "exceptionCnt=" + exceptionCnt + "; presumably that many person--safeNode combinations cannot be routed." );
						}
						
						// remove persons that don't have a plan:
						scenario.getPopulation().getPersons().values().removeIf( person -> person.getPlans().isEmpty() ) ;
						// This can, in principle, happen when a person sits in a subsector that does not have a network
						// connection to any of the safe nodes. yyyy maybe we should rather throw an exception here?
						// kai, jul'18
						
						// going through all plans of all persons and certify that we have valid plans:
						for ( Person person : scenario.getPopulation().getPersons().values() ) {
							for ( Plan plan : person.getPlans() ) {
								for ( Leg leg : TripStructureUtils.getLegs( plan ) ) {
									Gbl.assertNotNull( leg.getRoute() );
									Gbl.assertIf( leg.getRoute() instanceof NetworkRoute );
								}
							}
						}
//						PopulationUtils.writePopulation( scenario.getPopulation(), "pop.xml.gz" );
						
						// run the default PrepareForSimImpl:
						delegate.run();
					}
				} );
			}
		} );
		
		// yyyy should have the infrastructure that is not needed for justRun only enabled for the other runs.  kai, jul'18
		
		switch( femConfig.getFemRunType() ) {
			case optimizeSafeNodesBySubsector:
				controler.addOverridingModule( new AbstractModule() {
					@Override public void install() {
						this.addControlerListenerBinding().to( SelectOneBestSafeNodePerSubsector.class );
					}
				} );
				// no break since the following settings are shared by the two app
			case optimizeSafeNodesByPerson:
				controler.addOverridingModule( new DecongestionModule( scenario ) );
				// toll-dependent routing would have to be added elsewhere, but is not used here since all routes are
				// computed in prepareForSim. kai, jul'18
				break;
			case justRunInitialPlansFile:
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

