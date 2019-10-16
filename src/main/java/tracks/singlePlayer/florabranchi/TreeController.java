package tracks.singlePlayer.florabranchi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import core.game.StateObservation;
import javafx.util.Pair;
import ontology.Types;
import tracks.singlePlayer.florabranchi.models.TreeNode;
import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeController {

  private final static Logger logger = Logger.getLogger(TreeController.class.getName());

  private final static double C = 1 / Math.sqrt(2);

  private Map<Integer, List<Integer>> idsPerDepth = new HashMap<>();

  private TreeNode rootNode;

  private Random rand = new Random();

  public TreeController(StateObservation initialState) {

    int nodeChildren = initialState.getAvailableActions().size();
    int lastNode = 0;
    for (int i = 0; i < 12; i++) {
      int totalNodesInDepth = (int) Math.pow(nodeChildren, i);
      idsPerDepth.put(i, IntStream.range(lastNode, lastNode + totalNodesInDepth).boxed().collect(Collectors.toList()));
      lastNode = lastNode + totalNodesInDepth;
    }
  }

  private void logMessage(final String message) {
    logger.log(Level.INFO, message);
  }

  public void treeSearch(final int iterations,
                         final StateObservation initialState) {

    if (rootNode == null) {
      rootNode = new TreeNode(null, null);
    }

    for (int i = 0; i < iterations; i++) {
      final Pair<TreeNode, StateObservation> policyResult = executeTreePolicyV2(initialState);
      final TreeNode selectedNode = policyResult.getKey();
      logMessage(String.format("Rollouting selected node %s", selectedNode.id));
      rollout(selectedNode, initialState);
    }
  }

  public void pruneTree(final Types.ACTIONS selectedAction, final double nodeChildren) {
    rootNode = rootNode.children.stream().filter(child -> child.previousAction == selectedAction).findFirst()
        .orElse(null);
    rootNode.parent = null;

  }

  public List<ViewerNode> castRootNode(StateObservation initialState) {

    List<ViewerNode> viewerNodeList = new ArrayList<>();

    Queue<TreeNode> queue = new ArrayDeque<>();
    queue.add(rootNode);
    System.out.println(rootNode.nodeCount);

    while (!queue.isEmpty()) {
      TreeNode currentNode = queue.remove();
      System.out.println(String.format("Elements in queue: %s", queue.size()));
      int nodeDepth = getNodeDepth(currentNode);

      if (!idsPerDepth.containsKey(nodeDepth)) {
        continue;
      }
      int nodeId = idsPerDepth.get(nodeDepth).get(0);

      idsPerDepth.get(nodeDepth).remove(0);
      System.out.println(String.format("Node id: %d Depth: %d", currentNode.id, nodeDepth));
      final List<Integer> childrenId = currentNode.children.stream().map(node -> node.id).collect(Collectors.toList());
      viewerNodeList.add(new ViewerNode(nodeId, currentNode));

      if (currentNode.children != null && currentNode.children.size() > 0) {
        System.out.println(String.format("Adding children of id: %d children: %s", currentNode.id, currentNode.children.size()));
        queue.addAll(currentNode.children);
      }
    }


    //castLayer(rootNode, depth, 0, viewerNodeList, nodeChildren);
    return viewerNodeList;


    //List<TreeNode> treeNodes = createListOfNodes(rootNode);
    //return treeNodes.stream().map(ViewerNode::new).collect(Collectors.toList());
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

  private List<TreeNode> createListOfNodes(final TreeNode rootNode) {
    List<TreeNode> list = new ArrayList<>();
    flattenNodes(rootNode, list);
    return list;
  }


  private void castLayer(final TreeNode treeNode,
                         final int currDepth,
                         final int nodeId,
                         final List<ViewerNode> listOfNodes,
                         final int nodeChildren) {
    int nextLayerFirstElement = (int) Math.pow(nodeChildren, currDepth);
    int index = 0;
    int tempCurrDepth = currDepth;
    tempCurrDepth++;
    System.out.println(String.format("Node id: %d Depth: %d", nodeId, currDepth));
    listOfNodes.add(new ViewerNode(nodeId, treeNode));
    for (TreeNode child : treeNode.children) {
      castLayer(child, tempCurrDepth, nextLayerFirstElement + index, listOfNodes, nodeChildren);
      index++;
    }
  }

  private void flattenNodes(final TreeNode node,
                            final List<TreeNode> listOfNodes) {
    listOfNodes.add(node);
    for (TreeNode child : node.children) {
      flattenNodes(child, listOfNodes);
    }
  }

  private Pair<TreeNode, StateObservation> executeTreePolicyV2(final StateObservation stateObservation) {

    TreeNode selectedNode = rootNode;

    while (!stateObservation.isGameOver()) {

      if (selectedNode.children.isEmpty()) {
        logMessage(String.format("Expanding children of node %s", selectedNode.id));
        for (Types.ACTIONS action : stateObservation.getAvailableActions()) {
          expand(selectedNode, stateObservation);
        }
        return new Pair<>(selectedNode.children.get(0), stateObservation);
      }

      logMessage(String.format("Node %s have child", selectedNode.id));
      selectedNode = getBestChild(selectedNode);
      logMessage(String.format("Selected child %d", selectedNode.id));
      Types.ACTIONS actionToGetToNode = selectedNode.previousAction;
      if (actionToGetToNode != null) {
        stateObservation.advance(actionToGetToNode);
      }
    }

    return new Pair<>(selectedNode, stateObservation);
  }

  private boolean isLeafNode(final TreeNode node) {
    return node.children.isEmpty();
  }


  public void rollout(final TreeNode selectedNode,
                      final StateObservation initialState) {
    double initialScore = initialState.getGameScore();
    StateObservation copyState = initialState.copy();
    while (!copyState.isGameOver()) {
      final ArrayList<Types.ACTIONS> availableActions = copyState.getAvailableActions();
      copyState.advance(availableActions.get(rand.nextInt(availableActions.size() - 1)));
    }

    double finalScore = copyState.getGameScore();
    double stateReward = 0.5;
    double scoreDelta = initialScore - finalScore;
    stateReward += 0.01 * scoreDelta;

    final Types.WINNER gameWinner = copyState.getGameWinner();

    if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
      stateReward = 1;
    } else if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
      stateReward = 0;
    }

    updateTree(selectedNode, stateReward);
  }

  public void updateTree(final TreeNode selectedNode,
                         final double updatedValue) {

    selectedNode.visits++;
    selectedNode.value = selectedNode.value + updatedValue;
    logMessage(String.format("Node %s has been visited %d times", selectedNode.id, selectedNode.visits));
    if (selectedNode.parent != null) {
      updateTree(selectedNode.parent, updatedValue);
    }
  }

  public TreeNode expand(final TreeNode node,
                         final StateObservation currentState) {
    final List<Types.ACTIONS> exploredActions = node.children.stream().map(entry -> entry.previousAction).collect(Collectors.toList());
    final List<Types.ACTIONS> unexploredActions = new ArrayList<>(currentState.getAvailableActions());

    logMessage(String.format("Node %s has %s actions possible from initial state", node.id, unexploredActions.size()));

    unexploredActions.removeAll(exploredActions);

    final Types.ACTIONS selectedAction = unexploredActions.get(0);
    logMessage(String.format("Selected action %s to expand node %s", selectedAction.toString(), node.id));
    TreeNode tempNode = new TreeNode(node, selectedAction);
    node.children.add(tempNode);
    return tempNode;
  }

  private boolean isNodeFullyExpanded(final TreeNode node,
                                      final ArrayList<Types.ACTIONS> nodeAvailableActions) {
    if (nodeAvailableActions.isEmpty()) {
      return true;
    }
    return node.children.size() == nodeAvailableActions.size();
  }

  private TreeNode getBestChild(final TreeNode node) {

    System.out.println("Selecting best child of " + node.id);
    for (TreeNode n : node.children) {
      System.out.println(String.format("Node %d UCB %.2f ", n.id, getNodeUpperConfidenceBound(n, node.visits)));
    }


    final TreeNode maxUcbNode = Collections.max(node.children, Comparator.comparing(c -> getNodeUpperConfidenceBound(c, node.visits)));
    //System.out.println(String.format("Max UCB: %f.2 for action %s in node %s", maxUcbNode.getNodeUpperConfidenceBound(getTimesVisited()),
    //    maxUcbNode.getActionToGetToNode().toString(), maxUcbNode.getNodeId()));
    return maxUcbNode;
  }

  public double getNodeUpperConfidenceBound(final TreeNode node,
                                            final int parentVisits) {

    if (node.visits == 0) {
      return Double.MAX_VALUE;
    }
    return getChildrenAverageReward(node) + C * Math.sqrt((2 * (Math.log(parentVisits)) / node.visits));
  }

  public double getChildrenAverageReward(final TreeNode node) {

    if (node.children.isEmpty()) {
      return 0;
    }

    double averageSum = 0;
    for (TreeNode nodeId : node.children) {
      averageSum += nodeId.value;
    }
    return averageSum / node.children.size();
  }

  private StateObservation advanceStateToNodeState(final StateObservation initialState,
                                                   final List<Types.ACTIONS> actionsToGetToNode) {
    actionsToGetToNode.forEach(initialState::advance);
    return initialState;
  }

  public TreeNode getBestChild() {
    return getBestChild(rootNode);
  }


  public Types.ACTIONS getBestFoundAction() {
    final TreeNode bestNode = getBestChild(rootNode);
    return bestNode.previousAction;
  }

  void resetNodeCount() {
    if (rootNode != null) {
      rootNode.resetNodeCount();
    }
  }
}
