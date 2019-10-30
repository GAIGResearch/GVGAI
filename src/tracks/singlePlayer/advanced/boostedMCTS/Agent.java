package tracks.singlePlayer.advanced.boostedMCTS;

import java.util.ArrayList;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import video.basics.GameEvent;
import video.basics.Interaction;
import video.basics.PlayerAction;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is an implementation of MCTS UCT
 */
public class Agent extends AbstractPlayer {

    public int num_actions;
    public Types.ACTIONS[] actions;
    public ArrayList<GameEvent> critPath;

    protected SingleMCTSPlayer mctsPlayer;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for(int i = 0; i < actions.length; ++i)
        {
            actions[i] = act.get(i);
        }
        num_actions = actions.length;

        //Create the player.

        mctsPlayer = getPlayer(so, elapsedTimer);
        
    }

    public SingleMCTSPlayer getPlayer(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        critPath = new ArrayList<GameEvent>();
    	setupCritPath(this.critPath);
        return new SingleMCTSPlayer(new Random(), num_actions, actions, critPath);
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        //Set the state observation object as the new root of the tree.
        mctsPlayer.init(stateObs);

        //Determine the action using MCTS...
        int action = mctsPlayer.run(elapsedTimer);

        //... and return it.
        return actions[action];
    }
    
    public void setupCritPath(ArrayList<GameEvent> critPath) {
    	int gameIdx = 47;
		if(gameIdx == 47) {
			// zelda
//        	critPath.add(new PlayerAction("ACTION_USE"));
//        	critPath.add(new Interaction("KillSprite", "monsterQuick", "sword"));
//        	critPath.add(new Interaction("KillSprite", "monsterNormal", "sword"));
//        	critPath.add(new Interaction("KillSprite", "monsterSlow", "sword"));
        	critPath.add(new Interaction("TransformTo", "nokey",  "key"));
        	critPath.add(new Interaction("KillSprite", "goal", "withkey"));		        	
        } else if(gameIdx == 42) {
        	// survivezombies
        	critPath.add(new Interaction("SubtractHealthPoints", "avatar", "zombie"));
        	critPath.add(new Interaction("KillSprite", "avatar", "zombie"));
        	critPath.add(new Interaction("StepBack", "avatar", "wall"));
        	critPath.add(new Interaction("AddHealthPoints", "avatar", "honey"));
        } else if(gameIdx == 39) {
        	// solarfox
        	critPath.add(new Interaction("KillSprite","blib","avatar"));
        } else if(gameIdx == 34) {
        	// realportals
        	critPath.add(new PlayerAction("ACTION_USE"));
        	critPath.add(new Interaction("TransformTo", "avatarIn", "weaponToggle1"));
        	critPath.add(new Interaction("TransformTo", "avatarOut", "weaponToggle2"));
        	critPath.add(new Interaction("TransformTo", "wall", "missileOut"));
        	critPath.add(new Interaction("TransformTo", "wall", "missileIn"));
        	critPath.add(new Interaction("TeleportToExit","avatarIn","portalentry"));
        	critPath.add(new Interaction("TeleportToExit","avatarOut","portalentry"));
        	critPath.add(new Interaction("StepBack","avatarOut","portalExit"));
        	critPath.add(new Interaction("StepBack","avatarIn","portalExit"));
        	critPath.add(new Interaction("KillSprite", "key", "avatarIn"));
        	critPath.add(new Interaction("KillSprite", "key", "avatarOut"));
        	critPath.add(new Interaction("KillIfOtherHasMore", "lock", "avatarOut"));
        	critPath.add(new Interaction("KillIfOtherHasMore", "lock", "avatarIn"));
        	critPath.add(new Interaction("KillSprite", "goal", "avatarOut"));
        	critPath.add(new Interaction("KillSprite", "goal", "avatarIn"));
        	
        	// levels > 0
        	critPath.add(new Interaction("TeleportToExit","bolderm","portalentry"));
        	critPath.add(new Interaction("TransformTo","bolder","avatar"));
        	critPath.add(new Interaction("KillBoth", "water", "bolderm"));



        } else if(gameIdx == 30) {
        	// plants
        	critPath.add(new PlayerAction("ACTION_USE"));
        	critPath.add(new Interaction("TransformTo", "shovel", "marsh"));
        	critPath.add(new Interaction("TransformTo", "plant", "axe"));
        } else if(gameIdx == 4)
        {
        	// boulderdash
        	critPath.add(new Interaction("StepBack", "avatar", "wall"));
        	critPath.add(new Interaction("KillIfOtherHasMore", "exitdoor", "avatar"));
        	critPath.add(new Interaction("StepBack", "avatar", "boulder"));
        	critPath.add(new Interaction("CollectResource", "diamond", "avatar"));
        	critPath.add(new Interaction("KillSprite", "dirt", "avatar"));
        }
	}

}
