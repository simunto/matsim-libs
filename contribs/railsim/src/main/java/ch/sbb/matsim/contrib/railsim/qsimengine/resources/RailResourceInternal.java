package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

/**
 * Internal rail resource interface, which allows modifying the state.
 * Disposition should only interact with resources via {@link RailResourceManager}.
 */
interface RailResourceInternal extends RailResource {

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean hasCapacity(RailLink link, int track, TrainPosition position);

	/**
	 * The reserved distance on this link for an agent. Returns 0 if the agent has no reservation.
	 * @return the reserved distance, -1 if there is no reservation. A reservation with 0 dist could be possible.
	 */
	double getReservedDist(RailLink link, TrainPosition position);

	/**
	 * Reserves this resource for the given agent.
	 *
	 * @return the reserved distance on this link
	 */
	double reserve(RailLink link, int track, TrainPosition position);

	/**
	 * Releases this resource for the given agent.
	 */
	void release(RailLink link, MobsimDriverAgent driver);

}
