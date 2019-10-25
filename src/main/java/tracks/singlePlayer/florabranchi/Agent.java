package tracks.singlePlayer.florabranchi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.models.TreeNode;

/**
 * Flora Branchi (florabranchi@gmail.com) September 2019
 */
public class Agent extends AbstractPlayer {

  private final TreeController treeController;
  /**
   * Random generator for the agent.
   */
  protected Random randomGenerator;

  /**
   * List of available actions for the agent
   */
  protected ArrayList<ACTIONS> actions;

  private EAvailablePolicies agentPolicy;

  /**'
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
    treeController = new TreeController(stateObs);
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
        return monteCarloSearch(stateObs, elapsedTimer);
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

  public ACTIONS monteCarloSearch(final StateObservation stateObs,
                                  final ElapsedCpuTimer elapsedTimer) {

    // Perform tree Search

    treeController.treeSearch(10, stateObs);
    final TreeNode bestChild = treeController.getMostVisitedChild(treeController.rootNode);
    final ACTIONS bestFoundAction = treeController.getBestFoundAction();

    // Visualization shenenigans
    //treeController.updateTreeVisualization(stateObs, bestChild);

    //treeController.resetNodeCount();

    System.out.println(elapsedTimer.elapsedMillis());

    treeController.pruneTree(bestFoundAction);
    treeController.updateTreeVisualization(stateObs, 99);
    return bestFoundAction;
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
