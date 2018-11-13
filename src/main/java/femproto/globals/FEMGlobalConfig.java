package femproto.globals;

import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;
@Singleton
public final class FEMGlobalConfig extends ReflectiveConfigGroup {

	private static final String NAME = "FEMGlobal";

	public FEMGlobalConfig() {
		super(NAME);
	}

	public static FEMGlobalConfig getGlobalConfig(){
		Config config = ConfigUtils.createConfig();
		return ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
	}

	public static FEMGlobalConfig getGlobalConfig(String  configFileName){
		Config config = ConfigUtils.loadConfig(configFileName);
		return ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> map = super.getComments();
		map.put(EVACUATION_RATE, EVACUATION_RATE_CMT);
		map.put(ATTRIB_SUBSECTOR, ATTRIB_SUBSECTOR_CMT);
		map.put(ATTRIB_LOOK_AHEAD_TIME, ATTRIB_LOOK_AHEAD_TIME_CMT);
		map.put(ATTRIB_HYDROGRAPH_POINT_ID_FIELD, ATTRIB_HYDROGRAPH_POINT_ID_FIELD_CMT);
		map.put(ATTRIB_HYDROGRAPH_LINK_IDS, ATTRIB_HYDROGRAPH_LINK_IDS_CMT);
		map.put(ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD, ATTRIB_HYDROGRAPH_SELECTED_ALT_AHD_CMT);
		map.put(SAFE_ACTIVITY, SAFE_ACTIVITY_CMT);
		map.put(EVACUATION_ACTIVITY, EVACUATION_ACTIVITY_CMT);
		map.put(ATTRIB_NETWORKLINKS_I_NODE, ATTRIB_NETWORKLINKS_J_NODE_CMT);
		map.put(ATTRIB_NETWORKLINKS_J_NODE, ATTRIB_NETWORKLINKS_J_NODE_CMT);
		map.put(ATTRIB_EVAC_MARKER, ATTRIB_EVAC_MARKER_CMT);
		map.put(ATTRIB_EVAC_NODE_ID_FOR_SUBSECTOR, ATTRIB_EVAC_NODE_ID_FOR_SUBSECTOR_CMT);
		map.put(ATTRIB_SAFE_NODE_IDS_FOR_SUBSECTOR, ATTRIB_SAFE_NODE_IDS_FOR_SUBSECTOR_CMT);
		map.put(ATTRIB_TOTAL_VEHICLES_FOR_SUBSECTOR, ATTRIB_TOTAL_VEHICLES_FOR_SUBSECTOR_CMT);
		map.put(ATTRIB_NETWORKLINKS_LENGTH, ATTRIB_NETWORKLINKS_LENGTH_CMT);
		map.put(ATTRIB_NETWORKLINKS_LANES, ATTRIB_NETWORKLINKS_LANES_CMT);
		map.put(ATTRIB_NETWORKLINKS_SPEED, ATTRIB_NETWORKLINKS_SPEED_CMT);
		map.put(ATTRIB_NETWORKLINKS_CAPSES, ATTRIB_NETWORKLINKS_CAPSES_CMT);
		map.put(ATTRIB_NETWORKLINKS_MODES, ATTRIB_NETWORKLINKS_MODES_CMT);
		return map;
	}

	// ====================== MODEL PARAMETERS ==========================
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

	private String attribLookAheadTime = "LOOK_AHEAD";
	private static final String ATTRIB_LOOK_AHEAD_TIME = "attribLookAheadTime";
	private static final String ATTRIB_LOOK_AHEAD_TIME_CMT = "Column name convention identifying buffer time before flooding (Floating point value, hours).";

	@StringGetter(ATTRIB_LOOK_AHEAD_TIME)
	public String getAttribLookAheadTime() {
		return attribLookAheadTime;
	}

	@StringSetter(ATTRIB_LOOK_AHEAD_TIME)
	public void setAttribLookAheadTime(String attribLookAheadTime) {
		this.attribLookAheadTime = attribLookAheadTime;
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

	private String attribEvacMarker = "EVAC_SES";
	private static final String ATTRIB_EVAC_MARKER = "attribEvacMarker";
	private static final String ATTRIB_EVAC_MARKER_CMT = "Column name convention identifying LINKS AND NODES for evacuation (Integer value, 0 or 1).";

	@StringGetter(ATTRIB_EVAC_MARKER)
	public String getAttribEvacMarker() {
		return attribEvacMarker;
	}

	@StringSetter(ATTRIB_EVAC_MARKER)
	public void setAttribEvacMarker(String attribEvacMarker) {
		this.attribEvacMarker = attribEvacMarker;
	}
	
	// ==================================================================
	
	private String attribEvacNodeIdForSubsector = "EVAC_NODE";
	private static final String ATTRIB_EVAC_NODE_ID_FOR_SUBSECTOR = "attribEvacNodeIdForSubsector";
	private static final String ATTRIB_EVAC_NODE_ID_FOR_SUBSECTOR_CMT = "Column name convention identifying evacuation node ID for a subsector in the suibsector shapefile (Integer value).";

	@StringGetter(ATTRIB_EVAC_NODE_ID_FOR_SUBSECTOR)
	public String getAttribEvacNodeIdForSubsector() {
		return attribEvacNodeIdForSubsector;
	}

	@StringSetter(ATTRIB_EVAC_NODE_ID_FOR_SUBSECTOR)
	public void setAttribEvacNodeIdForSubsector(String attribEvacNodeIdForSubsector) {
		this.attribEvacNodeIdForSubsector = attribEvacNodeIdForSubsector;
	}	
	
	// ==================================================================
	
	private String attribSafeNodeIdsForSubsector = "SAFE_NODES";
	private static final String ATTRIB_SAFE_NODE_IDS_FOR_SUBSECTOR = "attribSafeNodeIdsForSubsector";
	private static final String ATTRIB_SAFE_NODE_IDS_FOR_SUBSECTOR_CMT = "Column name convention identifying safe node IDs for a subsector in the subsector shapefile (Comma-separated integer values).";

	@StringGetter(ATTRIB_SAFE_NODE_IDS_FOR_SUBSECTOR)
	public String getAttribSafeNodeIdsForSubsector() {
		return attribSafeNodeIdsForSubsector;
	}

	@StringSetter(ATTRIB_SAFE_NODE_IDS_FOR_SUBSECTOR)
	public void setAttribSafeNodeIdsForSubsector(String attribSafeNodeIdsForSubsector) {
		this.attribSafeNodeIdsForSubsector = attribSafeNodeIdsForSubsector;
	}
	
	// ==================================================================
	
	private String attribTotalVehiclesForSubsector = "TOTAL_VEH";
	private static final String ATTRIB_TOTAL_VEHICLES_FOR_SUBSECTOR = "attribTotalVehiclesForSubsector";
	private static final String ATTRIB_TOTAL_VEHICLES_FOR_SUBSECTOR_CMT = "Column name convention identifying number of vehicles per subsector in the subsector shapefile (Integer).";

	@StringGetter(ATTRIB_TOTAL_VEHICLES_FOR_SUBSECTOR)
	public String getAttribTotalVehiclesForSubsector() {
		return attribTotalVehiclesForSubsector;
	}

	@StringSetter(ATTRIB_TOTAL_VEHICLES_FOR_SUBSECTOR)
	public void setAttribTotalVehiclesForSubsector(String attribTotalVehiclesForSubsector) {
		this.attribTotalVehiclesForSubsector = attribTotalVehiclesForSubsector;
	}
	
	// ==================================================================

	private String attribDescr = "T_DES";
	private static final String ATTRIB_DESCR = "attribDescr";
	private static final String ATTRIB_DESCR_CMT = "Column name convention identifying node or link descriptions (String).";

	@StringGetter(ATTRIB_DESCR)
	public String getAttribDescr() {
		return attribDescr;
	}

	@StringSetter(ATTRIB_DESCR)
	public void setAttribDescr(String attribDescr) {
		this.attribDescr= attribDescr;
	}

	// ==================================================================

	private String attribNetworkLinksINode = "INODE";
	private static final String ATTRIB_NETWORKLINKS_I_NODE = "attribNetworkLinksINode";
	private static final String ATTRIB_NETWORKLINKS_I_NODE_CMT = "Column name convention identifying 'from' node foreign key in links shapefile (Integer value).";

	@StringGetter(ATTRIB_NETWORKLINKS_I_NODE)
	public String getAttribNetworkLinksINode() {
		return attribNetworkLinksINode;
	}

	@StringSetter(ATTRIB_NETWORKLINKS_I_NODE)
	public void setAttribNetworkLinksINode(String attribNetworkLinksINode) {
		this.attribNetworkLinksINode = attribNetworkLinksINode;
	}

	// ==================================================================

	private String attribNetworkLinksJNode = "JNODE";
	private static final String ATTRIB_NETWORKLINKS_J_NODE = "attribNetworkLinksJNode";
	private static final String ATTRIB_NETWORKLINKS_J_NODE_CMT = "Column name convention identifying 'to' node foreign key in links shapefile (Integer value).";

	@StringGetter(ATTRIB_NETWORKLINKS_J_NODE)
	public String getAttribNetworkLinksJNode() {
		return attribNetworkLinksJNode;
	}

	@StringSetter(ATTRIB_NETWORKLINKS_J_NODE)
	public void setAttribNetworkLinksJNode(String attribNetworkLinksJNode) {
		this.attribNetworkLinksJNode = attribNetworkLinksJNode;
	}
	
	// ==================================================================

	private String attribNetworkLinksLength= "LENGTH";
	private static final String ATTRIB_NETWORKLINKS_LENGTH = "attribNetworkLinksLength";
	private static final String ATTRIB_NETWORKLINKS_LENGTH_CMT = "Column name convention identifying link length in links shapefile (Floating point, positive value in kilometres).";

	@StringGetter(ATTRIB_NETWORKLINKS_LENGTH)
	public String getAttribNetworkLinksLength() {
		return attribNetworkLinksLength;
	}

	@StringSetter(ATTRIB_NETWORKLINKS_LENGTH)
	public void setAttribNetworkLinksLength(String attribNetworkLinksLength) {
		this.attribNetworkLinksLength= attribNetworkLinksLength;
	}
	
	// ==================================================================

	private String attribNetworkLinksLanes= "LANES";
	private static final String ATTRIB_NETWORKLINKS_LANES = "attribNetworkLinksLanes";
	private static final String ATTRIB_NETWORKLINKS_LANES_CMT = "Column name convention identifying number of lanes in links shapefile (Floating point, positive value).";

	@StringGetter(ATTRIB_NETWORKLINKS_LANES)
	public String getAttribNetworkLinksLanes() {
		return attribNetworkLinksLanes;
	}

	@StringSetter(ATTRIB_NETWORKLINKS_LANES)
	public void setAttribNetworkLinksLanes(String attribNetworkLinksLanes) {
		this.attribNetworkLinksLanes= attribNetworkLinksLanes;
	}
	
	// ==================================================================

	private String attribNetworkLinksSpeed= "SPEED";
	private static final String ATTRIB_NETWORKLINKS_SPEED = "attribNetworkLinksSpeed";
	private static final String ATTRIB_NETWORKLINKS_SPEED_CMT = "Column name convention identifying link speed (km/h) in links shapefile (Floating point, positive value in km/h).";

	@StringGetter(ATTRIB_NETWORKLINKS_SPEED)
	public String getAttribNetworkLinksSpeed() {
		return attribNetworkLinksSpeed;
	}

	@StringSetter(ATTRIB_NETWORKLINKS_SPEED)
	public void setAttribNetworkLinksSpeed(String attribNetworkLinksSpeed) {
		this.attribNetworkLinksSpeed= attribNetworkLinksSpeed;
	}
	
	// ==================================================================

	private String attribNetworkLinksCapSES= "CAP_SES";
	private static final String ATTRIB_NETWORKLINKS_CAPSES = "attribNetworkLinksCapSES";
	private static final String ATTRIB_NETWORKLINKS_CAPSES_CMT = "Column name convention identifying TOTAL link evacuation capacity PER MINUTE  in links shapefile (Floating point, positive value).";

	@StringGetter(ATTRIB_NETWORKLINKS_CAPSES)
	public String getAttribNetworkLinksCapSES() {
		return attribNetworkLinksCapSES;
	}

	@StringSetter(ATTRIB_NETWORKLINKS_CAPSES)
	public void setAttribNetworkLinksCapSES(String attribNetworkLinksCapSES) {
		this.attribNetworkLinksCapSES= attribNetworkLinksCapSES;
	}
	
	// ==================================================================

	private String attribNetworkLinksModes= "MODES";
	private static final String ATTRIB_NETWORKLINKS_MODES = "attribNetworkLinksModes";
	private static final String ATTRIB_NETWORKLINKS_MODES_CMT = "Column name convention identifying allowed modes in links shapefile (lowercase String values, no separating characters: w(alk) c(ar) b(us) r(ail).";

	@StringGetter(ATTRIB_NETWORKLINKS_MODES)
	public String getAttribNetworkLinksModes() {
		return attribNetworkLinksModes;
	}

	@StringSetter(ATTRIB_NETWORKLINKS_MODES)
	public void setAttribNetworkLinksModes(String attribNetworkLinksModes) {
		this.attribNetworkLinksModes= attribNetworkLinksModes;
	}

	// ==================================================================

	private String attribHydrographLinkIds = "LINK_IDS";
	private static final String ATTRIB_HYDROGRAPH_LINK_IDS = "attribHydrographLinkIds";
	private static final String ATTRIB_HYDROGRAPH_LINK_IDS_CMT = "Column name convention identifying links associated with hydrograph point (String value, comma-separated, no spaces)";

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
