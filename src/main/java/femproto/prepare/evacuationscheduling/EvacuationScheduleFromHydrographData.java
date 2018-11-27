package femproto.prepare.evacuationscheduling;

import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.HydrographPoint;
import femproto.run.FEMPreferEmergencyLinksTravelDisutility;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Time;

import java.util.HashSet;
import java.util.Set;

//todo write this class as well as its instrumentation
public final class EvacuationScheduleFromHydrographData {


	private final Network network;
	private final EvacuationSchedule evacuationSchedule;
	private final HydrographParser hydrographParser;
	private final FEMPathCalculator femPathCalculator;
	Logger log = Logger.getLogger(EvacuationScheduleFromHydrographData.class);

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
	 * If the link gets flooded, departures are scheduled to start at look ahead time specced in the config (see {@link femproto.globals.FEMGlobalConfig})
	 * before flooding starts.
	 */
	public void createEvacuationSchedule() {
		log.info("Generating evacuation plan from hydrograph data...");
		double lastPriorityEvacuationStartTime = 0;
		Set<String> prioritySubsectors = new HashSet<>();
		log.warn("About to check the shortest paths between evacuation nodes and priority safe nodes for flooding times.");
		log.warn("This process will hang if the network is disconnected between these nodes.");

		double latestFloodTime = Double.NEGATIVE_INFINITY;
		for (HydrographPoint hydrographPoint : hydrographParser.getConsolidatedHydrographPointMap().values()) {
			latestFloodTime = Math.max(latestFloodTime, hydrographPoint.getFloodTime());
		}

		for (SubsectorData subsectorData : evacuationSchedule.getSubsectorDataMap().values()) {

			subsectorData.clearSafeNodesByTime();

			double floodTime = Double.NEGATIVE_INFINITY;
			Node assignedSafeNode = subsectorData.getSafeNodesByDecreasingPriority().iterator().next();
			for (Node prioritySafeNode : subsectorData.getSafeNodesByDecreasingPriority()) {
				String message = "Checking the path for subsector %s from evac node %s to safe node %s";
				double safenodeFloodTime = Double.POSITIVE_INFINITY;
				log.info(String.format(message, subsectorData.getSubsector(), subsectorData.getEvacuationNode().getId().toString(), prioritySafeNode.getId().toString()));
				LeastCostPathCalculator.Path path;
				path = femPathCalculator.getPath(subsectorData.getEvacuationNode().getId(),
						prioritySafeNode.getId(), latestFloodTime + 1);
				LeastCostPathCalculator.Path freePath = femPathCalculator.getPath(subsectorData.getEvacuationNode().getId(),
						prioritySafeNode.getId(), 0);
				if (path.travelCost > 3 * freePath.travelCost) {
					safenodeFloodTime = latestFloodTime;
					double lowerBoundFloodTime = 0.;
					double potentialTime = safenodeFloodTime / 2;
					boolean converged = false;
					while (!converged) {
						path = femPathCalculator.getPath(subsectorData.getEvacuationNode().getId(),
								prioritySafeNode.getId(), potentialTime);
						if (path.travelCost > 3 * freePath.travelCost) {
							safenodeFloodTime = potentialTime;
						} else {
							lowerBoundFloodTime = potentialTime;
						}
						potentialTime = lowerBoundFloodTime + (safenodeFloodTime - lowerBoundFloodTime) / 2;
						if (Math.abs(potentialTime - safenodeFloodTime) < 2) {
							converged = true;
							safenodeFloodTime = Math.floor(lowerBoundFloodTime);
							path = femPathCalculator.getPath(subsectorData.getEvacuationNode().getId(),
									prioritySafeNode.getId(), safenodeFloodTime);
						}
					}
				}
				if (safenodeFloodTime < Double.POSITIVE_INFINITY) {
					log.info(String.format("Subsector %s last possible path to safe node %s gets flooded at %05.0f seconds = %05.0f mins = %s", subsectorData.getSubsector(), prioritySafeNode.getId().toString(), safenodeFloodTime, safenodeFloodTime / 60, Time.writeTime(safenodeFloodTime)));
					// according to the nicta doc they find the latest possible evac time
					if (safenodeFloodTime > floodTime) {
						assignedSafeNode = prioritySafeNode;
						floodTime = safenodeFloodTime;
					}
				} else {
					log.info(String.format("Subsector %s has a path to safe node %s that never gets flooded.", subsectorData.getSubsector(), prioritySafeNode.getId().toString()));
					floodTime = Double.POSITIVE_INFINITY;
					break;
				}
			}

			// yoyo subsector flooding trumps path flooding
			HydrographPoint hydrographPoint = hydrographParser.getConsolidatedHydrographPointMap().get(subsectorData.getSubsector());
			if (hydrographPoint != null && hydrographPoint.getFloodTime() > 0) {
				log.info(String.format("Subsector %s has raising road access flooding at %05.0f seconds = %05.0f mins = %s", subsectorData.getSubsector(), hydrographPoint.getFloodTime(), hydrographPoint.getFloodTime() / 60, Time.writeTime(hydrographPoint.getFloodTime())));
				floodTime = Math.min(floodTime, hydrographPoint.getFloodTime());
			}
//			for (Link link : path.links) {
//				HydrographPoint hydrographPoint = hydrographParser.getConsolidatedHydrographPointMap().get(link.getId().toString());
//				if (hydrographPoint != null && hydrographPoint.getFloodTime() > 0)
//					floodTime = Math.min(floodTime, hydrographPoint.getFloodTime());
//
//			}

			if (floodTime < Double.POSITIVE_INFINITY) {
				double evacTime = floodTime - subsectorData.getLookAheadTime();
				subsectorData.addSafeNodeAllocation(evacTime, assignedSafeNode);
				log.info(String.format("Subsector %s gets trapped at %05.0f = %s, will evacuate %05.2f hours earlier at %05.0f seconds = %05.0f mins = %s", subsectorData.getSubsector(), floodTime, Time.writeTime(floodTime), subsectorData.getLookAheadTime() / 3600, evacTime, evacTime / 60, Time.writeTime(evacTime)));
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
		log.info("DONE Generating evacuation plan from hydrograph data.");
	}

}
