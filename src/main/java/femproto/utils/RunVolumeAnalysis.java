package femproto.utils;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

public class RunVolumeAnalysis {
	public static void main(String[] args) throws IOException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("output_network.xml.gz");
		EventsManagerImpl eventsManager = new EventsManagerImpl();
		int timeBinSize = Integer.parseInt(args[0]);
		int maxTime = Integer.parseInt(args[1]) * 3600;
		VolumesAnalyzer volumesAnalyzer = new VolumesAnalyzer(timeBinSize, maxTime, network);
		eventsManager.addHandler(volumesAnalyzer);
		new MatsimEventsReader(eventsManager).readFile("output_events.xml.gz");
		BufferedWriter writer = IOUtils.getBufferedWriter("output_linkVolumes.txt");
		LINKS:
		for (Link link : network.getLinks().values()) {
			for (int i = 0; i < maxTime / timeBinSize; i++) {
				try {

					writer.write(String.format("%s\t%d\t%d\t%f\n", link.getId(), i * timeBinSize, volumesAnalyzer.getVolumesForLink(link.getId())[i], link.getFlowCapacityPerSec() * timeBinSize));
				} catch (NullPointerException ne) {
					//no data for this link, go to next
					continue LINKS;
				}
			}
		}
		writer.close();
	}
}
