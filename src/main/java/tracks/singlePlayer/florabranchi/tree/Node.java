package tracks.singlePlayer.florabranchi.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import core.game.StateObservation;
import ontology.Types;

public class Node {

  private Double UPPER_BOUND = Double.MAX_VALUE;
  private Double LOWER_BOUND = -Double.MAX_VALUE;

  static int nodeId = 0;

  public int id;

  public Node parent;

  public List<Node> children = new ArrayList<>();

  public double totalValue;

  public double currentValue;

  public int visits;

  public Types.ACTIONS previousAction;

  public StateObservation currentGameState;

  public int depth;

  public void resetTree() {
    nodeId = 0;
  }

  // root node builder
  public Node(final StateObservation currentGameState) {
    resetTree();
    this.id = nodeId++;
    this.parent = null;
    this.previousAction = null;
    this.currentGameState = currentGameState;
    this.depth = 1;
  }

  public Node(final Node parent,
              final Types.ACTIONS previousAction,
              final StateObservation currentGameState) {
    this.id = nodeId++;
    this.parent = parent;
    this.previousAction = previousAction;
    this.currentGameState = currentGameState;
    if (parent != null) {
      depth = parent.depth + 1;
    } else {
      depth = 1;
    }
  }

  public void updateNodeReward(final double reward) {
    this.visits++;

    double finalReward = this.totalValue + reward;
    if (finalReward >= UPPER_BOUND) {
      finalReward = UPPER_BOUND;
      currentValue = LOWER_BOUND;
    } else if (finalReward <= LOWER_BOUND) {
      finalReward = LOWER_BOUND;
      currentValue = LOWER_BOUND;
    } else {
      this.currentValue = finalReward / this.visits;
    }
    this.totalValue = finalReward;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TreeNode.class.getSimpleName() + "[", "]")
        .add("id=" + id)
        .add("currentValue=" + currentValue)
        .add("visits=" + visits)
        .add("totalValue=" + totalValue)
        .add("depth=" + depth)
        .add("previousAction=" + previousAction)
        .toString();
  }
}

