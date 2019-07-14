package femproto.prepare.parsers;

import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.run.FEMUtils;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.IOException;

public class SubsectorShapeFileParserTest {
	{
		if (FEMUtils.getGlobalConfig() == null)
			FEMUtils.setGlobalConfig(FEMGlobalConfig.getDefaultGlobalConfig());
	}

	@Test
	public void test() {
		String inputDir = "test/input/femproto/scenario/";
		String inputshapefile = inputDir + "source/FEM2_Test_Subsectorvehicles_2016/FEM2_Test_Subsectorvehicles_2016.shp";
		String networkFile = inputDir + "input_network.xml";

		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		SubsectorShapeFileParser subSectorsToPopulation = new SubsectorShapeFileParser(evacuationSchedule, network);
		subSectorsToPopulation.readSubSectorsShapeFile(inputshapefile);
	}
}