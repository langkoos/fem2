package femproto.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.core.network.NetworkUtils.getEuclideanDistance;

class FEMUtils {
	private static final Logger log = Logger.getLogger(FEMUtils.class) ;
	
	private FEMUtils(){} // do not instantiate
	
	static void preparationsForRmitHawkesburyScenario( Scenario scenario ) {
		
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
	
	private static int cnt = 10 ;
	
	static void moveFirstActivityEndTimesTowardsZero( final Scenario scenario ) {
		// move all activity end times to 00:00:00 or 00:00:01
		boolean first = true ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				Activity firstAct = (Activity) plan.getPlanElements().get(0);
				double origEndTime = firstAct.getEndTime();;

				if (first) {
					firstAct.setEndTime(0);
				} else {
					// have all other agents start one sec later so that in VIA we can still see all these others at home:
					final double newEndTime = 1. ;
					firstAct.setEndTime(newEndTime);
				}
				if ( cnt > 0 ) {
					log.info( "origEndTime=" + origEndTime + "; newEndTime=" + firstAct.getEndTime() );
				}
				cnt-- ;
				if ( cnt==0 ) {
					log.info( Gbl.FUTURE_SUPPRESSED ) ;
				}

			}
			if ( first ) {
				first = false ;
			}
		}
		
	}
	static void haveOneAgentStartOneSecondEarlierThanEverybodyElse( Scenario scenario ) {
		// find earliest departure time:
		double earliest = Double.POSITIVE_INFINITY ;
		Id<Person> personId = null ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			Activity firstAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			if ( firstAct.getEndTime() < earliest ) {
				earliest = firstAct.getEndTime() ;
				personId = person.getId() ;
			}
		}
		// set that departure time one second earlier:
		if ( earliest >= 1.0 ) {
			for ( Plan plan : scenario.getPopulation().getPersons().get( personId ).getPlans() ) {
				Activity firstAct = (Activity) plan.getPlanElements().get(0);
				firstAct.setEndTime( earliest-1 );
			}
		}
	}
	
	static void sampleDown( Scenario scenario, double sample ) {
		List<Id<Person>> list = new ArrayList<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if ( MatsimRandom.getRandom().nextDouble() < (1. - sample)) {
				list.add(person.getId());
			}
			
		}
		for (Id<Person> toBeRemoved : list) {
			scenario.getPopulation().removePerson(toBeRemoved);
		}
		scenario.getConfig().qsim().setFlowCapFactor(sample);
		scenario.getConfig().qsim().setStorageCapFactor(sample);
	}
	
	static void giveAllSafeNodesToAllAgents( Scenario scenario ) {
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
			
			// memorize some things:
			final Activity activity = (Activity) person.getPlans().get(0).getPlanElements().get(0);
			Id<Link> evacLinkId = activity.getLinkId();
			double endTime = activity.getEndTime();;
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
					act.setEndTime( endTime );
					plan.addActivity(act);
				}
				{
					Leg leg = pf.createLeg( TransportMode.car);
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
