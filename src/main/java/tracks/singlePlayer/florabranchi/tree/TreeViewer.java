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

import core.game.StateObservation;
import ontology.Types;

public class TreeViewer implements ViewerListener {

  private ViewerPipe pipeIn;
  private SpriteManager sman;
  private Graph graph;

  private HashMap<Integer, List<Integer>> nodeChildrenMapByIndex = new HashMap<>();
  private HashMap<Integer, List<Edge>> edgesMap = new HashMap<>();
  private HashMap<Integer, Node> nodeMap = new HashMap<>();
  private HashMap<String, Sprite> gameStateSpriteMap = new HashMap<>();


  private TreeHelper treeHelper;

  // Viewer properties
  final static int TREE_DEPTH = 4;

  private final static int LAYER_WEIGHT = 300;
  private final static int LAYER_WIDTH = 50;

  private final static int SPRITE_DISPLACEMENT_X = 0;
  private final static double SPRITE_DISPLACEMENT_Y = -2;

  private final static String GAME_SCORE = "GAME_SCORE";
  private final static String GAME_TICK = "GAME_TICK";

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

    String myStyle = "node {"
        + "size: 10px;"
        + "fill-color: red, green;"
        + "fill-mode: dyn-plain;"
        + "}"

        + "edge {"
        + "shape: line;"
        + "fill-color: #222;"
        + "}"

        + "edge.selected {"
        + "shape: line;"
        + "size: 10px;"
        + "fill-color: yellow;"
        + "arrow-size: 3px, 2px;"
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

  /**
   * Initial node placement.
   *
   * @param treeDepth        desired fixed tree depth.
   * @param availableActions node children
   */
  public void createBaseNodesWithFixedPositionFromTop(int treeDepth,
                                                      List<Types.ACTIONS> availableActions) {

    addGameStateSprites();

    int levelNodes = availableActions.size();
    int maxWidth = LAYER_WIDTH * (int) Math.pow(levelNodes, treeDepth);

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

    // Size = maxDepth * LAYER_WEIGHT
    // -1 to grow from bottom to top
    int nodeY = -depth * TreeViewer.LAYER_WEIGHT;

    node.addAttribute("xyz", nodeX, nodeY, 0);

    // Attach property Sprite
    final Sprite sprite = sman.addSprite("S" + desiredId);
    sprite.attachToNode(String.valueOf(desiredId));
    sprite.setPosition(nodeX + SPRITE_DISPLACEMENT_X, -depth * TreeViewer.LAYER_WEIGHT + SPRITE_DISPLACEMENT_Y, 0);
  }

  public int getNodeIndex(final Integer nodeId) {
    return graph.getNode(String.valueOf(nodeId)).getIndex();
  }

  /**
   * Updates viewer tree with explored node values.
   */
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

          // Add Action label to first level nodes
          final Collection<Edge> edgeSet = edgesMap.get(0);
          edgeSet.forEach(edge -> {
            final Node node1 = edge.getNode1();
            final ViewerNode tempNode = nodeMap.get(Integer.parseInt(node1.getId()));

            if (tempNode != null && tempNode.action != null) {
              edge.removeAttribute("ui.class");
              if (selectedAction != null && tempNode.action.equals(selectedAction.toString())) {
                edge.addAttribute("ui.class", "selected");
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
