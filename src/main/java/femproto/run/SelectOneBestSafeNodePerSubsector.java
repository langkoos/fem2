package femproto.run;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SelectOneBestSafeNodePerSubsector implements StartupListener,
											    ReplanningListener
//	IterationEndsListener
{
	private static final Logger log = Logger.getLogger( SelectOneBestSafeNodePerSubsector.class ) ;
	
	private static final Random rnd = MatsimRandom.getLocalInstance() ;
	
	@Inject Population population ;
	@Inject Config config ;
	@Override public void notifyReplanning( ReplanningEvent event) {
//	@Override public void notifyIterationEnds(IterationEndsEvent event) {
		
		Table<String,Id<Link>,Double> cnts = HashBasedTable.create() ;
		Table<String,Id<Link>,Double> sums = HashBasedTable.create() ;

		// go through all agents and memorize, for each subsector starting point, the score for each
		// safe node destination:
		for ( Person person : population.getPersons().values() ) {
			
			// origin comes from the attributes (could, in theory, be persons on different links):
			String subsector = FEMUtils.getSubsectorName(person);

			// destination must come from plan, since in the attributes there are multiple safe nodes and
			// we don't know which one was used:
			for ( Plan plan : person.getPlans() ) {
				if ( plan.getScore() != null ) {
					Id<Link> destinationLinkId = getDestinationLinkId(plan);
					
					final Double cnt = cnts.get(subsector, destinationLinkId);
					if (cnt == null) {
						cnts.put(subsector, destinationLinkId, 1.);
					} else {
						cnts.put(subsector, destinationLinkId, cnt + 1);
					}
					
					final Double sum = sums.get(subsector, destinationLinkId);
					if (sum == null) {
						sums.put(subsector, destinationLinkId, plan.getScore());
					} else {
						sums.put(subsector, destinationLinkId, sum + plan.getScore());
					}
				}
			}
		}
		
		// find for each subsector the preferred destination:
		Map<String,Id<Link>> safeLinkIds = new HashMap<>() ;
		
		int cnt2 = 10 ;
		
		for (Map.Entry<String, Map<Id<Link>, Double>> entry : cnts.rowMap().entrySet() ) {
			// for each subsector:
			String subsector = entry.getKey();
			final Map<Id<Link>, Double> cntsRow = entry.getValue();
			
			if ( cnt2>0 ) {
				log.info( "subsector=" + subsector );
			}
			
			Id<Link> linkId = null;
			
			if ( rnd.nextDouble() < 0.9 || event.getIteration() > 0.9 * config.controler().getLastIteration() ) {
				// yy two parameters that are quite ad-hoc!  kai, aug'18
				
				Map<Id<Link>, Double> sumsRow = sums.row(subsector);;
				double max = Double.NEGATIVE_INFINITY ;
				for (Map.Entry<Id<Link>, Double> entry2 : cntsRow.entrySet()) {
					// go through each possible safe node and find best score:
					final Id<Link> aLinkId = entry2.getKey();
					final Double sum = sumsRow.get(aLinkId);
					final Double cnt = entry2.getValue();
					Double val = sum / cnt;
					if ( cnt2 > 0 ) {
						log.info( "linkId=" + aLinkId + "; score av=" + val );
						if ( val == 1. ) {
							log.info( "sum=" + sum + "; cnt=" + cnt );
						}
					}
					if (val > max) {
						max = val;
						linkId = aLinkId;
					}
				}
				Gbl.assertNotNull(linkId);
			} else {
				// pick random destination:
				List<Id<Link>> list = new ArrayList<>( cntsRow.keySet() ) ;
				int index = MatsimRandom.getRandom().nextInt( list.size() ) ;
				linkId = list.get(index) ;
			}
			safeLinkIds.put(subsector, linkId);
			
			if ( cnt2>0 ) {
				log.info( "selectedLinkId=" + linkId );
				log.info( "" );
			}
			cnt2-- ;
			if ( cnt2 == 0 ) {
				log.info( Gbl.FUTURE_SUPPRESSED );
			}
		}
		
		// select for each person the plan we want:
		RandomUnscoredPlanSelector<Plan, Person> selector = new RandomUnscoredPlanSelector<>();

		for ( Person person : population.getPersons().values() ) {
			// first find if there is still an unscored plan, and if so use that:
			;
			Plan unscoredPlan = selector.selectPlan(person);
			;
			if (unscoredPlan != null) {
				person.setSelectedPlan(unscoredPlan);
			} else {
				// else, set safe node as computed above:
				String subsector = FEMUtils.getSubsectorName(person);
				Id<Link> newDestinationLinkId = safeLinkIds.get(subsector);
				;
				Gbl.assertNotNull(subsector);
				for (Plan plan : person.getPlans()) {
					if (getDestinationLinkId(plan).equals(newDestinationLinkId)) {
						person.setSelectedPlan(plan);
						break;
					}
				}
			}
		}
		
		int cnt = 10 ;
		
		Map< Id<Link>, Id<Link> > memDestLink = new HashMap<>() ;
		Map< Id<Link>, String > memSubsector = new HashMap<>() ;
		for ( Person person : population.getPersons().values() ) {
			Plan plan = person.getSelectedPlan() ;
			Id<Link> originLinkId = PopulationUtils.getFirstActivity(plan).getLinkId();
			Id<Link> destLinkId = PopulationUtils.getLastActivity(plan).getLinkId();
			final Id<Link> memorizedLinkId = memDestLink.get(originLinkId);
			if ( memorizedLinkId==null ) {
				// memorizing destLinkId and subsector that are connected to this originLinkId:
				memDestLink.put( originLinkId, destLinkId );
				memSubsector.put( originLinkId, FEMUtils.getSubsectorName(person) ) ;
			} else if ( !memorizedLinkId.equals( destLinkId ) && cnt > 0 ) {
				// (that is we have seen another destLinkId assigned to the same origLinkId)
				
				// writing this to the console:
				log.info( "originLinkId=" + originLinkId + "; subsector=" + memSubsector.get( originLinkId ) + "; dest1=" + memorizedLinkId )  ;
				log.info( "originLinkId=" + originLinkId + "; subsector=" + FEMUtils.getSubsectorName(person)  + "; dest2=" + destLinkId ) ;
				// note that this can happen during the first 10 or so iterations when agents are still just selecting
				// unscored plans. kai, jul'18
				
				cnt-- ;
				if ( cnt==0 ) {
					log.warn( Gbl.FUTURE_SUPPRESSED ) ;
				}
			}
		}

	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		// remember to set strategy to "keepSelected"
//		Gbl.assertIf( config.strategy().getStrategySettings().size()==1 ) ;
//		for ( StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings() ) {
//			Gbl.assertIf( settings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected ) );
//		}
	}
	
	static Id<Link> getDestinationLinkId(Plan plan) {
		return PopulationUtils.getLastActivity(plan).getLinkId();
	}
	

}
