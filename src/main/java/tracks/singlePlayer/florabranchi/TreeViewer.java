package tracks.singlePlayer.florabranchi;


import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSourceDGS;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.swingViewer.Viewer;
import org.graphstream.ui.swingViewer.ViewerListener;
import org.graphstream.ui.swingViewer.ViewerPipe;
import org.graphstream.ui.swingViewer.basicRenderer.SwingBasicGraphRenderer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import core.game.StateObservation;
import tracks.singlePlayer.florabranchi.models.TreeNode;
import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeViewer implements ViewerListener {

  ViewerPipe pipeIn;
  SpriteManager sman;
  private Graph graph;

  private HashMap<Integer, List<Integer>> nodeChildrenMapByIndex = new HashMap<>();
  private HashMap<Integer, Integer> translatedIdsMap = new HashMap<>();
  private HashMap<Integer, List<Integer>> idsPerDepth;

  public TreeViewer() {

    idsPerDepth = new HashMap<>();

    System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

    graph = new SingleGraph("Monte Carlo Viewer 1");
    sman = new SpriteManager(graph);

    Viewer display = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);

    display.disableAutoLayout();

    pipeIn = display.newViewerPipe();
    display.addView("view1", new SwingBasicGraphRenderer());
    pipeIn.addViewerListener(this);
    pipeIn.addSink(graph);
    pipeIn.pump();

    String myStyle = "node {"
        + "size: 10px;"
        + "fill-color: red, green;"
        + "fill-mode: dyn-plain;"
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

  public void addChildrenNodeToMap(final Integer parentId,
                                   final Integer childId) {

    if (nodeChildrenMapByIndex.containsKey(parentId)) {
      final List<Integer> existingChildren = nodeChildrenMapByIndex.get(parentId);
      existingChildren.add(childId);
      nodeChildrenMapByIndex.put(parentId, existingChildren);
    } else {
      final ArrayList<Integer> children = new ArrayList<>();
      children.add(childId);
      nodeChildrenMapByIndex.put(parentId, children);
    }
  }

  public void createBaseNodesWithFixedPosition(int treeDepth,
                                               int levelNodes) {

    int layerWeight = 50;
    int layerWidth = 15;
    int spriteDisplacementX = 0;
    double spriteDisplacementY = -2;

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
        //node.setAttribute("ui.label", nodeCount);

        // Attach property Sprite
        final Sprite sprite = sman.addSprite("S" + String.valueOf(nodeCount));
        sprite.attachToNode(String.valueOf(nodeCount));
        sprite.setPosition(nodeX + spriteDisplacementX, -depth * layerWeight + spriteDisplacementY, 0);

        // Position debug
        //final double[] doubles = Toolkit.nodePosition(node);
        //System.out.println(String.format("%s - %s %s %s", nodeCount, doubles[0], doubles[1], doubles[2]));
        //node.setAttribute("ui.label", node.getId());

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
            int currentLayerNodeIndex = getNodeIndex(currentNode);
            int lastLayerNodeIndex = getNodeIndex(lastLayerNodesList.get(index));
            final String edgeId = String.valueOf(currentNode) + String.valueOf((lastLayerNodesList.get(index)));
            graph.addEdge(edgeId, currentLayerNodeIndex, lastLayerNodeIndex);
            addChildrenNodeToMap(currentNode, lastLayerNodesList.get(index));
            //edge.setAttribute("ui.label", edgeId);
            index++;
          }
        }
      }

      lastLayerNodesList = new ArrayList<>(currentLayerNodesList);
      currentLayerNodesList.clear();
    }

    graph.setAttribute("ui.screenshot", String.format("tests/sample%d%d.png", 99, 2));
    pipeIn.pump();
  }

  public int getNodeIndex(final Integer nodeId) {
    return graph.getNode(String.valueOf(nodeId)).getIndex();
  }

  public List<ViewerNode> castRootNode(final StateObservation initialState,
                                       final TreeNode rootNode) {

    List<ViewerNode> viewerNodeList = new ArrayList<>();
    int lastNode = 1;
    for (int i = 0; i < 13; i++) {
      int totalNodesInDepth = (int) Math.pow(initialState.getAvailableActions().size(), i);
      idsPerDepth.put(i, IntStream.range(lastNode, lastNode + totalNodesInDepth).boxed().collect(Collectors.toList()));
      lastNode = lastNode + totalNodesInDepth;
    }

    Queue<TreeNode> queue = new ArrayDeque<>();
    queue.add(rootNode);

    // Create copy of parent map
    Map<Integer, List<Integer>> mapCopy = new HashMap<>();

    nodeChildrenMapByIndex.forEach((key, value) -> mapCopy.put(key,
        new ArrayList<>(value)));


    while (!queue.isEmpty()) {

      // Get element
      TreeNode currentNode = queue.remove();

      Integer parentNode = null;

      // Find parent in base 1
      if (currentNode.parent != null) {
        parentNode = translatedIdsMap.get(currentNode.parent.id);
        //System.out.println("Parent casted " + parentNode);
      }

      int nodeDepth = getNodeDepth(currentNode);

      // Skip nodes not available in visualization
      if (!idsPerDepth.containsKey(nodeDepth)
          || (parentNode != null
          && !mapCopy.containsKey(parentNode))) {
        continue;
      }

      final int nodeId = getNodeId(parentNode, mapCopy);
      translatedIdsMap.put(currentNode.id, nodeId);
      //System.out.println("Node id" + nodeId + " Parent: " + parentNode);
      viewerNodeList.add(new ViewerNode(nodeId, currentNode));

      if (currentNode.children != null && currentNode.children.size() > 0) {
        queue.addAll(currentNode.children);
      }
    }

    return viewerNodeList;
  }

  public Integer getNodeId(final Integer parentId,
                           final Map<Integer, List<Integer>> availableIdentifiers) {

    if (parentId == null) {
      return 1;
    }

    final Integer nodeId = availableIdentifiers.get(parentId).get(0);
    availableIdentifiers.get(parentId).remove(0);
    return nodeId;
  }

  public int getNodeDepth(final TreeNode node) {
    int treeDepth = 0;
    TreeNode selectedNode = node;
    while (selectedNode.parent != null) {
      treeDepth++;
      selectedNode = selectedNode.parent;
    }
    return treeDepth;
  }


  public void updateNodes(final int gameTick,
                          final int iteration,
                          final TreeNode rootNode,
                          final StateObservation stateObs) {

    final List<ViewerNode> viewerNodes = castRootNode(stateObs, rootNode);

    final Map<Integer, ViewerNode> nodeMap = new HashMap<>();
    viewerNodes.forEach(n -> nodeMap.put(n.id, n));

    for (Node n : graph.getEachNode()) {

      try {
        final Sprite sprite = sman.getSprite("S" + n.getId());
        n.removeAttribute("ui.color");
        sprite.removeAttribute("ui.label");

        int nodeId = Integer.parseInt(n.getId());

        if (nodeMap.containsKey(nodeId)) {

          final ViewerNode viewerNode = nodeMap.get(nodeId);

          // Add value to balance color
          n.setAttribute("ui.color", viewerNode.value);

          // Update data sprite
          sprite.setAttribute("ui.label", viewerNode);

          // Add edges
          final Collection<Edge> edgeSet = n.getEdgeSet();
          edgeSet.forEach(edge -> {
            final Node node1 = edge.getNode1();
            final ViewerNode tempNode = nodeMap.get(Integer.parseInt(node1.getId()));
            edge.addAttribute("ui.label", tempNode.action);
          });
        }

      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }


    graph.setAttribute("ui.screenshot", String.format("tests/sample%d%d.png", gameTick, iteration));
    pipeIn.pump();
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
