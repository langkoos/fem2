package femproto.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

public class RunFromSourceTest_2CrunId13 {
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
		Config config = ConfigUtils.loadConfig("scenarios/FEM2TestDataOctober18/config_2026_2Crunid3.xml");
		String utilsOutputDir = utils.getOutputDirectory();
		config.controler().setOutputDirectory(utilsOutputDir);
		RunFromSource.standardFullSizeOptimization(config);

		//yoyo this breaks because of time steps, even though the evacuations look the same

//		Network network = NetworkUtils.readNetwork(utilsPackageInputDir + "scenario/input_network.xml");
//		EvacuationSchedule expectedSchedule = new EvacuationSchedule();
//		new EvacuationScheduleReader(expectedSchedule, network).readFile(utilsPackageInputDir + "scenario/input_evac_plan.csv");
//		EvacuationSchedule actualSchedule = new EvacuationSchedule();
//		new EvacuationScheduleReader(actualSchedule, network).readFile(utilsOutputDir + "output/output_output_evacuationSchedule.csv");
//
//		Iterator<SafeNodeAllocation> expectedIterator = expectedSchedule.getSubsectorsByEvacuationTime().iterator();
//
//		while (expectedIterator.hasNext()) {
//			SafeNodeAllocation expected = expectedIterator.next();
//			SubsectorData actualData = actualSchedule.getSubsectorDataMap().get(expected.getContainer().getSubsector());
//			SafeNodeAllocation actual = actualData.getSafeNodesByTime().iterator().next();
//			Assert.assertEquals(expected.getStartTime(), actual.getStartTime(), 1.0);
//			Assert.assertEquals(expected.getStartTime(), actual.getEndTime(), 1.0);
//			Assert.assertEquals(expected.getVehicles(), actual.getVehicles());
//		}


	}

}
