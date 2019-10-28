package tracks.singlePlayer.florabranchi.tree;

import java.util.List;
import java.util.stream.Collectors;

public class ViewerNode {

  public Integer id;

  public double value;

  public String action;

  public List<Integer> children;

  public Integer parent;

  public ViewerNode(final Integer id,
                    final double value,
                    final List<Integer> children) {
    this.id = id;
    this.value = value;
    this.children = children;
  }

  public ViewerNode(final int id,
                    final TreeNode node) {
    this.id = id;
    this.value = node.value / (node.visits != 0 ? node.visits : 1);
    this.action = String.valueOf(node.previousAction);
    this.parent = node.parent != null ? node.parent.id : -1;
    this.children = node.children.stream().map(c -> c.id).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return String.format("%.2f", value);
  }
}