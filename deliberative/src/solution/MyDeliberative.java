package solution;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class MyDeliberative implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }

	// Environment
	Topology topology;

	// Determines which algorithm to use for the search
	Algorithm algorithm;

	
	
	/* Only used to read the algorithm preference from the .xml configuration file */
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;

		// Read the algorithm
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
	}
	

	/* Computes the current state of the vehicle, then lets the class State do the job,
	 * by calling either bfs() or aStar() on initialState. */
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		State initialState = new State(tasks, vehicle, topology);
		Plan plan;
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = initialState.aStar();
			break;
		case BFS:
			plan = initialState.bfs();
			break;
		default:
			throw new AssertionError("Should not happen.");
		}	
		
		return plan;
	}


	/* Only logs the event: we are not interested in keeping the TaskSet carriedTasks,
	 * as it will be available anyway in the next call to plan() as vehicle.getCurrentTasks() */
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		if (!carriedTasks.isEmpty()) {
			System.out.println("planCancelled called with non-empty carriedTasks");
		}
		
		return;
	}

}
