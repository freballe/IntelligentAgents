package planning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import planning.Azione.Type;

public class Solution {
	// The maximum number of times we look for a different vehicle to carry a task
	private static final int MAXDIFFVEHICLES = 20;
	// The log level
	private static final Level LOGLEVEL = Level.OFF;
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
	private final static Logger logger = Logger.getLogger("diomaiala");


	static {
		logger.setLevel(LOGLEVEL);
	}
	

	/* CONSTRUCTORS */


	/**
	 * Constructs an initial solution.
	 * @param vehicles: the list of vehicles, whose order matters
	 * @param tasks: the tasks to assign
	 */
	public Solution(List<Vehicle> vehicles, Set<Task> tasks) {
		this.vehicles = vehicles;
		this.firstActions = new HashMap<Vehicle, Node<Azione>>();
		this.nTasks = new HashMap<Vehicle, Integer>();
		this.totalTasks = tasks.size();
		this.coin = new Random(15);

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
		this.nTasks = new HashMap<Vehicle, Integer>();
		this.totalTasks = other.totalTasks;
		this.coin = new Random(7);
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

		// Shallow copy of each of the nTasks
		for(Vehicle vehicle : vehicles) {
			int numTasks = other.nTasks.get(vehicle);
			this.nTasks.put(vehicle, numTasks);
		}

		return;
	}

	
	public void updateTasks(TaskSet tasks) {
		Map<Integer, Task> idToNewTask = new HashMap<>();
		for(Task task : tasks) {
			idToNewTask.put(task.id, task);
		}
		
		for(Vehicle vehicle : vehicles) {
			Node<Azione> head = firstActions.get(vehicle);
			if(head == null) {
				continue;
			}
			for(Node<Azione> node : head) {
				Task oldTask = node.getElement().getTask();
				Task newTask = idToNewTask.get(oldTask.id);
				node.getElement().setTask(newTask);
			}
		}
		
		
	}

	/* SLS METHODS */	


	/**
	 * @return the list of plans for each vehicle, in the order they appear in the list "vehicles".
	 */
	public List<Plan> getJointPlan(){
		List<Plan> jointPlan = new LinkedList<Plan>();

		// Fill the joint plan, vehicle by vehicle
		for(Vehicle vehicle : vehicles) {
			City currentCity = vehicle.getCurrentCity();
			Plan plan = new Plan(currentCity);

			// If no tasks to deliver, skip
			if(nTasks.get(vehicle) == 0) {
				// Append the plan to the end of joint plan
				jointPlan.add(plan);
				continue;
			}

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
		logger.fine("Vamos a getRandomVehicle");
		vez = getRandomVehicle();
		// Get one of its tasks a random
		logger.fine("Vamos a getRandomTask");
		taz = getRandomTask(vez);

		// Try all possible reorderings of taz within vez
		logger.info("Vamos a findBestAssignment con vez-vez");
		currentNeighbour = findBestAssignment(vez, vez, taz);
		bestNeighbour = currentNeighbour;

		// If just one vehicle, return
		if(vehicles.size() == 1) {
			return bestNeighbour;
		}

		// Get a different random vehicle
		logger.fine("Vamos a ghettare un different randomVehicle");
		Vehicle zio = vez;
		int i;
		for(i = 0; (i < MAXDIFFVEHICLES) && (zio == vez || zio.capacity() < taz.weight); i++) {
			zio = getRandomVehicle();
		}
		// If we ran out of iterations, fall back to vez
		if(i == MAXDIFFVEHICLES) {
			return bestNeighbour;
		}
		logger.info("Vamos a findBestAssignment con vez-zio");
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
		logger.fine("Vamos a getRandomVehicle");
		vez = getRandomVehicle();
		// Get one of its tasks a random
		logger.fine("Vamos a getRandomTask");
		taz = getRandomTask(vez);

		// Get another random vehicle that can carry task
		logger.fine("Vamos a ghettare un different randomVehicle");
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
		logger.info("Vamos a findRandomAssignment con vez-zio");
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
		throw new RuntimeException("Could not find random vehicle. taskNum = " + taskNum +
				", cumul = " + cumul + ", totalTasks = " + totalTasks);
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
		// No need to check for nTasks == 0, since this is only called on a vehicle returned by getRandomVehicle
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
		throw new RuntimeException("Could not find random task. taskNum = " + taskNum +
				", nPickup = " + nPickup + ", nTasks[vehicle] = " + nTasks.get(vehicle));
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
		Solution currentSolution = new Solution(this);
		Solution bestSolution;
		Node<Azione> pickupNode = new Node<Azione>(new Azione(task, Type.PICKUP));
		Node<Azione> deliveryNode = new Node<Azione>(new Azione(task, Type.DELIVERY));
		
		if(logger.isLoggable(Level.FINE)) {
			logger.fine("INIZIO FINDBEST: nTotalTasks = " + currentSolution.getNumTasks());
			currentSolution.checkIntegrity();
		}

		logger.fine("oldVeh = " + oldVeh + ", newVeh = " + newVeh);

		// Unassign task from oldVeh
		logger.fine("Unassigning task from oldVeh");
		//currentSolution.checkIntegrity();
		currentSolution.unassignTask(oldVeh, task);	// Until the end of the loop, currentSolution.nTasks is wrong
		//currentSolution.deltaNTasks(oldVeh, -1);
		//currentSolution.deltaNTasks(newVeh, +1);

		// Initialise bestSolution
		bestSolution = new Solution(this);

		// Outer do-while: place pickupNode
		logger.fine("Begining outer loop");
		pickupNode.unhook();
		pickupNode.insertBefore(currentSolution.firstActions.get(newVeh));
		currentSolution.firstActions.put(newVeh, pickupNode);
		// Variables for outer loop
		Node<Azione> lastSwitchedOuter;
		int gropponeOuter = 0;
		int nIterOuter = 0;
		do{
			// Inner do-while: place deliveryNode
			logger.fine("Outer iteration " + nIterOuter++ + ".Beginning inner loop.");
			deliveryNode.unhook();
			deliveryNode.insertAfter(pickupNode);
			// Variables for inner loop
			Node<Azione> lastSwitchedInner;
			int gropponeInner = gropponeOuter + task.weight;
			int nIterInner = 0;
			do {
				logger.fine("Inner iteration " + nIterInner++);

				// Break right away if capacity exceeded: cannot delay delivery further
				if(gropponeInner > newVeh.capacity()) {
					logger.fine("Inner loop: capacity exceeded. Breaking");
					break;
				}

				// Copy in bestSolution if currentSolution is better
				if(bestSolution.getCost() > currentSolution.getCost()) {
					logger.fine("Inner loop: found better solution. Copying");
					//currentSolution.checkIntegrity();
					bestSolution = new Solution(currentSolution);
					if(logger.isLoggable(Level.FINE)) {
						logger.fine("DENTROOO FINDBEST: nTotalTasks = " + bestSolution.getNumTasks());
						logger.fine("DENTROOO FINDBEST: isTaskStillPresent = " + 
								bestSolution.isTaskPresent(task));
					}
				}

				// Push the delivery back by one position
				lastSwitchedInner = deliveryNode.pushBack();
				logger.fine("lastSwitchedInner = " + lastSwitchedInner + ", deliveryNode.previous = " 
						+ deliveryNode.getPrevious() + ", deliveryNode.next = " + deliveryNode.getNext());
				// If delivery was already at the end, break
				if (lastSwitchedInner == null) {
					logger.fine("Inner loop: reached last position for delivery");
					break;
				}
				// Otherwise, if we went past a pickup, increase gropponeInner
				if(lastSwitchedInner.getElement().getType() == Type.PICKUP) {
					gropponeInner += lastSwitchedInner.getElement().getTask().weight;
				}
				// Otherwise, decrease gropponeInner
				else {
					gropponeInner -= lastSwitchedInner.getElement().getTask().weight;
				}
			}while(true);

			// Inner loop ended
			logger.fine("Inner loop ended after " + nIterInner + " iterations");
			deliveryNode.unhook();

			// Push the pickup back by one position
			lastSwitchedOuter = pickupNode.pushBack();
			// Notify to firstActions that pickupNode has been pushed back
			if(currentSolution.firstActions.get(newVeh) == pickupNode && pickupNode.getPrevious() != null) {
				currentSolution.firstActions.put(newVeh, pickupNode.getPrevious());
			}
			logger.fine("lastSwitchedOuter = " + lastSwitchedOuter + ", pickupNode.previous = " 
					+ pickupNode.getPrevious() + ", pickupNode.next = " + pickupNode.getNext());
			// If pickup was already at the end, break
			if (lastSwitchedOuter == null) {
				logger.fine("Outer loop: reached last position for pickup");
				break;
			}
			// Otherwise, if we went past a pickup, increase gropponeOuter
			if(lastSwitchedOuter.getElement().getType() == Type.PICKUP) {
				gropponeOuter += lastSwitchedOuter.getElement().getTask().weight;
			}
			// Otherwise, decrease gropponeOuter
			else {
				gropponeOuter -= lastSwitchedOuter.getElement().getTask().weight;
			}
		}while(true);

		bestSolution.updateNTasks();
		if(logger.isLoggable(Level.FINE)) {
			logger.fine("FINE FINDBEST: nTotalTasks = " + bestSolution.getNumTasks());
			logger.fine("FINE FINDBEST: isTaskStillPresent = " + bestSolution.isTaskPresent(task));
			bestSolution.checkIntegrity();
		}
		return bestSolution;
	}


	private boolean isTaskPresent(Task task) {
		boolean found = false;
		for(Vehicle vehicle : vehicles) {
			if(firstActions.get(vehicle) == null) {
				continue;
			}
			for(Node<Azione> node : firstActions.get(vehicle)) {
				if(node.getElement().getTask() == task) {
					found = true;
					break;
				}
			}				
		} 
		return found;
	}


	private void updateNTasks() {
		for(Vehicle vehicle : vehicles) {
			int numNodes = 0;
			if(firstActions.get(vehicle) == null) {
				numNodes = 0;
			}else {
				for(Node<Azione> node : firstActions.get(vehicle)) {
					numNodes++;
				}				
			}
			this.nTasks.put(vehicle, numNodes/2);
		}
	}


	// return number of tasks in the system
	private int getNumTasks() {
		int numNodes = 0;
		for(Vehicle vehicle : vehicles) {
			if(firstActions.get(vehicle) == null) {
				continue;
			}
			
			for(Node<Azione> node : firstActions.get(vehicle)) {
				numNodes++;
			}
		}
		return numNodes/2;
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
		Solution currentSolution = new Solution(this);
		Node<Azione> pickupNode = new Node<Azione>(new Azione(task, Type.PICKUP));
		Node<Azione> deliveryNode = new Node<Azione>(new Azione(task, Type.DELIVERY));
		
		if(logger.isLoggable(Level.FINE)) {
			logger.fine("INIZIO FINDBEST: nTotalTasks = " + currentSolution.getNumTasks());
			currentSolution.checkIntegrity();
		}
		
		// Unassign task from oldVeh
		logger.fine("Unassigning task from oldVeh");
		currentSolution.unassignTask(oldVeh, task);	// Until the end of the loop, currentSolution.nTasks is wrong
		//currentSolution.deltaNTasks(oldVeh, -1);
		//currentSolution.deltaNTasks(newVeh, +1);

		int n = currentSolution.nTasks.get(newVeh);
		// Upper bound on the number of possible positions of pickup and delivery
		int counter = coin.nextInt((2*n+1) * (n+1));  

		// Outer do-while: place pickupNode
		logger.fine("Begining outer loop");
		pickupNode.unhook();
		pickupNode.insertBefore(currentSolution.firstActions.get(newVeh));
		currentSolution.firstActions.put(newVeh, pickupNode);
		// Variables for outer loop
		Node<Azione> lastSwitchedOuter;
		int gropponeOuter = 0;
		int nIterOuter = 0;
		do{
			// Inner do-while: place deliveryNode
			logger.fine("Outer iteration " + nIterOuter++ + ".Beginning inner loop.");
			deliveryNode.unhook();
			deliveryNode.insertAfter(pickupNode);
			// Variables for inner loop
			Node<Azione> lastSwitchedInner;
			int gropponeInner = gropponeOuter + task.weight;
			int nIterInner = 0;
			do {
				logger.fine("Inner iteration " + nIterInner++);

				// Break right away if capacity exceeded: cannot delay delivery further
				if(gropponeInner > newVeh.capacity()) {
					logger.fine("Inner loop: capacity exceeded. Breaking");
					break;
				}

				counter--;
				if(counter == 0) {
					currentSolution.updateNTasks();
					if(logger.isLoggable(Level.FINE)) {
						logger.fine("FINE FINDBEST: nTotalTasks = " + currentSolution.getNumTasks());
						logger.fine("FINE FINDBEST: isTaskStillPresent = " + 
								currentSolution.isTaskPresent(task));
						currentSolution.checkIntegrity();
					}
				
					return currentSolution;
				}

				// Push the delivery back by one position
				lastSwitchedInner = deliveryNode.pushBack();
				logger.fine("lastSwitchedInner = " + lastSwitchedInner + ", deliveryNode.previous = " 
						+ deliveryNode.getPrevious() + ", deliveryNode.next = " + deliveryNode.getNext());
				// If delivery was already at the end, break
				if (lastSwitchedInner == null) {
					logger.fine("Inner loop: reached last position for delivery");
					break;
				}
				// Otherwise, if we went past a pickup, increase gropponeInner
				if(lastSwitchedInner.getElement().getType() == Type.PICKUP) {
					gropponeInner += lastSwitchedInner.getElement().getTask().weight;
				}
				// Otherwise, decrease gropponeInner
				else {
					gropponeInner -= lastSwitchedInner.getElement().getTask().weight;
				}
			}while(true);

			// Inner loop ended
			logger.fine("Inner loop ended after " + nIterInner + " iterations");
			deliveryNode.unhook();

			// Push the pickup back by one position
			lastSwitchedOuter = pickupNode.pushBack();
			// Notify to firstActions that pickupNode has been pushed back
			if(currentSolution.firstActions.get(newVeh) == pickupNode && pickupNode.getPrevious() != null) {
				currentSolution.firstActions.put(newVeh, pickupNode.getPrevious());
			}
			logger.fine("lastSwitchedOuter = " + lastSwitchedOuter + ", pickupNode.previous = " 
					+ pickupNode.getPrevious() + ", pickupNode.next = " + pickupNode.getNext());
			// If pickup was already at the end, break
			if (lastSwitchedOuter == null) {
				logger.fine("Outer loop: reached last position for pickup");
				break;
			}
			// Otherwise, if we went past a pickup, increase gropponeOuter
			if(lastSwitchedOuter.getElement().getType() == Type.PICKUP) {
				gropponeOuter += lastSwitchedOuter.getElement().getTask().weight;
			}
			// Otherwise, decrease gropponeOuter
			else {
				gropponeOuter -= lastSwitchedOuter.getElement().getTask().weight;
			}
		}while(true);
		
		// If we haven't yet found anything, just return
		deliveryNode.insertAfter(pickupNode);
		currentSolution.updateNTasks();
		if(logger.isLoggable(Level.FINE)) {
			logger.fine("FINE FINDBEST: nTotalTasks = " + currentSolution.getNumTasks());
			logger.fine("FINE FINDBEST: isTaskStillPresent = " + 
					currentSolution.isTaskPresent(task));
			currentSolution.checkIntegrity();
		}
	
		return currentSolution;

	}


	private void unassignTask(Vehicle vehicle, Task task) {
		Node<Azione> pickupNode = null;
		Node<Azione> deliveryNode = null;
		Node<Azione> headNode = this.firstActions.get(vehicle);

		// Find the pickup and the delivery nodes associated to task
		logger.fine("Finding pickupNode and deliveryNode");
		for(Node<Azione> node : headNode) {
			if(node.getElement().getTask() != task) {
				continue;
			}
			if(node.getElement().getType() == Type.PICKUP) {
				pickupNode = node;
			} else {
				deliveryNode = node;
				break;
			}
		}
		
		// Assert both non null
		if(pickupNode == null) {
			throw new RuntimeException("pickupNode = null! deliveryNode = " + deliveryNode);
		}
		if(deliveryNode == null) {
			throw new RuntimeException("deliveryNode = null! pickupNode = " + pickupNode);
		}
		
		// Update firstActions
		if(headNode == pickupNode) {
			Node<Azione> restoredHead = pickupNode.getNext();
			if(restoredHead == deliveryNode) {
				restoredHead = restoredHead.getNext();
			}
			
			this.firstActions.put(vehicle, restoredHead);
		}

		// Unhook them
		logger.fine("Unhooking pickupNode and deliveryNode");
		pickupNode.unhook();
		deliveryNode.unhook();

		return;
	}


	private void deltaNTasks(Vehicle vehicle, int delta) {
		int numTasks = this.nTasks.get(vehicle);
		this.nTasks.put(vehicle, numTasks+delta);
		return;
	}


	/* GETTERS AND SETTERS */


	/**
	 * @return the cost of this solution.
	 */
	public double getCost() {
		initCost();
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


	public void checkIntegrity() {
		// Sum of the nTasks: must be totalTasks
		int cumul = 0;

		// Check every vehicle's integrity
		for(Vehicle vehicle : vehicles) {
			int numTasks = nTasks.get(vehicle);
			Node<Azione> headNode = firstActions.get(vehicle);
			int nPickup = 0;
			int nDelivery = 0;

			// Check numTask = 0 => headNode = null
			if(numTasks == 0) {
				if(headNode != null) {
					throw new RuntimeException("Integrity error: for vehicle = " + vehicle + ", numTasks = 0" + 
							" but headNode = " + headNode);
				}
				continue;
			}
			// Check headNode = null => numTasks = 0
			if(headNode == null) {
				throw new RuntimeException("Integrity error: for vehicle = " + vehicle + ", headNode = null but"
						+ " numTasks = " + numTasks);
			}
			// Check headNode.previous == null
			if(headNode.getPrevious() != null) {
				throw new RuntimeException("Integrity error: for vehicle = " + vehicle + ", headNode = " +
						headNode + ", headNode.previous = " + headNode.getPrevious());
			}

			for(Node<Azione> node : firstActions.get(vehicle)) {
				if(node.getElement().getType() == Type.PICKUP) {
					nPickup++;
				} else {
					nDelivery++;
				}
			}

			// Check nPickup == nDelivery
			if(nPickup != nDelivery) {
				throw new RuntimeException("Integrity error: nPickup != nDelivery. nPickup = " + nPickup + 
						", nDelivery = " + nDelivery + ", numTasks = " + numTasks + ", vehicle = " + vehicle);
			}
			// Check nPickup == nDelivery == numTasks
			if(nPickup != numTasks) {
				throw new RuntimeException("Integrity error: nDelivery == nPickup != numTasks. nPickup = " + 
						nPickup + ", numTasks = " + numTasks + ", vehicle = " + vehicle);
			}

			cumul += numTasks;			
		}

		// Check cumul == totalTasks
		if(cumul != totalTasks) {
			throw new RuntimeException("Integrity error: cumul != totalTasks. cumul = " + cumul +
					", totalTasks = " + totalTasks);
		}
	}


	/**
	 * Computes the variation of the cost when going from the sequence of visited cities
	 * A -> B -> C to the sequence A -> C.
	 * @param costPerKm: the cost per unit distance of the vehicle
	 * @param a: the first city in both sequences
	 * @param b: the city to be removed
	 * @param c: the city to be pulled ahead. Can be null.
	 */
	private void deltaCostUnhook(int costPerKm, City a, City b, City c) {
		double delta = 0;

		// If C is null, then we are just removing B from the end of the sequence
		if(c != null) {
			delta -= b.distanceTo(c);
			delta += a.distanceTo(c);
		}
		delta -= a.distanceTo(b);

		this.cost += delta * costPerKm;
	}


	/**
	 * Computes the variation of the cost when going from the sequence of visited cities
	 * A -> C to the sequence A -> B -> C.
	 * @param costPerKm: the cost per unit distance of the vehicle
	 * @param a: the first city in both sequences
	 * @param b: the city to be inserted in the middle
	 * @param c: the city to be pushed back. Can be null.
	 */
	private void deltaCostInsert(int costPerKm, City a, City b, City c) {
		double delta = 0;

		// If C is null, then we are just appending B at the end of the sequence
		if(c != null) {
			delta -= a.distanceTo(c);
			delta += b.distanceTo(c);
		}
		delta += a.distanceTo(b);

		this.cost += delta * costPerKm;
	}


	/**
	 * Computes the variation of the cost when going from the sequence of visited cities
	 * A -> B -> C -> D to the sequence A -> C -> B -> D.
	 * @param costPerKm: the cost per unit distance of the vehicle
	 * @param a: the first city in both sequences
	 * @param b: the city to be pushed back
	 * @param c: the city to be pulled ahead
	 * @param d: the last city in both sequences. Can be null
	 */
	private void deltaCostSwap(int costPerKm, City a, City b, City c, City d) {
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

		this.cost += delta * costPerKm;
	}

}
