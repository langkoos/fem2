package femproto.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;

import java.util.HashMap;
import java.util.Map;

public class SubsectorLeadAgents implements ReplanningListener {
	@Inject
	Scenario scenario;

	public Map<String, Person> getLeaders() {
		return leaders;
	}

	private Map<String,Person> leaders = new HashMap<>();

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if(person.getAttributes().getAttribute(scenario.getConfig().plans().getSubpopulationAttributeName()).equals("leader")){
				leaders.put(person.getAttributes().getAttribute(FEMUtils.getGlobalConfig().getAttribSubsector()).toString(),person);
			}
		}

	}
}
