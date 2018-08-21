package femproto.prepare.evacuationdata;

import com.google.inject.Inject;
import femproto.prepare.demand.SubSectorsToPopulation;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * I am trying to break the demand generation process up into subsector data parsing,
 * contained in a schedule that interprets subsector data to have departures from evacuation to safe nodes.
 *<p/>
 * Such a schedule can then be used to be re-organised into a new schedule, or to produce a plans file
 */
public class SubsectorShapeFileParsing {
	private static final Logger log = Logger.getLogger(SubSectorsToPopulation.class) ;

	private final EvacuationSchedule evacuationSchedule;

	//yoyo at some point this should work with injection. pieter aug'18
	@Inject
	SubsectorShapeFileParsing(EvacuationSchedule evacuationSchedule) {
//		log.setLevel(Level.DEBUG);
		this.evacuationSchedule = evacuationSchedule;
	}

	public static void main(String[] args) throws IOException {
		EvacuationSchedule evacuationSchedule = new EvacuationSchedule();
		SubsectorShapeFileParsing subSectorsToPopulation = new SubsectorShapeFileParsing(evacuationSchedule);
		subSectorsToPopulation.readSubSectorsShapeFile(args[0]);
	}


	private void readSubSectorsShapeFile(String fileName) throws IOException {
		log.info("entering readSubSectorsShapeFile with fileName=" + fileName);

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);

		//iterate through features and modify/generate SubsectorData structures
		Iterator<SimpleFeature> iterator = features.iterator();
		int totalVehicleCount = 0;
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();

			// get subsector name from shp file
			String subsector = feature.getAttribute("Subsector").toString();

			SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(subsector);

			int subsectorVehicleCount;
			try {
				subsectorVehicleCount = (int) (double) feature.getAttribute("Totalvehic");
				log.debug("Subsector "+subsector+" contains "+subsectorVehicleCount+" vehicles.");
			} catch (NullPointerException ne){
				subsectorVehicleCount = 0;
			}
			if (subsectorVehicleCount <= 0){
				log.warn("Subsector "+subsector+" had zero or null vehicles to evacuate.");
			}
			subsectorData.setVehicleCount(subsectorVehicleCount);
			totalVehicleCount += subsectorVehicleCount;

			try {
				String evacNodeFromShp = feature.getAttribute("EVAC_NODE").toString();
				subsectorData.setEvacuationNode(evacNodeFromShp);
			}catch (NullPointerException ne){
				log.warn("Subsector "+subsector+" has no EVAC_NODE attribute.");
			}

		}

		log.info("Parsed subsector shapefile. A total of "+totalVehicleCount+" vehicles need to be evacuated.");
	}
}
