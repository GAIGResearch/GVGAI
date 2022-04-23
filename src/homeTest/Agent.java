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
    protected int cubeDepth = 8;
    
    protected ArrayList<ArrayList<ArrayList<Integer>>> memCube;
    protected ArrayList<Types.ACTIONS> actions;
    protected Types.ACTIONS orienation = null;
    static String game; //The Agent needs to know which game is being played to create the feature set
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
    	randomGen = new Random();
    	grid = so.getObservationGrid();
    	block_size = so.getBlockSize();
    	actions = so.getAvailableActions();
    	
    	//Really ugly code but it's just creating a 3D Array List and setting all the values to 0
    	this.memCube = new ArrayList<ArrayList<ArrayList<Integer>>>(grid.length);
    	for(int i = 0; i < grid.length; i ++) {
    		this.memCube.add(new ArrayList<ArrayList<Integer>>(grid[0].length));
    		for(int j = 0; j < grid[0].length; j++) {
    			this.memCube.get(i).add(new ArrayList<Integer>(cubeDepth));
    			for(int k = 0; k < cubeDepth; k ++) {
    				this.memCube.get(i).get(j).add(0);
    			}
    		}
    	}
 
    }
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		ArrayList<Observation>[] npcPoistions = stateObs.getNPCPositions();
		
		ArrayList<Observation>[] resourcePositions = stateObs.getResourcesPositions();
		
		if(null != resourcePositions) {
			//System.out.println(npcPoistions[0]);
		}
		Vector2d playerPos = stateObs.getAvatarPosition();
		
		

		Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
		int index = randomGen.nextInt(actions.size());
		action = actions.get(index);
		
		
		incMem((int) (playerPos.x/block_size), (int) (playerPos.y/block_size), action);
		return action;
	}
	
	//We need the last action to know the orientation of the agent when the Use action is done
	private void incMem(int x, int y, Types.ACTIONS action) {
		
		
		int z = -1;
		if(Types.ACTIONS.ACTION_UP == action) {
			z = 0;
			orienation = action;
		}
		else if(Types.ACTIONS.ACTION_RIGHT == action) {
			z = 1;
			orienation = action;
		}
		else if(Types.ACTIONS.ACTION_DOWN == action) {
			z = 2;
			orienation = action;
		}
		else if(Types.ACTIONS.ACTION_LEFT == action) {
			z = 3;
			orienation = action;
		}
		else if(Types.ACTIONS.ACTION_USE == action && Types.ACTIONS.ACTION_UP == orienation) {
			z = 4;
			
		}
		else if(Types.ACTIONS.ACTION_USE == action && Types.ACTIONS.ACTION_RIGHT == orienation) {
			z = 5;
			
		}
		else if(Types.ACTIONS.ACTION_USE == action && Types.ACTIONS.ACTION_DOWN == orienation) {
			z = 6;
			
		}
		else if(Types.ACTIONS.ACTION_USE == action && Types.ACTIONS.ACTION_LEFT == orienation) {
			z = 7;
			
		}
		
		if(z != -1) {
			int temp = this.memCube.get(x).get(y).get(z);
			this.memCube.get(x).get(y).set(z, temp + 1);
		}
		
		
	}
	
	public void result(StateObservation stateObs, ElapsedCpuTimer elapsedCpuTimer)
    {
		System.out.println("test");
    }

}
