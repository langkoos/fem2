package femproto.prepare.evacuationscheduling;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import femproto.prepare.parsers.EvacuationToSafeNodeParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import femproto.run.FEMUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

import static femproto.prepare.network.NetworkConverter.EVACUATION_LINK;
import static org.matsim.contrib.analysis.vsp.qgis.RuleBasedRenderer.log;

/**
 * Once a schedule has been determined from various scheduling strategies, it needs to be translated to population departures.
 * <p>
 * This will only work for a certification run. Each agent gets only one plan.
 */
public final class EvacuationScheduleToPopulationDepartures {
	private Scenario scenario;
	private EvacuationSchedule evacuationSchedule;


	public EvacuationScheduleToPopulationDepartures(Scenario scenario, EvacuationSchedule evacuationSchedule) {
		this.scenario = scenario;
		this.evacuationSchedule = evacuationSchedule;
	}

	public static void main(String[] args) throws CsvRequiredFieldEmptyException, IOException, CsvDataTypeMismatchException {
		String inputshapefile = args[0];
		String networkFile = args[1];
		String inputEvactoSafeNode = args[2];

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		SubsectorShapeFileParser subSectorsToPopulation = new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork());
		subSectorsToPopulation.readSubSectorsShapeFile(inputshapefile);

		EvacuationToSafeNodeParser parser = new EvacuationToSafeNodeParser(scenario.getNetwork(), evacuationSchedule);
		parser.readEvacAndSafeNodes(inputEvactoSafeNode);

		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordNoVehicles("inputSchedule.csv");

		new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule).writePopulation("pop.xml.gz");

		new EvacuationScheduleToPopulationDepartures(scenario, evacuationSchedule).writeAttributes("pop_attribs.txt.gz");
	}

	public void createPlans() {
		// assume that the evacuation schedule has created all relevant subsector data

		PopulationFactory pf = scenario.getPopulation().getFactory();
		long personCnt = 0L;

		for (SubsectorData subsectorData : evacuationSchedule.getSubsectorDataMap().values()) {

			subsectorData.completeAllocations();

			for (SafeNodeAllocation safeNodeAllocation : subsectorData.getSafeNodesByTime()) {

				// find a qualifying outgoing link
				Node evacuationNode = safeNodeAllocation.getContainer().getEvacuationNode();
				Link startLink = null;
				for (Link link : evacuationNode.getOutLinks().values()) {
//					if (link.getAllowedModes().contains(TransportMode.car) && (boolean) link.getAttributes().getAttribute(EVACUATION_LINK)) {
					if (link.getId().toString().equals(evacuationNode.getId().toString())) {
						startLink = link;
					}
				}
//				if (startLink == null) {
//					String msg = "There seems to be no outgoing car mode EVAC link for EVAC node " +
//							evacuationNode + ". Defaulting to the highest capacity car link.";
//					log.warn(msg);
//					double maxCap = Double.NEGATIVE_INFINITY;
//					for (Link link : evacuationNode.getOutLinks().values()) {
//						if (link.getAllowedModes().contains(TransportMode.car) && link.getCapacity() > maxCap) {
//							maxCap = link.getCapacity();
//							startLink = link;
//						}
//					}
//				}
				if (startLink == null) {
					String msg = String.format("The evacuation node %s for subsector %s has no dummy link associated with it." +
							"Run the network converter.", evacuationNode.getId().toString(), subsectorData.getSubsector());
					log.error(msg);
					throw new RuntimeException(msg);
				}

				Node safeNode = safeNodeAllocation.getNode();
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


				int totalVehicles = safeNodeAllocation.getVehicles();
				int safeNodeAllocationPaxCounter = 0;
				for (int i = 0; i < safeNodeAllocation.getVehicles(); i++) {
					Person person = pf.createPerson(Id.createPersonId(personCnt++));
					FEMUtils.setSubsectorName(subsectorData.getSubsector(), person);
					Plan plan = pf.createPlan();

					Activity startAct = pf.createActivityFromLinkId("evac", startLink.getId());
					startAct.setEndTime(safeNodeAllocation.getStartTime() + safeNodeAllocationPaxCounter++ * (3600 / subsectorData.getLookAheadTime()));
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
	}

	public void createPlansForAllSafeNodes() {
		// assume that the evacuation schedule has created all relevant subsector data

		PopulationFactory pf = scenario.getPopulation().getFactory();
		long personCnt = 0L;

		for (SubsectorData subsectorData : evacuationSchedule.getSubsectorDataMap().values()) {

			if (subsectorData.getVehicleCount() <= 0){
				log.warn(String.format("Subsector %s has no vehicles associated with it.",subsectorData.getSubsector()));
				continue;
			}
			if (subsectorData.getSafeNodesByTime().size() == 0 ){
				String message = String.format("Subsector %s has no safe nodes associated with it.", subsectorData.getSubsector());
				log.error(message);
				throw new RuntimeException(message);
			}
			double startTime = subsectorData.getSafeNodesByTime().iterator().next().getStartTime();

			Set<Node> safeNodesByDecreasingPriority = subsectorData.getSafeNodesByDecreasingPriority();
			for (int i = 0; i < subsectorData.getVehicleCount(); i++) {
				Person person = pf.createPerson(Id.createPersonId(personCnt++));
				FEMUtils.setSubsectorName(subsectorData.getSubsector(), person);
				for (Node safeNode : safeNodesByDecreasingPriority) {


					// find a qualifying outgoing link
					Node evacuationNode = subsectorData.getEvacuationNode();
					Link startLink = null;
					for (Link link : evacuationNode.getOutLinks().values()) {
//					if (link.getAllowedModes().contains(TransportMode.car) && (boolean) link.getAttributes().getAttribute(EVACUATION_LINK)) {
						if (link.getId().toString().equals(evacuationNode.getId().toString())) {
							startLink = link;
						}
					}
					if (startLink == null) {
						String msg = String.format("The evacuation node %s for subsector %s has no dummy link associated with it." +
								"Run the network converter.", evacuationNode.getId().toString(), subsectorData.getSubsector());
						log.error(msg);
						throw new RuntimeException(msg);
					}

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


					Plan plan = pf.createPlan();

					Activity startAct = pf.createActivityFromLinkId(FEMUtils.getGlobalConfig().getEvacuationActivity(), startLink.getId());
					startAct.setEndTime(startTime + i * (3600 / subsectorData.getLookAheadTime()));
					plan.addActivity(startAct);

					Leg evacLeg = pf.createLeg(TransportMode.car);
					plan.addLeg(evacLeg);

					Activity safe = pf.createActivityFromLinkId(FEMUtils.getGlobalConfig().getSafeActivity(), safeLink.getId());
					plan.addActivity(safe);


					person.addPlan(plan);
				}
				scenario.getPopulation().addPerson(person);

			}
		}
	}

	public void writePopulation(String fileName) {
		new PopulationWriter(scenario.getPopulation()).write(fileName);
	}


	private void writeAttributes(String fileName) {
		// yoyo writing out attributes to a separate file for diagnostics
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("id\tsubsector\n");
			for (Person person : scenario.getPopulation().getPersons().values()) {
				writer.write(person.getId() + "\t" + person.getAttributes().getAttribute("SUBSECTOR").toString() + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
