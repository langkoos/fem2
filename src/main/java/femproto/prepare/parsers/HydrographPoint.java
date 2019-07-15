package femproto.prepare.parsers;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HydrographPoint {
	private static final Logger log = Logger.getLogger(HydrographPoint.class);

	int pointId;
	final Double ALT_AHD;
	final Coord coord;
	private Set<String> linkIds = new HashSet<>();
	List<HydrographPointData> data;
	private String subSector;

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
		for (HydrographPointData pointDatum : this.getData()) {
			if (pointDatum.getLevel_ahd() - this.getALT_AHD() > 0) {
				floodTime = pointDatum.getTime();
				System.out.println("flooding subsector " + this.getSubSector() + " starts flooding at " + pointDatum.getTime());
				break;
			}
		}
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
