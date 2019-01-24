package agents.bladerunner.agents.misc;

import java.awt.Graphics2D;
import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;

public class DrawingTools {

	protected static ArrayList<Observation>[][] grid;
	protected static int blockSize;
	
	public DrawingTools() {
		
	}
	
	
	/**
	 * This should be called in the agent's act method or whenever the drawn observation should be updated
	 * This does not draw anything 
	 * @param so
	 */
	public static void updateObservation(StateObservation so){
		grid = so.getObservationGrid();
		blockSize = so.getBlockSize();
	}
	
	
	
	/**
     * Gets the player the control to draw something on the screen.
     * It can be used for debug purposes.
     * The draw method of the agent is called by the framework (VGDLViewer) whenever it runs games visually
     * Comment this out, when you do not need it
     * We could draw anything!
     * @param g Graphics device to draw to.
     */
    public static void draw(Graphics2D g)
    {
        int half_block = (int) (blockSize*0.5);
        for(int j = 0; j < grid[0].length; ++j)
        {
            for(int i = 0; i < grid.length; ++i)
            {
                if(grid[i][j].size() > 0)
                {
                    Observation firstObs = grid[i][j].get(0); //grid[i][j].size()-1
                    //Three interesting options:
                    int print =firstObs.itype; //firstObs.category; //firstObs.itype; //firstObs.obsID;
                    g.drawString(print + "", i*blockSize+half_block,j*blockSize+half_block);
                }
            }
        }
    }
	
	
	
}
