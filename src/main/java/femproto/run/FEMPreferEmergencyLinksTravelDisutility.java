/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyTimeDependentTravelCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package femproto.run;

import com.google.inject.Inject;
import femproto.prepare.network.NetworkConverter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 *  A Travel Cost Calculator that uses the travel times as travel disutility.
 *
 * @author cdobler
 */
public final class FEMPreferEmergencyLinksTravelDisutility implements TravelDisutility {
	private static final Logger log = Logger.getLogger(FEMPreferEmergencyLinksTravelDisutility.class) ;
	
	private final Map<Id<Link>, Double> specialLinks;
	private final TravelDisutility delegate;
	private final TravelTime travelTime;
	
	private FEMPreferEmergencyLinksTravelDisutility(final TravelTime travelTime, Map<Id<Link>, Double> specialLinks, TravelDisutility delegate) {
//		log.setLevel(Level.DEBUG);
		this.travelTime = travelTime ;
		this.specialLinks = specialLinks;
		this.delegate = delegate;
		Gbl.assertNotNull(travelTime);
	}
	
	public double getAdditionalLinkTravelDisutility( final Link link, final double time, final Person person, final Vehicle vehicle ) {
		Double result = specialLinks.get( link.getId() ) ;
		if ( result != null ) { // found special link, in this case evac link, do not penalize
			return 0. ;
		} else { // non-evac link --> penalize
			return 100000. * delegate.getLinkTravelDisutility(link,time,person,vehicle) ;
		}
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		return delegate.getLinkTravelDisutility(link,time,person,vehicle) + getAdditionalLinkTravelDisutility(link,time,person,vehicle) ;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return delegate.getLinkMinimumTravelDisutility(link) +
					   getAdditionalLinkTravelDisutility(link,Time.getUndefinedTime(), null, null ) ;
	}
	
	public static final class Factory implements TravelDisutilityFactory {
		private final Map<Id<Link>, Double> specialLinks = new LinkedHashMap<>() ;
		private final TravelDisutilityFactory delegateFactory;
		//yoyoyo a quick fix
		@Inject
		private Factory(Network network, TollTimeDistanceTravelDisutilityFactory delegateFactory ) {
			for( Link link : network.getLinks().values() ) {
				boolean isEvacLink = isEvacLink(link);
				if ( isEvacLink ) {
					specialLinks.put( link.getId(), 0.01 ) ;
				}
			}
			this.delegateFactory = delegateFactory ;
		}
		public Factory(Network network, TravelDisutilityFactory delegateFactory ) {
			for( Link link : network.getLinks().values() ) {
				boolean isEvacLink = isEvacLink(link);
				if ( isEvacLink ) {
					specialLinks.put( link.getId(), 0.01 ) ;
				}
			}
			this.delegateFactory = delegateFactory ;
		}
		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
			TravelDisutility delegate = null ;
			if ( delegateFactory!=null ) {
				try {
					delegate = delegateFactory.createTravelDisutility(timeCalculator);
				}catch (NullPointerException ne){
					delegate = new OnlyTimeDependentTravelDisutilityFactory().createTravelDisutility(timeCalculator);
				}
			}
			final FEMPreferEmergencyLinksTravelDisutility femPreferEmergencyLinksTravelDisutility =
					new FEMPreferEmergencyLinksTravelDisutility(timeCalculator, specialLinks, delegate);
			return femPreferEmergencyLinksTravelDisutility;
		}
	}
	
	public static boolean isEvacLink(Link link) {
		if ( link==null ) {
			return false ;
		}
		return (boolean) link.getAttributes().getAttribute(FEMUtils.getGlobalConfig().getAttribEvacMarker());
	}
	
}
