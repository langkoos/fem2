package femproto.run;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class SelectOneBestRoutePerSubsector implements StartupListener,
		ReplanningListener
//	IterationEndsListener
{
	private static final Logger log = Logger.getLogger(SelectOneBestRoutePerSubsector.class);

	private static final Random rnd = MatsimRandom.getLocalInstance();

	@Inject
	Population population;
	@Inject
	Config config;
	private int noMoreInnovationIteration;

	@Override
	public void notifyReplanning(ReplanningEvent event) {
//	@Override public void notifyIterationEnds(IterationEndsEvent event) {

		if(event.getIteration() < 0.8 * config.controler().getLastIteration())
			return;

		Table<String, Id<Link>, Double> sums = HashBasedTable.create();
		Table<String, Id<Link>, Double> cnts = HashBasedTable.create();

		// go through all agents and memorize, for each subsector, the score for each
		// route:
		for (Person person : population.getPersons().values()) {

			// origin comes from the attributes (could, in theory, be persons on different links):
			String subsector = FEMUtils.getSubsectorName(person);

			// need to find consensus route, assuming single destination
			for (Plan plan : person.getPlans()) {
				if (plan.getScore() != null) {
					Leg leg = (Leg) plan.getPlanElements().get(1);
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					for (Id<Link> linkId : route.getLinkIds()) {

						final Double sum = sums.get(subsector, linkId);
						if (sum == null) {
							sums.put(subsector, linkId, plan.getScore());
						} else {
							sums.put(subsector, linkId, sum + plan.getScore());
						}
						final Double cnt = cnts.get(subsector, linkId);
						if (cnt == null) {
							cnts.put(subsector, linkId, 1.0);
						} else {
							cnts.put(subsector, linkId, cnt + 1);
						}
					}
				}
			}
		}


		// find for each subsector, the person with the highest score based on score of plans traversing links

		Table<String, Double, Id<Person>> linkBasedScores = HashBasedTable.create();

		for (Person person : population.getPersons().values()) {
			double personSum = 0.0;
			String subsector = FEMUtils.getSubsectorName(person);
			Plan plan = person.getSelectedPlan();
			if (plan.getScore() != null) {
				Leg leg = (Leg) plan.getPlanElements().get(1);
				NetworkRoute route = (NetworkRoute) leg.getRoute();
				for (Id<Link> linkId : route.getLinkIds()) {

					final Double sum = sums.get(subsector, linkId);
					final Double cnt = cnts.get(subsector, linkId);
					double avgScore = sum / cnt;
					personSum += avgScore;
				}
			}
			linkBasedScores.put(subsector, personSum, person.getId());
		}


		Map<String, NetworkRoute> bestRoutes = new HashMap<>();
		// find the best scoring route (the one that traverses the most popular, high scoring links) for each subSector
		for (String subsector : linkBasedScores.rowKeySet()) {
			TreeMap<Double, Id<Person>> row = new TreeMap<>();
			row.putAll(linkBasedScores.row(subsector));
			Leg leg = (Leg) population.getPersons().get(row.lastEntry().getValue()).getSelectedPlan().getPlanElements().get(1);
			NetworkRoute bestRoute = (NetworkRoute) leg.getRoute();
			bestRoutes.put(subsector, bestRoute);

		}


		// select for each person the plan we want:
		RandomUnscoredPlanSelector<Plan, Person> selector = new RandomUnscoredPlanSelector<>();
		double qualifyingProbability = ((double) event.getIteration()) / Math.max(event.getIteration(), 0.9 * (double) config.controler().getLastIteration());

		for (Person person : population.getPersons().values()) {
			// first find if there is still an unscored plan, and if so use that:
			Plan unscoredPlan = selector.selectPlan(person);
			if (unscoredPlan != null) {
				person.setSelectedPlan(unscoredPlan);
			} else {
				// else, set route as computed above, with the proportion of compliance increasing with increasing iterations:
//				if (rnd.nextDouble() <= qualifyingProbability) {
					NetworkRoute bestRoute = bestRoutes.get(FEMUtils.getSubsectorName(person)).clone();
					bestRoute.setVehicleId(Id.createVehicleId(person.getId().toString()));
					Plan selectedPlan = person.getSelectedPlan();
					Leg leg = (Leg) selectedPlan.getPlanElements().get(1);
					leg.setRoute(bestRoute);
//				}

			}
		}


	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// remember to set strategy to "keepSelected"
//		Gbl.assertIf(config.strategy().getStrategySettings().size() == 1);
//		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
//			Gbl.assertIf(settings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected));
//		}

	}



}
