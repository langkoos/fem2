package femproto.prepare.evacuationscheduling;

import femproto.prepare.parsers.HydrographPoint;
import org.matsim.api.core.v01.population.*;

import java.util.HashSet;
import java.util.Set;
//todo write this class as well as its instrumentation
public final class EvacuationScheduleFromHydrographData {

	@Deprecated
	public void triggerPopulationDepartures(EvacuationSchedule schedule,  double rate, double staggerTime) {
//		double lastDeparture = minTime;
//
//		for (HydrographPoint hydrographPoint : hydrographPointMap.values()) {
//			if (hydrographPoint.getSubSector() == null || hydrographPoint.getFloodTime() < 0) {
//				continue;
//			}
//			hydroSubsectors.add(hydrographPoint.getSubSector());
//			System.out.println(hydrographPoint.getSubSector());
//			System.out.println("flooding subsector "+hydrographPoint.getSubSector()+" ends departing at " +  (hydrographPoint.getFloodTime() - timeBuffer + paxCounter * rate));
//			lastDeparture = Math.max(lastDeparture, hydrographPoint.getFloodTime() - timeBuffer + paxCounter * rate);
//			minTime = Math.min(minTime, hydrographPoint.getFloodTime() - timeBuffer - paxCounter * rate);
//		}
////		subsectors.removeAll(hydroSubsectors);
//		//set the rest to depart at the latest departure from the high priority subsectors plus staggerTime for each zone
//		lastDeparture = minTime;
//		for (String sub : subsectors) {
//			paxCounter = 0;
//			for (Person person : population.getPersons().values()) {
//				paxCounter += 1;
//				String subsector = (String) person.getAttributes().getAttribute("SUBSECTOR");
//				if (subsector.equals(sub)) {
//					for (Plan plan : person.getPlans()) {
//						Activity departure = (Activity) plan.getPlanElements().get(0);
//						departure.setEndTime(lastDeparture + paxCounter * rate);
//					}
//				}
//			}
//			lastDeparture = lastDeparture + staggerTime ;
//			System.out.println("non-flooding subsector "+sub+" starts departing at " +  lastDeparture);
//		}
//		for (Person person : population.getPersons().values()) {
//			for (Plan plan : person.getPlans()) {
//				Activity departure = (Activity) plan.getPlanElements().get(0);
//				departure.setEndTime(departure.getEndTime() - minTime);
//			}
//		}
//		System.out.println("mintime is "+ minTime);
//		System.out.println("Expected EVAC TIME IS "+ ((lastDeparture - minTime)/3600));
//		new PopulationWriter(population).write(modifiedPopulationOutputFile);
	}
}
