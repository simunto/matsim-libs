/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimCalc;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import jakarta.inject.Inject;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * Simple disposition without deadlock avoidance.
 */
public class SimpleDisposition implements TrainDisposition {

	private final RailResourceManager resources;
	private final TrainRouter router;

	@Inject
	public SimpleDisposition(RailResourceManager resources, TrainRouter router) {
		this.resources = resources;
		this.router = router;
	}

	@Override
	public void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route) {
		// Nothing to do.
	}

	@Override
	public DispositionResponse requestNextSegment(double time, TrainPosition position, double dist) {

		RailLink currentLink = resources.getLink(position.getHeadLink());
		List<RailLink> segment = RailsimCalc.calcLinksToBlock(position, currentLink, dist);

		// Only re-routes if the link segment is occupied
//		for (RailLink link : segment) {
//			if (!resources.isBlockedBy(link, driver) && !resources.hasCapacity(link.getLinkId()))
//				return router.calcRoute(entry, exit);
//		}

		// This code was in rail engine, now needs to be part of disposition
		/*
			if (state.pt != null && RailsimCalc.considerReRouting(links, resources.getLink(state.headLink))) {

			int start = -1;
			int end = -1;
			RailLink entry = null;
			RailLink exit = null;

			for (int i = Math.max(0, state.routeIdx - 1); i < state.route.size(); i++) {
				RailLink l = state.route.get(i);

				if (l.isEntryLink()) {
					entry = l;
					start = i;
				} else if (start > -1 && l.isBlockedBy(state.driver)) {
					// check if any link beyond entry is already blocked
					// if that is the case re-route is not possible anymore
					break;
				} else if (start > -1 && l.isExitLink()) {
					exit = l;
					end = i;
					break;
				}
			}

			// there might be no exit link if this is the end of the route
			// exit will be set to null if re-route is too late
			// network could be wrong as well, but hard to verify
			if (exit != null) {
				....
			}
		 */

		// TODO: for moving block the logic might needs to be changed
		double reserveDist = resources.tryBlockLink(time, currentLink, RailResourceManager.ANY_TRACK, position);

		if (reserveDist == RailResource.NO_RESERVATION)
			return new DispositionResponse(0, 0, null);


		// TODO: loop and check of first link might be merged into one

		boolean stop = false;
		// Iterate all links that need to be blocked
		for (RailLink link : segment) {

			// first link does not need to be blocked again
			if (link == currentLink)
				continue;

			dist = resources.tryBlockLink(time, link, RailResourceManager.ANY_TRACK, position);

			if (dist == RailResource.NO_RESERVATION) {
				stop = true;
				break;
			}

			// partial reservation
			reserveDist += dist;

			// If the link is not fully reserved then stop
			// TODO: there might be a better advised speed
			if (dist < link.getLength()) {
				stop = true;
				break;
			}
		}

		return new DispositionResponse(reserveDist, stop ? 0 : Double.POSITIVE_INFINITY, null);
	}

	@Override
	public void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link) {

		// put resource handling into release track
		resources.releaseLink(time, link, driver);
	}
}
