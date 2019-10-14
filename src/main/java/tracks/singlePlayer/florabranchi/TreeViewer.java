package tracks.singlePlayer.florabranchi;


import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.Viewer;
import org.graphstream.ui.swingViewer.ViewerListener;
import org.graphstream.ui.swingViewer.ViewerPipe;
import org.graphstream.ui.swingViewer.basicRenderer.SwingBasicGraphRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeViewer implements ViewerListener {

  ViewerPipe pipeIn;
  private Graph graph;

  public TreeViewer() {
    System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    graph = new SingleGraph("Tutorial 1");
    Viewer display = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
    display.disableAutoLayout();
    pipeIn = display.newViewerPipe();
    display.addView("view1", new SwingBasicGraphRenderer());
    pipeIn.addViewerListener(this);
    pipeIn.pump();

    String myStyle = "node {"
        + "size: 10px;"
        //+ "fill-color: red;"
        + "}"

        + "edge {"
        + "shape: line;"
        + "fill-color: #222;"
        + "arrow-size: 3px, 2px;"
        + "}";

    graph.addAttribute("ui.stylesheet", myStyle);
    //createBaseNodes(4, 3);
  }

  public static void main(String[] args) {

    TreeViewer n = new TreeViewer();
    n.createBaseNodesWithFixedPosition(3, 3);
  }

  public void createBaseNodesWithFixedPosition(int treeDepth, int levelNodes) {

    int totalNodes = 0;
    for (int k = 0; k <= treeDepth; k++) {
      totalNodes += (int) Math.pow(levelNodes, k);
    }
    int absoluteTotalNodes = totalNodes;

    int currentLayerNodes = 0;
    int maxWidth = (int) Math.pow(levelNodes, treeDepth);
    List<Integer> lastLayerNodesList = new ArrayList<>();
    List<Integer> currentLayerNodesList = new ArrayList<>();
    for (int d = treeDepth; d >= 0; d--) {

      currentLayerNodes = (int) Math.pow(levelNodes, d);
      int nodeSpacing = maxWidth / currentLayerNodes;
      for (int i = 0; i < currentLayerNodes; i++) {
        final Node node = graph.addNode(String.valueOf(totalNodes));
        double nodeX = (double) nodeSpacing / (double) 2 + i * nodeSpacing;
        node.addAttribute("xyz", nodeX, -d * 10, 0);
        final double[] doubles = Toolkit.nodePosition(node);
        System.out.println(String.format("%s - %s %s %s", totalNodes, doubles[0], doubles[1], doubles[2]));
        //node.setAttribute("ui.label", totalNodes);
        currentLayerNodesList.add(totalNodes);
        totalNodes--;
      }

      // Add layer edges
      // for each node in last layer
      // link to n = levelNodes edges in current layer
      int index = 0;
      if (!lastLayerNodesList.isEmpty()) {
        for (Integer currentNode : currentLayerNodesList) {
          for (int i = 0; i < levelNodes; i++) {
            int currentNodeId = getNodeId(currentNode, absoluteTotalNodes);
            int targetNodeId = getNodeId(lastLayerNodesList.get(index), absoluteTotalNodes);
            Node n = graph.getNode(targetNodeId);
            final String edgeId = String.valueOf(currentNodeId + targetNodeId);
            graph.addEdge(edgeId, targetNodeId, currentNodeId);
            index++;
          }
        }
      }

      lastLayerNodesList = new ArrayList<>(currentLayerNodesList);
      currentLayerNodesList.clear();
    }

   /* int minNodeW = 0;
    int maxNodeW = (currentLayerNodes - 1);
    for (int i = 0; i < currentLayerNodes; i++) {
      final Node node = graph.addNode(String.valueOf(totalNodes));
      node.addAttribute("xyz", i, initialY, 0);
      final double[] doubles = Toolkit.nodePosition(node);
      System.out.println(String.format("%s - %s %s %s", totalNodes, doubles[0], doubles[1], doubles[2]));
      node.setAttribute("ui.label", totalNodes);
      totalNodes--;
    }

    double avgW = (maxNodeW - minNodeW) / (double) 2;

    currentLayerNodes = (int) Math.pow(levelNodes, treeDepth - 1);
    for (int i = 0; i < currentLayerNodes; i++) {
      final Node node = graph.addNode(String.valueOf(totalNodes));
      node.addAttribute("xyz", avgW, 1, 0);
      final double[] doubles = Toolkit.nodePosition(node);
      System.out.println(String.format("%s %s %s", doubles[0], doubles[1], doubles[2]));
      totalNodes--;
    }*/

  }

  public int getNodeId(final int nodeId,
                       final int absoluteTotalNodes) {
    return (absoluteTotalNodes - nodeId);
  }

  public void createBaseNodes(int treeDepth, int levelNodes) {

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

  public void addTestNodes(List<ViewerNode> nodes) {
    for (ViewerNode node : nodes) {
      try {
        final Node node1 = graph.getNode(node.id);
        if (node1 != null) {
          node1.addAttribute("ui.label", node);
        }
      } catch (final IndexOutOfBoundsException ex) {
        System.out.println("Node does not exist");
      }

    }

    pipeIn.pump();
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


  @Override
  public void viewClosed(final String s) {

  }

  @Override
  public void buttonPushed(final String s) {

  }

  @Override
  public void buttonReleased(final String s) {

  }
}
