package peppo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import peppo.Azione.Type;

class Solution {
	// The maximum number of times we look for a different vehicle to carry a task
	private static final int MAXDIFFVEHICLES = 20;
	// The first action for each agent
	private Map<Vehicle, Node<Azione>> firstActions;
	// List of vehicles: the order is to be respected when returning the joint plan
	private List<Vehicle> vehicles;
	// Map holding, for each vehicle, how many tasks it carries
	private Map<Vehicle, Integer> nTasks;
	// Total number of tasks
	private int totalTasks;
	// Cost of this solution:
	private double cost;
	// PRNG
	private Random coin;



	/* CONSTRUCTORS */


	/**
	 * Constructs an initial solution.
	 * @param vehicles: the list of vehicles, whose order matters
	 * @param tasks: the tasks to assign
	 */
	public Solution(List<Vehicle> vehicles, TaskSet tasks) {
		this.vehicles = vehicles;
		this.firstActions = new HashMap<Vehicle, Node<Azione>>();
		this.nTasks = new HashMap<Vehicle, Integer>();
		this.totalTasks = tasks.size();
		this.coin = new Random();

		// Fill firstActions with null, and nTasks with 0, for each vehicle
		for(Vehicle vehicle : vehicles) {
			firstActions.put(vehicle, null);
			nTasks.put(vehicle, 0);
		}

		// Place all tasks into vehicles
		Iterator<Vehicle> vezIter = vehicles.iterator();
		for(Task task : tasks) {
			Vehicle vez;
			int oldNTasks;

			// Find first vehicle (from where you left) that has enough capacity
			do {
				if(!vezIter.hasNext()) {
					vezIter = vehicles.iterator();
				}
				vez = vezIter.next();
			} while(vez.capacity() < task.weight);	// Loops forever if no suitable vehicle exists

			// Create Azioni and Nodes
			Azione pickup = new Azione(task, Type.PICKUP);
			Azione delivery = new Azione(task, Type.DELIVERY);
			Node<Azione> pickupNode = new Node<Azione>(pickup);
			Node<Azione> deliveryNode = new Node<Azione>(delivery);

			// Insert pickup and delivery to the head of the list of actions
			deliveryNode.insertBefore(firstActions.get(vez));
			pickupNode.insertBefore(deliveryNode);
			firstActions.put(vez, pickupNode);
			// Increase nTasks
			oldNTasks = nTasks.get(vez);
			nTasks.put(vez, oldNTasks+1);
		}

		// Compute the cost of this solution
		this.initCost();

		return;
	}


	/**
	 * Semi-shallow copy. Directly copies the reference of almost all the fields, except for
	 * firstActions (whose vehicle keys are copied by reference and whose node values are recursively
	 * shallow-copied), nTasks(which is shallow-copied), and coin (which is constructed anew).
	 * @param other: the solution to be copied.
	 */
	private Solution(Solution other) {
		this.vehicles = other.vehicles;
		this.firstActions = new HashMap<Vehicle, Node<Azione>>();
		this.nTasks = new HashMap<Vehicle, Integer>(other.nTasks);
		this.totalTasks = other.totalTasks;
		this.coin = new Random();
		this.cost = other.cost;

		// Shallow copy of each of the action nodes
		for(Vehicle vehicle : vehicles) {
			Node<Azione> firstAction = other.firstActions.get(vehicle);

			if(firstAction == null) {
				this.firstActions.put(vehicle, null);
			} else {
				this.firstActions.put(vehicle, firstAction.copy());
			}
		}

		return;
	}


	/* SLS METHODS */	


	/**
	 * @return the list of plans for each vehicle, in the order they appear in the list "vehicles".
	 */
	List<Plan> getJointPlan(){
		List<Plan> jointPlan = new LinkedList<Plan>();

		// Fill the joint plan, vehicle by vehicle
		for(Vehicle vehicle : vehicles) {
			City currentCity = vehicle.getCurrentCity();
			Plan plan = new Plan(currentCity);

			// Fill in each action, with the Moves in between
			for(Node<Azione> actionNode : firstActions.get(vehicle)) {
				// Move to the city where the action takes place
				City actionCity = actionNode.getElement().getCity();
				for(City transitCity : currentCity.pathTo(actionCity)) {
					plan.appendMove(transitCity);
				}

				// Update currentCity
				currentCity = actionCity;

				// Do the action
				plan.append(actionNode.getElement().getAction());
			}

			// Append the plan to the end of joint plan
			jointPlan.add(plan);
		}

		return jointPlan;
	}


	/**
	 * @return the best among a random set of neighbours.
	 */
	Solution getBestNeighbour() {
		Vehicle vez;
		Task taz;
		Solution bestNeighbour;
		Solution currentNeighbour;

		// Find a vehicle with at least a task
		for(vez=getRandomVehicle(); nTasks.get(vez).intValue() == 0; vez=getRandomVehicle());
		// Get one of its tasks a random
		taz = getRandomTask(vez);

		// Try all possible reorderings of taz within vez
		currentNeighbour = findBestAssignment(vez, vez, taz);
		bestNeighbour = currentNeighbour;

		// If just one vehicle, return
		if(vehicles.size() == 1) {
			return bestNeighbour;
		}

		// Get a different random vehicle
		Vehicle zio = vez;
		int i;
		for(i = 0; (i < MAXDIFFVEHICLES) && (zio == vez || zio.capacity() < taz.weight); i++) {
			zio = getRandomVehicle();
		}
		// If we ran out of iterations, fall back to vez
		if(i == MAXDIFFVEHICLES) {
			return bestNeighbour;
		}

		// Try all possible orderings of taz inside zio
		currentNeighbour = findBestAssignment(vez, zio, taz);
		if(currentNeighbour.getCost() < bestNeighbour.getCost()) {
			bestNeighbour = currentNeighbour;
		}

		return bestNeighbour;
	}


	/**
	 * @return a random neighbour.
	 */
	Solution getRandomNeighbour() {
		Vehicle vez;
		Task taz;

		// Find a vehicle with at least a task
		for(vez=getRandomVehicle(); nTasks.get(vez).intValue() == 0; vez=getRandomVehicle());
		// Get one of its tasks a random
		taz = getRandomTask(vez);

		// Get another random vehicle that can carry task
		Vehicle zio = vez;
		int i;
		for(i = 0; (i < MAXDIFFVEHICLES) && (zio.capacity() < taz.weight); i++) {
			zio = getRandomVehicle();
		}
		// If we ran out of iterations, fall back to vez
		if(i == MAXDIFFVEHICLES) {
			zio = vez;
		}

		// Try a random ordering of taz inside zio
		return findRandomAssignment(vez, zio, taz);
	}


	/* HELPERS */


	/**
	 * @return a random vehicle, with probability proportional to the number of carried tasks.
	 */
	private Vehicle getRandomVehicle() {
		// Random int between 0 ant totalTasks-1.
		int taskNum = coin.nextInt(totalTasks);

		// We return as soon as we cumulatively exceed taskNum
		int cumul = 0;
		for(Vehicle vehicle : vehicles) {
			cumul += nTasks.get(vehicle);

			if(cumul > taskNum) {
				return vehicle;
			}
		}

		// Should not happen
		throw new RuntimeException("Could not find random vehicle");
	}


	/**
	 * @param vehicle: the vehicle to sample a task from
	 * @return
	 */
	private Task getRandomTask(Vehicle vehicle) {
		// Random int between 0 and nTasks-1
		int taskNum = coin.nextInt(nTasks.get(vehicle));

		// We return when we encounter the taskNum-th pickup
		int nPickup = 0;
		for(Node<Azione> node : firstActions.get(vehicle)) {
			if(node.getElement().getType() == Type.DELIVERY) {
				continue;
			}

			if(nPickup == taskNum) {
				return node.getElement().getTask();
			}
			nPickup++;
		}

		// Should not happen
		throw new RuntimeException("Could not find random task");
	}


	/**
	 * Copies the current solution, then takes out task from oldVeh, and tries to assign it in every 
	 * possible way to newVeh. Returns the best assignment.
	 * @param oldVeh: the old assignee of task
	 * @param newVeh: the new assignee of task
	 * @param task: the task to be relocated
	 * @return the best assignment
	 */
	private Solution findBestAssignment(Vehicle oldVeh, Vehicle newVeh, Task task) {
		return null;
	}


	/**
	 * Copies the current solution, then takes out task from oldVeh, and assigns it in a 
	 * random admissible way to newVeh.
	 * @param oldVeh: the old assignee of task
	 * @param newVeh: the new assignee of task
	 * @param task: the task to be relocated
	 * @return the random assignment
	 */
	private Solution findRandomAssignment(Vehicle oldVeh, Vehicle newVeh, Task task) {
		return null;
	}


	/* GETTERS AND SETTERS */


	/**
	 * @return the cost of this solution, in constant time.
	 */
	double getCost() {
		return cost;
	}


	/**
	 * Calculates from scratch the cost of this solution, and sets the corresponding field.
	 */
	private void initCost() {
		this.cost = 0;

		// Compute the cost of each vehicle's journey
		for(Vehicle vehicle : vehicles) {
			City currentCity = vehicle.getCurrentCity();
			Node<Azione> firstAction = firstActions.get(vehicle);

			// If no tasks assigned to this vehicle, continue
			if(firstAction == null) {
				continue;
			}

			// nextNode is always one step ahead of currentCity
			for(Node<Azione> nextNode : firstAction) {
				City nextCity = nextNode.getElement().getCity();
				this.cost += vehicle.costPerKm() * currentCity.distanceTo(nextCity);
				currentCity = nextCity;
			}
		}

		return;
	}


	/**
	 * Computes the variation of the cost when going from the sequence of visited cities
	 * A -> C to the sequence A -> B -> C.
	 * @param costPerKm: the cost per unit distance of the vehicle
	 * @param a: the first city in both sequences
	 * @param b: the city to be inserted in the middle
	 * @param c: the city to be pushed back. Can be null.
	 * @return
	 */
	private double deltaCostInsert(int costPerKm, City a, City b, City c) {
		double delta = 0;

		// If C is null, then we are just appending B at the end of the sequence
		if(c != null) {
			delta -= a.distanceTo(c);
			delta += b.distanceTo(c);
		}
		delta += a.distanceTo(b);

		return delta * costPerKm;
	}


	/**
	 * Computes the variation of the cost when going from the sequence of visited cities
	 * A -> B -> C -> D to the sequence A -> C -> B -> D.
	 * @param costPerKm: the cost per unit distance of the vehicle
	 * @param a: the first city in both sequences
	 * @param b: the city to be pushed back
	 * @param c: the city to be pulled ahead
	 * @param d: the last city in both sequences. Can be null
	 * @return
	 */
	private double deltaCostSwap(int costPerKm, City a, City b, City c, City d) {
		double delta = 0;

		// If D is null, then we are just swapping the last two cities in the sequence
		if(d != null) {
			delta -= c.distanceTo(d);
			delta += b.distanceTo(d);
		}
		delta -= a.distanceTo(b);
		delta -= b.distanceTo(c);
		delta += a.distanceTo(c);
		delta += c.distanceTo(b);

		return delta * costPerKm;
	}

}
