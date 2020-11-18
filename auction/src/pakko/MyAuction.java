package pakko;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import planning.Planner;
import planning.Solution;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class MyAuction implements AuctionBehavior {
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private List<Vehicle> vehicles;

	private long timeoutBid;
	private long timeoutMargin;
	private double epsilon;
	private Planner planner;

	private Set<Task> wonAndPendingTasks;
	private Solution currentSolution;
	private Task pendingTask;
	private Solution pendingSolution;
	
	private Prezzer prezzer;


	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file: " + exc);
		}

		// The bid method cannot execute more than timeoutBid milliseconds
		this.timeoutBid = ls.get(LogistSettings.TimeoutKey.BID);
		// Reads the user-chosen timeout margin from the configuration file
		this.timeoutMargin = agent.readProperty("timeout-margin", Long.class, 100L);
		// Reads the user-chosen epsilon parameter from the configuration file
		this.epsilon = agent.readProperty("epsilon", Double.class, 0.2);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.planner = new Planner(this.vehicles);

		this.wonAndPendingTasks = new HashSet<Task>();
		this.currentSolution = new Solution(vehicles);	// Empty solution
		this.pendingTask = null;
		this.pendingSolution = null;
		
		this.prezzer = new Prezzer(vehicles, agent);
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		// Is previous == pendingTask?
		if(previous.id != pendingTask.id) {
			throw new RuntimeException("Provided previous is not the same as pendingTask. Previous: " + previous + ", PendingTask: "+ pendingTask);
		}
		// Is pendingSolution != null?
		if(pendingSolution == null) {
			throw new RuntimeException("pendingSolution is null");
		}
		// Does wonAndPendingTasks contain pendingTask?
		if(!wonAndPendingTasks.contains(pendingTask)) {
			throw new RuntimeException("wonAndPendingTasks does not contain pendingTask");
		}
		
		if (winner == agent.id()) {
			System.out.println("auctionResult WON task: " + previous);	
			currentSolution = pendingSolution;
		} else {
			System.out.println("auctionResult LOST task: " + previous);
			wonAndPendingTasks.remove(pendingTask);
		}
		pendingTask = null;
		pendingSolution = null;
		
		// Inform the prezzer
		prezzer.auctionResult(previous, winner, bids);
		
		return;
	}


	@Override
	public Long askPrice(Task task) {
		System.out.println("askPrice task: " +  task);
		
		// Is pendingTask == null?
		if(pendingTask != null) {
			throw new RuntimeException("pendingTasks is not null");
		}
		// Is pendingSolution == null?
		if(pendingSolution != null) {
			throw new RuntimeException("pendingSolution is not null");
		}

		// Get current cost
		double currentCost = 0.0;
		if(currentSolution != null) {
			currentCost = currentSolution.getCost();
		}

		// Can we carry this task?
		if (!canCarry(task)) {
			return null;
		}

		// Compute plan for accepting task
		pendingTask = task;
		wonAndPendingTasks.add(task);
		pendingSolution = planner.plan(currentSolution, task, epsilon, timeoutBid-timeoutMargin);

		// Compute marginal cost
		double marginalCost = pendingSolution.getCost() - currentCost;
		
		Long bid = prezzer.askPrice(marginalCost);
		
		System.out.println("askPrice bid: " +  Math.round(bid));	
		
		return (long) Math.round(bid);
	}


	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		// Is vehicles the same as the one we already have?
		if(!vehicles.equals(this.vehicles)) {
			throw new RuntimeException("Provided vehicles is not the same as the one we already had");
		}
		// Is tasks the same as the one we already have?
		if(!sameTasks(tasks)) {
			throw new RuntimeException("Provided tasks is not the same as wonAndPendingTasks");
		}
		if (currentSolution == null) {
			currentSolution = new Solution(vehicles, tasks);
		}
		currentSolution.updateTasks(tasks);
		
		return currentSolution.getJointPlan();
	}


	/* CHECKS */


	/**
	 * Checks whether any vehicle owned by the agent can carry task.
	 */
	private boolean canCarry(Task task) {
		for(Vehicle vehicle : vehicles) {
			if(vehicle.capacity() >= task.weight) {
				return true;
			}
		}

		return false;
	}


	/**
	 * Check whether tasks is the same as wonAndPendingTasks
	 */
	private boolean sameTasks(TaskSet tasks) {
		if(wonAndPendingTasks.size() != tasks.size()) {
			return false;
		}
		for(Task task1 : tasks) {
			boolean found = false;
			for(Task task2 : wonAndPendingTasks) {
				if(task1.id == task2.id) {
					found = true;
					break;
				}
			}
			if(found == false) {
				return false;
			}
		}
		return true;
		
	}

}
