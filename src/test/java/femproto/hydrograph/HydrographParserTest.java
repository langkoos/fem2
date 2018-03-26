package femproto.hydrograph;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

public class HydrographParserTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void test(){
		String inputDirectory = utils.getPackageInputDirectory();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("scenarios/fem2016/hn_net_ses_emme_2016_V12_network.xml.gz");
		HydrographParser hydrographParser = new HydrographParser();
		hydrographParser.hydroPointsShapefile2HydrographPointMap(inputDirectory + "/wma_ref_points_1_to_2056_link_nodesV12_2016.shp", network);

		hydrographParser.readHydrographData(inputDirectory + "/d00045_H_TS1.csv");
		hydrographParser.readHydrographData(inputDirectory + "/d00045_H_TS2.csv");
		hydrographParser.removeHydrographPointsWithNoData();

		int linkCount = 0;
		for (Map.Entry<String, HydrographPoint> stringEntry : hydrographParser.getHydrographPointMap().entrySet()) {
			System.out.printf("%s:\t%s\t%s\n",stringEntry.getKey(),java.util.Arrays.toString(stringEntry.getValue().linkIds), stringEntry.getValue().ALT_AHD);
			if (stringEntry.getValue().mappedToNetworkLink()) {
				linkCount++;
			}
		}
		System.out.println(linkCount + " out of " + hydrographParser.getHydrographPointMap().size()+ " points are associated with a link.");

	}
}
