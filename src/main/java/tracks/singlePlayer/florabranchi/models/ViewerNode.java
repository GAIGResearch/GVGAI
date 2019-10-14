package tracks.singlePlayer.florabranchi.models;

import java.util.List;
import java.util.stream.Collectors;

public class ViewerNode {

  public Integer id;

  public double value;

  public String action;

  public List<Integer> children;

  public ViewerNode(final Integer id,
                    final double value,
                    final List<Integer> children) {
    this.id = id;
    this.value = value;
    this.children = children;
  }

  public ViewerNode(final TreeNode node) {
    this.id = node.id;
    this.value = node.value / (node.visits != 0 ? node.visits : 1);
    this.action = String.valueOf(node.previousAction);
    this.children = node.children.stream().map(c -> c.id).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return String.format("id= %d, value= %.2f, action=%s", id, value, action);
  }
}