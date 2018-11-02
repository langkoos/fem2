package femproto.prepare.evacuationscheduling;

import femproto.globals.FEMGlobalConfig;
import femproto.run.FEMUtils;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class EvacuationScheduleFromExperiencedPlansTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;
	{
		if (FEMUtils.getGlobalConfig() == null)
			FEMUtils.setGlobalConfig(FEMGlobalConfig.getDefaultGlobalConfig());
	}
	@Test
	public void test() {
		String networkFile = "test/input/femproto/prepare/evacuationscheduling/output_network.xml.gz";
		String populationFile = "test/input/femproto/prepare/evacuationscheduling/output_plans.xml.gz";
		String experiencedPlansFile = "test/input/femproto/prepare/evacuationscheduling/output_experienced_plans.xml.gz";
		String outputScheduleFile = utils.getOutputDirectory()+"scheduleFromExperiencedPlans.csv";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationFile);

		EvacuationScheduleFromExperiencedPlans evacuationScheduleFromExperiencedPlans = new EvacuationScheduleFromExperiencedPlans(scenario.getPopulation(), network);

		scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(experiencedPlansFile);
		Map<Id<Person>, Plan> plans = new LinkedHashMap<>() ;
		scenario.getPopulation().getPersons().values().forEach( p -> plans.put( p.getId(), p.getSelectedPlan() ) );
		evacuationScheduleFromExperiencedPlans.parseExperiencedPlans(plans,network);

		EvacuationSchedule evacuationSchedule = evacuationScheduleFromExperiencedPlans.createEvacuationSchedule();
		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordComplete(outputScheduleFile);

	}

}
