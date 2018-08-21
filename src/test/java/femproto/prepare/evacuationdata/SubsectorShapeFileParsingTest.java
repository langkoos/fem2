package femproto.prepare.evacuationdata;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.junit.Assert.*;

public class SubsectorShapeFileParsingTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;
	@Test
	public void  testShapeFileParsing(){
		String inputshapefile = "test/input/femproto/prepare/demand/2016_scenario_1A_v20180706/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.shp";
		String network = "scenarios/fem2016_v20180706/hn_net_ses_emme_2016_V12_network.xml.gz";
		try {
			SubsectorShapeFileParsing.main(new String[]{inputshapefile,network});
		} catch (IOException e) {
			throw new RuntimeException("Input file not found");
		}
	}
}