package femproto.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

import static femproto.run.FEMConfigGroup.FEMEvacuationTimeAdjustment.takeTimesFromInput;
import static femproto.run.FEMConfigGroup.FEMRoutingMode.preferEvacuationLinks;

public final class FEMConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "FEM";

	private static final Logger log = Logger.getLogger(FEMConfigGroup.class);

	public FEMConfigGroup() {
		super(NAME);
	}

	// ===
	@Override
	public Map<String, String> getComments() {
		final Map<String, String> map = super.getComments();
		;
		{
			String str = FEM_RUN_TYPE_CMT + " Options: ";
			for (FEMRunType type : FEMRunType.values()) {
				str += type.name() + " ";
			}
			map.put(FEM_RUN_TYPE, str);
		}
		{
			String str = FEM_OPTIMIZATION_TYPE_CMT + " Options: ";
			for (FEMOptimizationType type : FEMOptimizationType.values()) {
				str += type.name() + " ";
			}
			map.put(FEM_OPTIMIZATION_TYPE, str);
		}
		map.put(INPUT_SUBSECTORS_SHAPEFILE, INPUT_SUBSECTORS_SHAPEFILE_CMT);
		map.put(INPUT_NETWORK_NODES_SHAPEFILE, INPUT_NETWORK_NODES_SHAPEFILE_CMT);
		map.put(INPUT_NETWORK_LINKS_SHAPEFILE, INPUT_NETWORK_LINKS_SHAPEFILE_CMT);
		map.put(HYDROGRAPH_SHAPE_FILE, HYDROGRAPH_SHAPE_FILE_CMT);
		map.put(HYDROGRAPH_DATA, HYDROGRAPH_DATA_CMT);
		map.put(SAMPLE_SIZE, SAMPLE_SIZE_CMT);
		return map;
	}


	// ============================================
	// ============================================
	enum FEMRoutingMode {preferEvacuationLinks}
	private FEMRoutingMode femRoutingMode = preferEvacuationLinks;
	public final FEMRoutingMode getFemRoutingMode() {
		return femRoutingMode;
	}
	// would need a setter if we needed a second routing mode.  kai, jul'18
	// ============================================
	// ============================================
	enum FEMEvacuationTimeAdjustment {takeTimesFromInput, allDepartAtMidnight }
	private FEMEvacuationTimeAdjustment femEvacuationTimeAdjustment = takeTimesFromInput;
	public FEMEvacuationTimeAdjustment getFemEvacuationTimeAdjustment() {
		return femEvacuationTimeAdjustment;
	}
	public void setFemEvacuationTimeAdjustment(final FEMEvacuationTimeAdjustment femEvacuationTimeAdjustment) {
		this.femEvacuationTimeAdjustment = femEvacuationTimeAdjustment;
	}
	// ============================================
	// ============================================
	enum FEMRunType {justRunInputPlansFile, runFromEvacuationSchedule, runFromSource}
	private FEMRunType femRunType = FEMRunType.runFromSource;
	private static final String FEM_RUN_TYPE = "FEMRunType";
	private static final String FEM_RUN_TYPE_CMT = "FEM run type. ";
	@StringGetter(FEM_RUN_TYPE)
	FEMRunType getFemRunType() {
		return femRunType;
	}
	@StringSetter(FEM_RUN_TYPE)
	public void setFemRunType(final FEMRunType femRunType) {
		this.femRunType = femRunType;
	}
	// ============================================
	// ============================================
	enum FEMOptimizationType {optimizeSafeNodesByPerson, optimizeSafeNodesBySubsector, optimizeLikeNICTA, followTheLeader, none}
	private FEMOptimizationType femOptimizationType = FEMOptimizationType.optimizeLikeNICTA;
	private static final String FEM_OPTIMIZATION_TYPE = "FEMOptimizationType";
	private static final String FEM_OPTIMIZATION_TYPE_CMT = "Optimization process for routing, safe node allocation, timing, etc.";
	@StringGetter(FEM_OPTIMIZATION_TYPE)
	FEMOptimizationType getFemOptimizationType() {
		return femOptimizationType;
	}
	@StringSetter(FEM_OPTIMIZATION_TYPE)
	public void setFemOptimizationType(final FEMOptimizationType femOptimizationTType) {
		this.femOptimizationType = femOptimizationTType;
	}
	// ============================================
	// ============================================
	private String inputNetworkNodesShapefile = null;
	private static final String INPUT_NETWORK_NODES_SHAPEFILE = "inputNetworkNodesShapefile";
	private static final String INPUT_NETWORK_NODES_SHAPEFILE_CMT = "EMME nodes in a shapefile. " +
			"Also contains connection to a network node, although MATSim would not really need that.";

	@StringGetter(INPUT_NETWORK_NODES_SHAPEFILE)
	public String getInputNetworkNodesShapefile() {
		return inputNetworkNodesShapefile;
	}

	@StringSetter(INPUT_NETWORK_NODES_SHAPEFILE)
	public void setInputNetworkNodesShapefile(String str) {
		inputNetworkNodesShapefile = str;
	}

	// ---
	private String inputNetworkLinksShapefile = null;
	private static final String INPUT_NETWORK_LINKS_SHAPEFILE = "inputNetworkLinksShapefile";
	private static final String INPUT_NETWORK_LINKS_SHAPEFILE_CMT = "EMME nodes in a shapefile. " +
			"Also contains connection to a network node, although MATSim would not really need that.";

	@StringGetter(INPUT_NETWORK_LINKS_SHAPEFILE)
	public String getInputNetworkLinksShapefile() {
		return inputNetworkLinksShapefile;
	}

	@StringSetter(INPUT_NETWORK_LINKS_SHAPEFILE)
	public void setInputNetworkLinksShapefile(String str) {
		inputNetworkLinksShapefile = str;
	}

	// ---
	private String inputSubsectorsShapefile = null;
	private static final String INPUT_SUBSECTORS_SHAPEFILE = "inputSubsectorsShapefile";
	private static final String INPUT_SUBSECTORS_SHAPEFILE_CMT = "Shapefile that contains the subsector shapes, and the number of vehicles per subsector.  " +
			"Also contains connection to a network node, although MATSim would not really need that.";

	@StringGetter(INPUT_SUBSECTORS_SHAPEFILE)
	public String getInputSubsectorsShapefile() {
		return inputSubsectorsShapefile;
	}

	@StringSetter(INPUT_SUBSECTORS_SHAPEFILE)
	public void setInputSubsectorsShapefile(String str) {
		inputSubsectorsShapefile = str;
	}

	// ---
	private String inputSubsectorsToSafeNodesMappingsFile = null;
	private static final String INPUT_SUBSECTORS_TO_SAFE_NODES_MAPPINGS_FILE = "inputSubsectorsToSafeNodesMappingsFile";

	@StringGetter(INPUT_SUBSECTORS_TO_SAFE_NODES_MAPPINGS_FILE)
	public String getInputSubsectorsToSafeNodesMappingsFile() {
		return inputSubsectorsToSafeNodesMappingsFile;
	}

	@StringSetter(INPUT_SUBSECTORS_TO_SAFE_NODES_MAPPINGS_FILE)
	public void setInputSubsectorsToSafeNodesMappingsFile(String str) {
		inputSubsectorsToSafeNodesMappingsFile = str;
	}

	// ---
	private double sampleSize = 1.0;
	private static final String SAMPLE_SIZE = "sampleSize";
	private static final String SAMPLE_SIZE_CMT = "Re-routing can probably be done at 0.01 sample size.";

	@StringGetter(SAMPLE_SIZE)
	public double getSampleSize() {
		return this.sampleSize;
	}

	@StringSetter(SAMPLE_SIZE)
	public void setSampleSize(double sampleSize) {
		this.sampleSize = sampleSize;
	}

	// ---
	private String evacuationScheduleFile = null;
	private static final String EVACUATION_SCHEDULE_FILE = "evacuationScheduleFile";

	@StringGetter(EVACUATION_SCHEDULE_FILE)
	public String getEvacuationScheduleFile() {
		return this.evacuationScheduleFile;
	}

	@StringSetter(EVACUATION_SCHEDULE_FILE)
	public void setEvacuationScheduleFile(String filename) {
		this.evacuationScheduleFile = filename;
	}

	// ---
	private String hydrographData = null;
	private static final String HYDROGRAPH_DATA = "hydrographData";
	private static final String HYDROGRAPH_DATA_CMT = "CSV file containing hydrograph data. First column is time in hours, " +
			"subsequent columns have the ID of the associated hydrograph point in the first line and floating point " +
			"values in subsequent lines denoting water level in AHD.";

	@StringGetter(HYDROGRAPH_DATA)
	public String getHydrographData() {
		return hydrographData;
	}

	@StringSetter(HYDROGRAPH_DATA)
	public void setHydrographData(String hydrographData) {
		this.hydrographData = hydrographData;
	}


	// ---
	private String hydrographShapeFile;
	private static final String HYDROGRAPH_SHAPE_FILE = "hydrographShapeFile";

	@StringGetter(HYDROGRAPH_SHAPE_FILE)
	public String getHydrographShapeFile() {
		return this.hydrographShapeFile;
	}

	@StringSetter(HYDROGRAPH_SHAPE_FILE)
	public void setHydrographShapeFile(String filename) {
		this.hydrographShapeFile = filename;
	}

	private static final String HYDROGRAPH_SHAPE_FILE_CMT = "Hydrograph shapefile. This file is assumed to be projection EPSG:28356 and requires the following fields: \n" +
			"            ID \t\t Integer unique id of point" +
			"            links_ids \t [sic, this typo now sticks] String referring to EMME link ids.\n" +
			"            SUBSECTOR \t String of the subsector name, needs to match exactly in other files.\n" +
			"            ALT_AHD \t Floating point value, when hydrograph at this coordinate is greater than this value, link/subsector is assumed to be flooded.";
}
