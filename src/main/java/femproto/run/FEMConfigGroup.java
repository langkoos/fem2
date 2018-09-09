package femproto.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

import static femproto.run.FEMConfigGroup.FEMEvacuationTimeAdjustment.takeTimesFromInput;
import static femproto.run.FEMConfigGroup.FEMRoutingMode.preferEvacuationLinks;

public final class FEMConfigGroup extends ReflectiveConfigGroup{
	
	private static final String NAME="FEM" ;

	private static final Logger log = Logger.getLogger(FEMConfigGroup.class) ;

	public FEMConfigGroup() {
		super(NAME);
	}
	// ===
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
		map.put( INPUT_SUBSECTORS_SHAPEFILE, INPUT_SUBSECTORS_SHAPEFILE_CMT ) ;
		return map ;
	}

	// ===
	enum FEMRoutingMode {preferEvacuationLinks}
	private FEMRoutingMode femRoutingMode = preferEvacuationLinks ;
	public final FEMRoutingMode getFemRoutingMode() {
		return femRoutingMode ;
	}
	// would need a setter if we needed a second routing mode.  kai, jul'18

	// ---
	enum FEMEvacuationTimeAdjustment{takeTimesFromInput, allDepartAtMidnight } ;
	private FEMEvacuationTimeAdjustment femEvacuationTimeAdjustment = takeTimesFromInput;
	public FEMEvacuationTimeAdjustment getFemEvacuationTimeAdjustment() {
		return femEvacuationTimeAdjustment;
	}
	public void setFemEvacuationTimeAdjustment( final FEMEvacuationTimeAdjustment femEvacuationTimeAdjustment ) {
		this.femEvacuationTimeAdjustment = femEvacuationTimeAdjustment;
	}

	// ---
	enum FEMRunType {justRunInputPlansFile, runFromEvacuationSchedule, optimizeSafeNodesByPerson, optimizeSafeNodesBySubsector }
	private FEMRunType femRunType = FEMRunType.runFromEvacuationSchedule;
	private static final String FEM_RUN_TYPE="FEMRunType" ;
	private static final String FEM_RUN_TYPE_CMT = "FEM run type. ";
	@StringGetter( FEM_RUN_TYPE )
	FEMRunType getFemRunType() {
		return femRunType;
	}
	@StringSetter( FEM_RUN_TYPE )
	public void setFemRunType( final FEMRunType femRunType ) {
		this.femRunType = femRunType;
	}
	// ---
	private String inputSubsectorsShapefile = null ;
	private static final String INPUT_SUBSECTORS_SHAPEFILE="inputSubsectorsShapefile" ;
	private static final String INPUT_SUBSECTORS_SHAPEFILE_CMT="Shapefile that contains the subsector shapes, and the number of vehicles per subsector.  " +
												     "Also contains connection to a network node, although MATSim would not really need that." ;
	@StringGetter( INPUT_SUBSECTORS_SHAPEFILE )
	public String getInputSubsectorsShapefile() {
		return inputSubsectorsShapefile ;
	}
	@StringSetter( INPUT_SUBSECTORS_SHAPEFILE )
	public void setInputSubsectorsShapefile( String str ) {
		inputSubsectorsShapefile = str ;
	}
	// ---
	private String inputSubsectorsToSafeNodesMappingsFile = null ;
	private static final String INPUT_SUBSECTORS_TO_SAFE_NODES_MAPPINGS_FILE="inputSubsectorsToSafeNodesMappingsFile" ;
	@StringGetter( INPUT_SUBSECTORS_TO_SAFE_NODES_MAPPINGS_FILE )
	public String getInputSubsectorsToSafeNodesMappingsFile() {
		return inputSubsectorsToSafeNodesMappingsFile ;
	}
	@StringSetter( INPUT_SUBSECTORS_TO_SAFE_NODES_MAPPINGS_FILE )
	public void setInputSubsectorsToSafeNodesMappingsFile( String str ) {
		inputSubsectorsToSafeNodesMappingsFile = str ;
	}
	// ---
	private double sampleSize = 0.01;
	private static final String SAMPLE_SIZE = "sampleSize";
	@StringGetter( SAMPLE_SIZE )
	public double getSampleSize() {
		return this.sampleSize;
	}
	@StringSetter( SAMPLE_SIZE )
	public void setSampleSize(double sampleSize){
		this.sampleSize = sampleSize;
	}
	// ---
	private String evacuationScheduleFile ;
	private static final String EVACUATION_SCHEDULE_FILE = "evacuationScheduleFile" ;
	@StringGetter( EVACUATION_SCHEDULE_FILE )
	public String getEvacuationScheduleFile() {
		return this.evacuationScheduleFile ;
	}
	@StringSetter( EVACUATION_SCHEDULE_FILE )
	public void setEvacuationScheduleFile( String filename ) {
		this.evacuationScheduleFile = filename ;
	}
}
