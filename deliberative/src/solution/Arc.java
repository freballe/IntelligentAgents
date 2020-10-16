package solution;

import java.util.LinkedList;
import java.util.List;

import logist.plan.Action;

public class Arc {
	private State start;
	private State end;
	private double cost;
	
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
	
	public void addActionAndCost(Action action, double cost) {
		actions.add(action);
		this.cost += cost;
	}
	
	public List<Action> getActions() {
		return actions;
		
	}
}
