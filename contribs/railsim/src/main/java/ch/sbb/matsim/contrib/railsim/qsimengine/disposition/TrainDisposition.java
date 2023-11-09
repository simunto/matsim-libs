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

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Disposition, handling route and track reservations.
 */
public interface TrainDisposition {

	/**
	 * Method invoked when a train is departing.
	 */
	void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route);

	/**
	 * Request the next segment to be reserved.
	 * @param time current time
	 * @param position position information
	 * @param segment links of the segment that should be blocked
	 */
	default DispositionResponse requestNextSegment(double time, TrainPosition position, List<RailLink> segment) {
		return null;
	}


	/**
	 * Inform the resource manager that the train has passed a link that can now be unblocked.
	 * This needs to be called after track states have been updated already.
	 */
	void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link);

}
