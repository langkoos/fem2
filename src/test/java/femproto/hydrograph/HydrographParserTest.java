package femproto.hydrograph;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

public class HydrographParserTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void test(){
		String inputDirectory = utils.getPackageInputDirectory();
		MutableScenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/fem2016/hn_net_ses_emme_2016_V12_network.xml.gz");
		new PopulationReader(scenario).readFile("scenarios/fem2016/pop.xml.gz");

		HydrographParser hydrographParser = new HydrographParser();
		hydrographParser.hydroPointsShapefile2HydrographPointMap(inputDirectory + "/wma_ref_points_1_to_2056_link_nodesV12_2016.shp", scenario.getNetwork());

		hydrographParser.readHydrographData(inputDirectory + "/d09693_H_TS1.csv.gz");
		hydrographParser.readHydrographData(inputDirectory + "/d09693_H_TS2.csv.gz");
		hydrographParser.removeHydrographPointsWithNoData();
		hydrographParser.setHydroFloodTimes();

		int linkCount = 0;
		for (Map.Entry<String, HydrographPoint> stringEntry : hydrographParser.getHydrographPointMap().entrySet()) {
			System.out.printf("%s:\t%s\t%s\n",stringEntry.getKey(),java.util.Arrays.toString(stringEntry.getValue().linkIds), stringEntry.getValue().ALT_AHD);
			if (stringEntry.getValue().mappedToNetworkLink()) {
				linkCount++;
			}
		}
		System.out.println(linkCount + " out of " + hydrographParser.getHydrographPointMap().size()+ " points are associated with a link.");

		hydrographParser.hydrographToViaXY("scenarios/fem2016/hydroxy.txt.gz");

		hydrographParser.hydrographToViaLinkAttributes("scenarios/fem2016/hydro_linkattrs.txt.gz",scenario.getNetwork());

		hydrographParser.networkChangeEventsFromHydrographData(scenario.getNetwork(),"scenarios/fem2016/d09693_H_change_events.xml.gz");

		hydrographParser.triggerPopulationDepartures(scenario.getPopulation(),"scenarios/fem2016/pop.xml.gz",0,10/360);

	}
}
