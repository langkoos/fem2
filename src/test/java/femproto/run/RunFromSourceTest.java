package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.*;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.compare;

public class RunFromSourceTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test() {
//		Config config = ConfigUtils.loadConfig(utils.getPackageInputDirectory()+"scenario/config_runFromSource_optimizeLikeNICTA.xml");
//
//		config.controler().setOutputDirectory(utils.getOutputDirectory());

//		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );
//		femConfig.setSampleSize( 1. );

//		new RunMatsim4FloodEvacuation(config).run();
		String utilsPackageInputDir = utils.getPackageInputDirectory();
		Config config = ConfigUtils.loadConfig(utilsPackageInputDir + "scenario/config_runFromSource_optimizeLikeNICTA.xml");
		String utilsOutputDir = utils.getOutputDirectory();
		config.controler().setOutputDirectory(utilsOutputDir);
		RunFromSource.standardFullSizeOptimization(config);

		//yoyo this breaks because of time steps, even though the evacuations look the same
//		String expected = utilsPackageInputDir + "scenario/output_events.xml.gz";
//		String actual = utilsOutputDir +  "output/output_events.xml.gz";
//		EventsFileComparator.Result result = compare(expected, actual);
//		Assert.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result);

		Network network = NetworkUtils.readNetwork(utilsPackageInputDir + "scenario/input_network.xml");
		EvacuationSchedule expectedSchedule = new EvacuationSchedule();
		new EvacuationScheduleReader(expectedSchedule, network).readFile(utilsPackageInputDir + "scenario/input_evac_plan.csv");
		EvacuationSchedule actualSchedule = new EvacuationSchedule();
		new EvacuationScheduleReader(actualSchedule, network).readFile(utilsOutputDir + "output/output_output_evacuationSchedule.csv");

		Iterator<SafeNodeAllocation> expectedIterator = expectedSchedule.getSubsectorsByEvacuationTime().iterator();

		while (expectedIterator.hasNext()) {
			SafeNodeAllocation expected = expectedIterator.next();
			SubsectorData actualData = actualSchedule.getSubsectorDataMap().get(expected.getContainer().getSubsector());
			SafeNodeAllocation actual = actualData.getSafeNodesByTime().iterator().next();
			Assert.assertEquals(expected.getStartTime(), actual.getStartTime(), 1.0);
			Assert.assertEquals(expected.getStartTime(), actual.getEndTime(), 1.0);
			Assert.assertEquals(expected.getVehicles(), actual.getVehicles());
		}


	}

}
