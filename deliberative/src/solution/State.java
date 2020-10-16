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
	private City currentCity;
	private TaskSet groppone;
	private TaskSet pettera;

	private Topology topo;
	private Plan planSoFar;
	private double costSoFar;
	private double heuristic;
	private int costPerKm;
	private int capacity;
	private Vehicle vehicle;

	public State(TaskSet pettera, Vehicle vehicle, Topology topo) {
		this(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), pettera, topo, new Plan(vehicle.getCurrentCity()), vehicle, 0);
	}

	public State(City currentCity, TaskSet groppone, TaskSet pettera, Topology topo, Plan planSoFar, Vehicle vehicle, double costSoFar) {
		super();
		this.currentCity = currentCity;
		this.groppone = groppone;
		this.pettera = pettera;
		this.topo = topo;
		this.costSoFar = costSoFar;
		this.vehicle = vehicle;
		this.heuristic = 0;
	}

	Plan getPlanSoFar() {
		// TODO Auto-generated method stub
		return null;
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


	public boolean isGoal() {
		return groppone.isEmpty() && pettera.isEmpty();
	}

	List<State> getSuccessorStates(){
		return null;
	}

	private double estimateTotalCost() {	// f
		return heuristic + costSoFar;
	}

	static class StateComparator implements Comparator<State>{ 
		public int compare(State s1, State s2) { 
			if (s1.estimateTotalCost() < s2.estimateTotalCost()) 
				return 1; 
			else if (s1.estimateTotalCost() > s2.estimateTotalCost()) 
				return -1; 
			return 0; 
		} 
	} 

}
