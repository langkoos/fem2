package femproto.network;

import femproto.network.NetworkConverter;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class NetworkConverterTest {
	public static final String basename = "hn_net_ses_emme_2016_V12_" ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	//@Ignore // will for time being not work since gitlab-ci does not support git-fat.
	// Could put normal file into test input directory, but it somewhat defeats the purpose.
	// Have now done exactly that.  kai, feb'18
	public void testMain() throws Exception {

		String dir = utils.getPackageInputDirectory() ;

		String testOutputDir = utils.getOutputDirectory() ;

		final String nodesFilename = dir + basename + "nodes.shp";
		final String linksFilename = dir + basename + "links.shp";
		final String outputFilePrefix = testOutputDir + "/" + basename + "network" ;

		NetworkConverter.main(new String[]{nodesFilename, linksFilename, outputFilePrefix}) ;

	}
}