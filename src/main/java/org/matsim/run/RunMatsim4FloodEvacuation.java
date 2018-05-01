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

import femproto.config.FEMConfigGroup;
import femproto.network.NetworkConverter;
import femproto.routing.FEMPreferEmergencyLinksTravelDisutility;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.analysis.kai.KNAnalysisEventsHandler;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.core.network.NetworkUtils.*;

/**
 * @author nagel
 *
 */
public class RunMatsim4FloodEvacuation {
	private static final Logger log = Logger.getLogger(RunMatsim4FloodEvacuation.class) ;
	private Controler controler;
	
	RunMatsim4FloodEvacuation(String[] args) {
		this( args, null ) ;
	}
	RunMatsim4FloodEvacuation(String[] args, String outputDir ) {
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
		
		config.controler().setLastIteration(10);
		
		if ( outputDir!=null && !outputDir.equals("") ) {
			config.controler().setOutputDirectory( outputDir );
		}
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		{
			Set<String> set = new HashSet<>();
			set.add(TransportMode.car);
			config.plansCalcRoute().setNetworkModes(set);
			config.qsim().setMainModes(set);
		}
		
//		config.qsim().setEndTime(264 * 3600);
		// not setting anything just means that the simulation means until everybody is safe or aborted. kai, apr'18
		StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
			strategySettings.setStrategyName("BestScore");
			strategySettings.setWeight(1);
		config.strategy().addStrategySettings(strategySettings);
		config.qsim().setRemoveStuckVehicles(true);
		config.qsim().setStuckTime(86400);
		
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
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		FEMConfigGroup femConfig = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		
		// === add overriding config material if there is something in that file:
		
		ConfigUtils.loadConfig(config, ConfigGroup.getInputFileURL(config.getContext(), "overridingConfig.xml"));
		
		// prepare scenario:
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// yyyyyy reduce to 10% for debugging:
		double sample = 0.1;
		List<Id<Person>> list = new ArrayList<>();
		for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
			if (MatsimRandom.getRandom().nextDouble() < (1. - sample)) {
				list.add(personId);
			}
		}
		for (Id<Person> toBeRemoved : list) {
			scenario.getPopulation().removePerson(toBeRemoved);
		}
		scenario.getConfig().qsim().setFlowCapFactor(sample);
		scenario.getConfig().qsim().setStorageCapFactor(sample);
		
		//		preparationsForRmitHawkesburyScenario();
		
		controler = new Controler(scenario);
		
		// ---
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.addControlerListenerBinding().to( KaiAnalysisListener.class ) ;
				
				switch (femConfig.getFEMRoutingMode()) {
					case preferEvacuationLinks:
						final String routingMode = TransportMode.car;
						// (the "routingMode" can be different from the "mode".  useful if, say, different cars should follow different routing
						// algorithms, but still executed as "car" on the network.  Ask me if this might be useful for this project.  kai, feb'18)
						
						// register this routing mode:
						addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode));
						
						// define how the travel time is computed:
						addTravelTimeBinding(routingMode).to(FreeSpeedTravelTime.class);
						
						// congested travel time:
//						bind(WithinDayTravelTime.class).in(Singleton.class);
//						addEventHandlerBinding().to(WithinDayTravelTime.class);
//						addMobsimListenerBinding().to(WithinDayTravelTime.class);
//						addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;
						
						// define how the travel disutility is computed:
						TravelDisutilityFactory disutilityFactory = new FEMPreferEmergencyLinksTravelDisutility.Factory(scenario.getNetwork());
						addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
						
						break;
					default:
						throw new RuntimeException("not implemented");
				}
			}
		});
		
	}
		
	public static void main(String[] args) {
		new RunMatsim4FloodEvacuation(args).run();
	}

	public void run() {
		controler.run();
		
		// need to do this fairly late since otherwise the directory is wiped out again when the controler gets going. kai, apr'18
		final String filename = controler.getConfig().controler().getOutputDirectory() + "/linkAttribs.txt.gz";
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
	
}
