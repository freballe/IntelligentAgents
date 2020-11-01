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
	// Random coins
	private Random coin;


	
	/* CONSTRUCTORS */
	
	
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
			} while(vez.capacity() < task.weight);
			
			// Create Azioni and Nodes
			Azione pickup = new Azione(task, Type.PICKUP);
			Azione delivery = new Azione(task, Type.DELIVERY);
			Node<Azione> pickupNode = new Node<Azione>(pickup);
			Node<Azione> deliveryNode = new Node<Azione>(delivery);
			
			// Insert pickup and delivery to the head of the list of actions, and increase nTasks
			deliveryNode.insertBefore(firstActions.get(vez));
			pickupNode.insertBefore(deliveryNode);
			firstActions.put(vez, pickupNode);
			oldNTasks = nTasks.get(vez);
			nTasks.put(vez, oldNTasks+1);
		}

		// Compute the cost of this solution
		this.initCost();
		
		return;
	}
	
	
	private Solution(Solution other) {
		this.vehicles = other.vehicles;
		this.firstActions = new HashMap<Vehicle, Node<Azione>>();
		this.nTasks = new HashMap<Vehicle, Integer>(other.nTasks);
		this.totalTasks = other.totalTasks;
		this.coin = new Random();
		this.cost = other.cost;
		
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


	Solution getBestNeighbour() {
		Vehicle vez = getRandomVehicle();
		Vehicle zio;
		Task taz = getRandomTask(vez);
		Solution bestNeighbour;
		Solution currentNeighbour;

		// Try all possible reorderings of taz within vez
		currentNeighbour = findBestAssignment(vez, taz);
		bestNeighbour = currentNeighbour;

		// If just one vehicle, return
		if(vehicles.size() == 1) {
			return bestNeighbour;
		}

		// Get a different random vehicle
		zio = vez;
		int i;
		for(i = 0; (i < MAXDIFFVEHICLES) && (zio == vez || zio.capacity() < taz.weight); i++) {
			zio = getRandomVehicle();
		}
		// If we ran out of iterations, fall back to vez
		if(i == MAXDIFFVEHICLES) {
			return bestNeighbour;
		}

		// Try all possible orderings of taz inside zio
		currentNeighbour = findBestAssignment(zio, taz);
		if(currentNeighbour.getCost() < bestNeighbour.getCost()) {
			bestNeighbour = currentNeighbour;
		}

		return bestNeighbour;
	}


	Solution getRandomNeighbour() {
		Vehicle vez = getRandomVehicle();
		Vehicle zio;
		Task taz = getRandomTask(vez);

		// Get a random vehicle that can carry task
		zio = vez;
		int i;
		for(i = 0; i < MAXDIFFVEHICLES && zio.capacity() < taz.weight; i++) {
			zio = getRandomVehicle();
		}
		// If we ran out of iterations, fall back to vez
		if(i == MAXDIFFVEHICLES) {
			zio = vez;
		}

		// Try a random ordering of taz inside zio
		return findRandomAssignment(zio, taz);
	}


	/* HELPERS */

	
	private Vehicle getRandomVehicle() {
		int taskNum = coin.nextInt(totalTasks);

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


	private Task getRandomTask(Vehicle vehicle) {
		int taskNum = coin.nextInt(nTasks.get(vehicle));

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


	private Solution findBestAssignment(Vehicle vehicle, Task task) {
		return null;
	}


	private Solution findRandomAssignment(Vehicle vehicle, Task task) {
		return null;
	}


	/* GETTERS AND SETTERS */


	double getCost() {
		return cost;
	}
	

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
	

}
