package pakko;

import java.util.List;
import java.util.Random;

import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.task.Task;

public class Prezzer {
	private long defaultMarginalCost;
	private int factorVehicles;
	private double minRatio;
	private double maxRatio;
	private double ratioIncreaseRate;
	private double ratioDecreaseRate;

	private Random random;

	List<Vehicle> vehicles;
	int[] nWonTasks = null;
	Agent agent;
	double interestRatio;

	Prezzer(List<Vehicle> vehicles, Agent agent){
		this.vehicles = vehicles;
		this.agent = agent;
		long seed = -9019554669489983951L * this.vehicles.get(0).homeCity().hashCode() * agent.id();
		this.random = new Random(seed);
		setParameters();
		interestRatio = minRatio;
	}

	//1
		
	void setParameters() {
		// Reads the user-chosen default marginal cost from the configuration file
		defaultMarginalCost = agent.readProperty("def-marg-cost", Long.class, 300L);
		factorVehicles = agent.readProperty("vehicles-factor", Integer.class, 2);		
		// Reads the user-chosen default marginal cost from the configuration file
		minRatio = agent.readProperty("min-ratio", Double.class, 0.05);
		maxRatio = agent.readProperty("max-ratio", Double.class, 0.5);
		ratioIncreaseRate = agent.readProperty("ratio-increase", Double.class, 0.2);
		ratioDecreaseRate = agent.readProperty("ratio-decrease", Double.class, 0.1);

	}
	
	
	//2
	/*
	void setParameters() {
		// Reads the user-chosen default marginal cost from the configuration file
		defaultMarginalCost = agent.readProperty("def-marg-cost", Long.class, 300L);
		factorVehicles = agent.readProperty("vehicles-factor", Integer.class, 2);		
		// Reads the user-chosen default marginal cost from the configuration file
		minRatio = agent.readProperty("min-ratio", Double.class, 0.01);
		maxRatio = agent.readProperty("max-ratio", Double.class, 1.0);
		ratioIncreaseRate = agent.readProperty("ratio-increase", Double.class, 0.5);
		ratioDecreaseRate = agent.readProperty("ratio-decrease", Double.class, 0.3);
	}
	*/
	//3
	/*
	void setParameters() {
		// Reads the user-chosen default marginal cost from the configuration file
		defaultMarginalCost = agent.readProperty("def-marg-cost", Long.class, 300L);
		factorVehicles = agent.readProperty("vehicles-factor", Integer.class, 2);		
		// Reads the user-chosen default marginal cost from the configuration file
		minRatio = agent.readProperty("min-ratio", Double.class, 0.1);
		maxRatio = agent.readProperty("max-ratio", Double.class, 0.3);
		ratioIncreaseRate = agent.readProperty("ratio-increase", Double.class, 0.1);
		ratioDecreaseRate = agent.readProperty("ratio-decrease", Double.class, 0.05);
	}
	*/


	private boolean isFirstPhase() {
		return nWonTasks == null || nWonTasks[agent.id()] < (factorVehicles * vehicles.size());
	}

	public void auctionResult(Task previous, int winner, Long[] bids){
		if (nWonTasks == null) {
			nWonTasks = new int[bids.length];
		}
		int oldBestAdversaryWonTasks = getBestAdversaryWonTasks();

		nWonTasks[winner]++;

		int newBestAdversaryWonTasks = getBestAdversaryWonTasks();

		if(isFirstPhase()) {
			return;
		}
		if (winner == agent.id()) {
			interestRatio *= (1 + ratioIncreaseRate);
			if (interestRatio > maxRatio) {
				interestRatio = maxRatio;
			}
		}
		if (newBestAdversaryWonTasks > oldBestAdversaryWonTasks) {
			interestRatio *= (1 - ratioDecreaseRate);
			if (interestRatio < minRatio) {
				interestRatio = minRatio;
			}
		}
	}

	private int getBestAdversaryWonTasks() {
		int maxValue = 0;
		for(int i = 0; i < nWonTasks.length; i++) {
			if(i == agent.id()) {
				continue;
			}
			
			int value = nWonTasks[i];
			if(value > maxValue) {
				maxValue = value;
			}
		}
		
		return maxValue;
	}

	public Long askPrice(double marginalCost) {
		// Default marginal cost if <= 0
		if(marginalCost < defaultMarginalCost) {
			marginalCost = defaultMarginalCost;
		}

		if(isFirstPhase()) {
			return Math.round(marginalCost + 1);
		}

		// Compute price to ask
		double ratio = 1.0 + interestRatio;
		double bid = ratio * marginalCost;
		return Math.round(bid);
	}

}
