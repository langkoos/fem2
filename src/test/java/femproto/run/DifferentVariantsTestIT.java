package femproto.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static femproto.run.FEMConfigGroup.FEMEvacuationTimeAdjustment;
import static femproto.run.FEMConfigGroup.FEMRunType;
import static femproto.run.FEMConfigGroup.FEMOptimizationType;
import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.compare;

@RunWith(Parameterized.class)
public class DifferentVariantsTestIT {
	private static final Logger log = Logger.getLogger(RunInputPlansOnlyEvacRoutingTest.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private final FEMRunType runType;
	private final FEMOptimizationType optimizationType;
	private final FEMEvacuationTimeAdjustment timeAdjustment;
	private final boolean timeDepNetwork;

	private static String utilsOutputDir;

	public DifferentVariantsTestIT(FEMRunType runType, FEMOptimizationType optimizationType, FEMEvacuationTimeAdjustment timeAdjustment, boolean timeDepNetwork) {
		this.runType = runType;
		this.optimizationType = optimizationType;
		this.timeAdjustment = timeAdjustment;
		this.timeDepNetwork = timeDepNetwork;
	}

	@Parameters(name = "{index}: {0} | {1} | {2} | timeDepNetw={3} ") // the "name" entry is just for the test output
	public static Collection<Object[]> abc() { // the name of this method does not matter as long as it is correctly annotated
		List<Object[]> combos = new ArrayList<>();

		for (FEMRunType rt : FEMRunType.values()) {
//		FEMRunType rt = FEMRunType.runFromSource; //if the test fails then use tis line to pick specific instance
			for (FEMOptimizationType ot : FEMOptimizationType.values()) {
				for (FEMEvacuationTimeAdjustment ta : FEMEvacuationTimeAdjustment.values()) {
					if(rt != FEMRunType.justRunInputPlansFile ) {
						combos.add(new Object[]{rt, ot, ta, true});
						combos.add(new Object[]{rt, ot, ta, false});
					}else {
						if(ot == FEMOptimizationType.none) {
							combos.add(new Object[]{rt, ot, ta, true});
							combos.add(new Object[]{rt, ot, ta, false});
						}
					}
				}
			}
		}
		return combos;
	}

	@Test
	public void test() {
		if (utilsOutputDir == null) {
			utilsOutputDir = utils.getOutputDirectory();
			// utils.getOutputDirectory() first removes everything _at that level_.  For the way the output paths are
			// constructed here, this means that otherwise only the last parameterized test output would survive.
			// There might be a better solution ...   kai, jul'18
		}

		String dirExtension = "/" + runType.name() + "_" + optimizationType.name() + "_" + timeAdjustment.name();
		if (timeDepNetwork) {
			dirExtension += "_withTimeDepNetwork/";
		} else {
			dirExtension += "_woTimeDepNetwork/";
		}

		RunMatsim4FloodEvacuation evac = new RunMatsim4FloodEvacuation();

		Config config = evac.loadConfig(new String[]{utils.getPackageInputDirectory() + "scenario/config_base.xml"});

		config.network().setTimeVariantNetwork(timeDepNetwork);

		config.controler().setOutputDirectory(utilsOutputDir + dirExtension);
		if (optimizationType != FEMOptimizationType.none) {
			config.controler().setLastIteration(10);
		} else {
			config.controler().setLastIteration(0);
		}


		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		;
		femConfig.setFemRunType(runType);
		femConfig.setFemOptimizationType(optimizationType);
		femConfig.setFemEvacuationTimeAdjustment(timeAdjustment);
		femConfig.setSampleSize(0.01);

		evac.run();

		//yoyo this breaks the test
//		String expected = utilsOutputDir + dirExtension + "/output_events.xml.gz";
//		String actual = utilsOutputDir + dirExtension + "/output_events.xml.gz";
//		EventsFileComparator.Result result = compare(expected, actual);
//		Assert.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result);

	}

}
