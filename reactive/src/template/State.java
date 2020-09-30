package template;

import java.util.LinkedList;
import java.util.List;

import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

class State {
	
	private City currCity;
	private City destCity;	// Destination of the task (null <==> no task is available)
	
	private double value;
	private City bestAction;	// Best city to move to (null <==> accept the task)
	
	private Vehicle vehicle;
	private Topology topo;
	private TaskDistribution td;
	
	
	public State(City currCity, City destCity, Vehicle vehicle, Topology topo, TaskDistribution td) {
		this.currCity = currCity;
		this.destCity = destCity;
		this.vehicle = vehicle;
		this.topo = topo;
		this.td = td;
		
		// TODO: initialise value and bestAction
	}



	public double getValue() {
		return value;
	}
	


	public void setValue(double value) {
		this.value = value;
	}
	


	public City getBestAction() {
		return bestAction;
	}
	


	public void setBestAction(City bestAction) {
		this.bestAction = bestAction;
	}

	/**
	 * @return A list of all actions possible from this state.
	 */
	public List<City> possibleActions() {
		List<City> actions = new LinkedList<City>(currCity.neighbors());
		
		// destCity != null <==> a task is available
		if(destCity != null) {
			// A null action means to accept the task
			actions.add(null);
		}
		
		return actions;
	}

	/**
	 * Given a state and an action, various next states are possible, but they all share the same currCity.
	 * @param action The action to perform from this state.
	 * @return The next city the vehicle will be in
	 */
	public City getNextCity(City action) {
		// action = null <==> the task is accepted
		// action != null <==> the task is rejected, and the vehicle moves to the city "action"
		return (action == null) ? destCity : action;
	}
	
	/**
	 * @return The reward associated to the task coded by this state, assuming there is one.
	 */
	public double getTaskReward() {
		// Doesn't check destCity != null
		return td.reward(currCity, destCity);
	}
	
	/**
	 * @return The probability of there being a task from currCity to destCity (null included)
	 */
	public double getProb() {
		// Here, destCity = null makes sense
		return td.probability(currCity, destCity);
	}
	
	/**
	 * Implements the R(s,a) table.
	 * @param action The action to be done from this state.
	 * @return The net reward associated with taking the given action from this state.
	 */
 	public double reward(City action) {
		double rew = 0.0;
		City nextCity = getNextCity(action);
		
		rew -= currCity.distanceTo(nextCity) * vehicle.costPerKm();
		if(action == null) {
			rew += getTaskReward();
		}
		
		return rew;
	}
	
}
