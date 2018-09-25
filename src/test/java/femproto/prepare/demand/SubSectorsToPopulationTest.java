package femproto.prepare.demand;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;


public class SubSectorsToPopulationTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	@Test
	public void readBadSubSectorsShapeFile() throws Exception {
		String dir = utils.getPackageInputDirectory() + "/2016_scenario_1A_v20180706/";
		final String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz" ;
//		final String networkFile = "scenarios/initial-2041-scenario/hn_net_ses_emme_2041_network.xml.gz" ;
		final String inputShapeFile = dir+ "hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp" ;
		final String evacNodesFile = dir + "2016_subsectors_safe_node_mapping.txt" ;
		final String outputPopFile = utils.getOutputDirectory() + "plans_from_hn_evacuationmodel_PL2016_V12subsectorsVehic2016.xml.gz" ;
//		final String mappingFile = utils.getOutputDirectory() + "safeNodeMapping.shp" ;
		final String attribsFile = utils.getOutputDirectory() + "pop_attribs.txt" ;
//		final String subsectorTravTime = utils.getOutputDirectory() + "subsectorMappingTravTime.txt" ;

		String [] str = new String [] {inputShapeFile,networkFile,evacNodesFile,outputPopFile,attribsFile} ;
		SubSectorsToPopulation.main(str);

	}
	
}
