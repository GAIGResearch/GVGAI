package tracks.singlePlayer.florabranchi.models;

import java.util.ArrayList;
import java.util.List;

import ontology.Types;

public class TreeNode {

  private static int nodeCount = 0;

  public int id;

  public TreeNode parent;

  public List<TreeNode> children = new ArrayList<>();

  public double value;

  public int visits;

  public Types.ACTIONS previousAction;

  public TreeNode(final TreeNode parent,
                  final Types.ACTIONS previousAction) {
    this.id = nodeCount;
    nodeCount++;
    this.parent = parent;
    this.previousAction = previousAction;
  }


}
