package femproto.prepare.evacuationscheduling;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

public class EvacuationScheduleFromExperiencedPlansTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;
	@Test
	public void test() throws CsvRequiredFieldEmptyException, IOException, CsvDataTypeMismatchException {
		String networkFile = "test/input/femproto/prepare/evacuationscheduling/output_network.xml.gz";
		String populationFile = "test/input/femproto/prepare/evacuationscheduling/output_plans.xml.gz";
		String experiencedPlansFile = "test/input/femproto/prepare/evacuationscheduling/output_experienced_plans.xml.gz";
		String outputScheduleFile = utils.getOutputDirectory()+"scheduleFromExperiencedPlans.csv";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		EvacuationScheduleFromExperiencedPlans evacuationScheduleFromExperiencedPlans = new EvacuationScheduleFromExperiencedPlans();

		Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationFile);
		evacuationScheduleFromExperiencedPlans.personToSubsectorCollection(scenario.getPopulation());

		scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(experiencedPlansFile);
		evacuationScheduleFromExperiencedPlans.parseExperiencedPlans(scenario.getPopulation(),network);

		EvacuationSchedule evacuationSchedule = evacuationScheduleFromExperiencedPlans.createEvacuationSchedule();
		new EvacuationScheduleWriter(evacuationSchedule).writeEvacuationScheduleRecordComplete(outputScheduleFile);

	}

}