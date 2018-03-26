package femproto.hydrograph;

import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.List;

public class HydrographPoint {
	final String pointId;
	final Double ALT_AHD;
	final Coord coord;
	String[] linkIds;
	List<HydrographPointData> data;

	public HydrographPoint(String pointId, Double alt_ahd, Coord coord) {
		this.pointId = pointId;
		ALT_AHD = alt_ahd;
		this.coord = coord;
	}

	public String[] getLinkIds() {
		return linkIds;
	}

	public void setLinkIds(String[] linkIds) {
		this.linkIds = linkIds;
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

	private class HydrographPointData{
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
