package femproto.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static femproto.run.FEMConfigGroup.FEMEvacuationTimeAdjustment;
import static femproto.run.FEMConfigGroup.FEMRunType;
import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result;
import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.compare;

@RunWith(Parameterized.class )
public class DifferentVariantsTestIT {
	private static final Logger log = Logger.getLogger( RunMatsim4FloodEvacuationTestIT.class );
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	private final FEMRunType runType;
	private final FEMEvacuationTimeAdjustment timeAdjustment;

	private static String utilsOutputDir ;
	
	public DifferentVariantsTestIT( FEMRunType runType, FEMEvacuationTimeAdjustment timeAdjustment ) {
		this.runType = runType;
		this.timeAdjustment = timeAdjustment;
	}
	
	@Parameters(name="{index}: {0} {1}") // the "name" entry is just for the test output
	public static Collection<Object[]> abc() { // the name of this method does not matter as long as it is correctly annotated
		List<Object[]> combos = new ArrayList<>() ;
		
		for ( FEMRunType rt : FEMRunType.values() ) {
			for ( FEMEvacuationTimeAdjustment ta : FEMEvacuationTimeAdjustment.values() ) {
				combos.add( new Object [] { rt, ta } ) ;
			}
		}
		return combos ;
	}
	
	@Test public void test() {

		if ( utilsOutputDir==null ) {
			utilsOutputDir = utils.getOutputDirectory() ;
			// utils.getOutputDirectory() first removes everything _at that level_.  For the way the output paths are
			// constructed here, this means that only the last parameterized test output will survive.
			// There might be a better solution ...   kai, jul'18
		}

		final String dirExtension = "/" + runType.name() + "_" + timeAdjustment.name() + "/";
		
		RunMatsim4FloodEvacuation evac = new RunMatsim4FloodEvacuation() ;

		Config config = evac.loadConfig( null ) ;
		
		config.controler().setOutputDirectory( utilsOutputDir + dirExtension );
		
		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );;
		femConfig.setFemRunType( runType );
		femConfig.setFemEvacuationTimeAdjustment( timeAdjustment );
		
		evac.run() ;
		
		String expected = utilsOutputDir + dirExtension + "/output_events.xml.gz" ;
		String actual = utilsOutputDir + dirExtension + "/output_events.xml.gz" ;
		Result result = compare( expected, actual ) ;
		Assert.assertEquals( Result.FILES_ARE_EQUAL, result );
		
	}
	
}
