package femproto.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.HashMap;
import java.util.Map;

public class SelectedPlanFromSubsectorLeadAgents implements ReplanningListener, ScoringListener {
	@Inject
	Scenario scenario;
	Map<String, Double> worstScoreInSubsector = new HashMap<>();
	Map<String, Person> leaders = new HashMap<>();


	@Override
	public void notifyReplanning(ReplanningEvent event) {

		//then, take your destination and route from them
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getAttributes().getAttribute(scenario.getConfig().plans().getSubpopulationAttributeName()).equals(LeaderOrFollower.LEADER.name())) {
				Id<Person> leaderId = Id.createPersonId(person.getAttributes().getAttribute(LeaderOrFollower.LEADER.name()).toString());
				Person leader = scenario.getPopulation().getPersons().get(leaderId);
				try {
					Id<Link> safeLinkId = PopulationUtils.getLastActivity(leader.getSelectedPlan()).getLinkId();
				PopulationUtils.getLastActivity(person.getSelectedPlan()).setLinkId(safeLinkId);

				}catch (NullPointerException ne){
					System.out.println();
				}
				Leg leaderLeg = (Leg) leader.getSelectedPlan().getPlanElements().get(1);
				NetworkRoute networkRoute = ((NetworkRoute) leaderLeg.getRoute()).clone();
				networkRoute.setVehicleId(Id.createVehicleId(person.getId().toString()));
				Plan selectedPlan = person.getSelectedPlan();
				Leg leg = (Leg) selectedPlan.getPlanElements().get(1);
				leg.setRoute(networkRoute);
			} else {
				leaders.put(person.getAttributes().getAttribute(FEMUtils.getGlobalConfig().getAttribSubsector()).toString(), person);
			}
		}

	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			String subsector = person.getAttributes().getAttribute(FEMUtils.getGlobalConfig().getAttribSubsector()).toString();
			double score = person.getSelectedPlan().getScore();
			Double worstScore = worstScoreInSubsector.get(subsector);
			if (worstScore == null) {
				worstScoreInSubsector.put(subsector, score);
			} else {
				worstScoreInSubsector.put(subsector, Math.min(score, worstScore));
			}
		}
		for (String subsector : leaders.keySet()) {
			leaders.get(subsector).getSelectedPlan().setScore(worstScoreInSubsector.get(subsector));
		}

	}
}
