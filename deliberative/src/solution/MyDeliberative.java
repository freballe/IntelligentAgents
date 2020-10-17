package solution;

import logist.simulation.Vehicle;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import solution.State.StateComparator;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class MyDeliberative implements DeliberativeBehavior {

	// Enum class for the options for the search algorithm
	enum Algorithm { BFS, ASTAR, DIJKSTRA }

	// User-supplied parameter dictating the search algorithm to employ
	private Algorithm algorithm;
	private Vehicle vehicle;
	private Logger logger;



	/* Only used to read user-supplied values from the configuration files, and to set vehicle. */
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the user-chosen search algorithm from the configuration file
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// Set logger
		logger = Logger.getLogger(agent.name());
		// Reads the user-chosen log level from the configuration file
		String logLvlName = agent.readProperty("log-level", String.class, "INFO");
		logger.setLevel(Level.parse(logLvlName));

		// Only one vehicle per agent
		this.vehicle = agent.vehicles().get(0);
	}


	/* Computes the current state of the agent, then runs the chosen search algorithm from it. */
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		State initialState = new State(tasks, vehicle, algorithm);	// Short constructor for initial state

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
		case DIJKSTRA:
			plan = aStar(initialState, algorithm);
			break;
		case BFS:
			plan = bfs(initialState);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}

		return plan;
	}


	/* Only logs the event: carriedTasks will be available as vehicle.getCurrentTasks()
	 *  in the next call to plan() anyway. */
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		if (!carriedTasks.isEmpty()) {
			logger.warning("planCancelled called with non-empty carriedTasks\n");
		}

		return;
	}


	/**
	 * Implements the A* search algorithm.
	 * @param initialState the root node of the search
	 * @return a minimum-cost Plan from the root to any goal state
	 */
	private Plan aStar(State initialState, Algorithm algorithm) {
		// The queue of pending states, sorted by decreasing estimated total cost
		PriorityQueue<State> fringe = new PriorityQueue<State>(new State.StateComparator());
		/* The set of visited states. Implemented as a map, so as to be able to retrieve an element.
		 * It is "append-only" in the following sense: when a State is added, it should never be
		 * removed (or replaced by another with the same identifying fields); instead, its 
		 * non-identifying fields (father and costSoFar) may be updated when a better path is discovered.
		 * This ensures that, among several copies of the same State that may be generated throughout
		 * the algorithm, there is only one (the first one to be encountered) that is always kept. */
		Map<State, State> visited = new HashMap<State, State>();
		// Only used for logging
		int nIter = 0;
		String algoName = (algorithm == Algorithm.ASTAR) ? "A*" : "Dijkstra";

		logger.info(algoName + " launched\n");

		fringe.add(initialState);
		while(true) {
			nIter++;
			if(nIter % 100 == 0) {
				// Don't log all iterations
				logger.fine("Beginning of iteration " + nIter + ", queue length = " + fringe.size() + "\n");
			}

			// Should not happen
			if(fringe.isEmpty()){
				logger.warning(algoName + " terminating: no goal state found\n");
				return null;
			}

			// Dequeue the most promising node from the fringe
			State n = fringe.poll();

			// Return immediately if it is a goal state: optimal by admissibility of the heuristic
			if(n.isGoal()) {
				logger.info(algoName +" terminating: found plan. \n" + "Number of iterations: " + 
						nIter + "\n" + "Number of visited nodes: " + visited.size() + "\n" + 
						"Total km: " + (int)(n.getCostSoFar()/vehicle.costPerKm()) + "\n" + 
						"Total cost: " + n.getCostSoFar() + "\n" + "Path found:\n" + 
						n.printPathSoFar() + "\n");

				return n.getPlanSoFar();
			}

			/* If a copy of n already exists, n is only used (possibly) to update its non-identifying
			 * attributes (father and costSoFar). */
			if(visited.containsKey(n)){
				// The old copy will survive in "visited", and will (again) enqueue its children.
				State oldCopy = visited.get(n);

				// Check whether new copy has a cheaper path from the source
				if(oldCopy.getCostSoFar() <= n.getCostSoFar()) {
					// Check that all paths to a node have the same depth
					if(oldCopy.getDepth() != n.getDepth() && logger.isLoggable(Level.WARNING)) {
						logger.warning("******************** ERROR: WORSE PATH AT HIGHER DEPTH "
								+ "********************\n\n" + "Child's path:\n" +
								n.printPathSoFar() + "\n" + "Old copy's path:\n" + 
								oldCopy.printPathSoFar() + "\n");
					}

					// If not, there's nothing to do
					continue;
				}

				// New copy (n) has a better path

				// Check that all paths to a node have the same depth
				if(oldCopy.getDepth() != n.getDepth()) {
					if(logger.isLoggable(Level.SEVERE)) {
						logger.severe("******************** ERROR: BETTER PATH AT HIGHER DEPTH "
								+ "********************\n\n" + "Child's path:\n" + 
								n.printPathSoFar() + "\n" + "Old copy's path:\n" + 
								oldCopy.printPathSoFar() + "\n");
					}

					throw new AssertionError("Found different-depth paths to the same node:"
							+ "child of node at iteration " + nIter);
				}

				// Update fields in oldCopy
				Arc newFather = n.getFatherArc();	// The new fatherArc of oldCopy
				newFather.setEnd(oldCopy);			// Make it point to oldCopy
				oldCopy.setFatherArc(newFather);
				oldCopy.setCostSoFar(n.getCostSoFar());

				/* The rest of the code operates on n. This assignment lets the new copy free
				 * to be collected by the GC, and makes sure that the enqueued children have
				 * oldCopy as their father. */
				n = oldCopy;
			} else {
				// Only mark a State as visited if it is the first copy encountered
				visited.put(n, n);
			}

			// Enqueue (or re-enqueue) all children
			List<State> children = n.getChildren();
			fringe.addAll(children);
		}
	}	


	/**
	 * Implements a modification of the BFS search algorithm.
	 * @param initialState the root node of the search
	 * @return a minimum-cost Plan from the root to any goal state
	 */
	private Plan bfs(State initialState) {
		// The queue of pending states
		Deque<State> fringe = new LinkedList<State>();
		/* The set of visited states. Implemented as a map, so as to be able to retrieve an element.
		 * It is "append-only" in the following sense: when a State is added, it should never be
		 * removed (or replaced by another with the same identifying fields); instead, its 
		 * non-identifying fields (father and costSoFar) may be updated when a better path is discovered.
		 * This ensures that, among several copies of the same State that may be generated throughout
		 * the algorithm, there is only one (the first one to be encountered) that is always kept. */
		Map<State, State> visited = new HashMap<State, State>();
		// Only used for logging
		int nIter = 0;

		/* The algorithm explores the whole graph. The goal state with the cheapest path 
		 * from the root is chosen. */
		State bestGoalState = null;
		double bestGoalCost = Double.MAX_VALUE;

		logger.info("BFS launched\n");

		fringe.add(initialState);
		visited.put(initialState, initialState);
		// Explore the whole graph
		while(!fringe.isEmpty()) {
			nIter++;
			if(nIter % 100 == 0) {
				// Don't log all iterations
				logger.fine("Beginning of iteration " + nIter + ", queue length = " + fringe.size() + "\n");
			}

			// Dequeue the next node from the fringe (FIFO order)
			State n = fringe.poll();

			// If needed, update bestGoalNode and bestGoalCost
			if(n.isGoal() && n.getCostSoFar() < bestGoalCost) {
				bestGoalState = n;
				bestGoalCost = n.getCostSoFar();
			}

			// Add the children at the bottom of the queue
			List<State> children = n.getChildren();
			for(State child : children) {				
				/* If a copy of child already exists, child is only used (possibly) to update its
				 *  non-identifying attributes. */
				if(visited.containsKey(child)){					
					// The old copy will survive in "visited" and in the queue
					State oldCopy = visited.get(child);

					// Check whether new copy has a cheaper path from the source
					if(oldCopy.getCostSoFar() <= child.getCostSoFar()) {						
						// Check that all paths to a node have the same depth
						if(oldCopy.getDepth() != child.getDepth() && logger.isLoggable(Level.WARNING)) {
							logger.warning("******************** ERROR: WORSE PATH AT HIGHER DEPTH "
									+ "********************\n\n" + "Child's path:\n" +
									child.printPathSoFar() + "\n" + "Old copy's path:\n" + 
									oldCopy.printPathSoFar() + "\n");
						}

						// If not, there's nothing to do
						continue;
					}

					// New copy (child) has a better path

					// Check that all paths to a node have the same depth
					if(oldCopy.getDepth() != child.getDepth()) {
						if(logger.isLoggable(Level.SEVERE)) {
							logger.severe("******************** ERROR: BETTER PATH AT HIGHER DEPTH "
									+ "********************\n\n" + "Child's path:\n" + 
									child.printPathSoFar() + "\n" + "Old copy's path:\n" + 
									oldCopy.printPathSoFar() + "\n");
						}

						throw new AssertionError("Found different-depth paths to the same node:"
								+ "child of node at iteration " + nIter);
					}

					// Update fields in oldCopy
					Arc newFather = child.getFatherArc();	// The new fatherArc of oldCopy
					newFather.setEnd(oldCopy);				// Make it point to oldCopy
					oldCopy.setFatherArc(newFather);
					oldCopy.setCostSoFar(child.getCostSoFar());
				} else {
					// If child has never been seen before, mark it as seen and add it to the queue
					visited.put(child, child);
					fringe.addLast(child);
				}
			}
		}

		logger.info("BFS terminating: found plan. \n" + "Number of iterations: " + nIter + "\n" +
				"Number of visited nodes: " + visited.size() + "\n" + "Total km: " + 
				(int)(bestGoalCost/vehicle.costPerKm()) + "\n" + "Total cost: " + bestGoalCost +
				"\n" + "Path found:\n" + bestGoalState.printPathSoFar() + "\n");

		return bestGoalState.getPlanSoFar();
	}

}
