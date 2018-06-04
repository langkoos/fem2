package femproto.prepare.demand;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;


public class SubSectorsToPopulationTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void readSubSectorsShapeFile() throws Exception {
		final String networkFile = "scenarios/fem2016/hn_net_ses_emme_2016_V12_network.xml.gz" ;
//		final String networkFile = "scenarios/initial-2041-scenario/hn_net_ses_emme_2041_network.xml.gz" ;
		final String inputShapeFile = utils.getPackageInputDirectory() + "hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp" ;
		final String evacNodesFile = utils.getPackageInputDirectory() + "2016_subsectors_safe_node_mapping.txt" ;
		final String outputPopFile = utils.getOutputDirectory() + "pop.xml.gz" ;
		final String mappingFile = utils.getOutputDirectory() + "safeNodeMapping.shp" ;

		String [] str = new String [] {inputShapeFile,networkFile,evacNodesFile,outputPopFile,mappingFile} ;
		SubSectorsToPopulation.main(str);

	}
	
}
