package femproto.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

import java.util.Map;

class NonEvacLinkPenalizingScoringFunctionFactory implements ScoringFunctionFactory {
	@Inject
	private ScoringParametersForPerson params;
	@Inject
	private Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	@Inject
	private Map<String, TravelTime> travelTimes;
	@Inject
	private Network network;

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		final ScoringParameters parameters = params.getScoringParameters(person);

		TravelTime travelTime = travelTimes.get(TransportMode.car);
		TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car).createTravelDisutility(travelTime);

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, network));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		sumScoringFunction.addScoringFunction(new NonevacLinksPenalizerV2(travelDisutility, person, network));
//		sumScoringFunction.addScoringFunction(new SafeNodePriorityPenaliser(person));
		return sumScoringFunction;
	}
}
