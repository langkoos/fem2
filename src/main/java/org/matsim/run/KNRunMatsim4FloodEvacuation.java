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
package org.matsim.run;

import com.google.inject.Inject;
import femproto.FEMAttributes;
import femproto.config.FEMConfigGroup;
import femproto.network.NetworkConverter;
import femproto.routing.FEMPreferEmergencyLinksTravelDisutility;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
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
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.PrepareForSimImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static femproto.routing.FEMPreferEmergencyLinksTravelDisutility.isEvacLink;
import static org.matsim.core.network.NetworkUtils.getEuclideanDistance;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.*;

/**
 * @author nagel
 *
 */
public class KNRunMatsim4FloodEvacuation {
	private static final Logger log = Logger.getLogger(KNRunMatsim4FloodEvacuation.class) ;
	private static boolean safeNodeBySector = false ;
	private Controler controler;
	
	KNRunMatsim4FloodEvacuation(String[] args) {
		this( args, null ) ;
	}
	KNRunMatsim4FloodEvacuation(String[] args, String outputDir ) {
		// (outputDir so we can set this from the test; not a terribly good solution, but still cleaner than hardcoding the
		// test output dir in the test config file. kai, apr'18)
		
		Config config;
		if (args == null || args.length == 0 || args[0] == "") {
			
			config = ConfigUtils.loadConfig("scenarios/fem2016/configSmall.xml");

//			config = ConfigUtils.createConfig() ;
//			config.network().setInputFile( "test/output/femproto/gis/NetworkConverterTest/testMain/netconvert.xml.gz");
//			config.plans().setInputFile("pop.xml.gz");

//			config = ConfigUtils.loadConfig( "workspace-csiro/proj1/wsconfig-for-matsim-v10.xml" ) ;
//			config = ConfigUtils.loadConfig( "scenarios/hawkesbury-from-bdi-project-2018-01-16/configSmall.xml" ) ;
			
		
		} else {
			log.info("found an argument, thus loading config from file ...");
			config = ConfigUtils.loadConfig(args[0]);
		}
		
		// === prepare config:
		
		// --- controler config group:
		final int lastIteration = 100;
		config.controler().setLastIteration(lastIteration);
//		config.qsim().setEndTime(3600.);
		
		if ( outputDir!=null && !outputDir.equals("") ) {
			config.controler().setOutputDirectory( outputDir );
		}
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setRoutingAlgorithmType( ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra );
		
		// --- plansCalcRoute config group:
		{
			Set<String> set = new HashSet<>();
			set.add(TransportMode.car);
			config.plansCalcRoute().setNetworkModes(set);
			config.qsim().setMainModes(set);
		}
		
		// --- strategies:
		if ( safeNodeBySector ){
			StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
			strategySettings.setStrategyName(DefaultSelector.KeepLastSelected);
			strategySettings.setWeight(1);
			config.strategy().addStrategySettings(strategySettings);
		} else {
//		{
//			StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
//			strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
//			strategySettings.setWeight(1);
//			strategySettings.setDisableAfter( (int) (0.9*lastIteration) ); // (50 iterations was not enough)
//			config.strategy().addStrategySettings(strategySettings);
//		}
			{
				StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
				strategySettings.setStrategyName(DefaultSelector.BestScore);
				strategySettings.setWeight(1);
				config.strategy().addStrategySettings(strategySettings);
			}
			{
				StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
				strategySettings.setStrategyName(DefaultSelector.SelectRandom);
				strategySettings.setWeight(0.1);
				strategySettings.setDisableAfter((int) (0.9 * lastIteration)); // (50 iterations was not enough)
				config.strategy().addStrategySettings(strategySettings);
			}
		}
		config.strategy().setMaxAgentPlanMemorySize(0);
		
		// --- routing:
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);

//		config.travelTimeCalculator().setMaxTime(72*3600); // congestion observation, also for decongestion
		
		
		// --- qsim:
		
		config.qsim().setRemoveStuckVehicles(true);
		config.qsim().setStuckTime(86400);
		
		//		config.qsim().setEndTime(264 * 3600);
		// not setting anything just means that the simulation means until everybody is safe or aborted. kai, apr'18
		
		// --- scoring:
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.9);
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
		
		// --- decongestion (toll):
		
		DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule(config, DecongestionConfigGroup.class);
		decongestionSettings.setEnableDecongestionPricing(true);
		decongestionSettings.setToleratedAverageDelaySec(30.);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
		decongestionSettings.setUpdatePriceInterval(1);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);

		decongestionSettings.setDecongestionApproach(DecongestionConfigGroup.DecongestionApproach.PID);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setKp(0.5);
		
//		decongestionSettings.setDecongestionApproach( DecongestionConfigGroup.DecongestionApproach.BangBang );
//		decongestionSettings.setInitialToll(20.);
//		decongestionSettings.setTollAdjustment(20.);

		decongestionSettings.setIntegralApproach(DecongestionConfigGroup.IntegralApproach.UnusedHeadway);
		decongestionSettings.setIntegralApproachUnusedHeadwayFactor(10.0);
		decongestionSettings.setIntegralApproachAverageAlpha(0.0);

		// --- fem:
		
		FEMConfigGroup femConfig = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		
		// === add overriding config material if there is something in that file:
		
		ConfigUtils.loadConfig(config, ConfigGroup.getInputFileURL(config.getContext(), "overridingConfig.xml"));
		
		// ===========================================================================
		// ===========================================================================
		// ===========================================================================
		// === prepare scenario === :
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// yyyy reduce to sample for debugging:
		sampleDown(scenario, 0.01);
		
		giveAllSafeNodesToAllAgents(scenario);
		
		// move activity end times towards 0:00 so that decongestion can work:
		boolean first = true ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				Activity firstAct = (Activity) plan.getPlanElements().get(0);
				
				if (first) {
					firstAct.setEndTime(0);
				} else {
					// have all other agents start one sec late so that in VIA we can still see all these others at home:
//					firstAct.setEndTime(Math.max(1,firstAct.getEndTime()-240.*3600.)) ;
					firstAct.setEndTime(1); // yyyyyy
				}
				
			}
			if ( first ) {
				first = false ;
			}
		}

		//		preparationsForRmitHawkesburyScenario();
		
		// ===========================================================================
		// ===========================================================================
		// ===========================================================================
		// === prepare controler:

		controler = new Controler(scenario);
		
		
		OutputEvents2TravelDiaries events2TravelDiaries = new OutputEvents2TravelDiaries(controler);
		
		// ---
		
		// congestion toll computation
		
		controler.addOverridingModule(new DecongestionModule(scenario));
		
		// toll-adjusted routing is included in FEM disutility below.
		
		// ---

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.addControlerListenerBinding().to( KaiAnalysisListener.class ) ;

				if ( safeNodeBySector) {
					this.addControlerListenerBinding().to(SelectOneBestSafeNodePerSubsector.class);
				}

				bind(OutputEvents2TravelDiaries.class).toInstance(events2TravelDiaries);
				
				switch (femConfig.getFEMRoutingMode()) {
					case preferEvacuationLinks:
						final String routingMode = TransportMode.car;
						// (the "routingMode" can be different from the "mode".  useful if, say, different cars should follow different routing
						// algorithms, but still executed as "car" on the network.  Ask me if this might be useful for this project.  kai, feb'18)

						// register this routing mode:
						addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode));

						// define how the travel time is computed:
//						addTravelTimeBinding(routingMode).to(FreeSpeedTravelTime.class);
						
						
						// congested travel time:
//						bind(WithinDayTravelTime.class).in(Singleton.class);
//						addEventHandlerBinding().to(WithinDayTravelTime.class);
//						addMobsimListenerBinding().to(WithinDayTravelTime.class);
//						addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;
						
						// define how the travel disutility is computed:
//						TravelDisutilityFactory delegateFactory = new TollTimeDistanceTravelDisutilityFactory() ;
						TravelDisutilityFactory delegateFactory = new OnlyTimeDependentTravelDisutilityFactory() ;
						addTravelDisutilityFactoryBinding(routingMode).toInstance(
								new FEMPreferEmergencyLinksTravelDisutility.Factory(scenario.getNetwork(),delegateFactory)
						);
						
						break;
					default:
						throw new RuntimeException("not implemented");
				}

				this.bindScoringFunctionFactory().to(NonEvacLinkPenalizingScoringFunctionFactory.class);
				
				// calculating all routes at initialization (assuming that they are sufficiently defined by the evac
				// network).  kai, may'18
				this.bind(PrepareForSimImpl.class) ;
				this.bind(PrepareForSim.class).toInstance(new PrepareForSim() {
					@Inject TripRouter tripRouter ;
					@Inject PrepareForSimImpl delegate ;
					@Override public void run() {
						log.info("running local PrepareForSim implementation ...") ;
						long exceptionCnt = 0;
						Counter counter = new Counter("person # ") ;
						for (Person person : scenario.getPopulation().getPersons().values()) {
							counter.incCounter();
							List<Plan> plansToRemove = new ArrayList<>();
							for (Plan plan : person.getPlans()) {
								List<Activity> acts = TripStructureUtils.getActivities(plan, tripRouter.getStageActivityTypes());
								Activity origAct = acts.get(0);
								Activity destAct = acts.get(1);
								Facility fromFacility = new ActivityWrapperFacility(origAct);
								Facility toFacility = new ActivityWrapperFacility(destAct);
								try {
									List<? extends PlanElement> trip = tripRouter.calcRoute(TransportMode.car, fromFacility, toFacility, origAct.getEndTime(), person);
									TripRouter.insertTrip(plan, origAct, trip, destAct);
								} catch (Exception ee) {
//									ee.printStackTrace();
									exceptionCnt++;
									plansToRemove.add(plan);
								}
							}
							for (Plan planToRemove : plansToRemove) {
								person.removePlan(planToRemove);
							}
						}
						log.warn("exceptionCnt=" + exceptionCnt + "; presumably that many person--safeNode combinations cannot be routed.");
						
						// persons needs to have at least one plan with one activity!!!!
						List<Id<Person>> personIdsToRemove = new ArrayList<>() ;
						for ( Person person : scenario.getPopulation().getPersons().values() ) {
							if ( person.getPlans().isEmpty() ) {
								personIdsToRemove.add( person.getId() ) ;
							}
						}
						for ( Id<Person> personId : personIdsToRemove ) {
							scenario.getPopulation().removePerson(personId);
						}
						
						for ( Person person : scenario.getPopulation().getPersons().values() ) {
							for ( Plan plan : person.getPlans() ) {
								for ( Leg leg : TripStructureUtils.getLegs(plan) ) {
									Gbl.assertNotNull(leg.getRoute());
									Gbl.assertIf( leg.getRoute() instanceof NetworkRoute);
								}
							}
						}
						PopulationUtils.writePopulation(scenario.getPopulation(), "pop.xml.gz");
						delegate.run() ;
					}
				});

			}
		});
		
	}
	
	public static void main(String[] args) {
		new KNRunMatsim4FloodEvacuation(args).run();
	}

	public void run() {
		controler.run();
		
		// need to do this fairly late since otherwise the directory is wiped out again when the controler gets going. kai, apr'18
		final String filename = controler.getConfig().controler().getOutputDirectory() + "/output_linkAttribs.txt.gz";
		log.info( "will write link attributes to " + filename ) ;
		
		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			writer.write("id\t" + NetworkConverter.EVACUATION_LINK);
			writer.newLine();
			for (Link link : controler.getScenario().getNetwork().getLinks().values()) {
				writer.write(link.getId().toString() + "\t");
				writer.write(Boolean.toString((boolean) link.getAttributes().getAttribute(NetworkConverter.EVACUATION_LINK)));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	private static void sampleDown(Scenario scenario, double sample) {
		List<Id<Person>> list = new ArrayList<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (MatsimRandom.getRandom().nextDouble() < (1. - sample)) {
				list.add(person.getId());
			}
			
		}
		for (Id<Person> toBeRemoved : list) {
			scenario.getPopulation().removePerson(toBeRemoved);
		}
		scenario.getConfig().qsim().setFlowCapFactor(sample);
		scenario.getConfig().qsim().setStorageCapFactor(sample);
	}
	
	private static void preparationsForRmitHawkesburyScenario( Scenario scenario ) {
		
		// yy That population (e.g. haw_pop_route_defined.xml.gz) has an "Evacuation" activity in between
		// "Home" and "Safe".  Without documentation I don't know what that means.  Thus removing it here. kai, jan/apr'18
		// yyyy Why did this work without also removing the routes? kai, apr'18
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			List<PlanElement> toRemove = new ArrayList<>() ;
			boolean justRemoved = false ;
			for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
				if ( pe instanceof Activity ) {
					final Activity act = (Activity) pe;
					if ( act.getType().equals("Evacuation")) {
						toRemove.add( act ) ;
						justRemoved = true ;
					}
					act.setLinkId(null);
				} else {
					Leg leg = (Leg) pe;
					if ( justRemoved ) {
						justRemoved = false ;
						toRemove.add(leg) ;
					}
					leg.setRoute(null);
				}
			}
			person.getSelectedPlan().getPlanElements().removeAll( toRemove ) ;
		}
		
		
		// There are some "weird" links in that scenario, way too short.  (Maybe centroid connectors that ended
		// up being used for routing?) Extending them to Euclidean length.  kai, jan/apr'18
		new NetworkCleaner().run(scenario.getNetwork());
		new NetworkSimplifier().run(scenario.getNetwork());
		new NetworkCleaner().run(scenario.getNetwork());
		
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double euclid = getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
			if ( euclid > link.getLength() ) {
				log.warn("linkId=" + link.getId() +  "; length=" + link.getLength()
								 + "; EuclideanLength=" + euclid ) ;
				link.setLength(euclid);
			}
			double maxSpeed = 100./3.6 ; // m/s
			if ( link.getFreespeed() > maxSpeed ) {
				log.warn("linkId=" + link.getId() + "; freespeed=" + link.getFreespeed() ) ;
				link.setFreespeed(maxSpeed);
			}
		}
	}
	
	private static class NonevacLinksPenalizer implements SumScoringFunction.ArbitraryEventScoring {
		private final TravelDisutility travelDisutility;
		private final Person person;
		private final Network network;
		private double score = 0. ;
		Link prevLink = null ;
		boolean hasBeenOnEvacNetwork = false ;
		boolean hasLeftEvacNetworkAfterHavingBeenOnIt = false ;
		NonevacLinksPenalizer(TravelDisutility travelDisutility, Person person, Network network) {
			this.travelDisutility = travelDisutility;
			this.person = person;
			this.network = network;
		}
		@Override public void finish() { }
		@Override public double getScore() {
			return score ;
		}
		@Override public void handleEvent(Event event) {
			if ( event instanceof LinkEnterEvent ) {
				// (by the framework, only link events where the person is involved (as driver or passenger) end up here!)
				
				Link link = network.getLinks().get( ((LinkEnterEvent) event).getLinkId() ) ;
				score -= ((FEMPreferEmergencyLinksTravelDisutility)travelDisutility).getAdditionalLinkTravelDisutility(link,event.getTime(), person,null) ;
				
				if ( isEvacLink(link) ) {
					hasBeenOnEvacNetwork = true ;
				}
				if ( hasBeenOnEvacNetwork && !isEvacLink(link) ) {
					hasLeftEvacNetworkAfterHavingBeenOnIt = true ;
				}
				if ( hasLeftEvacNetworkAfterHavingBeenOnIt) {
					if ( !isEvacLink(prevLink) && isEvacLink(link) ) {
						// (means has re-entered evac network for second time; this is what we penalize)
						score -= 100000.;
					}
				}
				
				prevLink = link ;
			}
		}
	}
	
	static Id<Link> getDestinationLinkId(Plan plan) {
		return PopulationUtils.getLastActivity(plan).getLinkId();
	}
	
	static String getSubsector(Person person) {
		final String attribute = (String) person.getAttributes().getAttribute(FEMAttributes.SUBSECTOR);
		Gbl.assertNotNull(attribute);
		return attribute;
	}
	
	private static class NonEvacLinkPenalizingScoringFunctionFactory implements ScoringFunctionFactory {
		@Inject private ScoringParametersForPerson params;
		@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;
		@Inject private Map<String,TravelTime> travelTimes ;
		@Inject private Network network ;
		
		@Override public ScoringFunction createNewScoringFunction(Person person) {

			final ScoringParameters parameters = params.getScoringParameters( person );

			TravelTime travelTime = travelTimes.get(TransportMode.car) ;
			TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car).createTravelDisutility(travelTime) ;

			SumScoringFunction sumScoringFunction = new SumScoringFunction();
			sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring( parameters ));
			sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring( parameters , network));
			sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
			sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
			sumScoringFunction.addScoringFunction(new NonevacLinksPenalizer( travelDisutility, person, network) );
			return sumScoringFunction;
		}
	}
	
	private static void giveAllSafeNodesToAllAgents(Scenario scenario) {
		// collect all safe locations (could also get this from the attributes, but currently can't iterate over them)
		List<Id<Link>> safeLinkIds = new ArrayList<>() ;
		{
			Set<Id<Link>> safeLinkIdsAsSet = new LinkedHashSet<>();
			
			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					for (PlanElement pe : plan.getPlanElements()) {
						if (pe instanceof Activity) {
							final Activity activity = (Activity) pe;
							if (activity.getType().equals("safe")) {
								safeLinkIdsAsSet.add(activity.getLinkId());
							}
						}
					}
				}
			}
			safeLinkIds.addAll( safeLinkIdsAsSet ) ;
		}
		log.info("safeLinkIDs:");
		for (Id<Link> safeLinkId : safeLinkIds) {
			log.info(safeLinkId);
		}
		// give all agents all safe locations (to compensate for errors we seem to have in input data):
		PopulationFactory pf = scenario.getPopulation().getFactory();
		;
		long cnt = 0 ;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			cnt++ ;
			
			// memorize evac link id:
			Id<Link> evacLinkId = ((Activity) person.getPlans().get(0).getPlanElements().get(0)).getLinkId();
			;
			// clear all plans:
			person.getPlans().clear();
			// add all safe locations as potential plans:
			
			if ( cnt % (long)(scenario.getPopulation().getPersons().size()/10) == 0 ) {
				Collections.shuffle(safeLinkIds, MatsimRandom.getLocalInstance());
			}
			// (so that we don't have everybody go to same safe location in 0th, 1st, 2nd, ... iteration. kai, may'18)
			
			for (Id<Link> safeLinkId :  safeLinkIds ) {
				Plan plan = pf.createPlan();
				{
					Activity act = pf.createActivityFromLinkId("evac", evacLinkId);
					act.setEndTime(0.);
					plan.addActivity(act);
				}
				{
					Leg leg = pf.createLeg(TransportMode.car);
					plan.addLeg(leg);
				}
				{
					Activity act = pf.createActivityFromLinkId("safe", safeLinkId);
					plan.addActivity(act);
				}
				person.addPlan(plan);
			}
			person.setSelectedPlan(person.getPlans().get(0));
		}
	}
	
	
}
