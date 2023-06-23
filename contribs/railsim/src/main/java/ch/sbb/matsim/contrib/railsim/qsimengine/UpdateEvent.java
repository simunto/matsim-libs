package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.Objects;

/**
 * Internal event that describes, when a {@link TrainState} is supposed to be updated.
 */
final class UpdateEvent implements Comparable<UpdateEvent> {

	final TrainState state;
	double plannedTime;
	Type type;

	/**
	 * Whether train is waiting on the very link end for the next to be unblocked.
	 */
	boolean waitingForLink;

	/**
	 * Currently waiting for reservation on some link.
	 */
	boolean waitingForReservation;

	/**
	 * Stores a link that is should to be released.
	 */
	final RailLink unblockLink;

	public UpdateEvent(TrainState state, Type type) {
		this.state = state;
		this.plannedTime = state.timestamp;
		this.type = type;
		this.waitingForLink = false;
		this.unblockLink = null;
	}

	public UpdateEvent(TrainState state, RailLink unblockLink, double time) {
		this.state = state;
		this.unblockLink = unblockLink;
		this.plannedTime = time + unblockLink.minimumHeadwayTime;
		this.type = Type.UNBLOCK_LINK;
	}

	@Override
	public int compareTo(UpdateEvent o) {
		int compare = Double.compare(plannedTime, o.plannedTime);

		if (compare == 0)
			return state.driver.getId().compareTo(o.state.driver.getId());

		return compare;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UpdateEvent that = (UpdateEvent) o;
		return Objects.equals(state, that.state);
	}

	@Override
	public int hashCode() {
		return Objects.hash(state);
	}

	/**
	 * This train currently waits for an reservation for blocked tracks.
	 */
	boolean isAwaitingReservation() {
		return waitingForReservation;
	}

	/**
	 * The type of the requested update.
	 */
	enum Type {

		IDLE,
		DEPARTURE,
		POSITION,
		ENTER_LINK,
		LEAVE_LINK,
		RELEASE_TRACK,
		BLOCK_TRACK,
		SPEED_CHANGE,
		UNBLOCK_LINK

	}

}
