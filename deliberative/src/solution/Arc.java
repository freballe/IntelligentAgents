package solution;

import java.util.LinkedList;
import java.util.List;

import logist.plan.Action;


/**
 * Class representing an (oriented) arc between two states.
 *
 */
public class Arc {
	private State start;
	private State end;
	private double cost;
	
	// The list of elementary Actions associated to this Arc
	private List<Action> actions;

	
	
	public Arc(State start) {
		super();
		this.start = start;
		this.actions = new LinkedList<Action>();
		this.cost = 0.0;
	}

	
	public State getStart() {
		return start;
	}

	
	public State getEnd() {
		return end;
	}
	
	
	public void setEnd(State end) {
		this.end = end;
	}

	
	public double getCost() {
		return cost;
	}
	
	
	public void setCost(double cost) {
		this.cost = cost;
	}


	/**
	 * Adds "action" to the list of Actions.
	 * @param action the Action to be added
	 */
	public void addAction(Action action) {
		actions.add(action);
	}
	
	
	/**
	 * Does not make a copy of its internal list of Actions.
	 * @return the list of Actions associated to this Arc
	 */
	public List<Action> getActions() {
		return actions;
		
	}
}
