package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		// Check that sum over j is 1 for every i
		printTaskDistribution(topology, td);
		
		// Reinforcement learning
		learnOptimalStrategy(topology, td, agent);

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;

		return action;
	}
	
	private void learnOptimalStrategy(Topology topology, TaskDistribution td, Agent agent) {
		// TODO Auto-generated method stub
		
	}

	private void printTaskDistribution(Topology topology, TaskDistribution td) {
		// Prints out the task distribution
		// Column indications
		for(int i=-1; i < topology.size(); i++) {
			System.out.printf("\t %d", i);
		}
		System.out.println("\t Sum");
		// Rows
		for(City from : topology) {
			double sum = 0.0;
			// Row indication
			System.out.printf("%d \t", from.id);
			// Null destination
			System.out.printf("%.3f   ", td.probability(from, null));
			sum += td.probability(from, null);
			// Other cities
			for(City to : topology) {
				System.out.printf("%.3f   ", td.probability(from, to));
				sum += td.probability(from, to);
			}
			// Sum
			System.out.printf("%.3f\n", sum);
		}
		System.out.print("\n\n");
		
		return;
	}
}
