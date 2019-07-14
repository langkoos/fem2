package femproto.prepare.hydrograph;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.EvacuationScheduleFromHydrographData;
import femproto.prepare.evacuationscheduling.EvacuationScheduleToPopulationDepartures;
import femproto.prepare.evacuationscheduling.EvacuationScheduleWriter;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.EvacuationToSafeNodeParser;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.HydrographPoint;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import femproto.run.FEMConfigGroup;
import femproto.run.FEMUtils;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HydrographParserTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	{
		if (FEMUtils.getGlobalConfig() == null)
			FEMUtils.setGlobalConfig(FEMGlobalConfig.getDefaultGlobalConfig());
	}

	@Test
	public void test() throws FileNotFoundException {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork()).readFile("test/input/femproto/scenario/input_network.xml");
		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile("test/input/femproto/scenario/source/FEM2_Test_Subsectorvehicles_2016/FEM2_Test_Subsectorvehicles_2016.shp");

		HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
		hydrographParser.parseHydrographShapefile("test/input/femproto/scenario/source/wma_ref_points_1_to_2056_link_V12_nodes_2016/wma_ref_points_1_to_2056_link_nodesV12_2016.shp");
		hydrographParser.readHydrographData("test/input/femproto/scenario/source/d00229_H_TS_Exg.csv", 0);
		hydrographParser.hydrographToViaXY(utils.getOutputDirectory() + "/hydrograph_XY_time.txt");
		List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork());
		new NetworkChangeEventsWriter().write(utils.getOutputDirectory() + "/input_change_events.xml.gz", networkChangeEvents);

		//compare network change events

		List<NetworkChangeEvent> referenceChangeEvents = new ArrayList<>();
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(scenario.getNetwork(), referenceChangeEvents);
		parser.parse(IOUtils.getUrlFromFileOrResource(utils.getInputDirectory() + "input_change_events.xml.gz"));

		int refEventCount = 0;
		int matchedEventCount = 0;
		for (NetworkChangeEvent referenceChangeEvent : referenceChangeEvents) {
			refEventCount += referenceChangeEvent.getLinks().size();
		}

		for (NetworkChangeEvent referenceChangeEvent : referenceChangeEvents) {

			for (NetworkChangeEvent networkChangeEvent : networkChangeEvents) {
				if (networkChangeEvent.getStartTime() == referenceChangeEvent.getStartTime()) {
					for (Link refLink : referenceChangeEvent.getLinks()) {
						for (Link link : networkChangeEvent.getLinks()) {
							if (link.getId().equals(refLink.getId())) {
								if (networkChangeEvent.getFlowCapacityChange().getValue() == referenceChangeEvent.getFlowCapacityChange().getValue())
									matchedEventCount++;
							}

						}
					}

				}
			}
		}
		if (refEventCount != matchedEventCount)
			throw new RuntimeException("Change veents don't match reference");

	}
}
