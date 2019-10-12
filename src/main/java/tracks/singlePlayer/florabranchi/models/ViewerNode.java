package tracks.singlePlayer.florabranchi.models;

import java.util.List;
import java.util.stream.Collectors;

public class ViewerNode {

  public Integer id;

  public double value;

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
    this.value = node.value;
    this.children = node.children.stream().map(c -> c.id).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return ("id=" + id) + "\n " +
        "\n value=" + value;
  }
}