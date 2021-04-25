/*
package tracks.singlePlayer.florabranchi.tree;


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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import core.game.StateObservation;
import ontology.Types;

public class TreeViewer implements ViewerListener {

  private ViewerPipe pipeIn;
  private SpriteManager sman;
  private Graph graph;

  private final HashMap<Integer, List<Integer>> nodeChildrenMapByIndex = new HashMap<>();
  private final HashMap<Integer, List<Edge>> edgesMap = new HashMap<>();
  private final HashMap<Integer, Edge> childrenEdgeMap = new HashMap<>();
  private final HashMap<Integer, Node> nodeMap = new HashMap<>();
  private HashMap<Integer, Node> extraNodes = new HashMap<>();
  private HashMap<Integer, Sprite> extraSprites = new HashMap<>();
  private final HashMap<String, Sprite> gameStateSpriteMap = new HashMap<>();


  private int totalFixedNodes;
  private final TreeHelper treeHelper;

  // Viewer properties
  final static int TREE_DEPTH = 5;

  private final static int LAYER_HEIGHT = 25000;
  private final static int LAYER_WIDTH = 150;

  private final static int LAYER_HEIGHT_LOWER = 10000;

  private final static int LAYER_WIDTH_LOWER = 150;

  private final static int SPRITE_DISPLACEMENT_X = 0;
  private final static double SPRITE_DISPLACEMENT_Y = -2;

  private final static String GAME_SCORE = "GAME_SCORE";
  private final static String GAME_TICK = "GAME_TICK";
  private int maxGraphWidth;

  public TreeViewer(final StateObservation stateObservation) {

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

    addStyle();

    List<Types.ACTIONS> availableActions = new ArrayList<>();
    if (stateObservation != null) {
      availableActions = stateObservation.getAvailableActions();
    } else {

      // debug
      availableActions.add(Types.ACTIONS.ACTION_USE);
      availableActions.add(Types.ACTIONS.ACTION_LEFT);
      availableActions.add(Types.ACTIONS.ACTION_RIGHT);
    }

    treeHelper = new TreeHelper(availableActions);

    createBaseNodesWithFixedPositionFromTop(TREE_DEPTH, availableActions);
  }

  private void addStyle() {

    String myStyle =

        "node {"
            + "size: 1px;"
            + "fill-color: gray;"
            + "}"

            + "node.selected {"
            + "size: 15px;"
            + "fill-color: red, green;"
            + "fill-mode: dyn-plain;"
            + "}"

            + "edge {"
            + "fill-color: white;"
            + "}"

            + "edge.populated {"
            + "shape: line;"
            + "fill-color: #bfbfbf;"
            + "}"

            + "edge.selected {"
            + "shape: line;"
            + "size: 10px;"
            + "fill-color: yellow;"
            + "arrow-size: 3px, 2px;"
            + "}"

            + "edge.extraNodes {"
            + "fill-color: #bfbfbf;"
            + "}"

            + "sprite {"
            + "size: 0px;"
            + "text-alignment: left;"
            + "}";

    graph.addAttribute("ui.stylesheet", myStyle);
  }

  public static void main(String[] args) {

    TreeViewer n = new TreeViewer(null);
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

*
   * Initial node placement.
   *
   * @param treeDepth        desired fixed tree depth.
   * @param availableActions node children


  public void createBaseNodesWithFixedPositionFromTop(int treeDepth,
                                                      List<Types.ACTIONS> availableActions) {

    addGameStateSprites();

    int levelNodes = availableActions.size();
    maxGraphWidth = LAYER_WIDTH * (int) Math.pow(levelNodes, treeDepth);

    int nodeCount = 0;
    for (int k = 0; k < treeDepth; k++) {
      nodeCount += (int) Math.pow(levelNodes, k);
    }

    totalFixedNodes = nodeCount;

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
      double nodeSpacing = maxGraphWidth / currentLayerNodes;

      // Create current Layer nodes
      if (!lastLayerNodesList.isEmpty()) {
        int currentLayerNodeCount = 0;
        for (Integer lastLayerNode : lastLayerNodesList) {

          final List<Integer> nodeChildren = treeHelper.getNodeChildren(lastLayerNode);
          for (Integer childrenNode : nodeChildren) {

            // Generate children nodes
            generateNewNode(childrenNode, depth, (int) nodeSpacing, currentLayerNodeCount);
            currentLayerNodesList.add(childrenNode);

            // Add edge from parent to new children
            int parentNodeIndex = getNodeIndex(lastLayerNode);
            int childNodeIndex = getNodeIndex(childrenNode);

            final String edgeId = childrenNode + String.valueOf(lastLayerNode);
            final Edge edge = graph.addEdge(edgeId, parentNodeIndex, childNodeIndex);

            childrenEdgeMap.put(childrenNode, edge);
            addEdgeToMap(lastLayerNode, edge);
            addChildrenNodeToMap(lastLayerNode, childrenNode);

            currentLayerNodeCount++;
            nodeCount++;
          }
        }

      } else {
        int initialNodeId = 0;
        generateNewNode(initialNodeId, depth, (int) nodeSpacing, 0);
        currentLayerNodesList.add(0);
        nodeCount++;
      }


      lastLayerNodesList = new ArrayList<>(currentLayerNodesList);
      currentLayerNodesList.clear();
    }
  }

  public void addGameStateSprites() {

    int labelDistance = 80;
    int initialX = 0;
    int initialY = -50;

    final Sprite gameTickSprite = sman.addSprite("S" + GAME_TICK);
    gameTickSprite.setPosition(initialX, initialY, 0);
    gameTickSprite.setAttribute("ui.label", GAME_TICK);
    gameStateSpriteMap.put(GAME_TICK, gameTickSprite);

    final Sprite gameScoreSprite = sman.addSprite("S" + GAME_SCORE);
    gameScoreSprite.setPosition(initialX, initialY - labelDistance, 0);
    gameScoreSprite.setAttribute("ui.label", GAME_SCORE);
    gameStateSpriteMap.put(GAME_SCORE, gameScoreSprite);

  }

  private void generateNewNode(final int desiredId,
                               final int depth,
                               final int nodeSpacing,
                               final int currentNodeInLayer) {
    // Add new base node
    final Node node = graph.addNode(String.valueOf(desiredId));
    nodeMap.put(desiredId, node);

    //node.setAttribute("ui.label", desiredId);

    // Calculate node X
    double nodeX = (double) nodeSpacing / (double) 2 + currentNodeInLayer * nodeSpacing;

    // Size = maxDepth * LAYER_HEIGHT
    // -1 to grow from bottom to top
    int nodeY = -depth * TreeViewer.LAYER_HEIGHT;

    node.addAttribute("xyz", nodeX, nodeY, 0);

    // Attach property Sprite
    final Sprite sprite = sman.addSprite("S" + desiredId);
    sprite.attachToNode(String.valueOf(desiredId));
    sprite.setPosition(nodeX + SPRITE_DISPLACEMENT_X, -depth * TreeViewer.LAYER_HEIGHT + SPRITE_DISPLACEMENT_Y, 0);

  }

  public void addNewNodesFromParent(Integer parentNodeId,
                                    final List<ViewerNode> viewerNodes,
                                    final int nodeSpacing) {

    final Node parentNode = graph.getNode(parentNodeId.toString());

    if (parentNode == null) {
      //System.out.println("ERROR FAILED TO FIND PARENT");
      return;
    }

    final Object[] parentCoord = parentNode.getAttribute("xyz");
    double parentX = (double) parentCoord[0];
    Double parentY = Double.valueOf(String.valueOf(parentCoord[1]));

    int lastLayerHeight = 100;

    double nodeY = parentY - lastLayerHeight;

    // Calculate node X
    // Split available size between node children
    double nodeSpacingForChildren = (double) nodeSpacing / viewerNodes.size();
    double startingX = viewerNodes.size() == 1 ? parentX : parentX - (nodeSpacingForChildren / 2);

    int currNode = 0;

    for (ViewerNode newNode : viewerNodes) {

      double nodeX = startingX + (currNode * nodeSpacingForChildren);

      Integer desiredId = newNode.id;

      // Add new base node
      final Node node = graph.addNode(String.valueOf(desiredId));
      extraNodes.put(desiredId, node);

      node.addAttribute("xyz", nodeX, nodeY, 0);
      node.setAttribute("ui.color", newNode.value);
      node.addAttribute("ui.class", "selected");

      // Attach property Sprite
      final Sprite sprite = sman.addSprite("S" + desiredId);
      sprite.attachToNode(String.valueOf(desiredId));
      sprite.setPosition(nodeX + SPRITE_DISPLACEMENT_X, nodeY + SPRITE_DISPLACEMENT_Y, 0);
      extraSprites.put(desiredId, sprite);

      // Add edge from parent to new children
      int parentNodeIndex = getNodeIndex(parentNodeId);
      int childNodeIndex = getNodeIndex(desiredId);

      final String edgeId = childNodeIndex + String.valueOf(parentNodeIndex);
      final Edge edge = graph.addEdge(edgeId, parentNodeIndex, childNodeIndex);
      edge.addAttribute("ui.class", "extraNodes");
      currNode++;
    }
  }

  public int getNodeIndex(final Integer nodeId) {
    return graph.getNode(String.valueOf(nodeId)).getIndex();
  }

  public void updateExtraNodes(final List<ViewerNode> newNodeList) {

    // Clean previous temp nodes
    for (Map.Entry<Integer, Node> newNode : extraNodes.entrySet()) {
      graph.removeNode(newNode.getValue());
    }
    for (Map.Entry<Integer, Sprite> sprite : extraSprites.entrySet()) {
      sman.removeSprite(sprite.getValue().getId());
    }

    extraSprites = new HashMap<>();
    extraNodes = new HashMap<>();

    // Cast by depth
    final Map<Integer, List<ViewerNode>> nodeDepthMap = getNodeDepthMap(newNodeList);

    int currParent = 1;
    int yOffset = LAYER_HEIGHT_LOWER / 5;

    for (Map.Entry<Integer, List<ViewerNode>> nodeDepthEntries : nodeDepthMap.entrySet()) {

      final List<ViewerNode> layerNodes = nodeDepthEntries.getValue();
      //int nodeSpacing = maxGraphWidth / layerNodes.size();
      final int nodeDepth = nodeDepthEntries.getKey();
      int currentNode = 0;

      Map<Integer, List<ViewerNode>> childrenByParentMap = new HashMap<>();
      for (ViewerNode node : layerNodes) {
        int index = node.parent;
        if (childrenByParentMap.containsKey(index)) {
          final List<ViewerNode> newList = childrenByParentMap.get(index);
          newList.add(node);
        } else {
          List<ViewerNode> newList = new ArrayList<>();
          newList.add(node);
          childrenByParentMap.put(index, newList);
        }
      }


      // For each node parent

      for (Map.Entry<Integer, List<ViewerNode>> childrenNodes : childrenByParentMap.entrySet()) {

        int parentsInDepth = childrenByParentMap.size();

        Integer parentId = childrenNodes.getKey();

        int nodeSpacing = LAYER_WIDTH_LOWER * parentsInDepth / 2;
        addNewNodesFromParent(parentId, childrenNodes.getValue(), nodeSpacing);
        currParent++;
      }
      currParent = 1;
    }
  }

  public Map<Integer, List<ViewerNode>> getNodeDepthMap(final List<ViewerNode> newNodeList) {
    Map<Integer, List<ViewerNode>> castedMap = new HashMap<>();
    for (ViewerNode newNode : newNodeList) {
      final int nodeDepth = treeHelper.getNodeDepth(newNode.id);
      if (castedMap.containsKey(nodeDepth)) {
        final List<ViewerNode> viewerNodes = castedMap.get(nodeDepth);
        viewerNodes.add(newNode);
      } else {
        List<ViewerNode> newList = new ArrayList<>();
        newList.add(newNode);
        castedMap.put(nodeDepth, newList);
      }
    }
    return castedMap;
  }

*
   * Updates viewer tree with explored node values.


  public void updateTreeObjects(final int experimentId,
                                final int gameTick,
                                final int iteration,
                                final TreeNode rootNode,
                                final StateObservation stateObs,
                                final Types.ACTIONS selectedAction) {

    // Update Game state Sprites
    Sprite gameScoreSprite = gameStateSpriteMap.get(GAME_SCORE);
    gameScoreSprite.setAttribute("ui.label", String.format("%s %s", GAME_SCORE, stateObs.getGameScore()));

    Sprite roundSprite = gameStateSpriteMap.get(GAME_TICK);
    roundSprite.setAttribute("ui.label", String.format("%s %s", GAME_TICK, stateObs.getGameTick()));

    final List<ViewerNode> viewerNodes = getViewerNodeList(rootNode);
    final Map<Integer, ViewerNode> nodeMap = new HashMap<>();
    viewerNodes.forEach(n -> nodeMap.put(n.id, n));

    // Cleanup
    for (Edge edge : graph.getEdgeSet()) {
      edge.removeAttribute("ui.class");
    }

    // Get undendered nodes
    List<ViewerNode> newNodeList = viewerNodes.stream()
        .filter(node -> node.id > totalFixedNodes)
        .collect(Collectors.toList());

    // Add new nodes
    updateExtraNodes(newNodeList);


    for (Node n : graph.getEachNode()) {

      if (extraNodes.containsKey(Integer.parseInt(n.getId()))) {
        continue;
      }

      try {

        n.removeAttribute("ui.color");
        n.removeAttribute("ui.class");

        final Sprite sprite = sman.getSprite("S" + n.getId());
        if (sprite != null) {
          sprite.removeAttribute("ui.label");
        }

        int nodeId = Integer.parseInt(n.getId());

        // If node was created
        if (nodeMap.containsKey(nodeId)) {

          final ViewerNode viewerNode = nodeMap.get(nodeId);

          // Add value to balance color
          n.addAttribute("ui.class", "selected");
          n.setAttribute("ui.color", viewerNode.value);

          // Update data sprite
          if (sprite != null) {
            sprite.setAttribute("ui.label", viewerNode);
          }

          // Activate related edge
          final Edge relatedEdge = childrenEdgeMap.get(nodeId);
          if (relatedEdge != null) {
            relatedEdge.setAttribute("ui.class", "populated");
          }

          // Add Action label to first level nodes
          final Collection<Edge> edgeSet = edgesMap.get(0);
          edgeSet.forEach(edge -> {
            final Node node1 = edge.getNode1();
            final ViewerNode tempNode = nodeMap.get(Integer.parseInt(node1.getId()));

            if (tempNode != null && tempNode.action != null) {
              if (selectedAction != null && tempNode.action.equals(selectedAction.toString())) {
                edge.setAttribute("ui.class", "selected");
              }
              edge.addAttribute("ui.label", tempNode.action);
            }
          });

        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }


    Path path = Paths.get(String.format("tests/test%s", experimentId));
    try {
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    graph.setAttribute("ui.screenshot", String.format("tests/test%s/sample%d%d.png", experimentId, gameTick, iteration));
    pipeIn.pump();
  }

  public List<ViewerNode> getViewerNodeList(final TreeNode rootNode) {

    List<ViewerNode> viewerNodeList = new ArrayList<>();
    Queue<TreeNode> queue = new ArrayDeque<>();
    queue.add(rootNode);

    while (!queue.isEmpty()) {

      // Get element
      TreeNode currentNode = queue.remove();

      // Remove unvisited children
      if (currentNode.visits == 0) {
        continue;
      }

      viewerNodeList.add(new ViewerNode(currentNode.id, currentNode));
      if (currentNode.children != null && currentNode.children.size() > 0) {
        queue.addAll(currentNode.children);
      }
    }

    return viewerNodeList;
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
*/
