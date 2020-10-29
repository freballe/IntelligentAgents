package peppo;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

class Planner {
	private static final int ITERSTOLOG = 1000;
	private static final Level LOGLEVEL = Level.INFO;
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
		
		this.coin = new Random();
		
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
		Solution currentSolution = Solution.randomInitialSolution();
		Solution bestSolution = currentSolution;
		double epsilon;		// The probability to move to a random neighbour
		
		for(int nIter = 1; elapsedTime < timeout; nIter++) {
			// Do not log all iterations
			if(nIter % ITERSTOLOG == 0) {
				logger.info("Iteration " + nIter + ": elapsed time = " + elapsedTime + 	", current cost = " + 
						currentSolution.getCost() + ", best cost = " + bestSolution.getCost());
			}
			
			// Decrease epsilon over time, from a certain point in time
			epsilon = Math.min(epsThresh, epsRate/nIter);
			
			// Toss a coin
			if(coin.nextDouble() < epsilon) {
				// With probability epsilon, move to random neighbour
				currentSolution = currentSolution.getRandomNeighbour();
			} 
			else {
				// Otherwise, move to best neighbour
				currentSolution = currentSolution.getBestNeighbour();
			}
			
			// Update bestSolution, if necessary
			if(currentSolution.getCost() < bestSolution.getCost()) {
				bestSolution = currentSolution;
			}
			
			elapsedTime = System.currentTimeMillis() - startTime;
		}
		
		return bestSolution.getJointPlan();
	}
	
}
