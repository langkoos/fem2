package femproto.globals;

public interface FEMAttributes {
	String SUBSECTOR = "SUBSECTOR";
	String SAFE_ACTIVITY = "safe";
	String EVACUATION_ACTIVITY = "evac";

	double EVAC_FLOWRATE = 600;

	// these fields are the columns expected in the hydrograph point shapefile.
	String HYDROGRAPH_POINT_ID_FIELD = "ID";
	String HYDROGRAPH_LINK_IDS = "links_ids";
	String HYDROGRAPH_SUBSECTOR = "SUBSECTOR";
	String HYDROGRAPH_ALT_AHD = "ALT_AHD";
}
