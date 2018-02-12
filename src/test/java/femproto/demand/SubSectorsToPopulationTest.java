package femproto.demand;

import femproto.network.NetworkConverterTest;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;


public class SubSectorsToPopulationTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void readSubSectorsShapeFile() throws Exception {
		final String networkFile = "test/input/scenarios/initial-2041-scenario/hn_net_ses_emme_2041_network.xml.gz" ;
		final String inputShapeFile = utils.getPackageInputDirectory() + "hn_subsectors_2041_lower.shp" ;
		final String evacNodesFile = utils.getPackageInputDirectory() + "2041_subsectors_safe_node_mapping.txt" ;
		final String outputPopFile = utils.getOutputDirectory() + "pop.xml.gz" ;
		
		String [] str = new String [] {inputShapeFile,networkFile,evacNodesFile,outputPopFile} ;
		SubSectorsToPopulation.main(str);
		
	}
	
}