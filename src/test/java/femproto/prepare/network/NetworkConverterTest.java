package femproto.prepare.network;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class NetworkConverterTest {
	public static final String basename2016 = "hn_net_ses_emme_2016_V12_" ;
	public static final String basename2026 = "hn_net_ses_emme_2026_V12_Scenario_2A_" ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	//@Ignore // will for time being not work since gitlab-ci does not support git-fat.
	// Could put normal file into test input directory, but it somewhat defeats the purpose.
	// Have now done exactly that.  kai, feb'18
	public void testMain2016() throws Exception {

		String dir = utils.getPackageInputDirectory() + "/2016_scenario_1A_v20180706/";

		String testOutputDir = utils.getOutputDirectory() ;

		final String nodesFilename = dir + basename2016 + "nodes.shp";
		final String linksFilename = dir + basename2016 + "links.shp";
		final String outputFilePrefix = testOutputDir + "/" + basename2016 + "network" ;

		NetworkConverter.main(new String[]{nodesFilename, linksFilename, outputFilePrefix}) ;

	}
	@Test
	public void testMain2026() throws Exception {

		String dir = utils.getPackageInputDirectory() + "/2026_scenario_2C_v20180706/";

		String testOutputDir = utils.getOutputDirectory() ;

		final String nodesFilename = dir + basename2026 + "nodes.shp";
		final String linksFilename = dir + basename2026 + "links.shp";
		final String outputFilePrefix = testOutputDir + "/" + basename2026 + "network" ;

		NetworkConverter.main(new String[]{nodesFilename, linksFilename, outputFilePrefix}) ;

	}
}
