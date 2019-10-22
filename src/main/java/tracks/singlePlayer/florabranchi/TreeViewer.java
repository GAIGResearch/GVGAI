package tracks.singlePlayer.florabranchi;


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
  private FileSinkImages pic;

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

    FileSourceDGS dgs = new FileSourceDGS();
    FileSinkImages.OutputType type = FileSinkImages.OutputType.PNG;
    FileSinkImages.Resolution resolution = FileSinkImages.Resolutions.HD720;

    pic = new FileSinkImages(type, resolution);
    dgs.addSink(pic);


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

  private void saveToFile(int iteration,
                          int gameTick) {


    pipeIn.pump();

/*    try {
      pic.writeAll(graph, String.format("tests/sample%d%s.png", iteration, gameTick));
    } catch (RuntimeException | IOException e) {
      e.printStackTrace();
    }*/
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
        node.setAttribute("ui.label", nodeCount);

        // Attach property Sprite
        final Sprite sprite = sman.addSprite("S" + String.valueOf(nodeCount));
        sprite.attachToNode(String.valueOf(nodeCount));
        sprite.setPosition(nodeX + spriteDisplacementX, -depth * layerWeight + spriteDisplacementY, 0);

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


  public Map<Integer, ViewerNode> createNodeMap(List<ViewerNode> nodes) {
    final Map<Integer, ViewerNode> nodeMap = new HashMap<>();
    nodes.forEach(node -> nodeMap.put(node.id, node));
    return nodeMap;
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
        }

      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }


    graph.setAttribute("ui.screenshot", String.format("tests/sample%d%d.png", gameTick, iteration));
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
      try {
        graph.addEdge(edgeId, parentNodeId, childId);
      } catch (RuntimeException ex) {
        ex.printStackTrace();
      }
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
