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
		this.planSoFar = planSoFar;
		this.costSoFar = costSoFar;
		this.vehicle = vehicle;
		this.heuristic = 0;
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

	private boolean isGoal() {
		return groppone.isEmpty() && pettera.isEmpty();
	}

	private Plan getPlanSoFar() {
		return planSoFar;
	}

	private List<State> getSuccessorStates(){
		return null;
	}

	private double estimateTotalCost() {	// f
		return heuristic + costSoFar;
	}

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
