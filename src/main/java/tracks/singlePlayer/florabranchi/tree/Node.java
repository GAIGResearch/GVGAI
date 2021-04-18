package tracks.singlePlayer.florabranchi.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import ontology.Types;

public class Node {

  static int nodeId = 0;

  public int id;

  public Node parent;

  public List<Node> children = new ArrayList<>();

  public double value;

  public int visits;

  public Types.ACTIONS previousAction;

  public Node(final Node parent,
              final Types.ACTIONS previousAction) {
    this.id = nodeId++;
    this.parent = parent;
    this.previousAction = previousAction;
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

