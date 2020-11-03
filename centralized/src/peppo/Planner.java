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


/**
 * A centralised planner using an epsilon-greedy (with decreasing epsilon) stochastic local search.
 */
class Planner {
	private static final int ITERSTOLOG = 1;
	private static final Level LOGLEVELPLAN = Level.INFO;
	private static final Level LOGLEVELSOL = Level.OFF;
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
		this.logger.setLevel(LOGLEVELPLAN);
	}


	/**
	 * Implements an epsilon-greedy (with decreasing epsilon) SLS.
	 * @return the best joint plan found.
	 */
	List<Plan> plan(){
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0L;
		Solution currentSolution = new Solution(vehicles, tasks);
		Solution bestSolution = currentSolution;
		double epsilon;		// The probability to move to a random neighbour
		
		for(int nIter = 1; elapsedTime < timeout; nIter++) {
			// Do not log all iterations
			if(nIter % ITERSTOLOG == 0) {
				logger.info("Iteration " + nIter + ": elapsed time = " + elapsedTime + 	", current cost = " + 
						currentSolution.getCost() + ", best cost = " + bestSolution.getCost());
				
				//currentSolution.checkIntegrity();
				
				Solution.logger.setLevel(LOGLEVELSOL);
			} else {
				Solution.logger.setLevel(Level.OFF);
			}
			
			// Decrease epsilon over time, from a certain point in time
			epsilon = Math.min(epsThresh, epsRate/nIter);
			
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
			
			// Update bestSolution, if necessary
			if(currentSolution.getCost() < bestSolution.getCost()) {
				bestSolution = currentSolution;
			}
			
			elapsedTime = System.currentTimeMillis() - startTime;
		}
		
		return bestSolution.getJointPlan();
	}
	
}
