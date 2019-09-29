package tracks.singlePlayer.florabranchi.src.mtcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import core.game.StateObservation;
import ontology.Types;

public class MCTNode {

  private static int nodeCount = 0;

  private final int nodeId;

  public static void reset() {
    nodeCount = 0;
  }

  public int getNodeId() {
    return nodeId;
  }

  private MCTNode parentNode;

  private List<MCTNode> childrenNodes = new ArrayList<>();

  private double childrenNodesReward;

  private Types.ACTIONS actionToGetToNode;

  private Random rand;

  private int timesVisited;

  public boolean isFullyExpanded() {

    if (childrenNodes.isEmpty()) {
      return false;
    }

    for (MCTNode node : childrenNodes) {
      if (node.getTimesVisited() == 0) {
        return false;
      }
    }
    return true;
  }

  public int getTimesVisited() {
    return timesVisited;
  }

  public Types.ACTIONS getActionToGetToNode() {
    return actionToGetToNode;
  }

  public MCTNode getParentNode() {
    return parentNode;
  }

  public List<MCTNode> getChildrenNodes() {
    return childrenNodes;
  }

  public double getChildrenNodesReward() {
    return childrenNodesReward;
  }

  public <T> T getRandomListElement(List<T> list) {
    return list.get(rand.nextInt(list.size() - 1));
  }

  public double getNodeUpperConfidenceBound(int parentVisits) {

    if (timesVisited == 0) {
      return Double.MAX_VALUE;
    }
    return getChildrenAverageReward() + 2 * Math.sqrt((Math.log(parentVisits) / timesVisited));
  }

  public int getChildrenAverageReward() {
    return childrenNodes.stream().mapToInt(MCTNode::getChildrenAverageReward).sum();
  }

  /**
   * Retrieves children node with maximum upper confidence bound.
   */
  public MCTNode getBestConfidenceNode() {
    final MCTNode maxUcbNode = Collections.max(childrenNodes, Comparator.comparing(c -> c.getNodeUpperConfidenceBound(this.getTimesVisited())));
    System.out.println(String.format("Max UCB: %s for action %s", maxUcbNode.getNodeUpperConfidenceBound(getTimesVisited()), maxUcbNode.getActionToGetToNode().toString()));
    return maxUcbNode;
  }

  public MCTNode getBestPlayNode() {
    return Collections.max(this.childrenNodes, Comparator.comparing(MCTNode::getChildrenNodesReward));
  }


  public MCTNode(final MCTNode parentNodeId,
                 final Types.ACTIONS actionToGetToNode) {
    this.actionToGetToNode = actionToGetToNode;
    this.nodeId = nodeCount;
    nodeCount++;
    this.parentNode = parentNodeId;

    rand = new Random();
  }

  public void expandPossibleChildren(final StateObservation stateObservation) {

    final List<Types.ACTIONS> availableActions = stateObservation.getAvailableActions();
    for (Types.ACTIONS availableAction : availableActions) {
      MCTNode tempNode = new MCTNode(this, availableAction);
      this.childrenNodes.add(tempNode);
    }

  }

  public void rolloutNode(final StateObservation stateObservation) {

    StateObservation copyState = stateObservation.copy();
    while (!copyState.isGameOver()) {
      copyState.advance(getRandomListElement(copyState.getAvailableActions()));
    }

    copyState.advance(getActionToGetToNode());
    this.childrenNodesReward = copyState.getGameScore();
    System.out.println(String.format("Score after advancing state %s", childrenNodesReward));
    timesVisited++;
    System.out.println(String.format("Node %s has been visited %d times", this.nodeId, this.timesVisited));
    backpropagateReward(childrenNodesReward);

  }

  private void backpropagateReward(final double reward) {
    if (parentNode != null) {
      this.parentNode.incrementTotalReward(reward);

    }
  }

  private void incrementTotalReward(final double reward) {
    this.timesVisited++;
    this.childrenNodesReward += reward;
    System.out.println(String.format("Node %s has been visited %d times", this.nodeId, this.timesVisited));
    this.backpropagateReward(reward);
  }

}
