package solution;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

class State {
	// This triplet of fields identifies the state
	private City currentCity;	// The city the agent is currently in
	private TaskSet groppone;	// The set of tasks picked up by the agent but not yet delivered
	private TaskSet pettera;	// The set of tasks not yet picked up by any agent (including others)

	/* Accessory information. Some of these fields (planSoFar and costSoFar) may vary within States with
	 * the same identifying fields, depending on the path through the search graph that led to them. */
	private Topology topo;
	private Plan planSoFar;		// The sequence of Actions that led to this State
	private double costSoFar;	// The total cost of the Actions that led to this State
	private double heuristic;	// The heuristic for the distance to any goal state
	private int costPerKm;
	private int capacity;

	
	
	/**
	 * Reduced constructor, called to instantiate the initial state, for which some parameters can be derived from the vehicle
	 */
	public State(TaskSet pettera, Vehicle vehicle, Topology topo) {
		this(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), pettera, topo, new Plan(vehicle.getCurrentCity()), 
				vehicle.costPerKm(), vehicle.capacity(), 0.0);
	}

	
	/**
	 * Full constructor.
	 */
	public State(City currentCity, TaskSet groppone, TaskSet pettera, Topology topo, Plan planSoFar, 
			int costPerKm, int capacity, double costSoFar) {
		super();
		this.currentCity = currentCity;
		this.groppone = groppone;
		this.pettera = pettera;
		
		this.topo = topo;
		this.planSoFar = planSoFar;
		this.costSoFar = costSoFar;
		this.costPerKm = costPerKm;
		this.capacity = capacity;
		
		// TODO: set properly
		this.heuristic = 0.0;
	}

	
	public Plan aStar() {
		PriorityQueue<State> Q = new PriorityQueue<State>(new StateComparator());
		HashSet<State> C = new HashSet<State>();

		Q.add(this);

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

	
	public Plan bfs() {
		// TODO
		return planSoFar;
	}


	/* Custom implementation that only takes into account the identifying fields. */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currentCity == null) ? 0 : currentCity.hashCode());
		result = prime * result + ((groppone == null) ? 0 : groppone.hashCode());
		result = prime * result + ((pettera == null) ? 0 : pettera.hashCode());
		return result;
	}


	/* Custom implementation that only takes into account the identifying fields. */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof State))
			return false;
		State other = (State) obj;
		if (currentCity == null) {
			if (other.currentCity != null)
				return false;
		} else if (!currentCity.equals(other.currentCity))
			return false;
		if (groppone == null) {
			if (other.groppone != null)
				return false;
		} else if (!groppone.equals(other.groppone))
			return false;
		if (pettera == null) {
			if (other.pettera != null)
				return false;
		} else if (!pettera.equals(other.pettera))
			return false;
		return true;
	}


	/**
	 * @return whether or not this State is a goal state. This is true iff no task is either not picked up 
	 * or picked up but not delivered.
	 */
	private boolean isGoal() {
		return groppone.isEmpty() && pettera.isEmpty();
	}

	
	/**
	 * @return the Plan containing the Actions that led to this State from the initial state.
	 */
	private Plan getPlanSoFar() {
		return planSoFar;
	}

	
	/**
	 * @return a List containing all the states reachable from this State.
	 */
	private List<State> getSuccessorStates(){
		return null;
	}

	
	/**
	 * @return the value of f(n) = g(n) + h(n), the estimated minimum cost to get from the initial state to 
	 * a goal state passing through this state.
	 */
	private double estimateTotalCost() {
		return heuristic + costSoFar;
	}

	
	/**
	 * The comparator used for the PriorityQueue, which compares based on the value of f(n).
	 */
	class StateComparator implements Comparator<State>{ 
		public int compare(State s1, State s2) { 
			if (s1.estimateTotalCost() < s2.estimateTotalCost()) 
				return 1; 
			else if (s1.estimateTotalCost() > s2.estimateTotalCost()) 
				return -1; 
			return 0; 
		} 
	} 

}
