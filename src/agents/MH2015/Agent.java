package agents.MH2015;

import java.awt.Graphics2D;
import java.util.ArrayList;

import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;

public class Agent extends AbstractPlayer {
	/**
     * Observation grid.
     */
    protected ArrayList<Observation> grid[][];
    protected int block_size;
    
    private Brain brain;
    private ArrayList<ACTIONS> actionList;

	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		// TODO Auto-generated constructor stub
		
		actionList = new ArrayList<ACTIONS>();
        brain = new Brain(so, elapsedTimer);
        grid = so.getObservationGrid();
        block_size = so.getBlockSize();
	}

	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		if ( (getLastAction() != ACTIONS.ACTION_USE) || (getLastAction() != ACTIONS.ACTION_NIL))
		{
			brain.update(stateObs);
		}
//		this.grid = stateObs.getObservationGrid();
		// TODO Auto-generated method stub
		ACTIONS action = null;
		boolean needUpdate = false;

		if (actionList.size() > 0)
		{
			action = actionList.get(0);
			StateObservation ob = stateObs.copy();
			ob.advance(action);
			if (ob.getGameWinner() == Types.WINNER.PLAYER_LOSES)
			{
				needUpdate = true;
			}
		}
		else 
		{
			needUpdate = true;
		}
		
		if (needUpdate)
		{
			actionList = brain.GA(stateObs, elapsedTimer);
		}
		
		action = actionList.get(0);
		actionList.remove(0);
		
		return action;
	}
	
	 /**
     * Prints the number of different types of sprites available in the "positions" array.
     * Between brackets, the number of observations of each type.
     * @param positions array with observations.
     * @param str identifier to print
     */
    private void printDebug(ArrayList<Observation>[] positions, String str)
    {
        if(positions != null){
            System.out.print(str + ":" + positions.length + "(");
            for (int i = 0; i < positions.length; i++) {
                System.out.print(positions[i].size() + ",");
            }
            System.out.print("); ");
        }else System.out.print(str + ": 0; ");
    }

    /**
     * Gets the player the control to draw something on the screen.
     * It can be used for debug purposes.
     * @param g Graphics device to draw to.
     */
    public void draw(Graphics2D g)
    {
        int half_block = (int) (block_size*0.5);
        for(int j = 0; j < grid[0].length; ++j)
        {
            for(int i = 0; i < grid.length; ++i)
            {
                if(grid[i][j].size() > 0)
                {
                    Observation firstObs = grid[i][j].get(0); //grid[i][j].size()-1
                    //Three interesting options:
                    int print = firstObs.category; //firstObs.itype; //firstObs.obsID;
                    g.drawString(print + "", i*block_size+half_block,j*block_size+half_block);
                }
            }
        }
    }

}
