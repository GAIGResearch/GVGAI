package tracks.singlePlayer.florabranchi;


import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeViewer {

  private final Graph graph;

  public TreeViewer() {
    graph = new SingleGraph("Tutorial 1");
    graph.display();
  }

  public Map<Integer, ViewerNode> createNodeMap(List<ViewerNode> nodes) {
    final Map<Integer, ViewerNode> nodeMap = new HashMap<>();
    nodes.forEach(node -> nodeMap.put(node.id, node));
    return nodeMap;
  }

  public void buildTreeNodes(final List<ViewerNode> nodes) {
    graph.clear();

    // Create all nodes
    nodes.forEach(this::createNode);

    // Create edges
    nodes.forEach(node -> createEdges(node.id, node.children));
  }

  public void createNode(final ViewerNode node) {
    final Node createdNode = graph.addNode(Integer.toString(node.id));
    createdNode.addAttribute("ui.label", node);
  }

  public void createEdges(final Integer node,
                          final List<Integer> nodeChildren) {
    final String parentNodeId = Integer.toString(node);
    nodeChildren.forEach(children -> {
      final String childId = Integer.toString(children);
      final String edgeId = parentNodeId + childId;
      graph.addEdge(edgeId, parentNodeId, childId);
    });
  }


}
