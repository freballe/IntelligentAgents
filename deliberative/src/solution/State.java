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


/**
 * Class representing the state of an agent.
 * It is identified by the current location of the agent, the set of tasks yet to be picked up,
 * and the set of tasks picked up but not yet delivered.
 * It also has attributes (fatherArc and costSoFar) that only makes sense during the execution
 * of a search algorithm, as they relate to the path from the root: they may vary among the copies
 * of the same state (instances with the same identifying fields).
 */
class State {
	// This triplet of fields identifies the state
	private City currentCity;	// The city the agent is currently in
	private TaskSet groppone;	// The set of tasks picked up by the agent but not yet delivered
	private TaskSet pettera;	// The set of tasks not yet picked up by any agent (including others)

	/* Accessory information. Some of these fields (fatherArc and costSoFar) may vary within States with
	 * the same identifying fields, depending on the path through the search graph that led to them. */
	private Arc fatherArc;		// The (oriented) arc coming from the predecessor State
	private double costSoFar;	// The cost of the path from the root
	private double heuristic;	// The (under)estimated cost to any goal state
	private Vehicle vehicle;

	
	
	/**
	 * Reduced constructor, called to instantiate the initial state, for which 
	 * some parameters can be derived from the vehicle.
	 */
	public State(TaskSet pettera, Vehicle vehicle) {
		this(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), pettera, null, vehicle, 0);
	}

	
	/**
	 * Full constructor.
	 */
	public State(City currentCity, TaskSet groppone, TaskSet pettera, Arc father, 
			Vehicle vehicle, double costSoFar) {
		super();
		this.currentCity = currentCity;
		this.groppone = groppone;
		this.pettera = pettera;
		this.costSoFar = costSoFar;
		this.vehicle = vehicle;
		this.fatherArc = father;
		
		initHeuristic();
	}


	/**
	 * Recursively constructs a Plan from the root, by stacking up the actions in the arcs.
	 * @return a Plan to reach this State from the initial State
	 */
	public Plan getPlanSoFar() {
		// Termination condition
		if(this.getFatherArc() == null) {
			return new Plan(currentCity);
		}
		
		// Add the actions of the last arc to the Plan leading to the father
		Plan plan = this.getFatherArc().getStart().getPlanSoFar();
		for(Action action : this.getFatherArc().getActions()){
			plan.append(action);
		}
		
		return plan;
	}

	
	/**
	 * @return whether or not this State is a goal state. This is true iff no task is either not picked up 
	 * or picked up but not delivered.
	 */
	public boolean isGoal() {
		return groppone.isEmpty() && pettera.isEmpty();
	}

	
	/**
	 * Computes all the children States by enumerating all the outgoing Arcs.
	 * Arcs are either a single Pickup, or a sequence of Moves (constituting the best path
	 * from currentCity to a city where a task is due to be picked up or delivered) followed by 
	 * a sequence of Deliveries (delivering every possible task in the destination city).
	 * @return a List containing all the States reachable from this State.
	 */
	public List<State> getChildren(){
		List<State> states = new LinkedList<State>();
		
		// Enumerate all single-Pickup Arcs
		for(Task task : pettera) {
			if(task.pickupCity == currentCity && task.weight + groppone.weightSum() <= vehicle.capacity()) {
				Arc arc = new Arc(this);
				arc.addAction(new Pickup(task));
				
				// A Pickup does not incur costs, so costSoFar is the same for the new State
				State end = new State(currentCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), arc, vehicle, costSoFar);
				// Modify the new State, so that "task" figures as picked up
				end.pickupTask(task);
				arc.setEnd(end);
				
				states.add(end);
			}
		}
		
		// Line up all the cities where there is something to do (either pick up or delivery a task)
		Set<City> citiesToGo = new HashSet<City>();
		for(Task task : pettera) {
			if(task.pickupCity != currentCity) {
				citiesToGo.add(task.pickupCity);
			}
		}
		for(Task task : groppone) {
			citiesToGo.add(task.deliveryCity);
		}
		
		// Enumerate all Move-Delivery Arcs
		for(City cityToGo : citiesToGo) {
			Arc arc = new Arc(this);
			
			// Add all Moves to the Arc and set total cost
			for(City transitCity : currentCity.pathTo(cityToGo)) {
				arc.addAction(new Move(transitCity));
			}
			arc.setCost(vehicle.costPerKm() * currentCity.distanceTo(cityToGo));
			
			// Set proper costSoFat for the new State
			State end = new State(cityToGo, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), arc, vehicle, costSoFar+arc.getCost());
			arc.setEnd(end);
			
			// Modify the new State, so that all deliverable tasks figure as delivered
			List<Task> delivered = end.deliveryTasks();
			// Add all Deliveries to the Arc
			for(Task task : delivered) {
				arc.addAction(new Delivery(task));
			}
			
			states.add(end);
		}

		return states;
	}

	
	/**
	 * Modifies groppone and pettera, so that all deliverable tasks figure as delivered
	 * @return the list of delivererd tasks
	 */
	private List<Task> deliveryTasks() {
		List<Task> delivered = new LinkedList<Task>();
		
		for(Task task : groppone) {
			if(task.deliveryCity == currentCity) {
				delivered.add(task);
				groppone.remove(task);
			}
		}
		
		return delivered;
	}


	/**
	 * Modifies groppone and pettera, so that "task" figures as picked up
	 */
	private void pickupTask(Task task) {
		groppone.add(task);
		pettera.remove(task);
	}

	
	// TODO
	private void initHeuristic() {
		this.heuristic = 0;
	}
	
	
	/**
	 * Implements the function f(n) = g(n) + h(n).
	 * @return the estimated cost from the root to a goal state, passing through this State
	 */
	private double estimateTotalCost() {
		return heuristic + costSoFar;
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
	
	
	public Arc getFatherArc() {
		return fatherArc;
	}

	
	public void setFatherArc(Arc fatherArc) {
		this.fatherArc = fatherArc;
	}

	
	public double getCostSoFar() {
		return costSoFar;
	}

	
	public void setCostSoFar(double costSoFar) {
		this.costSoFar = costSoFar;
	}

	
	/**
	 * Implements a reverse-order (in terms of f(n)) Comparator for States
	 */
	static class StateComparator implements Comparator<State>{ 
		public int compare(State s1, State s2) { 
			if (s1.estimateTotalCost() < s2.estimateTotalCost()) 
				return -1; 
			else if (s1.estimateTotalCost() > s2.estimateTotalCost()) 
				return +1; 
			return 0; 
		} 
	} 

}
