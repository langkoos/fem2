package femproto.run;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.SumScoringFunction;

import static femproto.run.FEMPreferEmergencyLinksTravelDisutility.isEvacLink;

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
	private boolean hasDiedFromStarvation = false;
	private double departureTime;

	NonevacLinksPenalizerV2(TravelDisutility travelDisutility, Person person, Network network) {
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

			if (isEvacLink(link)) {
				hasBeenOnEvacNetwork = true;
			}
			if (hasBeenOnEvacNetwork && !isEvacLink(link)) {
				hasLeftEvacNetworkAfterHavingBeenOnIt = true;
			}
			if (hasLeftEvacNetworkAfterHavingBeenOnIt) {
				if (!isEvacLink(prevLink) && isEvacLink(link)) {
					// (means has re-entered evac network for second time; this is what we penalize)
					score -= 100000.;
				}
			}

			prevLink = link;
		}
		if (event instanceof PersonEntersVehicleEvent) {
			departureTime = event.getTime();
		}
		if (event instanceof PersonLeavesVehicleEvent) {
			if (event.getTime() - departureTime > FEMUtils.getGlobalConfig().getLongestAllowedEvacuationTime() * 60) {
				score -= 100000.;
			}
		}
	}
}
