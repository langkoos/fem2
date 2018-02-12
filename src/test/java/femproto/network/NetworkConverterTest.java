package femproto.network;

import femproto.network.NetworkConverter;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class NetworkConverterTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	//@Ignore // will for time being not work since gitlab-ci does not support git-fat.
	// Could put normal file into test input directory, but it somewhat defeats the purpose.
	public void testMain() throws Exception {
//		String dir = "data/2011_evacuation_network/shapefiles/" ;
		String dir = "data/2041 Evacuation Modelling Data/2041_evacuation_network/" ;
//		String basename = "hn_net_ses_emme_2011_" ;
		String basename = "hn_net_ses_emme_2041_" ;
		String testOutputDir = utils.getOutputDirectory() ;
		final String nodesFilename = dir + basename + "nodes.shp";
		final String linksFilename = dir + basename + "links.shp";
		final String outputFilePrefix = testOutputDir + "/netconvert";
		NetworkConverter.main(new String[]{nodesFilename, linksFilename, outputFilePrefix}) ;
	}
}