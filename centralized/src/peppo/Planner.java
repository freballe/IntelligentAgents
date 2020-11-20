package peppo;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;


/**
 * A centralised planner using an epsilon-greedy (with decreasing epsilon) stochastic local search.
 */
class Planner {
	private static final int ITERSTOLOG = 10000;
	private static final int NUMRANDOMISE = 200;
	private static final int NUMBEST = 200;
	private static final int ITERSRESET = 300;
	private static final Level LOGLEVEL = Level.ALL;
	private List<Vehicle> vehicles;
	private TaskSet tasks;
	private Random coin;
	private double epsThresh;	// The maximum value of epsilon
	private double epsRate;		// The value of epsilon eventually decreases as epsRate/t
	private long timeout;
	private Logger logger;



	Planner(List<Vehicle> vehicles, TaskSet tasks, double epsThresh, double epsRate, long timeout) {
		super();
		this.vehicles = vehicles;
		this.tasks = tasks;
		this.epsThresh = epsThresh;
		this.epsRate = epsRate;
		this.timeout = timeout;

		this.coin = new Random(42);

		this.logger = Logger.getLogger("affogalagoffa");
		this.logger.setLevel(LOGLEVEL);
	}


	/**
	 * Implements an epsilon-greedy (with decreasing epsilon) SLS.
	 * @return the best joint plan found.
	 */
	List<Plan> plan(){
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0L;
		Solution currentSolution = new Solution(vehicles, tasks);
		Solution bestSolution;
		double epsilon;		// The probability to move to a random neighbour
		
		// Randomise currentSolution
		for(int i = 0; i < NUMRANDOMISE; i++) {
			currentSolution = currentSolution.getRandomNeighbour();
		}
		// Improve currentSolution
		for(int i = 0; i < NUMBEST; i++) {
			currentSolution = currentSolution.getBestNeighbour();
		}
		bestSolution = currentSolution;

		int itersSinceBest = 0;
		for(int nIter = 1; elapsedTime < timeout; nIter++) {
			// Do not log all iterations
			if(nIter % ITERSTOLOG == 0) {
				logger.info("Iteration " + nIter + ": elapsed time = " + elapsedTime + ", timeout = " + timeout
						+ 	", current cost = " + currentSolution.getCost() + ", best cost = " + 
						bestSolution.getCost());
			}

			// If too long since we found the best, reset to best
			if(itersSinceBest >= ITERSRESET) {
				logger.info("Too long since we found bestSolution: resetting current to best");
				currentSolution = bestSolution;
				itersSinceBest = 0;
			}

			/*
			// Decrease epsilon over time, from a certain point in time
			epsilon = Math.min(epsThresh, epsRate/nIter);
			*/
			epsilon = epsThresh;

			// Toss a coin
			if(coin.nextDouble() < epsilon) {
				// With probability epsilon, move to random neighbour
				if(nIter % ITERSTOLOG == 0) {
					logger.info("Vamos a random");
				}
				currentSolution = currentSolution.getRandomNeighbour();
			} 
			else {
				// Otherwise, move to best neighbour
				if(nIter % ITERSTOLOG == 0) {
					logger.info("Vamos ar colosseo");
				}
				currentSolution = currentSolution.getBestNeighbour();
			}

			// Increase itersSinceBest
			itersSinceBest++;

			// Update bestSolution, if necessary
			if(currentSolution.getCost() < bestSolution.getCost()) {
				bestSolution = currentSolution;
				itersSinceBest = 0;
			}

			elapsedTime = System.currentTimeMillis() - startTime;
		}

		logger.info("Finished. elapsed time = " + elapsedTime + 	", current cost = " + 
				currentSolution.getCost() + ", best cost = " + bestSolution.getCost());
		return bestSolution.getJointPlan();
	}

}
