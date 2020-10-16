package solution;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.plan.Action.Pickup;
import logist.plan.Action.Move;
import logist.plan.Action.Delivery;

class State {
	private City currentCity;
	private TaskSet groppone;
	private TaskSet pettera;

	private Arc father;
	private double costSoFar;
	private double heuristic;
	private Vehicle vehicle;

	public State(TaskSet pettera, Vehicle vehicle) {
		this(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), pettera,  null, vehicle, 0);
	}

	public State(City currentCity, TaskSet groppone, TaskSet pettera, Arc father, Vehicle vehicle, double costSoFar) {
		super();
		this.currentCity = currentCity;
		this.groppone = groppone;
		this.pettera = pettera;
		this.costSoFar = costSoFar;
		this.vehicle = vehicle;
		this.heuristic = 0;
		this.father = father;
	}

	public Plan getPlanSoFar() {
		if(this.getFather() == null) {
			return new Plan(currentCity);
		}
		Plan plan = this.getFather().getStart().getPlanSoFar();
		for(Action action : this.getFather().getActions()){
			plan.append(action);
		}
		return plan;
	}

	public Plan bfs() {
		// TODO
		return null;
	}

	public Arc getFather() {
		return father;
	}

	public void setFather(Arc father) {
		this.father = father;
	}

	public double getCostSoFar() {
		return costSoFar;
	}

	public void setCostSoFar(double costSoFar) {
		this.costSoFar = costSoFar;
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
		List<State> states = new LinkedList<State>();
		for(Task task : pettera) {
			if(task.pickupCity == currentCity && task.weight + groppone.weightSum() <= vehicle.capacity()) {
				Arc arc = new Arc(this);
				arc.addActionAndCost(new Pickup(task), 0);
				State end = new State(currentCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), arc, vehicle, costSoFar);
				end.pickupTask(task);
				states.add(end);
				arc.setEnd(end);
			}
		}
		
		Set<City> citiesToGo = new HashSet<City>();
		for(Task task : pettera) {
			if(task.pickupCity != currentCity) {
				citiesToGo.add(task.pickupCity);
			}
		}
		
		for(Task task : groppone) {
			citiesToGo.add(task.deliveryCity);
		}
		

		return null;
	}

	private void pickupTask(Task task) {
		groppone.add(task);
		pettera.remove(task);
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
