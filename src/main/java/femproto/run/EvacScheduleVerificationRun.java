package femproto.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class EvacScheduleVerificationRun {
	public static void main ( String [] args ) {
		final RunMatsim4FloodEvacuation evac = new RunMatsim4FloodEvacuation();
		final Config config = evac.loadConfig( args );
		ConfigUtils.addOrGetModule( config, FEMConfigGroup.class ).setFemRunType( FEMConfigGroup.FEMRunType.runFromEvacuationSchedule );
		evac.run() ;
	}
}
