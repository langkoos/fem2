package femproto.run;

import femproto.run.eventhandlers.FEMEvacuationLinkRoutingCounter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunInputPlansOnlyEvacRoutingTest {
	private static final Logger log = Logger.getLogger( RunInputPlansOnlyEvacRoutingTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	public void testA_startWithFullEvacRun() {
//		Config config = ConfigUtils.createConfig() ;
//
		String scenarioBase = "test/input/femproto/scenario/";
//

//		// ---
//
		String configFilename = scenarioBase + "config_justRunInputPlansFile.xml" ;
//
//		ConfigUtils.writeConfig( config, configFilename );
		
		// ---

			Config config = null;
		try {

			final RunMatsim4FloodEvacuation evac = new RunMatsim4FloodEvacuation();;

			config = evac.loadConfig( new String [] {configFilename} );

			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			// ---
			
			evac.run();
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong");
		}

		log.info("now checking event files for number of bad link entries");

		EventsManager eventsManager = new EventsManagerImpl();
		Network network = NetworkUtils.createNetwork();
//		new MatsimNetworkReader(network).readFile("scenarios/initial-2041-scenario/hn_net_ses_emme_2041_network.xml.gz");
		new MatsimNetworkReader(network).readFile(config.controler().getOutputDirectory()+"/output_network.xml.gz");
		FEMEvacuationLinkRoutingCounter counter = new FEMEvacuationLinkRoutingCounter(network);
		eventsManager.addHandler(counter);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);

		matsimEventsReader.readFile(config.controler().getOutputDirectory()+"/output_events.xml.gz");

		log.info(counter.getBadLinkEnterEventCount() + " out of "+ counter.getTotalLinkEnterEventCount() + " link entries on non-evac links.");
		
		Priority level = Level.INFO ;
		if ( counter.getEvacLinkFollowedByNonEvacLinkCount() > 0 ) {
			level = Level.WARN ;
		}

		log.log( level, "evac links followed by non-evac links=" + counter.getEvacLinkFollowedByNonEvacLinkCount() ) ;
		if ( counter.getEvacLinkFollowedByNonEvacLinkCount() > 0 ) {
			log.warn("yyyyyy That number should really be zero; need to investigate!!!") ;
		}
		
		if(counter.getBadLinkEnterEventCount()/counter.getTotalLinkEnterEventCount() > 0.01) {
			Assert.fail("Number of vehicles on non-evac links exceeds 1%");
		}

	}
}
