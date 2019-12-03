package tracks.singlePlayer.advanced.boostedMCTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import eveqt.EquationNode;
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

	// static var for quick hot-swapping of the critical path
	public static EquationNode _rewardEquation;
	public static List<GameEvent> _critPath;
	
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
        if (Agent._rewardEquation != null) {
        	mctsPlayer.rewardEquation = Agent._rewardEquation;
        	mctsPlayer.critPath = (ArrayList<GameEvent>) Agent._critPath;
        }
        
    }

    public SingleMCTSPlayer getPlayer(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        critPath = new ArrayList<GameEvent>();
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
    

}
