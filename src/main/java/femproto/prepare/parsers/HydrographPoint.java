package femproto.prepare.parsers;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import java.util.*;
import java.util.function.BinaryOperator;

public class HydrographPoint {
	private static final Logger log = Logger.getLogger(HydrographPoint.class);

	int pointId;
	Double ALT_AHD;
	final Coord coord;
	private Set<String> linkIds = new HashSet<>();
	List<HydrographPointData> data;
	private String subSector;
	private HydrographPoint referencePoint;

	public void setFloodTime(double floodTime) {
		this.floodTime = floodTime;
	}

	private double floodTime = -1.0;

	public HydrographPoint(int pointId, Double alt_ahd, Coord coord) {
		this.pointId = pointId;
		ALT_AHD = alt_ahd;
		this.coord = coord;
	}

	public Double getALT_AHD() {
		return ALT_AHD;
	}

	public String getSubSector() {
		return subSector;
	}

	public void setSubSector(String subSector) {
		this.subSector = subSector;
	}

	public String[] getLinkIds() {
		return linkIds.toArray(new String[linkIds.size()]);
	}

	public void addLinkId(String linkId) {
		this.linkIds.add(linkId);
	}

	public void addLinkIds(String[] linkIds) {
		for (String linkId : linkIds) {
			if (!linkId.equals(""))
				this.linkIds.add(linkId);
		}
	}

	public List<HydrographPointData> getData() {
		return data;
	}

	public boolean inHydrograph() {
		return data != null;
	}

	public boolean mappedToNetworkLink() {
		return linkIds != null;
	}

	public void addTimeSeriesData(double time, double level_ahd) {
		if (data == null)
			data = new ArrayList<>();
		data.add(new HydrographPointData(time, level_ahd));
	}

	public double getFloodTime() {
		return floodTime;
	}


	public void calculateFloodTimeFromData() {
		if (this.getData() == null) {
			if (referencePoint != null)
				this.data = referencePoint.data;
		}
		try {
			for (HydrographPointData pointDatum : this.getData()) {
				if (pointDatum.getLevel_ahd() - this.getALT_AHD() > 0) {
					floodTime = pointDatum.getTime();
					log.info("flooding subsector " + this.getSubSector() + " starts flooding at " + pointDatum.getTime());
					break;
				}
			}
		} catch (NullPointerException ne) {
			if (referencePoint == null || referencePoint.getData() == null) {
				String message = String.format("Hydrograph point %d has no flooding data associated with it", this.pointId);
				log.error(message);
				log.error(this.toString());
				throw new RuntimeException(message);
			}
		}
	}

	public void setReferencePoint(HydrographPoint referencePoint) {
		this.referencePoint = referencePoint;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID: " + (referencePoint == null ? this.pointId : this.referencePoint.pointId) + "\n");
		sb.append("Links: " + Arrays.stream(this.getLinkIds()).reduce("", new BinaryOperator<String>() {
			@Override
			public String apply(String s, String s2) {
				return s+" "+s2;
			}
		}) + "\n");
		sb.append("Subsector: " + this.getSubSector());

		return sb.toString();
	}

	public class HydrographPointData {
		private final double time;
		private final double level_ahd;

		private HydrographPointData(double time, double level_ahd) {
			this.time = time;
			this.level_ahd = level_ahd;
		}

		public double getTime() {
			return time;
		}

		public double getLevel_ahd() {
			return level_ahd;
		}
	}
}
