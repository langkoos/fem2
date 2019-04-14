package femproto.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

public class WriteSelectedPlanOnly {
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new PopulationReader(scenario).readFile(args[0]);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			List<Plan> plans = new ArrayList<>();
			plans.addAll(person.getPlans());
			for (Plan plan : plans) {
				if (!plan.equals(selectedPlan))
					person.removePlan(plan);
			}
		}

		new PopulationWriter(scenario.getPopulation()).write(args[1]);

	}
}
