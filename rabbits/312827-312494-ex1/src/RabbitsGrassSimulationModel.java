import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.engine.SimInit;
import java.awt.Color;
import java.util.ArrayList;
/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {		
	private int gridSize = 20;
	private int numInitRabbits = 30;
	private int numInitGrass = 70;
	private int grassGrowthRate = 10;
	private int birthThreshold = 10;
	private int agentInitEnergy = 7;
	private int unitGrassEnergy = 3;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rgSpace;
	private DisplaySurface displaySurf;

	private ArrayList agentList;

	private OpenSequenceGraph amountOfRabbitsInSpace;
	private OpenSequenceGraph amountOfGrassInSpace;

	class rabbitsInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double)rgSpace.getTotalRabbits();
		}
	}

	class grassInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double)rgSpace.getTotalGrass();
		}
	}

	public static void main(String[] args) {

		System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode 
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();
		displaySurf.display();
		amountOfRabbitsInSpace.display();
		amountOfGrassInSpace.display();
	}

	public void buildModel(){
		rgSpace = new RabbitsGrassSimulationSpace(gridSize);
		rgSpace.spreadGrass(numInitGrass);

		for(int i = 0; i < numInitRabbits; i++){
			addNewAgent();
		}
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
			rga.report();
		}
	}

	public void buildSchedule(){
		class RabbitsGrassStep extends BasicAction {
			public void execute() {
				reproduceAgents();
				killAgents();

				SimUtilities.shuffle(agentList);
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
					rga.step();
				}

				killAgents();
				displaySurf.updateDisplay();
				rgSpace.spreadGrass(grassGrowthRate);
				displaySurf.updateDisplay();
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitsGrassStep());

		class RabbitsGrassUpdateRabbitsInSpace extends BasicAction {
			public void execute(){
				amountOfRabbitsInSpace.step();
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitsGrassUpdateRabbitsInSpace());
		
		class RabbitsGrassUpdateGrassInSpace extends BasicAction {
			public void execute(){
				amountOfGrassInSpace.step();
			}
		}
		schedule.scheduleActionBeginning(0, new RabbitsGrassUpdateGrassInSpace());
	}

	public void buildDisplay(){
		ColorMap map = new ColorMap();

		for(int i = 1; i<32; i++){
			map.mapColor(i, new Color(0, (int)(255 - i * 4), 0));
		}

		for(int i = 32; i<10000; i++){
			map.mapColor(i, new Color(0, 126, 0));
		}

		map.mapColor(0, Color.white);

		Value2DDisplay displayGrass = new Value2DDisplay(rgSpace.getCurrentGrassSpace(), map);
		Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayAgents, "Agents");

		amountOfRabbitsInSpace.addSequence("R", new rabbitsInSpace());
		amountOfGrassInSpace.addSequence("G", new grassInSpace());
	}


	private void killAgents(){
		for(int i = (agentList.size() - 1); i >= 0 ; i--){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
			if(rga.getEnergy() < 1){
				rgSpace.removeAgentAt(rga.getX(), rga.getY());
				agentList.remove(i);
			}
		}
	}	

	private void reproduceAgents(){
		for(int i = (agentList.size() - 1); i >= 0 ; i--){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
			if(rga.getEnergy() >= birthThreshold){
				rga.decreaseEnergy(agentInitEnergy);
				addNewAgent();
			}
		}
	}	


	private void addNewAgent(){
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentInitEnergy, unitGrassEnergy);
		agentList.add(a);
		rgSpace.addAgent(a);
	}

	private int countLivingAgents(){
		int livingAgents = 0;
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentList.get(i);
			if(cda.getEnergy() > 0) livingAgents++;
		}
		System.out.println("Number of living agents is: " + livingAgents);

		return livingAgents;
	}

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
		String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "AgentInitEnergy", "UnitGrassEnergy"};
		return params;
	}

	public String getName() {
		return "Rabbits and babbits";
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setup() {
		rgSpace = null;
		agentList = new ArrayList();
		schedule = new Schedule(1);

		if (displaySurf != null){
			displaySurf.dispose();
		}
		if (amountOfRabbitsInSpace != null){
			amountOfRabbitsInSpace.dispose();
		}
		if (amountOfGrassInSpace != null){
			amountOfGrassInSpace.dispose();
		}

		displaySurf = null;
		amountOfRabbitsInSpace = null;
		amountOfGrassInSpace = null;

		// Create Displays
		displaySurf = new DisplaySurface(this, "Rabbit grass Model Window 1");
		amountOfRabbitsInSpace = new OpenSequenceGraph("Amount Of Rabbits In Space",this);
		amountOfGrassInSpace = new OpenSequenceGraph("Amount Of Grass In Space",this);

		// Register Displays
		registerDisplaySurface("Rabbit grass Model Window 1", displaySurf);
		this.registerMediaProducer("Plot rabbits", amountOfRabbitsInSpace);
		this.registerMediaProducer("Plot grass", amountOfGrassInSpace);

	}



	public int getGridSize(){
		return gridSize;
	}

	public void setGridSize(int gs){
		gridSize = gs;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		this.numInitRabbits = numInitRabbits;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public void setNumInitGrass(int numInitGrass) {
		this.numInitGrass = numInitGrass;
	}

	public int getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(int grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public int getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}

	public int getAgentInitEnergy() {
		return agentInitEnergy;
	}

	public void setAgentInitEnergy(int agentInitEnergy) {
		this.agentInitEnergy = agentInitEnergy;
	}

	public int getUnitGrassEnergy() {
		return unitGrassEnergy;
	}

	public void setUnitGrassEnergy(int unitGrassEnergy) {
		this.unitGrassEnergy = unitGrassEnergy;
	}

}
