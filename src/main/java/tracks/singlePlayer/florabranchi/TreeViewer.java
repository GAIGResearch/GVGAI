package tracks.singlePlayer.florabranchi;


import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.swingViewer.Viewer;
import org.graphstream.ui.swingViewer.ViewerListener;
import org.graphstream.ui.swingViewer.ViewerPipe;
import org.graphstream.ui.swingViewer.basicRenderer.SwingBasicGraphRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tracks.singlePlayer.florabranchi.models.TreeNode;
import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeViewer implements ViewerListener {

  ViewerPipe pipeIn;
  SpriteManager sman;
  private Graph graph;

  public TreeViewer() {
    System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    graph = new SingleGraph("Tutorial 1");
    sman = new SpriteManager(graph);
    Viewer display = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
    display.disableAutoLayout();
    pipeIn = display.newViewerPipe();
    display.addView("view1", new SwingBasicGraphRenderer());
    pipeIn.addViewerListener(this);
    pipeIn.pump();

    String myStyle = "node {"
        + "size: 10px;"
        + "fill-color: red, green;"
        + "fill-mode: dyn-plain;"
        + "stroke-mode: plain;"
        + "stroke-color: black;"

        + "}"

        + "edge {"
        + "shape: line;"
        + "fill-color: #222;"
        + "arrow-size: 3px, 2px;"
        + "}"

        + "sprite {"
        + "size: 0px;"
        + "text-alignment: left;"
        + "}";

    graph.addAttribute("ui.stylesheet", myStyle);
    createBaseNodesWithFixedPosition(3, 3);
  }

  public static void main(String[] args) {

    TreeViewer n = new TreeViewer();
    //n.createBaseNodesWithFixedPosition(4, 3);
  }

  public void createBaseNodesWithFixedPosition(int treeDepth,
                                               int levelNodes) {

    int layerWeight = 50;
    int layerWidth = 15;
    int spriteDisplacementX = 0;
    double spriteDisplacementY = -5;

    int maxWidth = layerWidth * (int) Math.pow(levelNodes, treeDepth);

    int nodeCount = 0;
    for (int k = 0; k <= treeDepth; k++) {
      nodeCount += (int) Math.pow(levelNodes, k);
    }

    int currentLayerNodes;

    List<Integer> lastLayerNodesList = new ArrayList<>();
    List<Integer> currentLayerNodesList = new ArrayList<>();

    // Build tree from bottom
    for (int depth = treeDepth; depth >= 0; depth--) {

      currentLayerNodes = (int) Math.pow(levelNodes, depth);

      int nodeSpacing = maxWidth / currentLayerNodes;

      for (int currentNode = 0; currentNode < currentLayerNodes; currentNode++) {

        // Add new base node
        final Node node = graph.addNode(String.valueOf(nodeCount));
        double nodeX = (double) nodeSpacing / (double) 2 + currentNode * nodeSpacing;

        // Size = maxDepth * layerWeight
        // -1 to grow from bottom to top
        node.addAttribute("xyz", nodeX, -depth * layerWeight, 0);

        // Attach property Sprite
        final Sprite sprite = sman.addSprite("S" + String.valueOf(nodeCount));
        sprite.attachToNode(String.valueOf(nodeCount));
        sprite.setPosition(nodeX + spriteDisplacementX, -depth * layerWeight + spriteDisplacementY, 0);

        //final double[] doubles = Toolkit.nodePosition(node);
        //System.out.println(String.format("%s - %s %s %s", nodeCount, doubles[0], doubles[1], doubles[2]));
        //node.setAttribute("ui.label", totalNodes);

        currentLayerNodesList.add(nodeCount);
        nodeCount--;
      }

      // Add layer edges
      // for each node in current layer
      // link to n = levelNodes edges in last layer
      int index = 0;
      if (!lastLayerNodesList.isEmpty()) {
        for (Integer currentNode : currentLayerNodesList) {
          for (int i = 0; i < levelNodes; i++) {
            int currentNodeIndex = getNodeIndex(currentNode);
            int targetNodeIndex = getNodeIndex(lastLayerNodesList.get(index));
            final String edgeId = String.valueOf(currentNodeIndex + targetNodeIndex);
            graph.addEdge(edgeId, targetNodeIndex, currentNodeIndex);
            index++;
          }
        }
      }

      lastLayerNodesList = new ArrayList<>(currentLayerNodesList);
      currentLayerNodesList.clear();
    }
  }

  public int getNodeIndex(final Integer nodeId) {
    return graph.getNode(String.valueOf(nodeId)).getIndex();
  }

  public void createFreeBaseNodes(int treeDepth, int levelNodes) {

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

  public void updateNodes(List<ViewerNode> nodes,
                          final TreeNode bestChild) {

    for (Node n : graph.getEachNode()) {
      n.removeAttribute("ui.color");

      try {
        final Sprite sprite = sman.getSprite("S" + n.getId());
        sprite.removeAttribute("ui.label");
      } catch (Exception ex) {

      }

    }

    for (ViewerNode node : nodes) {
      try {
        // Index is 1 based
        final Node node1 = graph.getNode(getNodeIndex(node.id + 1));
        if (node1 != null) {
          //node1.setAttribute("ui.label", node);
          if (node.id.equals(bestChild.id)) {
            node1.setAttribute("ui.label", "BEST");
          } else {
            // Add value to balance color
            node1.removeAttribute("ui.label");
            node1.setAttribute("ui.color", node.value);
          }

          // Update data sprite
          final Sprite sprite = sman.getSprite("S" + node.id);
          sprite.setAttribute("ui.label", node);

        }
      } catch (final Exception ex) {
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
