package ch.sbb.matsim.contrib.railsim.qsimengine;

/**
 * Marker interface for an objective that is either a link or a resource.
 */
public interface RailLinkOrResource {

	// TODO: maybe not necessary, probably could rely only on links

	boolean hasCapacity();

}
