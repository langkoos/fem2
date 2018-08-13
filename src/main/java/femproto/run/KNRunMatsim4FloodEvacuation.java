/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package femproto.run;

import femproto.run.FEMConfigGroup.FEMRunType;
import org.apache.log4j.Logger;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import static femproto.run.FEMConfigGroup.*;
import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.*;

/**
 * @author nagel
 *
 */
public class KNRunMatsim4FloodEvacuation {
	private static final Logger log = Logger.getLogger(KNRunMatsim4FloodEvacuation.class) ;
	
	public static void main ( String [] args ) {
		
		final RunMatsim4FloodEvacuation evac = new RunMatsim4FloodEvacuation();
		
		Config config = evac.loadConfig( args ) ;
		
		config.network().setTimeVariantNetwork( false );
		// yy I prefer running without.
		
		final FEMConfigGroup femConfig = ConfigUtils.addOrGetModule( config, FEMConfigGroup.class );
		
//		femConfig.setFemRunType( FEMRunType.optimizeSafeNodesBySubsector );
		femConfig.setFemRunType( FEMRunType.justRunInitialPlansFile );

		femConfig.setFemEvacuationTimeAdjustment( FEMEvacuationTimeAdjustment.allDepartAtMidnight );
		
		// ---
		
		evac.prepareConfig();
		
		final ActivityParams params = config.planCalcScore().getActivityParams( "dummy" );
		params.setScoringThisActivityAtAll( false );
		
		// ---
		
		evac.run() ;
	}
	
}
