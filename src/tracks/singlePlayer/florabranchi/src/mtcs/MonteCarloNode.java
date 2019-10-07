package tracks.singlePlayer.florabranchi.src.mtcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import core.game.StateObservation;
import ontology.Types;

public class MonteCarloNode {

  /**
   * Parameter that influences exploration.
   */
  private final static double C = 1 / Math.sqrt(2);

  private static int nodeCount = 0;

  public int nodeId;

  public MonteCarloNode parentNode;

  public List<MonteCarloNode> childrenNodes = new ArrayList<>();

  public double simulationReward;

  public Types.ACTIONS actionToGetToNode;

  public int timesVisited;

  private Random rand;

  private List<Types.ACTIONS> possibleActions;

  public MonteCarloNode(final MonteCarloNode parentNodeId,
                        final Types.ACTIONS actionToGetToNode,
                        final StateObservation nodeState) {
    this.actionToGetToNode = actionToGetToNode;
    this.nodeId = nodeCount;
    nodeCount++;
    this.parentNode = parentNodeId;
    this.possibleActions = nodeState.getAvailableActions();
    rand = new Random();
  }

  public static void reset() {
    nodeCount = 0;
  }

  public boolean isLeaf() {
    return childrenNodes.isEmpty();
  }

  public <T> T getRandomListElement(List<T> list) {
    return list.get(rand.nextInt(list.size() - 1));
  }

  public double getNodeUpperConfidenceBound(int parentVisits) {

    if (timesVisited == 0) {
      return Double.MAX_VALUE;
    }
    return getChildrenAverageReward() + C * Math.sqrt((2 * (Math.log(parentVisits)) / timesVisited));
  }

  public double getChildrenAverageReward() {

    if (childrenNodes.isEmpty()) {
      return 0;
    }

    double averageSum = 0;
    for (MonteCarloNode nodeId : childrenNodes) {
      averageSum += nodeId.simulationReward;
    }
    return averageSum / childrenNodes.size();
  }

  public boolean isFullyExtended() {
    final boolean isfullyExpanded = childrenNodes.size() == possibleActions.size();
    //System.out.println(String.format("Node %s is fully expanded: %s", getNodeId(), isfullyExpanded));
    return isfullyExpanded;
  }

  /**
   * Retrieves children node with maximum upper confidence bound.
   */
  public MonteCarloNode getBestChild() {

    //for (MCTNode n : childrenNodes) {
    //System.out.println(String.format("Node %d UCB %s ", n.getNodeId(), n.getNodeUpperConfidenceBound(getTimesVisited())));
    //}


    final MonteCarloNode maxUcbNode = Collections.max(childrenNodes, Comparator.comparing(c -> c.getNodeUpperConfidenceBound(timesVisited)));
    //System.out.println(String.format("Max UCB: %f.2 for action %s in node %s", maxUcbNode.getNodeUpperConfidenceBound(getTimesVisited()),
    //    maxUcbNode.getActionToGetToNode().toString(), maxUcbNode.getNodeId()));
    return maxUcbNode;
  }

  public MonteCarloNode expand(final StateObservation currentState) {
    final List<Types.ACTIONS> exploredActions = this.childrenNodes.stream().map(entry -> entry.actionToGetToNode).collect(Collectors.toList());
    final List<Types.ACTIONS> unexploredActions = new ArrayList<>(currentState.getAvailableActions());

    //System.out.println(String.format("Node %s has %s actions possible from initial state", this.nodeId, unexploredActions.size()));

    unexploredActions.removeAll(exploredActions);
    Collections.shuffle(unexploredActions);

    final Types.ACTIONS selectedAction = unexploredActions.get(0);
    StateObservation newNodeState = currentState.copy();
    newNodeState.advance(selectedAction);

    //System.out.println(String.format("Selected action %s to expand node %s", selectedAction.toString(), this.nodeId));
    MonteCarloNode tempNode = new MonteCarloNode(this, selectedAction, newNodeState);
    this.childrenNodes.add(tempNode);
    return tempNode;
  }


  public void rolloutNode(final StateObservation currentState) {

    double initialScore = currentState.getGameScore();
    StateObservation copyState = currentState.copy();
    while (!copyState.isGameOver()) {
      copyState.advance(getRandomListElement(copyState.getAvailableActions()));
    }

    double finalScore = copyState.getGameScore();
    double stateReward = 0.01;
    double scoreDelta = initialScore - finalScore;
    stateReward += 0.01 * scoreDelta;

    final Types.WINNER gameWinner = copyState.getGameWinner();

    if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
      stateReward += 1;
    } else if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
      stateReward = 0;
    }


    //System.out.println(String.format("Final reward after node %s rollout: %f5.2", getNodeId(), reward));

    //System.out.println(String.format("Score after advancing state %s", simulationReward));
    backpropagateReward(stateReward);
  }

  private void backpropagateReward(final double updatedReward) {
    //System.out.println(String.format("Node %s has been visited %d times", this.nodeId, this.timesVisited));
    this.timesVisited++;
    this.simulationReward = simulationReward + updatedReward;
    if (parentNode != null) {
      parentNode.backpropagateReward(updatedReward);
    }
  }

}
