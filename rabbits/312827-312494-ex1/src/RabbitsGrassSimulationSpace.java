import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid agentSpace;
	private int totalRabbits = 0;
	private int totalGrass = 0;

	public RabbitsGrassSimulationSpace(int size){
		grassSpace = new Object2DGrid(size, size);
		for(int i = 0; i < size; i++){
			for(int j = 0; j < size; j++){
				grassSpace.putObjectAt(i,j,new Integer(0));
			}
		}

		agentSpace = new Object2DGrid(size, size);
	}

	public void spreadGrass(int grass){
		// Randomly place grass in grassSpace
		for(int i = 0; i < grass; i++){

			// Choose coordinates
			int x = (int)(Math.random()*(grassSpace.getSizeX()));
			int y = (int)(Math.random()*(grassSpace.getSizeY()));

			// Get the value of the object at those coordinates
			int currentValue = getGrassAt(x, y);

			// Replace the Integer object with another one with the new value
			grassSpace.putObjectAt(x,y,new Integer(currentValue + 1));
			totalGrass ++;
		}
	}

	public int getGrassAt(int x, int y){
		int i;
		if(grassSpace.getObjectAt(x,y)!= null){
			i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
		}
		else{
			i = 0;
		}
		return i;
	}

	public Object2DGrid getCurrentGrassSpace(){
		return grassSpace;
	}

	public Object2DGrid getCurrentAgentSpace(){
		return agentSpace;
	}

	public boolean isCellOccupied(int x, int y){
		return (agentSpace.getObjectAt(x, y) != null);
	}
	
	private void putAgentAt(int x, int y, RabbitsGrassSimulationAgent agent) {
		agentSpace.putObjectAt(x,y,agent);
		totalRabbits++;
	}

	public boolean addAgent(RabbitsGrassSimulationAgent agent){
		boolean retVal = false;
		int count = 0;
		int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

		while((retVal==false) && (count < countLimit)){
			int x = (int)(Math.random()*(agentSpace.getSizeX()));
			int y = (int)(Math.random()*(agentSpace.getSizeY()));
			if(isCellOccupied(x,y) == false){
				putAgentAt(x,y,agent);
				agent.setXY(x,y);
				agent.setRgSpace(this);
				retVal = true;
			}
			count++;
		}

		return retVal;
	}

	public void removeAgentAt(int x, int y){
		totalRabbits--;
		agentSpace.putObjectAt(x, y, null);
	}

	public int takeGrassAt(int x, int y){
		int grass = getGrassAt(x, y);
		grassSpace.putObjectAt(x, y, new Integer(0));
		totalGrass -= grass;
		return grass;
	}

	public boolean moveAgentAt(int x, int y, int newX, int newY){
		boolean retVal = false;
		if(!isCellOccupied(newX, newY)){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentSpace.getObjectAt(x, y);
			removeAgentAt(x,y);
			rga.setXY(newX, newY);
			putAgentAt(newX, newY, rga);
			retVal = true;
		}
		return retVal;
	}

	public double getTotalRabbits() {
		return totalRabbits;
	}

	public double getTotalGrass() {
		return totalGrass;
	}
}
