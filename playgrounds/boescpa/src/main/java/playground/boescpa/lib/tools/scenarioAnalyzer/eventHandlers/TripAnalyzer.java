/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import playground.boescpa.lib.tools.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.lib.tools.scenarioAnalyzer.spatialEventCutters.SpatialEventCutter;
import playground.boescpa.lib.tools.tripCreation.TripHandler;
import playground.boescpa.lib.tools.tripCreation.TripProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Decorates playground.boescpa.lib.tools.tripCreation.TripHandler with the method createResults(..).
 *
 * Returns for every mode:
 *	o	Number of trips []
 *	o	Total distance travelled [km]
 *	o	Mean and variance of distances travelled [km]
 *	o	Total duration [min]
 *	o	Mean and variance of durations [min]
 *
 * @author boescpa
 */
public class TripAnalyzer implements ScenarioAnalyzerEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, PersonStuckEventHandler, LinkLeaveEventHandler {

	private final TripHandler tripHandler;
	private final Network network;
	private Map<String, ModeResult> modes = new HashMap<>();
	private Map<String, ActivityResult> activities = new HashMap<>();

	public TripAnalyzer(Network network) {
		this.tripHandler = new TripHandler();
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.tripHandler.reset(0);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.tripHandler.handleEvent(event);
	}

	/**
	 * * Returns for every mode:
	 *	o	Number of trips []
	 *	o	Total distance travelled [km]
	 *	o	Mean and variance of distances travelled [km]
	 *	o	Total duration [min]
	 *	o	Mean and variance of durations [min]
	 *
	 * @param spatialEventCutter
	 * @return A multiline String containing the above listed results.
	 */
	@Override
	public String createResults(SpatialEventCutter spatialEventCutter) {
		analyzeEvents(spatialEventCutter);
		String results = getTripResults();
		results += ScenarioAnalyzer.NL;
		results += getActivityResults();
		return results;
	}

	private String getTripResults() {
		String results = "Mode; NumberOfTrips; TotalDistance; MeanDistance; VarianceDistance; TotalDuration; MeanDuration; Variance Duration" + ScenarioAnalyzer.NL;
		for (String mode : modes.keySet()) {
			Double[] modeVals = modes.get(mode).getModeVals();
			results += mode + ScenarioAnalyzer.DEL;
			for (int i = 0; i < (modeVals.length - 1); i++) {
				results += modeVals[i] + ScenarioAnalyzer.DEL;
			}
			results += modeVals[modeVals.length - 1] + ScenarioAnalyzer.NL;
		}
		return results;
	}

	private String getActivityResults() {
		String results = "Activity; NumberOfExecutions; TotalDuration; MeanDuration; Variance Duration" + ScenarioAnalyzer.NL;
		for (String activity : activities.keySet()) {
			Double[] actVals = activities.get(activity).getActVals();
			results += activity + ScenarioAnalyzer.DEL;
			for (int i = 0; i < (actVals.length - 1); i++) {
				results += actVals[i] + ScenarioAnalyzer.DEL;
			}
			results += actVals[actVals.length - 1] + ScenarioAnalyzer.NL;
		}
		return results;
	}

	private void analyzeEvents(SpatialEventCutter spatialEventCutter) {
		for (Id personId : tripHandler.getStartLink().keySet()) {
			if (!personId.toString().contains("pt")) {
				ArrayList<Id> startLinks = tripHandler.getStartLink().getValues(personId);
				ArrayList<String> modes = tripHandler.getMode().getValues(personId);
				ArrayList<String> purposes = tripHandler.getPurpose().getValues(personId);
				ArrayList<Double> startTimes = tripHandler.getStartTime().getValues(personId);
				ArrayList<Id> endLinks = tripHandler.getEndLink().getValues(personId);
				ArrayList<Double> endTimes = tripHandler.getEndTime().getValues(personId);
				ArrayList<LinkedList<Id>> pathList = tripHandler.getPath().getValues(personId);

				// todo-boescpa: Account for possibility that there is no endLink...

				// Trip analysis:
				for (int i = 0; i < startLinks.size(); i++) {
					// todo-boescpa: Add spatial restriction...
					ModeResult modeVals = getMode(modes.get(i));
					modeVals.modeDistances.add((double)TripProcessor.calcTravelDistance(pathList.get(i), network, startLinks.get(i), endLinks.get(i)));
					modeVals.modeDurations.add(TripProcessor.calcTravelTime(startTimes.get(i),endTimes.get(i)));
				}

				// Activity analysis:
				// todo-boescpa: Add spatial restriction...
				ActivityResult actVals = getActivity("home");
				actVals.actDurations.add(startTimes.get(0));
				for (int i = 1; i < startTimes.size(); i++) {
					actVals = getActivity(purposes.get(i-1));
					actVals.actDurations.add(startTimes.get(i) - endTimes.get(i-1));
				}
				if (endTimes.get(endTimes.size()-1) < 24*60) {
					actVals = getActivity("home");
					actVals.actDurations.add(24*60 - endTimes.get(endTimes.size()-1));
				}
			}
		}
	}

	private ModeResult getMode(String mode) {
		ModeResult modeResult = this.modes.get(mode);
		if (modeResult == null) {
			modeResult = new ModeResult();
			this.modes.put(mode, modeResult);
		}
		return modeResult;
	}

	private class ModeResult {
		public ArrayList<Double> modeDistances = new ArrayList<>();
		public ArrayList<Double> modeDurations = new ArrayList<>();

		/**
		 * @return NumberOfTrips; TotalDistance; MeanDistance; VarianceDistance; TotalDuration; MeanDuration; Variance Duration;
		 */
		public Double[] getModeVals() {
			Double[] modeVals = new Double[7];

			// Number of Trips:
			modeVals[0] = (double)modeDistances.size();

			// Distance [km]: Total, Mean and Variance
			modeVals[1] = total(modeDistances);
			modeVals[2] = modeVals[1]/modeVals[0];
			modeVals[3] = var(modeDistances, modeVals[2]);
			for (int i = 1; i < 4; i++) {
				modeVals[i] /= 1000;
			}

			// Duration [min]: Total, Mean and Variance
			modeVals[4] = total(modeDurations);
			modeVals[5] = modeVals[4]/modeVals[0];
			modeVals[6] = var(modeDurations, modeVals[5]);
			for (int i = 4; i < 7; i++) {
				modeVals[i] /= 60;
			}

			return modeVals;
		}
	}

	private ActivityResult getActivity(String activity) {
		ActivityResult activityResult = this.activities.get(activity);
		if (activityResult == null) {
			activityResult = new ActivityResult();
			this.activities.put(activity, activityResult);
		}
		return activityResult;
	}

	private class ActivityResult {
		public ArrayList<Double> actDurations = new ArrayList<>();

		/**
		 * @return NumberOfExecutions; TotalDuration; MeanDuration; Variance Duration;
		 */
		public Double[] getActVals() {
			Double[] actVals = new Double[4];

			// Number of Trips:
			actVals[0] = (double)actDurations.size();

			// Duration [min]: Total, Mean and Variance
			actVals[1] = total(actDurations);
			actVals[2] = actVals[1]/actVals[0];
			actVals[3] = var(actDurations, actVals[2]);
			for (int i = 1; i < 4; i++) {
				actVals[i] /= 60;
			}

			return actVals;
		}
	}

	private double var(ArrayList<Double> allVals, double mean) {
		double var = 0;
		for (double val : allVals) {
			var += (val-mean)*(val-mean);
		}
		return var/allVals.size();
	}

	private double total(ArrayList<Double> allVals) {
		double total = 0;
		for (double val : allVals) {
			total += val;
		}
		return total;
	}
}