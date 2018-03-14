package routing;

import femproto.network.NetworkConverter;
import femproto.routing.FEMPreferEmergencyLinksTravelDisutility;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Network;

/**
 * A test to see to what extent the {@link FEMPreferEmergencyLinksTravelDisutility } makes agents stick to FEM routes
 */
public class FEMEvacuationLinkRoutingCounter implements LinkEnterEventHandler{

	private final Network network;

	public FEMEvacuationLinkRoutingCounter(Network network) {
		this.network = network;
	}

	public double getTotalLinkEnterEventCount() {
		return totalLinkEnterEventCount;
	}

	public double getBadLinkEnterEventCount() {
		return badLinkEnterEventCount;
	}

	private int totalLinkEnterEventCount = 0, badLinkEnterEventCount = 0;

	@Override
	public void handleEvent(LinkEnterEvent linkEnterEvent) {
		boolean isEvacLink = false;
		try{
			isEvacLink = (boolean) network.getLinks().get(linkEnterEvent.getLinkId()).getAttributes().getAttribute(NetworkConverter.EVACUATION_LINK);
		}catch (NullPointerException e){
			System.out.println();
		}

		if(isEvacLink)
			totalLinkEnterEventCount++;
		else {
			badLinkEnterEventCount++;
			totalLinkEnterEventCount++;

		}
	}

}
