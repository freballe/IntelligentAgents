package peppo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

class Solution {
	// The first action for each agent
	private Map<Vehicle, Node<Azione>> firstActions;
	// Only used to respect the order of vehicles
	private List<Vehicle> vehicles;
	// Cost of this solution:
	private double cost;
	
	
	
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
	
	
	/* GETTERS AND SETTERS */
	
	
	double getCost() {
		return cost;
	}

	
	
	public Solution getRandomNeighbour() {
		// TODO Auto-generated method stub
		return null;
	}


	public static Solution randomInitialSolution() {
		// TODO Auto-generated method stub
		return null;
	}


	public Solution getBestNeighbour() {
		
	}
	

}
