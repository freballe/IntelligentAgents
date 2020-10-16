package solution;

/* import table */
import logist.simulation.Vehicle;

import java.util.HashSet;
import java.util.List;
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
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = aStar(initialState);
			break;
		case BFS:
			plan = initialState.bfs();
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
	
	public Plan aStar(State initialState) {
		PriorityQueue<State> Q = new PriorityQueue<State>(new State.StateComparator());
		HashSet<State> C = new HashSet<State>();
		Q.add(initialState);

		while(true) {
			if(Q.isEmpty()){
				return null;
			}
			State n = Q.poll();
			if(n.isGoal()) {
				return n.getPlanSoFar();
			}
			if(C.contains(n)){	
				continue;
			}
			// reminder: here we don't check the second condition since it can't happen that we re-visit a state with lower cost, 
			// as long as the heuristic is consistent
			C.add(n);

			List<State> S = n.getSuccessorStates();
			Q.addAll(S);
		}
	}	
}
