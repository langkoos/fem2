package femproto.run;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.PrepareForSimImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.List;

class FEMPrepareForSim implements PrepareForSim {
	final TripRouter tripRouter;
	final PrepareForSimImpl delegate;
	final Scenario scenario;
	private static final Logger log = Logger.getLogger(FEMPrepareForSim.class);
	@Inject
	public FEMPrepareForSim(TripRouter tripRouter, PrepareForSimImpl delegate, Scenario scenario) {
		this.tripRouter = tripRouter;
		this.delegate = delegate;
		this.scenario = scenario;
	}

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
				Facility fromFacility = FacilitiesUtils.wrapActivity( origAct );
				Facility toFacility = FacilitiesUtils.wrapActivity( destAct );
				try {
					List<? extends PlanElement> trip = tripRouter.calcRoute( TransportMode.car, fromFacility, toFacility, origAct.getEndTime(), person );
					TripRouter.insertTrip( plan, origAct, trip, destAct );
				} catch ( Exception ee ) {
//									ee.printStackTrace();
					exceptionCnt++;
					plansToRemove.add( plan );

					// Exceptions here are ignored since the network may be disconnected, and so there may be no route from
					// some subsector to some safe node. yyyy maybe rather throw the exception?  kai, jul'18
					// tendency to tell them that it needs to be connected, and throw exceptions early.
					// yyyy means network connector needs to complain if subsectors/safeNodes are cut off from the main component.
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
		scenario.getPopulation().getPersons().values().removeIf(person -> person.getPlans().isEmpty() ) ;
		// This can, in principle, happen when a person sits in a subsector that does not have a network
		// connection to any of the safe nodes. yyyy maybe we should rather throw an exception here?
		// kai, jul'18

		// going through all plans of all persons and certify that we have valid plans:
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				for ( Leg leg : TripStructureUtils.getLegs( plan ) ) {
					Gbl.assertNotNull( leg.getRoute() );
					Gbl.assertIf( leg.getRoute() instanceof NetworkRoute);
				}
			}
		}

		// run the default PrepareForSimImpl:
		delegate.run();
	}
}
