package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

public class RunMatsim4FloodEvacuationTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public void test() {
		Config config = ConfigUtils.createConfig() ;
		
		String scenarioBase = "test/input/scenarios/initial-2041-scenario/" ;
		
		final String prefix = "../../../../"; // this is because the root for input files is where the config file resides.  kai, feb'18
		config.network().setInputFile( prefix + scenarioBase + "hn_net_ses_emme_2041_network.xml.gz ");
		
		config.plans().setInputFile( prefix + scenarioBase + "pop.xml.gz" ) ;
		
		String configFilename = utils.getOutputDirectory() + "inputConfig.xml" ;
		
		ConfigUtils.writeConfig( config, configFilename );
		
		try {
			RunMatsim4FloodEvacuation.main( new String [] {configFilename} );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong");
		}
	}
	
}
