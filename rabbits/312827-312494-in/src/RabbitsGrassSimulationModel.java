import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Value2DDisplay;
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

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rgSpace;
	private DisplaySurface displaySurf;

	private ArrayList agentList;

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
	}

	public void buildModel(){
		rgSpace = new RabbitsGrassSimulationSpace(gridSize);
		rgSpace.spreadGrass(numInitGrass);

		for(int i = 0; i < numInitRabbits; i++){
			addNewAgent();
		}
	}

	public void buildSchedule(){
	}

	public void buildDisplay(){
		ColorMap map = new ColorMap();

		for(int i = 1; i<32; i++){
			map.mapColor(i, new Color(0, (int)(i * 4 + 127), 0));
		}

		for(int i = 32; i<10000; i++){
			map.mapColor(i, new Color(0, 255, 0));
		}

		map.mapColor(0, Color.white);

		Value2DDisplay displayGrass =
				new Value2DDisplay(rgSpace.getCurrentGrassSpace(), map);

		displaySurf.addDisplayable(displayGrass, "Grass");
	}


	private void addNewAgent(){
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentInitEnergy);
		agentList.add(a);
	}

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
		String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "AgentInitEnergy"};
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

		if (displaySurf != null){
			displaySurf.dispose();
		}
		displaySurf = null;

		displaySurf = new DisplaySurface(this, "Rabbit grass Model Window 1");

		registerDisplaySurface("Rabbit grass Model Window 1", displaySurf);
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

}
