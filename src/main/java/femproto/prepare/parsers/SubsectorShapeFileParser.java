package femproto.prepare.parsers;

import com.google.inject.Inject;
import femproto.prepare.evacuationscheduling.EvacuationSchedule;
import femproto.prepare.evacuationscheduling.SubsectorData;
import femproto.run.FEMUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.Iterator;

/**
 * I am trying to break the demand generation process up into subsector data parsing,
 * contained in a schedule that interprets subsector data to have departures from evacuation to safe nodes.
 * <p/>
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

	public void readSubSectorsShapeFile(String fileName)  {
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
				log.info("Subsector " + subsector + " contains " + subsectorVehicleCount + " vehicles.");
			} catch (NullPointerException ne) {
				subsectorVehicleCount = 0;
				log.warn("Subsector " + subsector + " had null vehicles to evacuate, setting to zero.");
			}
			if (subsectorVehicleCount <= 0) {
				log.warn("Subsector " + subsector + " had zero or less than zero vehicles to evacuate, setting to zero.");
				subsectorVehicleCount = 0;
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
			try {
				evacNodeFromShp = feature.getAttribute("EVAC_NODE").toString();
			} catch (NullPointerException ne) {
				String message = "Subsector " + subsector + " has no " + "EVAC_NODE" + " attribute.";
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

			for (String safeNodeId : feature.getAttribute("Safe_Nodes").toString().split(",")) {
				Node safeNode = network.getNodes().get(Id.createNodeId(safeNodeId.trim()));
				if (safeNode == null) {
					String msg = "did not find evacNode for subsector " + subsector + " in matsim network file: " + safeNodeId;
					log.warn(msg);
					throw new RuntimeException(msg);
				}
				subsectorData.addSafeNode(safeNode);
			}

		}

		log.info("Parsed subsector shapefile. A total of " + totalVehicleCount + " vehicles need to be evacuated.");
	}
}
