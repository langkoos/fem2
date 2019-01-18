package femproto.run;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.SumScoringFunction;

class NonevacLinksPenalizerV1 implements SumScoringFunction.ArbitraryEventScoring {
	private final TravelDisutility travelDisutility;
	private final Person person;
	private final Network network;
	private double score = 0.;

	NonevacLinksPenalizerV1(TravelDisutility travelDisutility, Person person, Network network) {
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
	public void handleEvent(Event event) {
		if (event instanceof LinkEnterEvent) {
			// (by the framework, only link events where the person is involved (as driver or passenger) end up here!)

			Link link = network.getLinks().get(((LinkEnterEvent) event).getLinkId());
			score -= ((FEMPreferEmergencyLinksTravelDisutility) travelDisutility).getAdditionalLinkTravelDisutility(link, event.getTime(), person, null);
		}
	}
}
