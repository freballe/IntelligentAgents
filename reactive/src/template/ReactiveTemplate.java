package template;

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

	private Agent agent;
	private Topology topo;
	private TaskDistribution td;
	
	private int numActions;
	
	private double gamma;	// The discount factor
	private State states[][][];	// The 3D array of all possible states, indexed by (vehicle, currCity, destCity)
	// Since all task involve two distinct cities, the triple (v,i,i) is used to code a state where no task is available
	

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.agent = agent;
		this.topo = topology;
		this.td = td;
		this.numActions = 0;
		this.gamma = discount;
		

		// Check that sum over j is 1 for every i
		printTaskDistribution();
		
		// Pre-construct all states
		constructStates();
		
		// Reinforcement learning
		learnOptimalStrategy();

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		// currCityID = destCityID <==> no task is available
		int currCityID = vehicle.getCurrentCity().id;
		int destCityID = (availableTask == null) ? currCityID : availableTask.deliveryCity.id;
		
		// The state holds the information about the best action to do (learnt with RL)
		State currState = states[vehicle.id()][currCityID][destCityID];
		// An action is coded as the best city to move to (null means to accept the task)
		City bestAction = currState.getBestAction();

		
		if(bestAction == null) {
			// We trust the RL algorithm, and don't check whether availableTask == null
			action = new Pickup(availableTask);
		} else {
			// If bestAction != null, we refuse the task
			action = new Move(bestAction);
		}
		

		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + 
								agent.getTotalProfit() + " (average profit: " + 
								(agent.getTotalProfit() / (double)numActions) + ")");
		}
		numActions++;

		return action;
	}
	

	/** 
	 * Prints out the distribution.
	 * Check that, on each row, the sum is one, and that the diagonal is 0. 
	 */
	private void printTaskDistribution() {
		// Column names
		for(int i=-1; i < topo.size(); i++) {
			System.out.printf("\t %d", i);
		}
		System.out.println("\t Sum");
		
		// Rows
		for(City from : topo) {
			double sum = 0.0;
			// Row name
			System.out.printf("%d \t", from.id);
			
			// Null destination
			System.out.printf("%.3f   ", td.probability(from, null));
			sum += td.probability(from, null);
			
			// Other cities
			for(City to : topo) {
				System.out.printf("%.3f   ", td.probability(from, to));
				sum += td.probability(from, to);
			}
			
			// Sum
			System.out.printf("%.3f\n", sum);
		}
		
		System.out.print("\n\n");
		
		return;
	}

	/**
	 * Pre-constructs all the states, so that they don't have to be constructed at each action.
	 * The state's internal fields evolve during Value Iteration.
	 */
	private void constructStates() {
		states = new State[agent.vehicles().size()][topo.size()][topo.size()];
		
		for(Vehicle vehicle : agent.vehicles()) {
			for(City curr : topo.cities()) {
				for(City dest : topo.cities()) {
					City destCity = (dest == curr) ? null : dest;
					states[vehicle.id()][curr.id][dest.id] = 
							new State(curr, destCity, vehicle, topo, td);
				}
			}
		}
		
		return;
	}
	
	/**
	 * Implements the Value Iteration algorithm in this setting.
	 */
	private void learnOptimalStrategy() {
		
		// TODO: do value iteration with eps-delta
		for(Vehicle vehicle : agent.vehicles()) {
			// Iterate over vehicles
			
			for(int curr=0; curr < topo.size(); curr++) {
				for(int dest=0; dest < topo.size(); dest++) {
					// Iterate over states
					
					State state = states[vehicle.id()][curr][dest];
					double bestQLO = - Double.MAX_VALUE;	// The new V(s), initialised to -inf
					City bestAction = null;
					
					for(City action : state.possibleActions()) {
						// Iterate over possible actions
						
						double qlo = state.reward(action);	// The Q(s,a) in the slides
						
						int nextCurr = state.getNextCity(action).id;	// All the possible next states share the same currCity
						for(int nextDest=0; nextDest < topo.size(); nextDest++) {
							// Iterate over possible next states
							
							State nextState = states[vehicle.id()][nextCurr][nextDest];
							qlo += gamma * nextState.getProb() * nextState.getValue();
							
							// End of iteration over possible next states
						}
						
						// Update bestQLO and bestAction
						if(qlo > bestQLO) {
							bestQLO = qlo;
							bestAction = action;
						}
						
						// End of iteration over possible actions
					}
					
					// TODO: get variation in V(s) and compare with eps
					// Update value and bestAction in state
					state.setValue(bestQLO);
					state.setBestAction(bestAction);
					
					// End of iteration over states
				}
			}
			
			// End of iteration over vehicles
		}
		
		return;
	}
	
}