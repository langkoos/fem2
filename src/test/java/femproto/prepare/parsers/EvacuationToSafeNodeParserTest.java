package femproto.prepare.parsers;

import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class EvacuationToSafeNodeParserTest {

	@Test
	public void test(){
		String input = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/2016_subsectors_safe_node_mapping.txt";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		EvacuationToSafeNodeParser parser = new EvacuationToSafeNodeParser(network,evacuationSchedule);
		parser.readEvacAndSafeNodes(input);
	}
}