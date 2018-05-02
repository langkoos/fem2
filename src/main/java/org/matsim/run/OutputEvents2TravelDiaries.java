package org.matsim.run;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.travelsummary.events2traveldiaries.EventsToTravelDiaries;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.io.IOException;
@Singleton
public class OutputEvents2TravelDiaries implements IterationStartsListener, IterationEndsListener {

	EventsToTravelDiaries delegate;
	@Inject EventsManager eventsManager;
	@Inject Scenario scenario;

	@Inject
	OutputEvents2TravelDiaries(Controler controler) {
		controler.addControlerListener(this);
	}


	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() != scenario.getConfig().controler().getLastIteration())
			return;
		delegate = new EventsToTravelDiaries(scenario);
		eventsManager.addHandler(delegate);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() != scenario.getConfig().controler().getLastIteration())
			return;
		try {
			delegate.writeSimulationResultsToTabSeparated(scenario.getConfig().controler().getOutputDirectory(),"output_");
		} catch (IOException e) {
			Logger.getLogger(this.getClass()).error("Writing of traveldiaries failed.");
			e.printStackTrace();
		}
	}
}
