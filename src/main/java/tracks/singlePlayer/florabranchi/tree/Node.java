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

  public double value;

  public int visits;

  public Types.ACTIONS previousAction;

  public StateObservation currentGameState;

  public Node(final Node parent,
              final Types.ACTIONS previousAction,
              final StateObservation currentGameState) {
    this.id = nodeId++;
    this.parent = parent;
    this.previousAction = previousAction;
    this.currentGameState = currentGameState;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TreeNode.class.getSimpleName() + "[", "]")
        .add("id=" + id)
        .add("value=" + value)
        .add("visits=" + visits)
        .add("previousAction=" + previousAction)
        .toString();
  }
}

