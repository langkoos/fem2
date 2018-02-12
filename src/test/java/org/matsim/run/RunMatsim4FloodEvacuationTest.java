package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Set;

public class RunMatsim4FloodEvacuationTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public void test() {
		Config config = ConfigUtils.createConfig() ;
		
		String scenarioBase = "scenarios/initial-2041-scenario/" ;
		
		config.network().setInputFile( "hn_net_ses_emme_2041_network.xml.gz ");
		// (relative to config file location!)
		
		config.plans().setInputFile( "pop.xml.gz" ) ;
		// (relative to config file location!)
		
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		
		config.controler().setLastIteration(0);
		
		Set<String> set = new HashSet<>();
		set.add(TransportMode.car ) ;
		config.plansCalcRoute().setNetworkModes(set);
		config.qsim().setMainModes(set);
		
		config.qsim().setEndTime(36*3600);

		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("evac") ;
			params.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(params);
		}
		{
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("safe") ;
			params.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(params);
		}
		
		
		
		
		// ---
		
		String configFilename = scenarioBase + "testConfig.xml" ;
		
		ConfigUtils.writeConfig( config, configFilename );
		
		// ---

		try {
			RunMatsim4FloodEvacuation.main( new String [] {configFilename} );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong");
		}
	}
	
}
