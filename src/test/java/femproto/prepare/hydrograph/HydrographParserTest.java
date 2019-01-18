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
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HydrographParserTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;
	{
		if (FEMUtils.getGlobalConfig() == null)
			FEMUtils.setGlobalConfig(FEMGlobalConfig.getDefaultGlobalConfig());
	}
	@Test
	public void test(){

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/FEM2TestDataOctober18/testscenario/input_network.xml");
		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		new SubsectorShapeFileParser(evacuationSchedule, scenario.getNetwork()).readSubSectorsShapeFile("scenarios/FEM2TestDataOctober18/2016/FEM2_Test_Subsectorvehicles_2016/FEM2_Test_Subsectorvehicles_2016.shp");

		HydrographParser hydrographParser = new HydrographParser(scenario.getNetwork(), evacuationSchedule);
		hydrographParser.parseHydrographShapefile("scenarios/FEM2TestDataOctober18/2016/wma_ref_points_1_to_2056_link_V12_nodes_2016/wma_ref_points_1_to_2056_link_nodesV12_2016.shp");
		hydrographParser.readHydrographData("scenarios/FEM2TestDataOctober18/wma-flood-events/Exg/d00229_H_TS.csv",0);
		hydrographParser.hydrographToViaXY(utils.getOutputDirectory() + "/hydrograph_XY_time.txt");
		List<NetworkChangeEvent> networkChangeEvents = hydrographParser.networkChangeEventsFromConsolidatedHydrographFloodTimes(scenario.getNetwork());
		new NetworkChangeEventsWriter().write(utils.getOutputDirectory() + "/input_change_events.xml.gz",networkChangeEvents);
	}
}
