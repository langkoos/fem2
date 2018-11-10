package femproto.globals;

public class Gis {
	public static String EPSG3308 = "EPSG:3308"; //NSW Lambert state mapping
	// larger scale GDA94_MGA_zone_56 used in some of the emme files, so we should probably stick to this
	public static String EPSG28356 = "PROJCS[\"GDA_1994_MGA_Zone_56\",GEOGCS[\"GCS_GDA_1994\",DATUM[\"D_GDA_1994\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",153.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
}
