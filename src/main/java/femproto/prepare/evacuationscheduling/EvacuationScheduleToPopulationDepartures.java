package femproto.prepare.evacuationscheduling;

import femproto.globals.FEMAttributes;
import femproto.prepare.evacuationdata.SafeNodeAllocation;
import femproto.prepare.evacuationdata.SubsectorData;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;

import java.util.List;

import static femproto.prepare.network.NetworkConverter.EVACUATION_LINK;
import static org.matsim.contrib.analysis.vsp.qgis.RuleBasedRenderer.log;

/**
 * Once a schedule has been determined from various scheduling strategies, it needs to be translated to population departures.
 *
 * This will only work for a certification run. Each agent gets only one plan.
 */
public class EvacuationScheduleToPopulationDepartures {
	private Scenario scenario;
	private EvacuationSchedule evacuationSchedule;


	public EvacuationScheduleToPopulationDepartures(Scenario scenario, EvacuationSchedule evacuationSchedule) {
		this.scenario = scenario;
		this.evacuationSchedule = evacuationSchedule;
	}

	public void writePopulation(String fileName) {
		// assume that the evacuation schedule has created all relevant subsector data

		PopulationFactory pf = scenario.getPopulation().getFactory();
		long personCnt = 0L;

		for (SubsectorData subsectorData : evacuationSchedule.getSubsectorsBySubsectorName().values()) {

			subsectorData.completeAllocations();

			for (SafeNodeAllocation safeNodeAllocation : subsectorData.getSafeNodesByTime()) {

				// find a qualifying outgoing link
				Node evacuationNode = safeNodeAllocation.container.getEvacuationNode();
				Link startLink = null;
				for (Link link : evacuationNode.getOutLinks().values()) {
					if (link.getAllowedModes().contains(TransportMode.car) && (boolean) link.getAttributes().getAttribute(EVACUATION_LINK)) {
						startLink = link;
					}
				}
				if (startLink == null) {
					String msg = "There seems to be no outgoing car mode EVAC link for EVAC node " + evacuationNode + ". Defaulting to the highest capacity car link.";
					log.warn(msg);
					double maxCap = Double.NEGATIVE_INFINITY;
					for (Link link : evacuationNode.getOutLinks().values()) {
						if (link.getAllowedModes().contains(TransportMode.car) && link.getCapacity() > maxCap) {
							maxCap = link.getCapacity();
							startLink = link;
						}
					}
				}

				Node safeNode = safeNodeAllocation.node;
				Link safeLink = null;
				for (Link link : safeNode.getInLinks().values()) {
					if (link.getAllowedModes().contains(TransportMode.car) && (boolean) link.getAttributes().getAttribute(EVACUATION_LINK)) {
						safeLink = link;
					}
				}
				if (safeLink == null) {
					String msg = "There seems to be no incoming car mode EVAC link for SAFE node " + evacuationNode + ". Defaulting to the highest capacity car link.";
					log.warn(msg);
					double maxCap = Double.NEGATIVE_INFINITY;
					for (Link link : safeNode.getInLinks().values()) {
						if (link.getAllowedModes().contains(TransportMode.car) && link.getCapacity() > maxCap) {
							maxCap = link.getCapacity();
							safeLink = link;
						}
					}
				}


				int totalVehicles = safeNodeAllocation.vehicles;
				int safeNodeAllocationPaxCounter = 0;
				for (int i = 0; i < safeNodeAllocation.vehicles; i++) {
					Person person = pf.createPerson(Id.createPersonId(personCnt++));
					person.getAttributes().putAttribute("SUBSECTOR", subsectorData.getSubsector());
					Plan plan = pf.createPlan();

					Activity startAct = pf.createActivityFromLinkId("evac", startLink.getId());
					startAct.setEndTime(safeNodeAllocation.getStartTime() +  safeNodeAllocationPaxCounter++ * (3600 / FEMAttributes.EVAC_FLOWRATE));
					plan.addActivity(startAct);

					Leg evacLeg = pf.createLeg(TransportMode.car);
					plan.addLeg(evacLeg);

					Activity safe = pf.createActivityFromLinkId("safe", safeLink.getId());
					plan.addActivity(safe);


					person.addPlan(plan);
					scenario.getPopulation().addPerson(person);
				}

			}
		}

		new PopulationWriter(scenario.getPopulation()).write(fileName);
	}
}
