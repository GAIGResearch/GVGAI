/**
 * Code written by Adrien Couetoux, acouetoux@ulg.ac.be It is free to use,
 * distribute, and modify. User: adrienctx Date: 12/01/2015
 */
package adrienctx;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;

import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.Random;


public class Agent extends AbstractPlayer {

    public static int NUM_ACTIONS;
    public static Types.ACTIONS[] actions;
//    private boolean gameIsPuzzle;

    /**
     * Random generator for the agent.
     */
    private final TreeSearchPlayer treeSearchPlayer;

    /**
     * Public constructor with state observation and time due.
     *
     * @param so           state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
    	
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for (int i = 0; i < actions.length; ++i) {
            actions[i] = act.get(i);
        }
        NUM_ACTIONS = actions.length;
//        gameIsPuzzle = false;

        //Create the player.
        treeSearchPlayer = new TreeSearchPlayer(new Random());
    }

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     *
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        //Determine the action using MCTS...
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;
        int remainingLimit = 5;

//        if (stateObs.getGameTick() > 10) {
//        if (true) {
        //Set the state observation object as the new root of the tree.
//            if (gameIsPuzzle) {  //play in puzzle mode
//                return actOnPuzzle(stateObs, elapsedTimer);
//            }
//            else {
        if (true) {
            treeSearchPlayer.init(stateObs);

            while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
                treeSearchPlayer.iterate();
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
            }

            return actions[treeSearchPlayer.returnBestAction()];
        } else {
            if (!treeSearchPlayer.isFrozen()) {
                treeSearchPlayer.init(stateObs);
            }
            while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
                treeSearchPlayer.puzzleIterate();
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
            }
            if (treeSearchPlayer.ticksSinceFrozen > 1000) {
                treeSearchPlayer.deFrost();
                if(treeSearchPlayer.puzzleActionFound()){
                    System.out.format("action found!");
                }
                return actions[treeSearchPlayer.returnBestAction()];
            } else {
                treeSearchPlayer.ticksSinceFrozen += 1;
                treeSearchPlayer.freezeTree();
                System.out.format("best action has value %f \n", treeSearchPlayer.getHighestScoreFromRoot());
                Vector2d vectorNIL = new Vector2d(-1, -1);
                return Types.ACTIONS.fromVector(vectorNIL);
            }
        }
    }



//            }
//        }
//        else {
//            treeSearchPlayer.init(stateObs);
//            boolean _gameIsPuzzle = treeSearchPlayer.checkIfGameIsPuzzle();
////            _gameIsPuzzle = false;
//            if (!_gameIsPuzzle) {
//                gameIsPuzzle = false;
////                System.out.println("game is not puzzle");
//            }
//            Vector2d vectorNIL = new Vector2d(-1, -1);
//            return Types.ACTIONS.fromVector(vectorNIL);
//        }
    }

//    public Types.ACTIONS actOnPuzzle(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
//        double avgTimeTaken = 0;
//        double acumTimeTaken = 0;
//        long remaining = elapsedTimer.remainingTimeMillis();
//        int numIters = 0;
//        int remainingLimit = 5;
//
//        if (treeSearchPlayer.lastActionPicked > -1) {
//            treeSearchPlayer.initializeWithSelectedBranch(treeSearchPlayer.lastActionPicked);
//        } else {
//            treeSearchPlayer.init(stateObs);
//        }
//
//        while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
//            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
//            treeSearchPlayer.iterateOnPuzzle();
//
//            numIters++;
//            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
//            avgTimeTaken = acumTimeTaken / numIters;
//            remaining = elapsedTimer.remainingTimeMillis();
//        }
//
//        treeSearchPlayer.lastActionPicked = treeSearchPlayer.returnBestAction();
//        return actions[treeSearchPlayer.lastActionPicked];
//    }

