package femproto.prepare.evacuationscheduling;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import femproto.globals.FEMAttributes;
import femproto.run.FEMUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * When an optimization run has been performed, use this to construct an {@link EvacuationSchedule} for review and certification run.
 */
public final class EvacuationScheduleFromExperiencedPlans {
	private Map<Id<Person>, String> pax2SubSectors;
	private TreeMap<ODFlowCounter,ODFlowCounter> flowCounters = new TreeMap<>();

	/**
	 * Run this in an output folder
	 * @param args
	 */
	public static void main(String[] args) throws CsvRequiredFieldEmptyException, IOException, CsvDataTypeMismatchException {
		String networkFile = "output_network.xml.gz";
		String populationFile = "output_plans.xml.gz";
		String experiencedPlansFile = "output_experienced_plans.xml.gz";
		String outputScheduleFile = "scheduleFromExperiencedPlans.csv";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationFile);

		EvacuationScheduleFromExperiencedPlans evacuationScheduleFromExperiencedPlans = new EvacuationScheduleFromExperiencedPlans(scenario.getPopulation(), network);

		scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(experiencedPlansFile);
		Map<Id<Person>,Plan> plans = new LinkedHashMap<>() ;
		scenario.getPopulation().getPersons().values().forEach( p -> plans.put( p.getId(), p.getSelectedPlan() ) );
		evacuationScheduleFromExperiencedPlans.parseExperiencedPlans(plans,network);

		EvacuationSchedule evacuationSchedule = evacuationScheduleFromExperiencedPlans.createEvacuationSchedule();
		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordComplete(outputScheduleFile);
	}
	/**
	 * This takes standard plans, not experienced plans, to get information on the subsector of each person.
	 *
	 * @param population
	 */
	public EvacuationScheduleFromExperiencedPlans(Population population, Network network) {
		pax2SubSectors = new HashMap<>(population.getPersons().size());
		for (Person person : population.getPersons().values()) {
			pax2SubSectors.put(person.getId(), FEMUtils.getSubsectorName( person ) ) ;
//			Node origin = null;
//			Node destin = null;
//			double startTime = Double.POSITIVE_INFINITY;
//			double endTime = Double.NEGATIVE_INFINITY;
//
//			for (PlanElement planElement : person.getPlans().get(0).getPlanElements()) {
//				// yyyy why get(0)?  If anything, then it should be the selected plan.  kai, sep'18
//				if (planElement instanceof Activity) {
//					Activity activity = (Activity) planElement;
//					if (origin == null && activity.getType().equals(FEMAttributes.EVACUATION_ACTIVITY)) {
//						origin = network.getLinks().get(activity.getLinkId()).getFromNode();
//						startTime = activity.getEndTime();
//					}
//					if (destin == null && activity.getType().equals(FEMAttributes.SAFE_ACTIVITY)) {
//						destin = network.getLinks().get(activity.getLinkId()).getToNode();
//						endTime = activity.getStartTime();
//					}
//				}
//			}
//			ODFlowCounter odFlowCounter = addOrCreateODFlowCounter(FEMUtils.getSubsectorName( person ), origin, destin);
			// yyyy is any of the now commented out material truly needed?  kai, sep'18
		}
	}

	public void parseExperiencedPlans( Map<Id<Person>, Plan> plans, Network network) {
		// I changed the signature from Population to Map since that is the way in which it is returned
		// by the ExperiencedPlansService.  kai, sep'18
		
		for (Plan plan : plans.values()) {
			
			Gbl.assertNotNull( plan.getPerson() );
			Gbl.assertNotNull( plan.getPerson().getId() );
			String subSector = pax2SubSectors.get(plan.getPerson().getId());
			Node origin = null;
			Node destin = null;
			double startTime = Double.POSITIVE_INFINITY;
			double endTime = Double.NEGATIVE_INFINITY;

			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					if (origin == null && activity.getType().equals(FEMAttributes.EVACUATION_ACTIVITY)) {
						origin = network.getLinks().get(activity.getLinkId()).getFromNode();
						startTime = activity.getEndTime();
					}
					if (destin == null && activity.getType().equals(FEMAttributes.SAFE_ACTIVITY)) {
						destin = network.getLinks().get(activity.getLinkId()).getToNode();
						endTime = activity.getStartTime();
					}
				}
			}

			/**
			 * add an {@link ODFlowCounter} or increment an existing one for this combination of nodes, and adjust timing.
			 */
			ODFlowCounter odFlowCounter = addOrCreateODFlowCounter(subSector, origin, destin);
			odFlowCounter.vehicles++;
			odFlowCounter.setStartTime(startTime);
			odFlowCounter.setEndTime(endTime);
		}
	}

	private ODFlowCounter addOrCreateODFlowCounter(String subSector, Node origin, Node destin) {
		ODFlowCounter flowCounter = new ODFlowCounter(subSector,origin,destin);
		ODFlowCounter outflowCounter = flowCounters.get(flowCounter);
		if(outflowCounter == null){
			flowCounters.put(flowCounter,flowCounter);
			outflowCounter = flowCounter;
		}
		if(flowCounter.destin == null)
			outflowCounter.setEndTime(200*3600);
		return outflowCounter;
	}

	public EvacuationSchedule createEvacuationSchedule() {
		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		for (ODFlowCounter flowCounter : flowCounters.values()) {
			SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(flowCounter.subSector);
			subsectorData.setEvacuationNode(flowCounter.origin);
			subsectorData.addSafeNodeAllocation(flowCounter.startTime,flowCounter.endTime,flowCounter.destin,flowCounter.vehicles);
		}

		return evacuationSchedule;
	}

	class ODFlowCounter implements Comparable<ODFlowCounter> {
		final String subSector;
		final Node origin;
		final Node destin;
		int vehicles;
		double startTime = Double.POSITIVE_INFINITY;
		double endTime = Double.NEGATIVE_INFINITY;

		ODFlowCounter(String subSector, Node origin, Node destin) {
			this.subSector = subSector;
			this.origin = origin;
			this.destin = destin;
		}

		void setStartTime(double startTime) {
			if (startTime < this.startTime)
				this.startTime = startTime;
		}

		void setEndTime(double endTime) {
			if (endTime > this.endTime)
				this.endTime = endTime;
		}

		@Override
		public int compareTo(ODFlowCounter o) {
			if (subSector.equals(o.subSector)) {
				if (origin == o.origin)
					//yoyoyo bad fix for dealing with stranded guys - need to come up with something better
					if (destin == null || destin == o.destin)
						return 0;
					else
						return destin.getId().toString().compareTo(o.destin.getId().toString());
				else
					return origin.getId().toString().compareTo(o.origin.getId().toString());
			} else {
				return subSector.compareTo(o.subSector);
			}
		}

		@Override
		public boolean equals(Object obj) {
			ODFlowCounter o = (ODFlowCounter) obj;
			return (subSector.equals(o.subSector) && origin == o.origin && destin == o.destin);
		}


	}
}
