package femproto.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

import static femproto.run.FEMConfigGroup.FEMEvacuationTimeAdjustment.takeTimesFromInputPlans;
import static femproto.run.FEMConfigGroup.FEMRoutingMode.preferEvacuationLinks;

public final class FEMConfigGroup extends ReflectiveConfigGroup{
	
	private static final String NAME="FEM" ;

	private static final Logger log = Logger.getLogger(FEMConfigGroup.class) ;

	public FEMConfigGroup() {
		super(NAME);
	}
	
	@Override
	public Map<String,String> getComments() {
		final Map<String, String> map = super.getComments();;
		{
			String str = FEM_RUN_TYPE_CMT + " Options: " ;
			for ( FEMRunType type : FEMRunType.values() ) {
				str += type.name() + " " ;
			}
			map.put( FEM_RUN_TYPE, str ) ;
		}
		return map ;
	}
	
	// ---
	enum FEMRoutingMode {preferEvacuationLinks}
	private FEMRoutingMode femRoutingMode = preferEvacuationLinks ;
	final FEMRoutingMode getFemRoutingMode() {
		return femRoutingMode ;
	}
	// would need a setter if we needed a second routing mode.  kai, jul'18

	// ---
	enum FEMEvacuationTimeAdjustment{takeTimesFromInputPlans, allDepartAtMidnight } ;
	private FEMEvacuationTimeAdjustment femEvacuationTimeAdjustment = takeTimesFromInputPlans;
	FEMEvacuationTimeAdjustment getFemEvacuationTimeAdjustment() {
		return femEvacuationTimeAdjustment;
	}
	void setFemEvacuationTimeAdjustment( final FEMEvacuationTimeAdjustment femEvacuationTimeAdjustment ) {
		this.femEvacuationTimeAdjustment = femEvacuationTimeAdjustment;
	}

	// ---
	enum FEMRunType { justRunInitialPlansFile, optimizeSafeNodesByPerson, optimizeSafeNodesBySubsector }
	private FEMRunType femRunType = FEMRunType.justRunInitialPlansFile ;
	private static final String FEM_RUN_TYPE="FEMRunType" ;
	private static final String FEM_RUN_TYPE_CMT = "FEM run type. ";
	@StringGetter( FEM_RUN_TYPE )
	FEMRunType getFemRunType() {
		return femRunType;
	}
	@StringSetter( FEM_RUN_TYPE )
	void setFemRunType( final FEMRunType femRunType ) {
		this.femRunType = femRunType;
	}
	// ---
	
}
