package tracks.singlePlayer.florabranchi.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import core.game.StateObservation;
import ontology.Types;

public class Node {

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
    this.totalValue = this.totalValue + reward;
    this.currentValue = this.totalValue / this.visits;
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

