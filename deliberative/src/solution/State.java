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

	@Override
	public int hashCode() {
		return currentCity.hashCode()+groppone.hashCode()+ pettera.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof State)) {
			return false;			
		}

		State s=(State) obj;

		return s.currentCity.equals(this.currentCity) && s.groppone.equals(this.groppone) && s.pettera.equals(this.pettera);
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
