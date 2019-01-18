package femproto.run.eventhandlers;

import femproto.prepare.network.NetworkConverter;
import femproto.run.FEMPreferEmergencyLinksTravelDisutility;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * A test to see to what extent the {@link FEMPreferEmergencyLinksTravelDisutility } makes agents stick to FEM routes
 */
public class FEMEvacuationLinkRoutingCounter implements LinkLeaveEventHandler{
	private static final Logger log = Logger.getLogger( FEMEvacuationLinkRoutingCounter.class ) ;

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

	private long totalLinkEnterEventCount = 0, badLinkEnterEventCount = 0, evacLinkFollowedByNonEvacLinkCount = 0 ;

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		final Link link = network.getLinks().get(event.getLinkId());
		Gbl.assertNotNull(link);
		Boolean isEvacLink = (Boolean) link.getAttributes().getAttribute(NetworkConverter.EVACUATION_LINK);
		
//		log.info( "vehId=" + event.getVehicleId() + "; linkId=" + event.getLinkId() + "; isEvacLink=" + isEvacLink ) ;

		if(isEvacLink) {
			totalLinkEnterEventCount++;
			map.put( event.getVehicleId(), event.getLinkId() );
		} else {
			log.info( "vehId=" + event.getVehicleId() + "; linkId=" + event.getLinkId() + "; isEvacLink=" + isEvacLink );
			badLinkEnterEventCount++;
			Id<Link> prevLinkId = map.get(event.getVehicleId());
			if ( prevLinkId!=null ) {
//				Assert.fail("evacLink=" + prevLinkId + " followed by nonEvacLink=" + event.getLinkId() +
//				" by vehicle=" + event.getVehicleId() );
				evacLinkFollowedByNonEvacLinkCount ++ ;
			}
		}
		totalLinkEnterEventCount++;
	}
	
	public long getEvacLinkFollowedByNonEvacLinkCount() {
		return evacLinkFollowedByNonEvacLinkCount;
	}
}
