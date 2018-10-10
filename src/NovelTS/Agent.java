package NovelTS;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Random;


public class Agent extends AbstractPlayer {

    /**
     * Number of feasible actions
     */
    public static int NUM_ACTIONS;
    /**
     * Feasible actions array, of length NUM_ACTIONS
     */
    public static Types.ACTIONS[] actions;
    /**
     * IW agent algorithm
     */
    private final IWPlayer iwPlayer;


    /**
     * Public constructor of the Agent
     *
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        // Get available Actions and store it in a static array
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        NUM_ACTIONS = act.size();

        actions = new Types.ACTIONS[act.size()];
        for (int i = 0; i < actions.length; ++i) {
            actions[i] = act.get(i);
        }

        // Create the player.
        iwPlayer = new IWPlayer(new Random());
    }


    /**
     * Act will pick a best action and return it
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Set the state observation object as the new root of the tree.
        iwPlayer.init(stateObs);

        // Determine the action using iwSearch
        int action = iwPlayer.run(elapsedTimer);
        return actions[action];
    }

    public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
       System.out.print("Avg iters: " + iwPlayer.totalNumIters / iwPlayer.numRun);
       System.out.println(" Avg Depth: " + iwPlayer.totalDepth / iwPlayer.numRun);
    }
}
