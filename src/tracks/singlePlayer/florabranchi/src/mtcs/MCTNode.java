package tracks.singlePlayer.florabranchi.src.mtcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import core.game.StateObservation;
import ontology.Types;

public class MCTNode {

  /**
   * Parameter that influences exploration.
   */
  private final static double C = 1 / Math.sqrt(2);

  private static int nodeCount = 0;

  public StateObservation getNodeState() {
    return nodeState;
  }

  private Random rand;

  private int nodeId;

  public static void reset() {
    nodeCount = 0;
  }

  public int getNodeId() {
    return nodeId;
  }

  private MCTNode parentNode;

  private List<MCTNode> childrenNodes = new ArrayList<>();

  private double simulationReward;

  private Types.ACTIONS actionToGetToNode;

  private int timesVisited;

  public boolean isLeaf() {
    return childrenNodes.isEmpty();
  }

  public int getTimesVisited() {
    return timesVisited;
  }

  private final StateObservation nodeState;

  private List<Types.ACTIONS> possibleActions;

  public Types.ACTIONS getActionToGetToNode() {
    return actionToGetToNode;
  }

  public MCTNode getParentNode() {
    return parentNode;
  }

  public List<MCTNode> getChildrenNodes() {
    return childrenNodes;
  }

  public double getSimulationReward() {
    return simulationReward;
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
    for (MCTNode nodeId : childrenNodes) {
      averageSum += nodeId.getSimulationReward();
    }
    return averageSum / childrenNodes.size();
  }

  public boolean isFullyExtended() {
    final boolean isfullyExpanded = childrenNodes.size() == getNodeState().getAvailableActions().size();
    //System.out.println(String.format("Node %s is fully expanded: %s", getNodeId(), isfullyExpanded));
    return isfullyExpanded;
  }

  /**
   * Retrieves children node with maximum upper confidence bound.
   */
  public MCTNode getBestChild() {

    //for (MCTNode n : childrenNodes) {
    //System.out.println(String.format("Node %d UCB %s ", n.getNodeId(), n.getNodeUpperConfidenceBound(getTimesVisited())));
    //}


    final MCTNode maxUcbNode = Collections.max(childrenNodes, Comparator.comparing(c -> c.getNodeUpperConfidenceBound(getTimesVisited())));
    //System.out.println(String.format("Max UCB: %f.2 for action %s in node %s", maxUcbNode.getNodeUpperConfidenceBound(getTimesVisited()),
    //    maxUcbNode.getActionToGetToNode().toString(), maxUcbNode.getNodeId()));
    return maxUcbNode;
  }

  public MCTNode(final MCTNode parentNodeId,
                 final Types.ACTIONS actionToGetToNode,
                 final StateObservation nodeState) {
    this.actionToGetToNode = actionToGetToNode;
    this.nodeId = nodeCount;
    nodeCount++;
    this.parentNode = parentNodeId;
    this.nodeState = nodeState;
    this.possibleActions = nodeState.getAvailableActions();
    rand = new Random();
  }

  public MCTNode expand() {
    final List<Types.ACTIONS> exploredActions = this.childrenNodes.stream().map(MCTNode::getActionToGetToNode).collect(Collectors.toList());
    final List<Types.ACTIONS> unexploredActions = new ArrayList<>(this.nodeState.getAvailableActions());

    //System.out.println(String.format("Node %s has %s actions possible from initial state", this.nodeId, unexploredActions.size()));

    unexploredActions.removeAll(exploredActions);
    Collections.shuffle(unexploredActions);

    final Types.ACTIONS selectedAction = unexploredActions.get(0);
    StateObservation newNodeState = this.nodeState.copy();
    newNodeState.advance(selectedAction);

    //System.out.println(String.format("Selected action %s to expand node %s", selectedAction.toString(), this.nodeId));
    MCTNode tempNode = new MCTNode(this, selectedAction, newNodeState);
    this.childrenNodes.add(tempNode);
    return tempNode;
  }


  public void rolloutNode() {

    double initialScore = nodeState.getGameScore();
    StateObservation copyState = nodeState.copy();
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
      stateReward += 0;
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
