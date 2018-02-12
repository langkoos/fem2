package femproto.demand;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;


public class SubSectorsToPopulationTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void readSubSectorsShapeFile() throws Exception {
		final String networkFile = "test/output/femproto/gis/NetworkConverterTest/testMain/netconvert.xml.gz" ;
		// yyyy might be easier to convert everything in one go? kai, feb'18
		
		final String inputShapeFile = "data/2041 Evacuation Modelling Data/2041_evacuation_network/hn_subsectors_2041_lower.shp";
		final String evacNodesFile = "data/2041 Evacuation Modelling Data/2041_evacuation_network/Assumptions evacuation and safe nodes mapping/2041_subsectors_safe_node_mapping.txt";
		final String outputPopFile = "pop.xml.gz" ;
		
		SubSectorsToPopulation subSectorsToPopulation = new SubSectorsToPopulation();
		subSectorsToPopulation.readNetwork(networkFile);
		subSectorsToPopulation.readEvacAndSafeNodes(evacNodesFile);
		subSectorsToPopulation.readSubSectorsShapeFile(inputShapeFile);
		subSectorsToPopulation.writePopulation(outputPopFile);
		
	}
	
}