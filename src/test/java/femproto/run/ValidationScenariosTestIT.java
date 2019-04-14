package femproto.run;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static femproto.run.FEMConfigGroup.*;

//yoyo  this test to compare result against version 1.2
@RunWith(Parameterized.class)
public class ValidationScenariosTestIT {
	private static final Logger log = Logger.getLogger(RunInputPlansOnlyEvacRoutingTest.class);
	private final Config config;
	private final int iters;
	private final String year;
	private final int runId;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	static final Map<Integer, String> runIdToFloodEvent = new HashMap<>();

	static {
		runIdToFloodEvent.put(3, "d00229_H_TS");
		runIdToFloodEvent.put(8, "d00938_H_TS");
		runIdToFloodEvent.put(13, "d01889_H_TS");
	}

	private final FEMOptimizationType optimizationType;

	private String utilsOutputDir;

	public ValidationScenariosTestIT(FEMOptimizationType optimizationType, String year, int iters, int runId) {
		this.optimizationType = optimizationType;
		this.config = ConfigUtils.loadConfig("scenarios/FEM2TestDataOctober18/config_" + year + ".xml");
		this.iters = iters;
		this.year = year;
		this.runId = runId;
	}

	@Parameters(name = "{index}: {0} | {1} | iters={2} | runId={3} ") // the "name" entry is just for the test output
	public static Collection<Object[]> abc() { // the name of this method does not matter as long as it is correctly annotated
		List<Object[]> combos = new ArrayList<>();
		int[] maxIters = new int[]{20, 40, 60};
		String[] years = new String[]{"2016", "2026"};


		for (FEMOptimizationType ot : FEMOptimizationType.values()) {
			for (String year : years) {
				for (Integer runId : runIdToFloodEvent.keySet()) {
					for (int maxIter : maxIters) {
						if (ot == FEMOptimizationType.optimizeLikeNICTA || ot == FEMOptimizationType.userEquilibriumDecongestion) {
							combos.add(new Object[]{ot, year, maxIter, runId});
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
		}

		String dirExtension = "_" + optimizationType.name() + "_" + year + "_" + runId + "_" + iters;


		final String outputDirectory = utilsOutputDir.substring(0, utilsOutputDir.length() - 1) + dirExtension;
		log.warn(outputDirectory);

		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(iters);


		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		femConfig.setFemOptimizationType(optimizationType);
		femConfig.setHydrographData("wma-flood-events/Exg/" + runIdToFloodEvent.get(runId) + ".csv");
		femConfig.setSampleSize(1.0);

		RunFromSource.standardFullSizeOptimization(config);
	}

}
