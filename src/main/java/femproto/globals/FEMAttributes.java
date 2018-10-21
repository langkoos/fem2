package femproto.globals;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jfree.io.FileUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FEMAttributes {
	private static Logger log = Logger.getLogger(FEMAttributes.class);
	private static Properties FEMProperties = new Properties();
	public static final double BUFFER_TIME;
	public static final String SUBSECTOR;
	public static final String SAFE_ACTIVITY;
	public static final String EVACUATION_ACTIVITY;

	public static final double EVAC_FLOWRATE;

	public static final String HYDROGRAPH_POINT_ID_FIELD;
	public static final String HYDROGRAPH_LINK_IDS;
	public static final String HYDROGRAPH_SUBSECTOR;
	public static final String HYDROGRAPH_ALT_AHD;

	//static initializer
	static {
		try {
			InputStream propertiesInputStream = new FileInputStream("FEMGlobalAttributes.properties");
			FEMProperties.load(propertiesInputStream);
		} catch (IOException e) {
			log.warn("Could not load the FEMGlobalAttributes.properties file from the working directory.\n");
			log.warn("Reading the release version from resources and copying to the working directory for future use.");
			try {
				InputStream propertiesInputStream = ClassLoader.getSystemResourceAsStream("FEMGlobalAttributes.properties");
				FEMProperties.load(propertiesInputStream);
				writeDefaultPropertiesFile();
			} catch (IOException finalException) {
				log.error("Could not load the FEMGlobalAttributes.properties file from resources. Something is seriously wrong, get help.");
			}
		}
		//yoyoyo if any of these attributes are loaded incorrectly, or are missing, will get an unhandled exception...
		BUFFER_TIME= Double.parseDouble(FEMProperties.getProperty("BUFFER_TIME"));
		SUBSECTOR = FEMProperties.getProperty("SUBSECTOR");
		SAFE_ACTIVITY = FEMProperties.getProperty("SAFE_ACTIVITY");
		EVACUATION_ACTIVITY = FEMProperties.getProperty("EVACUATION_ACTIVITY");
		EVAC_FLOWRATE = Double.parseDouble(FEMProperties.getProperty("EVAC_FLOWRATE"));
		HYDROGRAPH_POINT_ID_FIELD = FEMProperties.getProperty("HYDROGRAPH_POINT_ID_FIELD");
		HYDROGRAPH_LINK_IDS = FEMProperties.getProperty("HYDROGRAPH_LINK_IDS");
		HYDROGRAPH_SUBSECTOR = FEMProperties.getProperty("HYDROGRAPH_SUBSECTOR");
		HYDROGRAPH_ALT_AHD = FEMProperties.getProperty("HYDROGRAPH_ALT_AHD");
	}

	public static void writeDefaultPropertiesFile() {
		File source = new File(ClassLoader.getSystemResource("FEMGlobalAttributes.properties").getFile());
		try {
			FileUtils.copyFile(source, new File("FEMGlobalAttributes.properties"));
		} catch (IOException e) {
			log.warn("Could not copy the default FEMGlobalAttributes.properties file from the release resources to working directory.");
			log.warn("The simulation will run using release defaults for all configurable attributes");
		}

	}
}
