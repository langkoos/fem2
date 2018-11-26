package femproto.run;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction;

/**
 * This represents a one-time penalty for using a higher priority safe node.
 * The agents should favour lower priority nodes unless they cant manage to evacuate in time.
 */
public class SafeNodePriorityPenaliser implements SumScoringFunction.BasicScoring {
	private final Person person;
	private boolean called = false;

	public SafeNodePriorityPenaliser(Person person) {
		this.person = person;
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		if (!called) {
			called = true;
			return -10 * (double) (int) person.getSelectedPlan().getAttributes().getAttribute("priority");
		} else {
			return 0;
		}
	}
}
