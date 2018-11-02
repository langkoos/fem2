package femproto.prepare.evacuationscheduling;

import com.opencsv.bean.CsvBindByName;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;

public class EvacuationScheduleRecordCompleteWithRoutes extends EvacuationScheduleRecordComplete {
	// needs to be public otherwise the CsvBind magic does not work. kai, sep'18
	public static final String routeDelimiter = "/---/";

	@CsvBindByName(required = true)
	private int duration;
	@CsvBindByName(required = true)
	private String networkRouteAsString;

	public EvacuationScheduleRecordCompleteWithRoutes(int time, String subsector, String evac_node, String safe_node, int vehicles, int duration, NetworkRoute networkRoute) {
		super(time, subsector, evac_node, safe_node, vehicles, duration);
		this.duration = duration;
		setNetworkRouteAsString(networkRoute);
	}

	public EvacuationScheduleRecordCompleteWithRoutes() {
		super();
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String[] getNetworkRoute() {
		return networkRouteAsString.split(routeDelimiter);
	}
	public String getNetworkRouteAsString() {
		return networkRouteAsString;
	}

	public void setNetworkRouteAsString(NetworkRoute networkRouteAsString) {
		this.networkRouteAsString = "";
		this.networkRouteAsString += networkRouteAsString.getStartLinkId().toString() + routeDelimiter;
		for (Id<Link> linkId : networkRouteAsString.getLinkIds()) {
			this.networkRouteAsString += linkId.toString() + routeDelimiter;
		}
		this.networkRouteAsString += networkRouteAsString.getEndLinkId().toString();
	}
}
