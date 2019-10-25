package tracks.singlePlayer.florabranchi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.logging.Logger;

import core.game.StateObservation;
import javafx.util.Pair;
import ontology.Types;
import tracks.singlePlayer.florabranchi.models.TreeNode;

public class TreeController {

  private final static Logger logger = Logger.getLogger(TreeController.class.getName());

  private final static double C = 1 / Math.sqrt(2);

  public TreeNode rootNode;

  private Random rand = new Random();

  private TreeViewer treeViewer = new TreeViewer();

  public TreeController(StateObservation initialState) {

  }

  public void updateTreeVisualization(final StateObservation stateObs,
                                      final int i) {
    treeViewer.updateNodes(stateObs.getGameTick(), i, rootNode, stateObs);
  }

  private void logMessage(final String message) {
    //logger.log(Level.INFO, message);
  }

  public void treeSearch(final int iterations,
                         final StateObservation initialState) {

    if (rootNode == null) {
      rootNode = new TreeNode(null, null);
    }

    for (int i = 0; i < iterations; i++) {

      //System.out.println("ITERATION " + i);

      // Selection - Get most promising node
      final Pair<TreeNode, StateObservation> mostPromisingNodePair = getMostPromisingLeafNode(initialState);
      TreeNode mostPromisingNode = mostPromisingNodePair.getKey();
      StateObservation mostPromisingNodeState = mostPromisingNodePair.getValue();

      TreeNode selectedNode = mostPromisingNode;
      double simulationReward;

      // Expansion - Expand node if not terminal
      if (!mostPromisingNodeState.isGameOver()) {
        expand(mostPromisingNode, mostPromisingNodeState);

        // Simulation with random children
        selectedNode = mostPromisingNode.children.get(rand.nextInt(mostPromisingNode.children.size() - 1));
        mostPromisingNodeState.advance(selectedNode.previousAction);
        logMessage(String.format("Rollouting selected node %s", selectedNode.id));
        simulationReward = rollout(mostPromisingNodeState);
      } else {
        simulationReward = 0;
      }

      // Backpropagation
      updateTree(selectedNode, simulationReward);
      updateTreeVisualization(initialState, i);
    }

    updateTreeVisualization(initialState, 0);

  }

  public Pair<TreeNode, StateObservation> getMostPromisingLeafNode(final StateObservation initialState) {
    TreeNode selectedNode = rootNode;
    StateObservation newState = initialState.copy();
    while (!selectedNode.children.isEmpty()) {
      selectedNode = getBestChild(selectedNode);
      newState.advance(selectedNode.previousAction);
    }
    return new Pair<>(selectedNode, newState);
  }

  public void pruneTree(final Types.ACTIONS selectedAction) {
    rootNode = rootNode.children.stream().filter(child -> child.previousAction == selectedAction).findFirst()
        .orElse(null);
    rootNode.parent = null;

  }

  public double rollout(final StateObservation initialState) {
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

    return stateReward;
  }

  public void updateTree(final TreeNode selectedNode,
                         final double updatedValue) {

    selectedNode.visits++;
    selectedNode.value = selectedNode.value + updatedValue;
    logMessage(String.format("Node %s has been visited %d times", selectedNode.id, selectedNode.visits));
    logMessage(String.format("Node %s value %s", selectedNode.id, selectedNode.value / selectedNode.visits));
    if (selectedNode.parent != null) {
      updateTree(selectedNode.parent, updatedValue);
    }
  }

  public void expand(final TreeNode node,
                     final StateObservation currentState) {
    TreeNode tempNode = node;
    for (Types.ACTIONS action : currentState.getAvailableActions()) {
      logMessage(String.format("Selected action %s to expand node %s", action.toString(), node.id));
      tempNode = new TreeNode(node, action);
      node.children.add(tempNode);
    }
  }

  private boolean isNodeFullyExpanded(final TreeNode node,
                                      final ArrayList<Types.ACTIONS> nodeAvailableActions) {
    if (nodeAvailableActions.isEmpty()) {
      return true;
    }
    return node.children.size() == nodeAvailableActions.size();
  }

  public TreeNode getMostVisitedChild(final TreeNode node) {
    return Collections.max(node.children, Comparator.comparing(c -> c.visits));
  }

  public TreeNode getBestChild(final TreeNode node) {
    return Collections.max(node.children, Comparator.comparing(c -> getNodeUpperConfidenceBound(c, node.visits)));
  }

  public double getNodeUpperConfidenceBound(final TreeNode node,
                                            final int parentVisits) {

    if (node.visits == 0) {
      return Double.MAX_VALUE;
    }
    return node.value / node.visits + C * Math.sqrt((2 * (Math.log(parentVisits)) / node.visits));
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
