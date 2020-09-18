import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int energy;
	private static int NextID = 1;
	private int ID;

	public RabbitsGrassSimulationAgent(int energyInit){
		x = -1;
		y = -1;
		energy = energyInit;
		ID = NextID;
		NextID++;
	}

	public void draw(SimGraphics G) {
		if(energy >= 4){
			G.drawFastRoundRect(Color.blue);
		} else {
			G.drawFastRoundRect(Color.red);
		}
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
	    energy--;
	}
	
}
