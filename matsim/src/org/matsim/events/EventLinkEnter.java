/* *********************************************************************** *
 * project: org.matsim.*
 * EventLinkEnter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.events;

import org.matsim.network.Link;
import org.matsim.plans.Person;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class EventLinkEnter extends LinkEvent {

	public EventLinkEnter(final double time, final String agentId, final int legNumber, final String linkId, final Person agent, final Link link) {
		super(time, agentId, linkId, agent, legNumber, link);
	}

	public EventLinkEnter(final double time, final String agentId, final int legNumber, final String linkId) {
		super(time, agentId, legNumber, linkId);
	}

	@Override
	public Attributes getAttributes() {
		AttributesImpl impl = getAttributesImpl();
		//impl.addAttribute("","","Flag", "", Integer.toString(5));
		impl.addAttribute("","","type", "", "entered link");
		return impl;
	}

	@Override
	public String toString() {
		return asString() + "5\tentered link";
	}

}
