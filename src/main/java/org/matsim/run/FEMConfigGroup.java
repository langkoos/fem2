package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

import static org.matsim.run.FEMConfigGroup.FEMRoutingMode.preferEvacuationLinks;

class FEMConfigGroup extends ReflectiveConfigGroup{
	
	private static final String NAME="FEM" ;

	private static final Logger log = Logger.getLogger(FEMConfigGroup.class) ;

	public FEMConfigGroup() {
		super(NAME);
	}
	
	public enum FEMRoutingMode {preferEvacuationLinks}
	private FEMRoutingMode femRoutingMode = preferEvacuationLinks ;
	public final FEMRoutingMode getFEMRoutingMode () {
		return femRoutingMode ;
	}
	
}
