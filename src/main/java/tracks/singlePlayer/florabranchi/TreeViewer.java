package tracks.singlePlayer.florabranchi;


import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
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
import ontology.Types;
import tracks.singlePlayer.florabranchi.models.TreeNode;
import tracks.singlePlayer.florabranchi.models.ViewerNode;
import tracks.singlePlayer.florabranchi.tree.TreeHelper;

public class TreeViewer implements ViewerListener {

  private ViewerPipe pipeIn;
  private SpriteManager sman;
  private Graph graph;

  private HashMap<Integer, List<Integer>> nodeChildrenMapByIndex = new HashMap<>();
  private HashMap<Integer, Integer> translatedIdsMap = new HashMap<>();
  private HashMap<Integer, List<Integer>> idsPerDepth;
  private HashMap<Integer, List<Edge>> edgesMap;

  private TreeHelper treeHelper;

  public TreeViewer() {

    idsPerDepth = new HashMap<>();
    edgesMap = new HashMap<>();

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

    int treeDepth = 3;
    int levelNodes = 3;

    // todo change for sso
    List<Types.ACTIONS> availableActions = new ArrayList<>();
    availableActions.add(Types.ACTIONS.ACTION_USE);
    availableActions.add(Types.ACTIONS.ACTION_LEFT);
    availableActions.add(Types.ACTIONS.ACTION_RIGHT);

    treeHelper = new TreeHelper(treeDepth, availableActions);

    //createBaseNodesWithFixedPosition(treeDepth, levelNodes);
  }

  public static void main(String[] args) {

    List<Types.ACTIONS> availableActions = new ArrayList<>();
    availableActions.add(Types.ACTIONS.ACTION_USE);
    availableActions.add(Types.ACTIONS.ACTION_LEFT);
    availableActions.add(Types.ACTIONS.ACTION_RIGHT);

    TreeViewer n = new TreeViewer();
    n.createBaseNodesWithFixedPositionFromTop(4, availableActions);
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

  public void addEdgeToMap(final Integer parentId,
                           final Edge edge) {

    if (edgesMap.containsKey(parentId)) {
      final List<Edge> existingChildren = edgesMap.get(parentId);
      existingChildren.add(edge);
      edgesMap.put(parentId, existingChildren);
    } else {
      final ArrayList<Edge> children = new ArrayList<>();
      children.add(edge);
      edgesMap.put(parentId, children);
    }
  }

  public void createBaseNodesWithFixedPositionFromTop(int treeDepth,
                                                      List<Types.ACTIONS> availableActions) {

    int levelNodes = availableActions.size();
    int layerWeight = 30;
    int layerWidth = 20;
    int spriteDisplacementX = 0;
    double spriteDisplacementY = -2;

    int maxWidth = layerWidth * (int) Math.pow(levelNodes, treeDepth);

    int nodeCount = 0;
    for (int k = 0; k <= treeDepth; k++) {
      nodeCount += (int) Math.pow(levelNodes, k);
    }

    int firstLayerNode = 0;

    List<Integer> lastLayerNodesList = new ArrayList<>();
    List<Integer> currentLayerNodesList = new ArrayList<>();

    //   //\\
    //  ///\\\
    // ////\\\\\
    //   //\\

    // Build tree from top
    for (int depth = 0; depth < treeDepth; depth++) {

      double currentLayerNodes = Math.pow(levelNodes, depth);
      double nodeSpacing = maxWidth / currentLayerNodes;
      firstLayerNode = nodeCount;

      // Create current Layer nodes
      if (!lastLayerNodesList.isEmpty()) {
        int currentLayerNodeCount = 0;
        for (Integer lastLayerNode : lastLayerNodesList) {

          final List<Integer> nodeChildren = treeHelper.getNodeChildren(lastLayerNode);
          for (Integer childrenNode : nodeChildren) {

            // Generate children nodes
            generateNewNode(layerWeight, childrenNode, depth, (int) nodeSpacing, currentLayerNodeCount);
            currentLayerNodesList.add(childrenNode);

            // Add edge from parent to new children
            int parentNodeIndex = getNodeIndex(lastLayerNode);
            int childNodeIndex = getNodeIndex(childrenNode);

            final String edgeId = childrenNode + String.valueOf(lastLayerNode);
            graph.addEdge(edgeId, parentNodeIndex, childNodeIndex);

            currentLayerNodeCount++;
            nodeCount++;
          }
        }

      } else {
        int initialNodeId = 0;
        generateNewNode(layerWeight, initialNodeId, depth, (int) nodeSpacing, 0);
        currentLayerNodesList.add(0);
        nodeCount++;
      }


      lastLayerNodesList = new ArrayList<>(currentLayerNodesList);
      currentLayerNodesList.clear();
    }

    graph.setAttribute("ui.screenshot", String.format("tests/sample%d%d.png", 1, 2));
    pipeIn.pump();
  }


  /**
   * Initial node placement.
   *
   * @param treeDepth  desired fixed tree depth.
   * @param levelNodes derided children
   */
  public void createBaseNodesWithFixedPosition(int treeDepth,
                                               int levelNodes) {

    int layerWeight = 20;
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

        double nodeX = generateNewNode(layerWeight, nodeCount, depth, nodeSpacing, currentNode);

        // add node id
        //node.setAttribute("ui.label", nodeCount);

        // Attach property Sprite
        final Sprite sprite = sman.addSprite("S" + nodeCount);
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

            final String edgeId = currentNode + String.valueOf((lastLayerNodesList.get(index)));
            final Edge edge = graph.addEdge(edgeId, currentLayerNodeIndex, lastLayerNodeIndex);

            addEdgeToMap(currentLayerNodeIndex, edge);
            addChildrenNodeToMap(currentNode, lastLayerNodesList.get(index));
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

  private double generateNewNode(final int layerWeight,
                                 final int desiredId,
                                 final int depth,
                                 final int nodeSpacing,
                                 final int currentNodeInLayer) {
    // Add new base node
    final Node node = graph.addNode(String.valueOf(desiredId));
    node.setAttribute("ui.label", desiredId);

    // Calculate node X
    double nodeX = (double) nodeSpacing / (double) 2 + currentNodeInLayer * nodeSpacing;

    // Size = maxDepth * layerWeight
    // -1 to grow from bottom to top
    node.addAttribute("xyz", nodeX, -depth * layerWeight, 0);
    return nodeX;
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

          // Add first layer adge and selection
          final Collection<Edge> edgeSet = edgesMap.get(0);
          edgeSet.forEach(edge -> {
            final Node node1 = edge.getNode1();
            final ViewerNode tempNode = nodeMap.get(Integer.parseInt(node1.getId()));

            if (tempNode != null && tempNode.action != null) {
              edge.addAttribute("ui.label", tempNode.action);
            }

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
