package femproto.run;

import femproto.globals.FEMGlobalConfig;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

/**
 * a utility to write out the latest default config format
 */
public class DefaultConfigWriter {
	public static void main(String[] args) {
		if (args.length > 1)
			transformDefaultConfig(args);
		else
			newDefaultConfig(args[0]);
	}

	private static void newDefaultConfig(String arg) {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		new ConfigWriter(config, ConfigWriter.Verbosity.minimal).write(arg);
	}

	private static void transformDefaultConfig(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		ConfigUtils.addOrGetModule(config, FEMConfigGroup.class);
		ConfigUtils.addOrGetModule(config, FEMGlobalConfig.class);
		new ConfigWriter(config, ConfigWriter.Verbosity.minimal).write(args[1]);
	}
}
