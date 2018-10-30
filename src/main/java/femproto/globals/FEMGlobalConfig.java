package femproto.globals;

import com.google.inject.Singleton;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;
@Singleton
public final class FEMGlobalConfig extends ReflectiveConfigGroup {

	private static final String NAME = "FEMGlobal";

	FEMGlobalConfig(String name) {
		super(name);
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> map = super.getComments();
		map.put(BUFFER_TIME_BEFORE_FLOODING, BUFFER_TIME_BEFORE_FLOODING_CMT);
		map.put(EVACUATION_RATE, EVACUATION_RATE_CMT);
		map.put(CRS_EPSG3308, CRS_EPSG3308_CMT);
		map.put(CRS_EPSG28356, CRS_EPSG28356_CMT);
		map.put(ATTRIB_SUBSECTOR, ATTRIB_SUBSECTOR_CMT);
		map.put(ATTRIB_HYDROGRAPH_POINT_ID_FIELD, ATTRIB_HYDROGRAPH_POINT_ID_FIELD_CMT);
		map.put(ATTRIB_HYDROGRAPH_LINK_IDS, ATTRIB_HYDROGRAPH_LINK_IDS_CMT);
		map.put(ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD, ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD_CMT);
		map.put(SAFE_ACTIVITY, SAFE_ACTIVITY_CMT);
		map.put(EVACUATION_ACTIVITY, EVACUATION_ACTIVITY_CMT);
		return map;
	}

	// ====================== MODEL PARAMETERS ==========================
	// ==================================================================

	private double bufferTimeBeforeFlooding = 15;
	private static final String BUFFER_TIME_BEFORE_FLOODING = "bufferTimeBeforeFlooding";
	private static final String BUFFER_TIME_BEFORE_FLOODING_CMT = "Buffer time for evacuation to start before the subsector or a link in the path to one of its safe nodes gets flooded.";

	@StringGetter(BUFFER_TIME_BEFORE_FLOODING)
	public double getBufferTimeBeforeFlooding() {
		return bufferTimeBeforeFlooding;
	}

	@StringSetter(BUFFER_TIME_BEFORE_FLOODING)
	public void setBufferTimeBeforeFlooding(double bufferTimeBeforeFlooding) {
		this.bufferTimeBeforeFlooding = bufferTimeBeforeFlooding;
	}

	// ==================================================================

	private double evacuationRate = 600;
	private static final String EVACUATION_RATE = "evacuationRate";
	private static final String EVACUATION_RATE_CMT = "The default rate at which agents are allowed to depart from the evacuation node (Floating point value, positive).";

	@StringGetter(EVACUATION_RATE)
	public double getEvacuationRate() {
		return evacuationRate;
	}

	@StringSetter(EVACUATION_RATE)
	public void setEvacuationRate(double evacuationRate) {
		this.evacuationRate = evacuationRate;
	}

	// ======================== GIS GLOBALS =============================
	// ==================================================================

	private String crsEPSG3308 = "EPSG:3308";
	private static final String CRS_EPSG3308 = "crsEPSG3308";
	private static final String CRS_EPSG3308_CMT = "NSW Lambert state mapping EPSG:3308, used in some shapefiles.";

	@StringGetter(CRS_EPSG3308)
	public String getCrsEPSG3308() {
		return crsEPSG3308;
	}

	// ==================================================================

	private String crsEPSG28356 = "EPSG:28356";
	private static final String CRS_EPSG28356 = "crsEPSG28356";
	private static final String CRS_EPSG28356_CMT = "GDA94_MGA_zone_56 used for large scale infrastructure mapping in NSW (EPSG:28356). Projection of RMS EMME shapefiles.";

	@StringGetter(CRS_EPSG28356)
	public String getCrsEPSG28356() {
		return crsEPSG28356;
	}


	// =================== SHAPEFILE COLUMN NAMES =======================
	// ==================================================================

	private String attribSubsector = "SUBSECTOR";
	private static final String ATTRIB_SUBSECTOR = "attribSubsector";
	private static final String ATTRIB_SUBSECTOR_CMT = "Column name convention identifying subsectors (String value, spaces allowed but no leading or trailing spaces).";

	@StringGetter(ATTRIB_SUBSECTOR)
	public String getAttribSubsector() {
		return attribSubsector;
	}

	@StringSetter(ATTRIB_SUBSECTOR)
	public void setAttribSubsector(String attribSubsector) {
		this.attribSubsector = attribSubsector;
	}

	// ==================================================================

	private String attribHydrographPointId = "ID";
	private static final String ATTRIB_HYDROGRAPH_POINT_ID_FIELD = "attribHydrographPointId";
	private static final String ATTRIB_HYDROGRAPH_POINT_ID_FIELD_CMT = "Column name convention identifying hydrograph points in the shapefile (Integer value).";

	@StringGetter(ATTRIB_HYDROGRAPH_POINT_ID_FIELD)
	public String getAttribHydrographPointId() {
		return attribHydrographPointId;
	}

	@StringSetter(ATTRIB_HYDROGRAPH_POINT_ID_FIELD)
	public void setAttribHydrographPointId(String attribHydrographPointId) {
		this.attribHydrographPointId = attribHydrographPointId;
	}

	// ==================================================================

	private String attribHydrographLinkIds = "links_ids";
	private static final String ATTRIB_HYDROGRAPH_LINK_IDS = "attribHydrographLinkIds";
	private static final String ATTRIB_HYDROGRAPH_LINK_IDS_CMT = "Column name convention identifying links associated with hydrograph point (String value, comma-separated,no spaces)";

	@StringGetter(ATTRIB_HYDROGRAPH_LINK_IDS)
	public String getAttribHydrographLinkIds() {
		return attribHydrographLinkIds;
	}

	@StringSetter(ATTRIB_HYDROGRAPH_LINK_IDS)
	public void setAttribHydrographLinkIds(String attribHydrographLinkIds) {
		this.attribHydrographLinkIds = attribHydrographLinkIds;
	}

	// ==================================================================

	private String attribHydrographSelectedAltAHD = "ALT_AHD";
	private static final String ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD = "attribHydrographSelectedAltAHD";
	private static final String ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD_CMT = "Column name convention identifying the chosen field identifying the ground level for flooding (Floating point value, assumed to be positive).";

	@StringGetter(ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD)
	public String getAttribHydrographSelectedAltAHD() {
		return attribHydrographSelectedAltAHD;
	}

	@StringSetter(ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD)
	public void setAttribHydrographSelectedAltAHD(String attribHydrographSelectedAltAHD) {
		this.attribHydrographSelectedAltAHD = attribHydrographSelectedAltAHD;
	}


	// ===================== MODEL CONVENTIONS ==========================
	// ==================================================================

	private String safeActivity = "safe";
	private static final String SAFE_ACTIVITY = "safeActivity";
	private static final String SAFE_ACTIVITY_CMT = "Model convention for the naming of the activity for agents arriving at a safe node.";

	@StringGetter(SAFE_ACTIVITY)
	public String getSafeActivity() {
		return safeActivity;
	}

	@StringSetter(SAFE_ACTIVITY)
	public void setSafeActivity(String safeActivity) {
		this.safeActivity = safeActivity;
	}

	// ==================================================================

	private String evacuationActivity = "evac";
	private static final String EVACUATION_ACTIVITY = "evacuationActivity";
	private static final String EVACUATION_ACTIVITY_CMT = "Model convention for the naming of the activity for agents departing from an evacuation node.";

	@StringGetter(EVACUATION_ACTIVITY)
	public String getEvacuationActivity() {
		return evacuationActivity;
	}

	@StringSetter(EVACUATION_ACTIVITY)
	public void setEvacuationActivity(String evacuationActivity) {
		this.evacuationActivity = evacuationActivity;
	}

	// ==================================================================

}
