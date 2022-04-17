package homeTest;

import java.util.ArrayList;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class Agent extends AbstractPlayer{
	
	
	protected Random randomGen;
    protected ArrayList<Observation> grid[][];
    protected int block_size;
    static String game; //The Agent needs to know which game is being played to create the feature set
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
    	randomGen = new Random();
    	grid = so.getObservationGrid();
    	block_size = so.getBlockSize();
    	ArrayList<Types.ACTIONS> actions = so.getAvailableActions();
    	
    	
    }
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
		ArrayList<Observation>[] npcPoistions = stateObs.getNPCPositions();
		
		ArrayList<Observation>[] resourcePositions = stateObs.getResourcesPositions();
		
		if(null != resourcePositions) {
			//System.out.println(npcPoistions[0]);
		}
		Vector2d playerPos = stateObs.getAvatarPosition();
		System.out.println( grid[(int) playerPos.x / this.block_size][(int) playerPos.y / this.block_size].toString());
		
		Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
		int index = randomGen.nextInt(actions.size());
		action = actions.get(index);
		
		return action;
	}

}
