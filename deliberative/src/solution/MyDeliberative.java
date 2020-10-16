package solution;

/* import table */
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

	enum Algorithm { BFS, ASTAR }

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
	}

	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		State initialState = new State(tasks, vehicle);
		
		System.out.printf("Planning: total reward = %d\n", tasks.rewardSum());
		
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

	
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
			System.out.println("planCancelled called with non empty carriedTasks");
		}
	}
	
	
	private Plan aStar(State initialState) {
		PriorityQueue<State> Q = new PriorityQueue<State>(new State.StateComparator());
		Map<State, State> visited = new HashMap<State, State>();
		int nIter = 0;
		
		System.out.println("A* launched");
		Q.add(initialState);
		while(true) {
			nIter++;
			if(nIter % 100 == 0) {
				System.out.printf("Beginning of iteration %d, queue length = %d\n", nIter, Q.size());
			}
			
			if(Q.isEmpty()){
				System.out.println("A* terminating: no goal state found");
				return null;
			}
			
			State n = Q.poll();
			if(n.isGoal()) {
				System.out.println("A* terminating: found goal state");
				return n.getPlanSoFar();
			}
			
			if(visited.containsKey(n)){
				State oldCopy = visited.get(n);
				
				if(oldCopy.getCostSoFar() <= n.getCostSoFar()) {
					continue;
				}
				
				Arc newFather = n.getFather();
				newFather.setEnd(oldCopy);
				oldCopy.setFather(newFather);
				oldCopy.setCostSoFar(n.getCostSoFar());
				
				n = oldCopy;
			} else {
				visited.put(n, n);
			}

			List<State> S = n.getSuccessorStates();
			Q.addAll(S);
		}
	}	

	
	private Plan bfs(State initialState) {
		Deque<State> Q = new LinkedList<State>();
		Map<State, State> visited = new HashMap<State, State>();
		int nIter = 0;
		
		Q.add(initialState);
		
		State bestGoalState = null;
		double bestGoalCost = Double.MAX_VALUE;


		System.out.println("BFS launched");
		while(!Q.isEmpty()) {
			nIter++;
			if(nIter % 100 == 0) {
				System.out.printf("Beginning of iteration %d, queue length = %d\n", nIter, Q.size());
			}
			
			State n = Q.poll();
			
			if(visited.containsKey(n)){
				State oldCopy = visited.get(n);
				
				if(oldCopy.getCostSoFar() <= n.getCostSoFar()) {
					continue;
				}
				
				Arc newFather = n.getFather();
				newFather.setEnd(oldCopy);
				oldCopy.setFather(newFather);
				oldCopy.setCostSoFar(n.getCostSoFar());
				
				if(oldCopy.isGoal() && oldCopy.getCostSoFar() < bestGoalCost) {
					bestGoalState = oldCopy;
					bestGoalCost = oldCopy.getCostSoFar();
				}
				
				continue;
			}
			
			visited.put(n, n);

			List<State> S = n.getSuccessorStates();
			for(State succ : S) {
				Q.addLast(succ);
			}
			
			if(n.isGoal() && n.getCostSoFar() < bestGoalCost) {
				bestGoalState = n;
				bestGoalCost = n.getCostSoFar();
			}
		}
		
		System.out.println("BFS terminating");
		return bestGoalState.getPlanSoFar();
	}

}
