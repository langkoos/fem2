package org.matsim.gis;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.*;

public class NetworkConverterTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	//@Ignore // will for time being not work since gitlab-ci does not support git-fat.
	// Could put normal file into test input directory, but it somewhat defeats the purpose.
	public void testMain() throws Exception {
		String dir = "data/2011_evacuation_network/shapefiles/" ;
		String basename = "hn_net_ses_emme_2011_" ;
		String outputDir = utils.getOutputDirectory() ;
		NetworkConverter.main(new String[]{dir+basename+"nodes.shp",dir+basename+"links.shp", outputDir + "/netconvert"}) ;
	}
}