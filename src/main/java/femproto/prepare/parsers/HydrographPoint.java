package femproto.prepare.parsers;

import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HydrographPoint {
	final String pointId;
	final Double ALT_AHD;
	final Coord coord;
	private Set<String> linkIds = new HashSet<>();
	List<HydrographPointData> data;
	private String subSector;
	private double floodTime;
	public HydrographPoint(String pointId, Double alt_ahd, Coord coord) {
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
			this.linkIds.add(linkId);
		}
	}

	public List<HydrographPointData> getData() {
		return data;
	}

	public boolean inHydrograph(){
		return data != null;
	}

	public boolean mappedToNetworkLink(){
		return linkIds != null;
	}

	public void addTimeSeriesData(double time, double level_ahd){
		if(data == null)
			data = new ArrayList<>();
		data.add(new HydrographPointData(time,level_ahd));
	}

	public double getFloodTime() {
		return floodTime;
	}


	public void setHydrographFloodTimes() {
			double floodtime = -1;
			for (HydrographPointData pointDatum : this.getData()) {
				if (pointDatum.getLevel_ahd() - this.getALT_AHD() > 0) {
					floodtime = pointDatum.getTime();
					System.out.println("flooding subsector " + this.getSubSector() + " starts flooding at " + pointDatum.getTime());
					break;
				}
			}
			this.floodTime = floodtime;
	}

	public class HydrographPointData{
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
