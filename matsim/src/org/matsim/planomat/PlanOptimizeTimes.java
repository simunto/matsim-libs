/* *********************************************************************** *
 * project: org.matsim.*
 * PlanOptimizeTimes.java
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

package org.matsim.planomat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.config.groups.PlanomatConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.scoring.ScoringFunction;
import org.matsim.utils.misc.Time;

/**
 * The "heart" of the planomat external strategy module:
 * Optimize a the departure times and activity durations
 * of a given <code>Plan</code>
 * <ul>
 * <li> according to a <code>ScoringFunction</code>
 * <li> with respect to time-of-day dependent travel costs as perceived
 *   by a <code>LegtravelTimeEstimator</code>.
 * </ul>
 * @author meisterk
 *
 */
public class PlanOptimizeTimes implements PlanAlgorithm {

	private LegTravelTimeEstimator legTravelTimeEstimator = null;

	private Random seedGenerator = null;
	
	public PlanOptimizeTimes(final LegTravelTimeEstimator legTravelTimeEstimator) {

		this.legTravelTimeEstimator = legTravelTimeEstimator;
		this.seedGenerator = MatsimRandom.getLocalInstance();
		
	}

	public void run(final Plan plan) {

		// distinguish for optimization tools
		String optiToolboxName = Gbl.getConfig().planomat().getOptimizationToolbox();
		if (optiToolboxName.equals(PlanomatConfigGroup.OPTIMIZATION_TOOLBOX_JGAP)) {

			// analyze plan: how many activities and subtours do we have?
			PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
			planAnalyzeSubtours.run(plan);

			org.jgap.Configuration jgapConfiguration = this.initJGAPConfiguration();

			org.jgap.Configuration.reset();
			
			IChromosome sampleChromosome = this.initSampleChromosome(planAnalyzeSubtours, jgapConfiguration);
			try {
				jgapConfiguration.setSampleChromosome(sampleChromosome);
			} catch (InvalidConfigurationException e1) {
				e1.printStackTrace();
			}
			ScoringFunction sf = Gbl.getConfig().planomat().getScoringFunctionFactory().getNewScoringFunction(plan);
			PlanomatFitnessFunctionWrapper fitnessFunction = new PlanomatFitnessFunctionWrapper( 
					sf, 
					plan, 
					this.legTravelTimeEstimator, 
					planAnalyzeSubtours );
			Genotype population = null;
			try {
				jgapConfiguration.setFitnessFunction( fitnessFunction );
				population = Genotype.randomInitialGenotype( jgapConfiguration );
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
			population.evolve( Gbl.getConfig().planomat().getJgapMaxGenerations() );
			IChromosome fittest = population.getFittestChromosome();
			this.writeChromosome2Plan(fittest, plan, planAnalyzeSubtours );

		}
	}

	protected org.jgap.Configuration initJGAPConfiguration() {

		DefaultConfiguration jgapConfiguration = new DefaultConfiguration();

		try {
			// the following settings are copied from org.jgap.DefaultConfiguration
			// TODO configuration shouldnt be inited for every plan, but once
			// but currently do not know how to deal with threads because cloning doesn't work because jgap.impl.configuration writes System.Properties
			Configuration.reset();

			// JGAP random number generator is initialized for each run
			// but use a random number as seed so every run will draw a different, but deterministic sequence of random numbers
			long seed = this.seedGenerator.nextLong();
			//System.out.println("Seed: " + Long.toString(seed));
			((StockRandomGenerator) jgapConfiguration.getRandomGenerator()).setSeed( seed );
			
			// elitist selection (DeJong, 1975)
			jgapConfiguration.setPreservFittestIndividual(true);
			jgapConfiguration.setPopulationSize( Gbl.getConfig().planomat().getPopSize() );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return jgapConfiguration;

	}

	protected IChromosome initSampleChromosome(final PlanAnalyzeSubtours planAnalyzeSubtours, final org.jgap.Configuration jgapConfiguration) {

		double planLength = 24.0 * 3600;

		ArrayList<Gene> sampleGenes = new ArrayList<Gene>();
		try {

			for (int ii=0; ii < planAnalyzeSubtours.getSubtourIndexation().length; ii++) {
				sampleGenes.add(new DoubleGene(jgapConfiguration, 0.0, planLength));
			}

			for (int ii=0; ii < planAnalyzeSubtours.getNumSubtours(); ii++) {
				sampleGenes.add(new IntegerGene(jgapConfiguration, 0, Gbl.getConfig().planomat().getPossibleModes().size() - 1));
			} 

		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		IChromosome sampleChromosome = null;
		try {
			sampleChromosome = new Chromosome( jgapConfiguration, sampleGenes.toArray(new Gene[sampleGenes.size()]) );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return sampleChromosome;

	}

	/**
	 * Writes a JGAP chromosome back to matsim plan object.
	 *
	 * @param individual the GA individual (usually the fittest after evolution) whose values will be written back to a plan object
	 * @param plan the plan that will be altered
	 */
	protected void writeChromosome2Plan(
			final IChromosome individual, 
			final Plan plan, 
			final PlanAnalyzeSubtours planAnalyzeSubtours ) {

		Act activity = null;
		Leg leg = null;

		HashMap<Leg, Route> originalRoutes = PlanOptimizeTimes.getLegsRoutes(plan);

		Gene[] fittestGenes = individual.getGenes();
//		for (Gene gene: fittestGenes) {
//			System.out.println(gene.getAllele().toString());
//		}

		int max = plan.getActsLegs().size();
		double now = 0.0;

		for (int ii = 0; ii < max; ii++) {

			Object o = plan.getActsLegs().get(ii);

			if (o.getClass().equals(Act.class)) {

				activity = ((Act) o);

				// handle first activity and middle activities
				if (ii < (max - 1)) {

					activity.setStartTime(now);
					activity.setDur(((DoubleGene) fittestGenes[ii / 2]).doubleValue());
					now += activity.getDur();
					activity.setEndTime(now);

					// handle last activity
				} else if (ii == (max - 1)) {

					// assume that there will be no delay between arrival time and activity start time
					activity.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					activity.setDur(Time.UNDEFINED_TIME);
					activity.setEndTime(Time.UNDEFINED_TIME);

				}

			} else if (o.getClass().equals(Leg.class)) {

				leg = ((Leg) o);

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepTime(now);

				// set mode to result from optimization
				int subtourIndex = planAnalyzeSubtours.getSubtourIndexation()[ii / 2];
				int modeIndex = ((IntegerGene) individual.getGene(planAnalyzeSubtours.getSubtourIndexation().length + subtourIndex)).intValue();
				Mode mode = Gbl.getConfig().planomat().getPossibleModes().get(modeIndex);
//				System.out.println(ii + "\t" + subtourIndex + "\t" + modeIndex + "\t" + modeName);
				leg.setMode(mode);

				// set arrival time to estimation
				Act origin = ((Act) plan.getActsLegs().get(ii - 1));
				Act destination = ((Act) plan.getActsLegs().get(ii + 1));

				double travelTimeEstimation = this.legTravelTimeEstimator.getLegTravelTimeEstimation(
						plan.getPerson().getId(),
						now,
						origin,
						destination,
						leg);

				leg.setTravTime(travelTimeEstimation);
				
				// correctly set route object
				// restore original routes, because planomat must not alter routes at all
				leg.setRoute(originalRoutes.get(leg));
				leg.getRoute().setTravTime(travelTimeEstimation);

				now += leg.getTravTime();
				// set planned arrival time accordingly
				leg.setArrTime(now);

			}
		}

		// invalidate score information
		plan.setScore(Double.NaN);

	}

	public static HashMap<Leg, Route> getLegsRoutes(final Plan plan) {
		
		HashMap<Leg, Route> routes = new HashMap<Leg, Route>();
		
		LegIterator legIterator = plan.getIteratorLeg();
		while (legIterator.hasNext()) {
			Leg curLeg = (Leg) legIterator.next();
			routes.put(curLeg, new Route(curLeg.getRoute()));
		}
		
		return routes;
		
	}
	
}