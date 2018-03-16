package routing;

import femproto.network.NetworkConverter;
import femproto.routing.FEMPreferEmergencyLinksTravelDisutility;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * A test to see to what extent the {@link FEMPreferEmergencyLinksTravelDisutility } makes agents stick to FEM routes
 */
public class FEMEvacuationLinkRoutingCounter implements LinkEnterEventHandler{

	private final Network network;
	
	private Map<Id<Vehicle>,Id<Link>> map = new HashMap<>() ;

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

		if(isEvacLink) {
			totalLinkEnterEventCount++;
			map.put( linkEnterEvent.getVehicleId(), linkEnterEvent.getLinkId() );
		} else {
			badLinkEnterEventCount++;
			Id<Link> prevLinkId = map.get(linkEnterEvent.getVehicleId());
			if ( prevLinkId!=null ) {
				Assert.fail("evacLink=" + prevLinkId + " followed by nonEvacLink=" + linkEnterEvent.getLinkId()
				" by vehicle=" + linkEnterEvent.getVehicleId() );
			}
		}
		totalLinkEnterEventCount++;
	}

}
