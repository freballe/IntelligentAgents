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
import solution.MyDeliberative.Algorithm;
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
	private Algorithm algo;		// Dictates the heuristic



	/**
	 * Reduced constructor, called to instantiate the initial state, for which 
	 * some parameters can be derived from the vehicle.
	 */
	public State(TaskSet pettera, Vehicle vehicle, Algorithm algo) {
		this(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), pettera, null, vehicle, 0.0, 0, algo);
	}


	/**
	 * Full constructor.
	 */
	public State(City currentCity, TaskSet groppone, TaskSet pettera, Arc father, 
			Vehicle vehicle, double costSoFar, int depth, Algorithm algo) {
		super();
		this.currentCity = currentCity;
		this.groppone = groppone;
		this.pettera = pettera;
		this.costSoFar = costSoFar;
		this.vehicle = vehicle;
		this.fatherArc = father;
		this.depth = depth;
		this.algo = algo;

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
	 * If some tasks can be delivered in currentCity, then there is only one outgoing arc,
	 * and it is the Delivery of the task (among those that can be delivered in currentCity),
	 * with the smallest ID.
	 * Otherwise, arcs are either a single Pickup, or a sequence of Moves (constituting the best path
	 * from currentCity to a city where a task is due to be delivered) followed by 
	 * the Delivery of the task with the smallest ID among those that have to be delivered in the 
	 * destination city, or a sequence of Moves (constituting the best path from currentCity to a city
	 *  where a task is due to be picked up and no task is due to be delivered) followed by a single Pickup.
	 * @return a List containing all the States reachable from this State.
	 */
	public List<State> getChildren(){
		List<State> states = new LinkedList<State>();
		Task toBeDelivered = null;
		
		// See if a task can be delivered in currentCity
		toBeDelivered = smallestDeliverableTask();
		if(toBeDelivered != null) {
			Arc arc = new Arc(this);
			
			// Add the Delivery and the type to the Arc
			arc.addAction(new Delivery(toBeDelivered));
			arc.setType("DELIVERY(" + toBeDelivered.id + ")");

			// A Delivery does not incur costs, so costSoFar is the same for the new State
			State end = new State(currentCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), 
					arc, vehicle, costSoFar, this.depth+1, algo);
			
			// Modify the new State, so that "task" figures as delivered
			end.deliveryTask(toBeDelivered);
			
			arc.setEnd(end);
			states.add(end);
			// Only one outgoing Arc
			return states;
		}
		
		// Otherwise, enumerate all Arcs

		// Enumerate all single-Pickup Arcs
		for(Task task : pettera) {
			if(task.pickupCity == currentCity && task.weight + groppone.weightSum() <= vehicle.capacity()) {
				Arc arc = new Arc(this);
				
				// Add the Pickup and the type to the Arc
				arc.addAction(new Pickup(task));
				arc.setType("PICKUP(" + task.id + ")");

				// A Pickup does not incur costs, so costSoFar is the same for the new State
				State end = new State(currentCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), 
						arc, vehicle, costSoFar, this.depth+1, algo);
				
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

		// Enumerate all Moves-Delivery Arcs
		for(City deliveryCity : deliveryCities) {
			Arc arc = new Arc(this);

			// Add all Moves to the Arc and set total cost
			for(City transitCity : currentCity.pathTo(deliveryCity)) {
				arc.addAction(new Move(transitCity));
			}
			arc.setCost(vehicle.costPerKm() * currentCity.distanceTo(deliveryCity));

			// Set proper costSoFar and depth for the new State
			State end = new State(deliveryCity, TaskSet.copyOf(groppone), TaskSet.copyOf(pettera), 
					arc, vehicle, costSoFar+arc.getCost(), this.depth+1, algo);
			
			// Modify the new State, so that the smallest deliverable task figures as delivered
			Task toDeliver = end.smallestDeliverableTask();
			if(toDeliver == null) {
				throw new AssertionError("toDeliver is null\n" + "Child state:\n" + end);
			}
			end.deliveryTask(toDeliver);
			// Add the Delivery and the type to the Arc
			arc.addAction(new Delivery(toDeliver));
			arc.setType("MOVES(" + deliveryCity.name + ") + DELIVERY(" + toDeliver.id + ")");

			arc.setEnd(end);
			states.add(end);
		}

		// Line up all the light tasks to be picked up in a city where no task is to be delivered
		Set<Task> tasksToPickup = new HashSet<Task>();
		for(Task task : pettera) {
			if(task.pickupCity != currentCity && !deliveryCities.contains(task.pickupCity) &&
					task.weight + groppone.weightSum() <= vehicle.capacity()) {
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
					arc, vehicle, costSoFar+arc.getCost(), this.depth+1, algo);

			// Modify the new State, so that "task" figures as picked up
			end.pickupTask(taskToPickup);
			// Add the Pickup and the type to the Arc
			arc.addAction(new Pickup(taskToPickup));
			arc.setType("MOVES(" + pickupCity.name + ") + PICKUP(" + taskToPickup.id + ")");

			arc.setEnd(end);
			states.add(end);
		}

		return states;
	}


	/**
	 * Returns the task with the smallest ID (among those that can be 
	 * delivered in currentCity) 
	 */
	private Task smallestDeliverableTask() {
		Task deliverable = null;
		
		if(groppone.isEmpty()) {
			return null;
		}
		
		// Iteration is in increasing order of ID
		for(Task task : groppone) {
			if(task.deliveryCity == currentCity) {
				deliverable = task;
			}
		}
		
		return deliverable;
	}
	
	
	/**
	 * Modifies groppone, so that "task" figures as delivered
	 */
	private void deliveryTask(Task task) {
		groppone.remove(task);
	}


	/**
	 * Modifies groppone and pettera, so that "task" figures as picked up
	 */
	private void pickupTask(Task task) {
		groppone.add(task);
		pettera.remove(task);
	}


	/**
	 * Sets the heuristic.
	 * If algo is DIJKSTRA (or not ASTAR, anyway), then heuristic is set to 0.
	 * Else the heuristic is set to the maximum, over all tasks, of the cost of the optimal
	 * path to the delivery city (if the task is picked up but not delivered) or the cost
	 * of the optimal path to the pickup city and, from there, to the delivery city (if the
	 * task is not picked up yet).
	 */
	private void initHeuristic() {
		this.heuristic = 0.0;
		
		if(algo != Algorithm.ASTAR) {
			return;
		}
		
		// Sweep all tasks to deliver
		for(Task taskToDeliver : groppone) {
			double cost = vehicle.costPerKm() * currentCity.distanceTo(taskToDeliver.deliveryCity);
			
			if(cost > this.heuristic) {
				this.heuristic = cost;
			}
		}
		
		// Sweep all tasks to pick up
		for(Task taskToPickup : pettera) {
			double cost = vehicle.costPerKm() * currentCity.distanceTo(taskToPickup.pickupCity) +
					vehicle.costPerKm() * taskToPickup.pickupCity.distanceTo(taskToPickup.deliveryCity);
			
			if(cost > this.heuristic) {
				this.heuristic = cost;
			}
		}
		
		return;
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
				", pettera=" + printTaskSet(pettera) + ", costSoFar=" + costSoFar + ", depth=" + depth + "]";
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
			s = this.fatherArc.getStart().printPathSoFar() + this.fatherArc.getType() + "\n";
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
