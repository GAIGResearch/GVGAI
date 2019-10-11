package tracks.singlePlayer.florabranchi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.*;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.mtcs.MonteCarloTree;
import tracks.singlePlayer.florabranchi.mtcs.TreeController;
import tracks.singlePlayer.florabranchi.mtcs.TreeViewer;

/**
 * Flora Branchi (florabranchi@gmail.com) September 2019
 */
public class Agent extends AbstractPlayer {

  /**
   * Random generator for the agent.
   */
  protected Random randomGenerator;
  /**
   * List of available actions for the agent
   */
  protected ArrayList<ACTIONS> actions;
  protected Map<Integer, ACTIONS> bestActionMap = new HashMap<>();
  private EAvailablePolicies agentPolicy;

  private TreeViewer treeViewer = new TreeViewer();

  private JFrame treeViewerFrame = new JFrame();

  /**
   * initialize all variables for the agent
   *
   * @param stateObs     Observation of the current state.
   * @param elapsedTimer Timer when the action returned is due.
   */
  public Agent(final StateObservation stateObs,
               final ElapsedCpuTimer elapsedTimer) {

    agentPolicy = EAvailablePolicies.MONTE_CARLO_TREE_SEARCH;
    System.out.println(String.format("Creating agent with policy %s", agentPolicy.name()));
    randomGenerator = new Random();
    actions = stateObs.getAvailableActions();

    treeViewer.init();
  }

  public void setAgentPolicy(final EAvailablePolicies agentPolicy) {
    this.agentPolicy = agentPolicy;
  }

  /**
   * return Action given selected policy.
   */
  @Override
  public ACTIONS act(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedTimer) {

    System.out.println("SCORE: " + stateObs.getGameScore());
    switch (this.agentPolicy) {
      case ONE_STEP_LOOK_AHEAD:
        return oneStepLookAhead(stateObs, elapsedTimer);
      case MONTE_CARLO_TREE_SEARCH:
        //treeViewer.showTree(treeViewerFrame);
        return monteCarloSearchV2(stateObs, elapsedTimer);
      //return monteCarloTreeSearch(stateObs, elapsedTimer);
      case RANDOM:
      default:
        return returnRandomAction();
    }
  }

  /**
   * Returns random action.
   */
  public ACTIONS returnRandomAction() {
    int index = randomGenerator.nextInt(actions.size());
    return actions.get(index);
  }

  public ACTIONS monteCarloTreeSearch(final StateObservation stateObs,
                                      final ElapsedCpuTimer elapsedTimer) {
    MonteCarloTree tree = new MonteCarloTree();
    tree.monteCarloSearch(20, stateObs);
    System.out.println(elapsedTimer.elapsedMillis());
    return tree.getBestFoundAction();
  }

  public ACTIONS monteCarloSearchV2(final StateObservation stateObs,
                                    final ElapsedCpuTimer elapsedTimer) {
    TreeController treeController = new TreeController();
    treeController.buildTree(20, stateObs);
    System.out.println(elapsedTimer.elapsedMillis());
    return treeController.getBestFoundAction();
  }

  /**
   * Returns the best action considering the outcome of a single play.
   */
  public ACTIONS oneStepLookAhead(final StateObservation stateObs,
                                  final ElapsedCpuTimer elapsedTimer) {

    GameStateHeuristic gameStateHeuristic = new GameStateHeuristic();

    final SortedMap<Double, ACTIONS> lookAheadRewardMap = new TreeMap<>(Collections.reverseOrder());
    stateObs.getAvailableActions().parallelStream()
        .forEach(action -> {

          // Evaluate state
          StateObservation newState = stateObs.copy();
          newState.advance(action);

          double actionReward = stateObs.getGameScore() - stateObs.getGameScore();
          double heuristicScore = gameStateHeuristic.evaluateState(stateObs, newState);

          double stepScore = actionReward + heuristicScore;
          // Save data
          lookAheadRewardMap.put(stepScore, action);
          System.out.println(String.format("Choosing action %s would result in Score %f", action, stepScore));
        });

    final Double bestScore = lookAheadRewardMap.firstKey();
    final ACTIONS bestActionAtm = lookAheadRewardMap.get(lookAheadRewardMap.firstKey());
    System.out.println(String.format("SELECTED ACTION %s would result in Score %f", bestActionAtm, bestScore));
    return bestActionAtm;


  }

  enum EAvailablePolicies {
    RANDOM,
    ONE_STEP_LOOK_AHEAD,
    MONTE_CARLO_TREE_SEARCH
  }
}
