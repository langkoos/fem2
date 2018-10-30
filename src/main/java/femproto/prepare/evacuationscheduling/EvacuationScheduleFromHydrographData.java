package femproto.prepare.evacuationscheduling;

import com.google.inject.Inject;
import femproto.globals.FEMGlobalConfig;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.HydrographPoint;
import femproto.run.FEMPreferEmergencyLinksTravelDisutility;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.HashSet;
import java.util.Set;

//todo write this class as well as its instrumentation
public final class EvacuationScheduleFromHydrographData {
	@Inject
	FEMGlobalConfig globalConfig;

	private final Network network;
	private final EvacuationSchedule evacuationSchedule;
	private final HydrographParser hydrographParser;
	private final FEMPathCalculator femPathCalculator;

	public EvacuationScheduleFromHydrographData(Network network, EvacuationSchedule evacuationSchedule, HydrographParser hydrographParser) {
		this.network = network;
		this.evacuationSchedule = evacuationSchedule;
		this.hydrographParser = hydrographParser;
		femPathCalculator = new FEMPathCalculator();
	}

	class FEMPathCalculator {
		TravelDisutilityFactory delegateFactory = new OnlyTimeDependentTravelDisutilityFactory();
		TravelDisutilityFactory disutilityFactory = new FEMPreferEmergencyLinksTravelDisutility.Factory(network, delegateFactory);
		FreeSpeedTravelTime freeSpeedTravelTime = new FreeSpeedTravelTime();
		// define how the travel disutility is computed:
		LeastCostPathCalculator dijkstra = new DijkstraFactory().createPathCalculator(network,
				disutilityFactory.createTravelDisutility(freeSpeedTravelTime), freeSpeedTravelTime
		);

		private LeastCostPathCalculator.Path getPath(Id<Node> fromNode, Id<Node> toNode, double time) {
			Node from = network.getNodes().get(fromNode);
			Node to = network.getNodes().get(toNode);

			return dijkstra.calcLeastCostPath(from, to, time, null, null);
		}

	}

	/**
	 * This goes through each subsector in the input schedule, then checks the path to its first safe node.
	 * If the path contains a link in the hydrograph map, then the flood time for that link is checked.
	 * If the link gets flooded, departures are scheduled to start BUFFER_TIME (see {@link FEMGlobalConfig})
	 * before flooding starts.
	 */
	public void createEvacuationSchedule() {
		double lastPriorityEvacuationStartTime = 0;
		Set<String> prioritySubsectors = new HashSet<>();
		for (SubsectorData subsectorData : evacuationSchedule.getSubsectorDataMap().values()) {

			subsectorData.clearSafeNodesByTime();

			Node prioritySafeNode = subsectorData.getSafeNodesByDecreasingPriority().iterator().next();
			LeastCostPathCalculator.Path path = femPathCalculator.getPath(subsectorData.getEvacuationNode().getId(),
					prioritySafeNode.getId(), 0);

			double floodTime = Double.POSITIVE_INFINITY;
			for (Link link : path.links) {
				HydrographPoint hydrographPoint = hydrographParser.getConsolidatedHydrographPointMap().get(link.getId().toString());
				if (hydrographPoint != null && hydrographPoint.getFloodTime() > 0)
					floodTime = Math.min(floodTime, hydrographPoint.getFloodTime());

			}

			if (floodTime < Double.POSITIVE_INFINITY) {
				subsectorData.addSafeNodeAllocation(floodTime - globalConfig.getBufferTimeBeforeFlooding() * 3600, prioritySafeNode);
				lastPriorityEvacuationStartTime = Math.max(lastPriorityEvacuationStartTime, floodTime);
				prioritySubsectors.add(subsectorData.getSubsector());
			}
		}
		// go through the other subsectors and set them to have zero departures
		for (SubsectorData subsectorData : evacuationSchedule.getSubsectorDataMap().values()) {
			if (prioritySubsectors.contains(subsectorData.getSubsector()))
				continue;
			subsectorData.setVehicleCount(0);
//			evacuationSchedule.removeSubSectorData(subsectorData);
		}
		evacuationSchedule.createSchedule();
		evacuationSchedule.completeAllocations();

	}

}
