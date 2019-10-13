package tracks.singlePlayer.florabranchi;


import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.Viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeViewer {


  private Graph graph;

  public TreeViewer() {
    graph = new SingleGraph("Tutorial 1");
    final Viewer display = graph.display();

    String myStyle = "node {"
        + "size: 10px;"
        + "fill-color: red;"
        + "}"

        + "edge {"
        + "shape: line;"
        + "fill-color: #222;"
        + "arrow-size: 3px, 2px;"
        + "}";

    graph.addAttribute("ui.stylesheet", myStyle);
    createBaseNodes(4, 5);
  }

  public static void main(String[] args) {

    TreeViewer n = new TreeViewer();
  }

  public void createBaseNodes(int treeDepth, int levelNodes) {

    int totalNodes = 0;
    for (int d = 0; d < treeDepth; d++) {
      totalNodes += Math.pow(levelNodes, d);
    }

    int nodeCount = 0;
    List<Integer> lastLayerNodes = new ArrayList<>();
    List<Integer> currentLayerNodes = new ArrayList<>();

    for (int k = 0; k < treeDepth; k++) {

      int layerNodes = (int) Math.pow(levelNodes, k);

      // Add layer nodes
      for (int l = 0; l < layerNodes; l++) {
        final Node currentNode = graph.addNode(String.valueOf(nodeCount));
        currentNode.setAttribute("ui.label", nodeCount);
        currentLayerNodes.add(nodeCount);
        nodeCount++;
      }

      // Add layer edges
      // for each node in last layer
      // link to n = levelNodes edges in current layer
      int index = 0;
      for (Integer currentNode : lastLayerNodes) {
        for (int i = 0; i < levelNodes; i++) {
          int targetNode = currentLayerNodes.get(index);
          final String edgeId = currentNode + Integer.toString(targetNode);
          System.out.println(edgeId);
          graph.addEdge(edgeId, currentNode, targetNode);
          index++;
        }
      }
      lastLayerNodes = new ArrayList<>(currentLayerNodes);
      currentLayerNodes.clear();
    }
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
