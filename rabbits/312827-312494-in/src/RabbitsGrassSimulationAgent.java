import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;
	private static int NextID = 1;
	private int ID;
	
	private int unitGrassEnergy;

	private RabbitsGrassSimulationSpace rgSpace;

	public RabbitsGrassSimulationAgent(int energyInit, int unitGrassEnergy){
		x = -1;
		y = -1;
		energy = energyInit;
		this.unitGrassEnergy = unitGrassEnergy;
		ID = NextID;
		NextID++;
	}

	private void setVxVy(){
		vX = 0;
		vY = 0;
		while(Math.abs(vX) + Math.abs(vY) != 1){
			vX = (int)Math.floor(Math.random() * 3) - 1;
			vY = (int)Math.floor(Math.random() * 3) - 1;
		}
	}

	public void draw(SimGraphics G) {
		if(energy >= 4){
			G.drawFastRoundRect(Color.blue);
		} else {
			G.drawFastRoundRect(Color.red);
		}
	}

	public void decreaseEnergy(int loss) {
		energy -= loss;	
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	public String getID(){
		return "A-" + ID;
	}

	public int getEnergy(){
		return energy;
	}

	public void report(){
		System.out.println(getID() +
				" at (" +
				x + ", " + y +
				") has " +
				getEnergy() + " energy");
	}

	public void step(){
		setVxVy();
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = rgSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();	// torus
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		tryMove(newX, newY);	    

		energy += unitGrassEnergy * rgSpace.takeGrassAt(x, y);
		energy--;
	}

	public void setRgSpace(RabbitsGrassSimulationSpace rgSpace) {
		this.rgSpace = rgSpace;
	}

	private boolean tryMove(int newX, int newY){
		return rgSpace.moveAgentAt(x, y, newX, newY);
	}
}
