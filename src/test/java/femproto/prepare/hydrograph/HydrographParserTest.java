package femproto.prepare.hydrograph;

import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleWriter;
import femproto.prepare.parsers.EvacuationToSafeNodeParser;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.HydrographPoint;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.util.Map;

public class HydrographParserTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void test(){
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String networkFile = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		String inputEvactoSafeNode = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/2016_subsectors_safe_node_mapping.txt";

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		MutableScenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		SubsectorShapeFileParser subSectorsToPopulation = new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork());
		try {
			subSectorsToPopulation.readSubSectorsShapeFile(inputshapefile);
		} catch (IOException e) {
			throw new RuntimeException("Input shapefile not found, or some other IO error");
		}

		EvacuationToSafeNodeParser parser = new EvacuationToSafeNodeParser(scenario.getNetwork(),evacuationSchedule);
		parser.readEvacAndSafeNodes(inputEvactoSafeNode);

		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordNoVehiclesNoDurations(utils.getOutputDirectory()+"simpleEvacuationScheduleV1.csv");

		evacuationSchedule.createSchedule();
		evacuationSchedule.completeAllocations();
		String inputDirectory = utils.getPackageInputDirectory()+"v20180706/";

		HydrographParser hydrographParser = new HydrographParser();
		hydrographParser.hydroPointsShapefile2HydrographPointMap(inputDirectory + "/wma_ref_points_1_to_2056_link_nodesV12_2016.shp", scenario.getNetwork(),evacuationSchedule);

//		hydrographParser.readHydrographData(inputDirectory + "/d06391_H_TS.csv.gz");
		hydrographParser.readHydrographData(inputDirectory + "/d00285_H_TS.csv.gz");
		hydrographParser.removeHydrographPointsWithNoData();
		hydrographParser.setHydroFloodTimes();

		int linkCount = 0;
		for (Map.Entry<String, HydrographPoint> stringEntry : hydrographParser.getHydrographPointMap().entrySet()) {
			System.out.printf("%s:\t%s\t%s\n",stringEntry.getKey(),java.util.Arrays.toString(stringEntry.getValue().getLinkIds()), stringEntry.getValue().getALT_AHD());
			if (stringEntry.getValue().mappedToNetworkLink()) {
				linkCount++;
			}
		}
		System.out.println(linkCount + " out of " + hydrographParser.getHydrographPointMap().size()+ " points are associated with a link.");

		hydrographParser.hydrographToViaXY(utils.getOutputDirectory()+"hydroxy.txt.gz");

		hydrographParser.hydrographToViaLinkAttributes(utils.getOutputDirectory()+"hydro_linkattrs.txt.gz",scenario.getNetwork());

		hydrographParser.networkChangeEventsFromHydrographData(scenario.getNetwork(),utils.getOutputDirectory()+"d00285_H_change_events.xml.gz");
//		hydrographParser.networkChangeEventsFromHydrographData(scenario.getNetwork(),utils.getOutputDirectory()+"d06391_H_change_events.xml.gz");

//		hydrographParser.readEvacAndSafeNodes( "test/output/femproto/demand/SubSectorsToPopulationTest/readSubSectorsShapeFile/subsectorMappingTravTimeRanked.csv");

//		hydrographParser.triggerPopulationDepartures(scenario.getPopulation(),utils.getOutputDirectory()+"/plans_from_hn_evacuationmodel_PL2016_V12subsectorsVehic2016.xml.gz",36000,60/360, 300);

	}
}
