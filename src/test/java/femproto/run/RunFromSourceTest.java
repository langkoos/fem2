package femproto.run;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.*;
import femproto.prepare.network.NetworkConverter;
import femproto.prepare.parsers.HydrographParser;
import femproto.prepare.parsers.SubsectorShapeFileParser;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RunFromSourceTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test(){
		Config config = ConfigUtils.loadConfig(utils.getPackageInputDirectory()+"scenario/config_runFromSource_optimizeLikeNICTA.xml");

		config.controler().setOutputDirectory(utils.getOutputDirectory());

//		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );
//		femConfig.setSampleSize( 1. );
		
		new RunMatsim4FloodEvacuation(config).run();


	}

}
