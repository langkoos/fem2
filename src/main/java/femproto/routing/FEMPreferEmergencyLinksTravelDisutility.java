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

package femproto.routing;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import femproto.network.NetworkConverter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
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
	
	private final TravelTime travelTime;
	
	private FEMPreferEmergencyLinksTravelDisutility(final TravelTime travelTime, Map<Id<Link>, Double> specialLinks) {
//		log.setLevel(Level.DEBUG);
		this.specialLinks = specialLinks;
		Gbl.assertNotNull(travelTime);
		this.travelTime = travelTime;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		Double result = specialLinks.get(link.getId());
		if ( result != null ) {
//			log.debug("found link being special link; link=" + link.getId() + "; factor=" + factor ) ;
			return result * this.travelTime.getLinkTravelTime(link,time,person,vehicle) ;
		} else {
			return 100. + 100. * this.travelTime.getLinkTravelTime(link,time,person,vehicle) ;
		}
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		Double result = specialLinks.get(link.getId());;
		if ( result != null ) {
//			log.debug("found link being special link; link=" + link.getId() + "; factor=" + factor ) ;
			return result * this.travelTime.getLinkTravelTime(link,Time.getUndefinedTime(),null,null) ;
		} else {
			return 100. + 100. * this.travelTime.getLinkTravelTime(link,Time.getUndefinedTime(), null, null ) ;
		}
	}
	
	public static final class Factory implements TravelDisutilityFactory {
		private final Map<Id<Link>, Double> specialLinks = new LinkedHashMap<>() ;
		
		public Factory(Network network) {
			for( Link link : network.getLinks().values() ) {
				boolean isEvacLink = (boolean) link.getAttributes().getAttribute(NetworkConverter.EVACUATION_LINK);
				if ( isEvacLink ) {
					specialLinks.put( link.getId(), 0.01 ) ;
				}
			}
		}
		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
			return new FEMPreferEmergencyLinksTravelDisutility(timeCalculator, specialLinks);
		}
	}
	
}
