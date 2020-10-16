package solution;

import logist.simulation.Vehicle;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

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
	enum Algorithm { BFS, ASTAR }
	
	private Vehicle vehicle;

	// User-supplied parameter dictating the search algorithm to employ
	private Algorithm algorithm;
	
	

	/* Only used to read user-supplied values from the configuration files, and to set vehicle. */
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the user-chosen search algorithm from the configuration file
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// Only one vehicle per agent
		this.vehicle = agent.vehicles().get(0);
	}

	
	/* Computes the current state of the agent, then runs the chosen search algorithm from it. */
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		State initialState = new State(tasks, vehicle);	// Short constructor for initial state
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = aStar(initialState);
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
			System.out.println("planCancelled called with non empty carriedTasks");
		}
		
		return;
	}
	
	
	/**
	 * Implements the A* search algorithm.
	 * @param initialState the root node of the search
	 * @return a minimum-cost Plan from the root to any goal state
	 */
	private Plan aStar(State initialState) {
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
		
		System.out.println("A* launched");
		
		fringe.add(initialState);
		while(true) {
			nIter++;
			if(nIter % 100 == 0) {
				// Don't log all iterations
				System.out.printf("Beginning of iteration %d, queue length = %d\n", nIter, fringe.size());
			}
			
			// Should not happen
			if(fringe.isEmpty()){
				System.out.println("A* terminating: no goal state found");
				return null;
			}
			
			// Dequeue the most promising node from the fringe
			State n = fringe.poll();
			
			// Return immediately if it is a goal state: optimal by admissibility of the heuristic
			if(n.isGoal()) {
				System.out.printf("A* terminating: found goal state.\nTotal km: %.0f\nTotal cost:%.0f\n",
						n.getCostSoFar()/vehicle.costPerKm(), n.getCostSoFar());
				return n.getPlanSoFar();
			}
			
			/* If a copy of n already exists, n is only used (possibly) to update its non-identifying
			 * attributes (father and costSoFar). */
			if(visited.containsKey(n)){
				// The old copy will survive in "visited", and will (again) enqueue its children.
				State oldCopy = visited.get(n);
				
				// Nothing to do if n doesn't have a cheaper path from the source
				if(oldCopy.getCostSoFar() <= n.getCostSoFar()) {
					continue;
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

		System.out.println("BFS launched");
		
		fringe.add(initialState);
		// Explore the whole graph
		while(!fringe.isEmpty()) {
			nIter++;
			if(nIter % 100 == 0) {
				// Don't log all iterations
				System.out.printf("Beginning of iteration %d, queue length = %d\n", nIter, fringe.size());
			}
			
			// Dequeue the next promising node from the fringe (FIFO order)
			State n = fringe.poll();
			
			/* If a copy of n already exists, n is only used (possibly) to update its non-identifying
			 * attributes (father and costSoFar). */
			if(visited.containsKey(n)){
				// The old copy will survive in "visited"
				State oldCopy = visited.get(n);
				
				// Nothing to do if n doesn't have a cheaper path from the source
				if(oldCopy.getCostSoFar() <= n.getCostSoFar()) {
					continue;
				}
				
				// Update fields in oldCopy
				Arc newFather = n.getFatherArc();	// The new fatherArc of oldCopy
				newFather.setEnd(oldCopy);			// Make it point to oldCopy
				oldCopy.setFatherArc(newFather);
				oldCopy.setCostSoFar(n.getCostSoFar());
				
				// If needed, update bestGoalNode and bestGoalCost
				if(oldCopy.isGoal() && oldCopy.getCostSoFar() < bestGoalCost) {
					bestGoalState = oldCopy;
					bestGoalCost = oldCopy.getCostSoFar();
				}
				
				/* Unlike A*, we never re-enqueue the children, even if a better path is found.
				 * This is because, with a suitably-defined set of actions (such as ours), nodes 
				 * are always at the same depth, no matter the path from the root: this ensures that,
				 * even if a node is re-discovered, its children have not been visited yet. */
				continue;
			}
			
			// Only reachable for new-found States
			visited.put(n, n);

			// Add the children at the bottom of the queue
			List<State> children = n.getChildren();
			for(State child : children) {
				fringe.addLast(child);
			}
			
			// If needed, update bestGoalNode and bestGoalCost
			if(n.isGoal() && n.getCostSoFar() < bestGoalCost) {
				bestGoalState = n;
				bestGoalCost = n.getCostSoFar();
			}
		}
		
		System.out.printf("BFS terminating: found plan.\nTotal km: %.0f\nTotal cost:%.0f\n",
								bestGoalCost/vehicle.costPerKm(), bestGoalCost);
		return bestGoalState.getPlanSoFar();
	}

}
