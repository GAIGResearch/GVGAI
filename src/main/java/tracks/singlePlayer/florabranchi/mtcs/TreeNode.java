package tracks.singlePlayer.florabranchi.mtcs;

import java.util.ArrayList;
import java.util.List;

import ontology.Types;

public class TreeNode {

  public int id;

  public TreeNode parent;

  public List<TreeNode> children = new ArrayList<>();

  public double value;

  public int visits;

  public Types.ACTIONS previousAction;

  public TreeNode(final int id,
                  final TreeNode parent,
                  final Types.ACTIONS previousAction) {
    this.id = id;
    this.parent = parent;
    this.previousAction = previousAction;
  }
}
