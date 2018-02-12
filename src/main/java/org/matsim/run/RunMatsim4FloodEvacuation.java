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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.scenario.ScenarioUtils;

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
	
	public static void main(String[] args) {
		Set<String> set = new HashSet<>();
		set.add(TransportMode.car ) ;
		
		Config config ;
		if ( args==null || args.length==0 || args[0]=="" ) {
			config = ConfigUtils.createConfig() ;
			config.network().setInputFile( "test/output/femproto/gis/NetworkConverterTest/testMain/netconvert.xml.gz");
			config.plans().setInputFile("pop.xml.gz");

//			config = ConfigUtils.loadConfig( "workspace-csiro/proj1/wsconfig-for-matsim-v10.xml" ) ;
//			config = ConfigUtils.loadConfig( "scenarios/hawkesbury-from-bdi-project-2018-01-16/config.xml" ) ;
			
			
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
			
			config.plansCalcRoute().setNetworkModes(set);
			
			config.qsim().setMainModes(set);
			
			config.qsim().setEndTime(36*3600);
			{
				PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("home") ;
				params.setScoringThisActivityAtAll(false);
				config.planCalcScore().addActivityParams(params);
			}
			{
				PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("start") ;
				params.setScoringThisActivityAtAll(false);
				config.planCalcScore().addActivityParams(params);
			}
			{
				PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("safe") ;
				params.setScoringThisActivityAtAll(false);
				config.planCalcScore().addActivityParams(params);
			}
			
		} else {
			config = ConfigUtils.loadConfig(args[0]) ;
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			link.setAllowedModes(  set ); // yyyyyy fix in network generator; needs to be comma separated!!
			link.setCapacity( link.getCapacity() * 60.); // yyyyyy seems to be capacity per minute; correct in netconvert
			link.setLength( link.getLength()*1000. ); // yyyyyy correct in netconvert
		}
		
		// yyyyyy reduce to 10% for debugging:
		List<Id<Person>> list = new ArrayList<>() ;
		for ( Id<Person> personId : scenario.getPopulation().getPersons().keySet() ) {
			if (MatsimRandom.getRandom().nextDouble() < 0.9 ) {
				list.add( personId) ;
			}
		}
		for ( Id<Person> toBeRemoved : list ) {
			scenario.getPopulation().removePerson( toBeRemoved ) ;
		}

		
		//		preparationsForRmitHawkesburyScenario();
		
		Controler controler = new Controler( scenario ) ;
		
		controler.run();
		
	}
	
	private static void preparationsForRmitHawkesburyScenario( Scenario scenario ) {
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
