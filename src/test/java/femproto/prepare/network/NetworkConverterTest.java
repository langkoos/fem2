package femproto.prepare.network;

import femproto.globals.FEMGlobalConfig;
import femproto.run.FEMUtils;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class NetworkConverterTest {
	public static final String basename2016 = "hn_net_ses_emme_2016_V12_" ;
	public static final String basename2026 = "hn_net_ses_emme_2026_V12_Scenario_2A_" ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	{
		if (FEMUtils.getGlobalConfig() == null)
			FEMUtils.setGlobalConfig(FEMGlobalConfig.getDefaultGlobalConfig());
	}
	@Test
	//@Ignore // will for time being not work since gitlab-ci does not support git-fat.
	// Could put normal file into test input directory, but it somewhat defeats the purpose.
	// Have now done exactly that.  kai, feb'18
	public void test() throws Exception {

		String testOutputDir = utils.getOutputDirectory() ;

		final String nodesFilename = "scenarios/FEM2TestDataOctober18/2016/FEM2_TEST_Nodes_2016/FEM2_TEST_Nodes_2016.shp";
		final String linksFilename = "scenarios/FEM2TestDataOctober18/2016/FEM2__TEST_Links_Scenrio1A_2016/FEM2__TEST_Links_Scenrio1A_2016.shp";
		final String outputFilePrefix = testOutputDir + "/" + basename2016 + "network" ;

		NetworkConverter.main(new String[]{nodesFilename, linksFilename, outputFilePrefix}) ;

	}

}
