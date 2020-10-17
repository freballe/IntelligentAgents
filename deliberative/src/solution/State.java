package solution;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
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
	private int depth;			// The number of arcs traversed from the root
	private double heuristic;	// The (under)estimated cost to any goal state
	private Vehicle vehicle;



	/**
	 * Reduced constructor, called to instantiate the initial state, for which 
	 * some parameters can be derived from the vehicle.
	 */
	public State(TaskSet pettera, Vehicle vehicle) {
		this(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), pettera, null, vehicle, 0.0, 0);
	}


	/**
	 * Full constructor.
	 */
	public State(City currentCity, TaskSet groppone, TaskSet pettera, Arc father, 
			Vehicle vehicle, double costSoFar, int depth) {
		super();
		this.currentCity = currentCity;
		this.groppone = groppone;
		this.pettera = pettera;
		this.costSoFar = costSoFar;
		this.vehicle = vehicle;
		this.fatherArc = father;
		this.depth = depth;

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
	 * from currentCity to a city where a task is due to be delivered) followed by 
	 * a sequence of Deliveries (delivering every possible task in the destination city), or a 
	 * sequence of Moves (constituting the best path from currentCity to a city where a task is 
	 * due to be picked up and no task is due to be delivered) followed by a single Pickup.
	 * @return a List containing all the States reachable from this State.
	 */
	public List<State> getChildren(){
		List<State> states = new LinkedList<State>();

		// Enumerate all single-Pickup Arcs
		for(Task task : pettera) {
			if(task.pickupCity == currentCity && task.weight + groppone.weightSum() <= vehicle.capacity()) {
				Arc arc = new Arc(this);
				
				// Add the Pickup to the Arc
				arc.addAction(new Pickup(task));

				// A Pickup does not incur costs, so costSoFar is the same for the new State
				State end = new State(currentCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), 
						arc, vehicle, costSoFar, this.depth+1);
				
				// Modify the new State, so that "task" figures as picked up
				end.pickupTask(task);
				
				arc.setEnd(end);
				states.add(end);
			}
		}

		// Line up all the cities where there is a task to deliver
		Set<City> deliveryCities = new HashSet<City>();
		for(Task task : groppone) {
			deliveryCities.add(task.deliveryCity);
		}

		// Enumerate all Moves-Deliveries Arcs
		for(City deliveryCity : deliveryCities) {
			Arc arc = new Arc(this);

			// Add all Moves to the Arc and set total cost
			for(City transitCity : currentCity.pathTo(deliveryCity)) {
				arc.addAction(new Move(transitCity));
			}
			arc.setCost(vehicle.costPerKm() * currentCity.distanceTo(deliveryCity));

			// Set proper costSoFar for the new State (depth is not definitive)
			State end = new State(deliveryCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), 
					arc, vehicle, costSoFar+arc.getCost(), this.depth+1);
			
			// Modify the new State, so that all deliverable tasks figure as delivered
			List<Task> delivered = end.deliveryTasks();
			// Add all Deliveries to the Arc
			for(Task task : delivered) {
				arc.addAction(new Delivery(task));
			}
			
			// "Artificially" set depth so as to make each Delivery count as one move
			end.setDepth(this.depth + delivered.size());

			arc.setEnd(end);
			states.add(end);
		}

		// Line up all the tasks to be picked up in a city where no task is to be delivered
		Set<Task> tasksToPickup = new HashSet<Task>();
		for(Task task : pettera) {
			if(task.pickupCity != currentCity && !deliveryCities.contains(task.pickupCity)) {
				tasksToPickup.add(task);
			}
		}

		// Enumerate all Moves-Pickup Arcs
		for(Task taskToPickup : tasksToPickup) {
			City pickupCity = taskToPickup.pickupCity;
			Arc arc = new Arc(this);

			// Add all Moves to the Arc and set total cost
			for(City transitCity : currentCity.pathTo(pickupCity)) {
				arc.addAction(new Move(transitCity));
			}
			arc.setCost(vehicle.costPerKm() * currentCity.distanceTo(pickupCity));

			// Set proper costSoFar for the new State
			State end = new State(pickupCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), 
					arc, vehicle, costSoFar+arc.getCost(), this.depth+1);

			// Modify the new State, so that "task" figures as picked up
			end.pickupTask(taskToPickup);
			// Add the Pickup to the Arc
			arc.addAction(new Pickup(taskToPickup));

			arc.setEnd(end);
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


	public int getDepth() {
		return depth;
	}




	public void setDepth(int depth) {
		this.depth = depth;
	}
	


	@Override
	public String toString() {
		return "State [currentCity=" + currentCity.name + ", groppone=" + printTaskSet(groppone) + 
				", pettera=" + printTaskSet(pettera) + "\ncostSoFar=" + costSoFar + ", depth=" + depth + "]";
	}






	private String printTaskSet(TaskSet taskSet) {
		String s = "TaskSet [";
		int i = 0;

		for(Task task : taskSet) {
			i++;
			if(i == 1) {
				s += task.id;
			} else {
				s += " " + task.id;
			}
		}

		return s + "]";
	}


	public String printPathSoFar() {
		String s = "";

		if(this.fatherArc != null) {
			s = this.fatherArc.getStart().printPathSoFar();
		}

		return s + this + "\n";
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
