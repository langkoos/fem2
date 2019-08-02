package femproto.prepare.parsers;

import com.google.inject.Inject;
import femproto.globals.FEMGlobalConfig;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.SubsectorData;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

/**
 * I am trying to break the demand generation process up into subsector data parsing,
 * contained in a schedule that interprets subsector data to have departures from evacuation to safe nodes.
 *
 * Such a schedule can then be used to be re-organised into a new schedule, or to produce a plans file
 */
public class SubsectorShapeFileParser {
	private static final Logger log = Logger.getLogger(SubsectorShapeFileParser.class);

	private final EvacuationSchedule evacuationSchedule;
	private final Network network;

	//yoyo at some point this should work with injection. pieter aug'18
	@Inject
	public SubsectorShapeFileParser(EvacuationSchedule evacuationSchedule, Network network) {
//		log.setLevel(Level.DEBUG);
		this.evacuationSchedule = evacuationSchedule;
		this.network = network;
	}

	public void readSubSectorsShapeFile(String fileName) {
		URL url = IOUtils.newUrl(null, fileName);
		readSubSectorsShapeFile(url);
	}

	public void readSubSectorsShapeFile(URL fileName) {
		log.info("entering readSubSectorsShapeFile with fileName=" + fileName.getPath());

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName.getPath());

		//iterate through features and modify/generate SubsectorData structures
		Iterator<SimpleFeature> iterator = features.iterator();
		int totalVehicleCount = 0;
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();

			// get subsector name from shp file
			String subsector = feature.getAttribute(FEMUtils.getGlobalConfig().getAttribSubsector()).toString();

			SubsectorData subsectorData = evacuationSchedule.getOrCreateSubsectorData(subsector);

			int subsectorVehicleCount;
			try {
				String totalvehic = FEMUtils.getGlobalConfig().getAttribTotalVehiclesForSubsector();
				try {
					subsectorVehicleCount = (int) feature.getAttribute(totalvehic);
				}catch(ClassCastException e){
					subsectorVehicleCount = (int) (double) feature.getAttribute(totalvehic);
				}
				log.info("Subsector " + subsector + " contains " + subsectorVehicleCount + " vehicles.");
			} catch (NullPointerException ne) {
				subsectorVehicleCount = 0;
				log.warn("Subsector " + subsector + " had null vehicles to evacuate, setting to zero.");
			}
			if (subsectorVehicleCount == 0) {
				log.warn("Subsector " + subsector + " sad zero vehicles to evacuate.");
			}
			if (subsectorVehicleCount < 0) {
				log.warn("Subsector " + subsector + " had less than zero vehicles to evacuate, setting to zero.");
				subsectorVehicleCount = 0;
			}

			try {
				if(feature.getAttribute(FEMUtils.getGlobalConfig().getAttribGaugeId()) != null) {
					int gaugeId = (int) (long) feature.getAttribute(FEMUtils.getGlobalConfig().getAttribGaugeId());
					double altAHD = (double) feature.getAttribute(FEMUtils.getGlobalConfig().getAttribHydrographSelectedAltAHD());
					subsectorData.setGaugeId(gaugeId);
					subsectorData.setAltAHD(altAHD);
				}
			}catch (Exception e){
				throw new RuntimeException("Trouble parsing GAUGE_ID or ALT_AHD for subsector "+ subsector);
			}
			subsectorData.setVehicleCount(subsectorVehicleCount);
			totalVehicleCount += subsectorVehicleCount;
			double lookAheadTime;
			try {
				lookAheadTime = Double.parseDouble(feature.getAttribute(FEMUtils.getGlobalConfig().getAttribLookAheadTime()).toString()) * 3600;
				subsectorData.setLookAheadTime(lookAheadTime);
			} catch (NullPointerException ne) {
				String message = "Need to define " + FEMUtils.getGlobalConfig().getAttribLookAheadTime() + " on a subsector basis";
				log.error(message);
				throw new RuntimeException(message);
			}

			String evacNodeFromShp;
			String evacNodeIdForSubsector = FEMUtils.getGlobalConfig().getAttribEvacNodeIdForSubsector();
			try {
				evacNodeFromShp = feature.getAttribute(evacNodeIdForSubsector).toString();
			} catch (NullPointerException ne) {
				String message = "Subsector " + subsector + " has no " + evacNodeIdForSubsector + " attribute.";
				log.warn(message);
				throw new RuntimeException(message);
			}
			Node node = network.getNodes().get(Id.createNodeId(evacNodeFromShp));
			if (node == null) {
				String msg = String.format("Did not find evacuation node %s for subsector %s in matsim network. ", evacNodeFromShp, subsector);
				log.warn(msg);
				throw new RuntimeException(msg);
			}
			subsectorData.setEvacuationNode(node);

			String safe_nodes = FEMUtils.getGlobalConfig().getAttribSafeNodeIdsForSubsector();
			for (String safeNodeId : feature.getAttribute(safe_nodes).toString().split(",")) {
				Node safeNode = network.getNodes().get(Id.createNodeId(safeNodeId.trim()));
				if (safeNode == null) {
					String msg = String.format("Did not find safe node %s for subsector %s in matsim network. ", safeNodeId, subsector);
					log.warn(msg);
					throw new RuntimeException(msg);
				}
				subsectorData.addSafeNode(safeNode);
			}

		}

		log.info("Parsed subsector shapefile. A total of " + totalVehicleCount + " vehicles need to be evacuated.");
	}
}
