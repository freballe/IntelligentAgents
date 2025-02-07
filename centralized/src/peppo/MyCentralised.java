package peppo;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


@SuppressWarnings("unused")
public class MyCentralised implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeoutPlan;
	private long timeoutMargin;
	private double epsThresh;
	private double epsRate;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the plan method cannot execute more than timeout_plan milliseconds
		timeoutPlan = ls.get(LogistSettings.TimeoutKey.PLAN);
		
		// Reads the user-chosen timeout margin from the configuration file
		Long timeoutMarginName = agent.readProperty("timeout-margin", Long.class, 100L);

		// Reads the user-chosen epsilon parameters from the configuration file
		Double epsThreshName = agent.readProperty("eps-thresh", Double.class, 0.4);
		Double epsRateName = agent.readProperty("eps-rate", Double.class, 1000.0);
		
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.timeoutMargin = timeoutMarginName.longValue();
		this.epsThresh = epsThreshName.doubleValue();
		this.epsRate = epsRateName.doubleValue();
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("timeoutPlan = " + timeoutPlan + ", timeoutMargn = " + timeoutMargin);
		Planner planner = new Planner(vehicles, tasks, epsThresh, epsRate, timeoutPlan-timeoutMargin);
		return planner.plan();
	}

}
