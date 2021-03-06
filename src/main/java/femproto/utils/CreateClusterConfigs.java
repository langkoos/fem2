package femproto.utils;

import femproto.run.FEMConfigGroup;
import org.apache.commons.io.FileUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.*;

import static org.matsim.core.config.ConfigWriter.Verbosity.minimal;

/**
 * Use this class to create a set of numbered config files for each possible combination of
 * scenario, dam development, etc.
 * <p>
 * These can then be run by AWS Batch array job on docker instances created by the code in docker folder
 */
public class CreateClusterConfigs {
    public static void main(String[] args) throws IOException {
        FileUtils.forceMkdir(new File("cluster"));
        Set<File> scenarios = new HashSet<>();
        File folder = new File("./");
        File[] folders = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return dir.isDirectory() && name.toLowerCase().contains("scenario");
            }
        });
        for (File file : folders) {
            scenarios.add(file);
        }


//		scenarios.addAll(Arrays.asList(new String[]{"A0", "A1", "A2"}));
        List<String> damScenarios = new ArrayList<>();
        List<File[]> floodEventFiles = new ArrayList<>();
        folder = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return dir.isDirectory() && name.toLowerCase().contains("events");
            }
        })[0];
        folders = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return dir.isDirectory();
            }
        });
        for (File file : folders) {
            damScenarios.add(file.getName());
            floodEventFiles.add(file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith("csv");
                }
            }));
        }

//		damScenarios.addAll(Arrays.asList(new String[]{"Exg", "FSL-5", "D_14m", "D_20m"}));
        BufferedWriter bufferedWriter = IOUtils.getBufferedWriter("cluster/runids.csv");
        bufferedWriter.write(String.format("%s,%s,%s,%s\n", "runId", "scenario", "damScenario", "floodEvent"));
        int runId = 0;
        Config config = ConfigUtils.loadConfig("./template_config.xml");
        FEMConfigGroup femConfigGroup = ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
        for (File scenario : scenarios) {

            File[] inputFiles = scenario.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith("shp");
                }
            });
            if (inputFiles.length == 0) {
                folders = scenario.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File dir) {
                        return dir.isDirectory();
                    }
                });
                for (File innerfolder : folders) {
                    inputFiles = innerfolder.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith("shp");
                        }
                    });
                    for (File inputFile : inputFiles) {
                        configure(femConfigGroup, inputFile);
                    }
                }

            } else
                for (File inputFile : inputFiles) {
                    configure(femConfigGroup, inputFile);
                }


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
//				if(scenario.getName().contains("A3")||scenario.getName().contains("B05")) {
//					if (damScenario.contains("2015"))
//						continue;
//				}else {
//					if (damScenario.contains("2019"))
//						continue;
//				}


                folder = new File("wma-flood-events/" + damScenario);
                File[] files = folder.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".csv");
                    }
                });
                for (File file : files) {
                    femConfigGroup.setHydrographData(file.getPath().toString());
                    new ConfigWriter(config, minimal).write(String.format("cluster/configtemp_%d.xml", runId));
                    BufferedWriter dataPathWriter = IOUtils.getBufferedWriter(String.format("cluster/inputs_%d.txt", runId));

                    bufferedWriter.write(String.format("%d,%s,%s,%s\n", runId, scenario.getName(), damScenario, file.getName().split("\\.")[0]));
                    BufferedReader bufferedReader = IOUtils.getBufferedReader(String.format("cluster/configtemp_%d.xml", runId));
                    BufferedWriter writer = IOUtils.getBufferedWriter(String.format("cluster/config_%d.xml", runId));
                    dataPathWriter.write(scenario.getName().toString() +"\n");
                    dataPathWriter.write(femConfigGroup.getHydrographData()+"\n");

                    //yoyo this cuts out unnecessary stuff from the config that causes run to fail
                    for (int i = 0; i < 62; i++) {
                        writer.write(bufferedReader.readLine() + "\n");
                    }
                    writer.write("<module name=\"qsim\" >\n");
                    writer.write("<param name=\"numberOfThreads\" value=\"1\" />\n");
                    writer.write("</module>\n");
                    writer.write("</config>");
                    writer.close();
                    FileUtils.forceDelete(new File(String.format("cluster/configtemp_%d.xml", runId)));
                    runId++;
                    dataPathWriter.close();
                }
            }

        }
        bufferedWriter.close();

    }

    private static void configure(FEMConfigGroup femConfigGroup, File inputFile) {
        String str = inputFile.getPath().toString();
        if (inputFile.getName().toLowerCase().contains("link"))
            femConfigGroup.setInputNetworkLinksShapefile(str);
        if (inputFile.getName().toLowerCase().contains("node") && !inputFile.getName().toLowerCase().contains("wma"))
            femConfigGroup.setInputNetworkNodesShapefile(str);
        if (inputFile.getName().toLowerCase().contains("wma"))
            femConfigGroup.setHydrographShapeFile(str);
        if (inputFile.getName().toLowerCase().contains("vehicle") | inputFile.getName().toLowerCase().contains("sector"))
            femConfigGroup.setInputSubsectorsShapefile(str);
    }
}
