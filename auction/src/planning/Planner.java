package planning;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import logist.simulation.Vehicle;
import logist.task.Task;


/**
 * A centralised planner using an epsilon-greedy (with decreasing epsilon) stochastic local search.
 */
public class Planner {
	private static final int ITERSTOLOG = 100000;
	private static final int NUMRANDOMISE = 200;
	private static final int NUMBEST = 200;
	private static final int ITERSRESET = 1000;
	private static final int RESETSTOLOG = ITERSTOLOG / ITERSRESET;
	private static final Level LOGLEVEL = Level.ALL;
	private List<Vehicle> vehicles;
	private Random coin;
	private Logger logger;



	public Planner(List<Vehicle> vehicles) {
		super();
		this.vehicles = vehicles;

		this.coin = new Random(42);

		this.logger = Logger.getLogger("affogalagoffa");
		this.logger.setLevel(LOGLEVEL);
	}


	/**
	 * Implements an epsilon-greedy (with decreasing epsilon) SLS.
	 * @return the best joint plan found.
	 */
	public Solution plan(Solution pastSolution, Task newTask, double epsilon, long timeout){
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0L;
		Solution currentSolution;
		Solution bestSolution;
		
		// Initialise currentSolution
		currentSolution = new Solution(pastSolution);
		currentSolution.addTask(newTask);
		
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
		int nReset = 0;
		for(int nIter = 1; elapsedTime < timeout; nIter++) {
			// Do not log all iterations
			if(nIter % ITERSTOLOG == 0) {
				logger.info("Iteration " + nIter + ": elapsed time = " + elapsedTime + 	", current cost = " + 
						currentSolution.getCost() + ", best cost = " + bestSolution.getCost());
			}

			// If too long since we found the best, reset to best
			if(itersSinceBest >= ITERSRESET) {
				nReset ++;
				if(nReset % RESETSTOLOG == 0) {
					logger.info("Too long since we found bestSolution: resetting current to best");
				}
				currentSolution = bestSolution;
				itersSinceBest = 0;	
			}

			
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

		return bestSolution;
	}

}
