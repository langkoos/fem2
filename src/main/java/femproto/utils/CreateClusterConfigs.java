package femproto.utils;

import femproto.run.FEMConfigGroup;
import org.apache.commons.io.FileUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.matsim.core.config.ConfigWriter.Verbosity.minimal;

/**
 * Use this class to create a set of numbered config files for each possible combination of
 * scenario, dam development, etc.
 * <p>
 * These can then be run by AWS Batch array job on docker instances created by the code in docker folder
 */
public class CreateClusterConfigs {
	public static void main(String[] args) throws IOException {
		Set<String> scenarios = new HashSet<>();
		File folder = new File("./");
		File[] folders = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return dir.isDirectory() && name.toLowerCase().contains("scenario");
			}
		});
		for (File file : folders) {
			scenarios.add(file.getName());
		}


//		scenarios.addAll(Arrays.asList(new String[]{"A0", "A1", "A2"}));
		Set<String> damScenarios = new HashSet<>();
		folder = new File("./wma-flood-events/");
		folders = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return dir.isDirectory() ;
			}
		});
		for (File file : folders) {
			damScenarios.add(file.getName());
		}
//		damScenarios.addAll(Arrays.asList(new String[]{"Exg", "FSL-5", "D_14m", "D_20m"}));
		BufferedWriter bufferedWriter = IOUtils.getBufferedWriter("cluster/runids.csv");
		bufferedWriter.write(String.format("%s,%s,%s,%s\n", "runId", "scenario", "damScenario", "floodEvent"));
		int runId = 0;
		Config config = ConfigUtils.loadConfig("template_config.xml");
		FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		for (String scenario : scenarios) {


//			config.planCalcScore().getActivityParams("car interaction").setTypicalDuration(1);
//			config.planCalcScore().getActivityParams("pt interaction").setTypicalDuration(1);
//			config.planCalcScore().getActivityParams("bike interaction").setTypicalDuration(1);
//			config.planCalcScore().getActivityParams("other interaction").setTypicalDuration(1);
//			config.planCalcScore().getActivityParams("walk interaction").setTypicalDuration(1);
			config.plansCalcRoute().removeModeRoutingParams("bike");
			config.plansCalcRoute().removeModeRoutingParams("walk");
			config.plansCalcRoute().removeModeRoutingParams("access_walk");
			config.plansCalcRoute().removeModeRoutingParams("egress_walk");
			config.plansCalcRoute().removeModeRoutingParams("undefined");
			config.plansCalcRoute().removeModeRoutingParams("ride");
			config.plansCalcRoute().removeModeRoutingParams("pt");
			config.controler().setOutputDirectory("output");
			for (String damScenario : damScenarios) {
				folder = new File("wma-flood-events/" + damScenario);
				File[] files = folder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".csv");
					}
				});
				for (File file : files) {
					femConfigGroup.setHydrographData(file.getPath().toString());
					new ConfigWriter(config, minimal).write(String.format("clusterConfigs/configtemp_%d.xml", runId));
					bufferedWriter.write(String.format("%d,%s,%s,%s\n", runId, scenario, damScenario, file.getName()));
					BufferedReader bufferedReader = IOUtils.getBufferedReader(String.format("clusterConfigs/configtemp_%d.xml", runId));
					BufferedWriter writer = IOUtils.getBufferedWriter(String.format("clusterConfigs/config_%d.xml", runId));
					//yoyo this cuts out unnecessary stuff from the config that causes run to fail
					for (int i = 0; i < 61; i++) {
						writer.write(bufferedReader.readLine() + "\n");
					}
					writer.write("</config>");
					writer.close();
					FileUtils.forceDelete(new File(String.format("clusterConfigs/configtemp_%d.xml", runId)));
					runId++;
				}
			}

		}
		bufferedWriter.close();

	}
}
