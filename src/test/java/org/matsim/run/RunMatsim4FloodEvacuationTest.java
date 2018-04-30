package org.matsim.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;
import routing.FEMEvacuationLinkRoutingCounter;

import java.util.HashSet;
import java.util.Set;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunMatsim4FloodEvacuationTest {
	private static final Logger log = Logger.getLogger( RunMatsim4FloodEvacuationTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	public void testA_startWithFullEvacRun() {
//		Config config = ConfigUtils.createConfig() ;
//
		String scenarioBase = "scenarios/fem2016/" ;
//
//		config.network().setInputFile( "hn_net_ses_emme_2016_V12_network.xml.gz");
//		// (relative to config file location!)
//
//		config.plans().setInputFile( "pop.xml.gz" ) ;
//		// (relative to config file location!)
//
//		config.controler().setOutputDirectory( utils.getOutputDirectory() );
//		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
//
//		config.controler().setLastIteration(0);
//
//		config.network().setChangeEventsInputFile("d09693_H_change_events.xml.gz");
//		config.network().setTimeVariantNetwork(true);
//
//		Set<String> set = new HashSet<>();
//		set.add(TransportMode.car ) ;
//		config.plansCalcRoute().setNetworkModes(set);
//		config.qsim().setMainModes(set);
//
////		config.qsim().setEndTime(36*3600);
//
//		{
//			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("evac") ;
//			params.setScoringThisActivityAtAll(false);
//			config.planCalcScore().addActivityParams(params);
//		}
//		{
//			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("safe") ;
//			params.setScoringThisActivityAtAll(false);
//			config.planCalcScore().addActivityParams(params);
//		}
//
//
//
//
//		// ---
//
		String configFilename = scenarioBase + "configSmall.xml" ;
//
//		ConfigUtils.writeConfig( config, configFilename );
		
		// ---

		try {
			new RunMatsim4FloodEvacuation( new String [] {configFilename}, utils.getOutputDirectory() ).run() ;
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong");
		}
	}

	@Test
	public void testB_FEMEvacuationLinkRouting() {
		EventsManager eventsManager = new EventsManagerImpl();
		Network network = NetworkUtils.createNetwork();
//		new MatsimNetworkReader(network).readFile("scenarios/initial-2041-scenario/hn_net_ses_emme_2041_network.xml.gz");
		new MatsimNetworkReader(network).readFile(utils.getOutputDirectory()+"../testA_startWithFullEvacRun/output_network.xml.gz");
		FEMEvacuationLinkRoutingCounter counter = new FEMEvacuationLinkRoutingCounter(network);
		eventsManager.addHandler(counter);
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);

		matsimEventsReader.readFile(utils.getOutputDirectory()+"../testA_startWithFullEvacRun/output_events.xml.gz");

		log.info(counter.getBadLinkEnterEventCount() + " out of "+ counter.getTotalLinkEnterEventCount() + " link entries on non-evac links.");
		
		Priority level = Level.INFO ;
		if ( counter.getEvacLinkFollowedByNonEvacLinkCount() > 0 ) {
			level = Level.WARN ;
		}

		log.log( level, "evac links followed by non-evac links=" + counter.getEvacLinkFollowedByNonEvacLinkCount() ) ;
		if ( counter.getEvacLinkFollowedByNonEvacLinkCount() > 0 ) {
			log.warn("yyyyyy That number should really be zero; need to investigate!!!") ;
		}
		
		if(counter.getBadLinkEnterEventCount()/counter.getTotalLinkEnterEventCount() > 0.1) {
			Assert.fail("Number fo vehicles on non-evac links exceeds 10%");
		}

	}
}
