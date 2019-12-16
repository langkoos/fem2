package femproto.utils;

import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleReader;
import femproto.prepare.evacuationscheduling.SafeNodeAllocation;
import femproto.prepare.evacuationscheduling.SubsectorData;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import sun.jvm.hotspot.utilities.Assert;


import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.Math.abs;

public class CompareExpectedVsActual {
	public static void main(String[] args) {
		Network network = NetworkUtils.readNetwork("output/input_network.xml");
		EvacuationSchedule expectedSchedule = new EvacuationSchedule();
		new EvacuationScheduleReader(expectedSchedule, network).readFile("test/output_output_evacuationSchedule.csv");
		expectedSchedule.createSchedule();
		EvacuationSchedule actualSchedule = new EvacuationSchedule();
		new EvacuationScheduleReader(actualSchedule, network).readFile("output/output/output_output_evacuationSchedule.csv");
		actualSchedule.createSchedule();

		TreeSet<SafeNodeAllocation> expectedSubsByEvacTime = expectedSchedule.getSubsectorsByEvacuationTime();
		TreeSet<SafeNodeAllocation> actualSubsByEvacTime = actualSchedule.getSubsectorsByEvacuationTime();

		for (int i = 0; i < expectedSubsByEvacTime.size(); i++) {
			SafeNodeAllocation expected = expectedSubsByEvacTime.first();
			SafeNodeAllocation actual = actualSubsByEvacTime.first();
			if (abs(expected.getStartTime() - actual.getStartTime()) > 1.0)
				throw new RuntimeException(String.format("evac start time %f different from reference %f for subsector %s", actual.getStartTime(), expected.getStartTime(), actual.getContainer().getSubsector()));
			if (abs(expected.getEndTime() - actual.getEndTime()) > 1.0)
				throw new RuntimeException(String.format("evac end time %f different from reference %f for subsector %s", actual.getEndTime(), expected.getEndTime(), actual.getContainer().getSubsector()));
			if (abs(expected.getVehicles() - actual.getVehicles()) > 1.0)
				throw new RuntimeException(String.format("evac demand %d different from reference %d for subsector %s", actual.getVehicles(), expected.getVehicles(), actual.getContainer().getSubsector()));
			expectedSubsByEvacTime.remove(expected);
			actualSubsByEvacTime.remove(actual);
		}
		System.out.println("ALL SUBSECTORS HAVE SAME DEMAND, START TIME AND END TIME AS THE REFERENCE CASE.");
	}
}
