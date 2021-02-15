package tracks.singlePlayer.florabranchi.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.logging.Logger;

import core.game.StateObservation;
import javafx.util.Pair;
import ontology.Types;

public class TreeController {

  private final static Logger logger = Logger.getLogger(TreeController.class.getName());

  private final static double C = 1 / Math.sqrt(2);

  private final TreeHelper helper;

  public TreeNode rootNode;

  private Random rand = new Random();

  private TreeViewer treeViewer;

  private boolean showTree;

  public int ROLLOUT_LOOK_AHEADS;

  public TreeController(StateObservation initialState,
                        boolean showTree,
                        int rolloutLookAheads) {

    this.showTree = showTree;
    if (showTree) {
      treeViewer = new TreeViewer(initialState);
    }
    ROLLOUT_LOOK_AHEADS = rolloutLookAheads;
    helper = new TreeHelper(initialState.getAvailableActions());

  }

  public void updateTreeVisualization(final StateObservation stateObs,
                                      final int iteration,
                                      final Types.ACTIONS selectedAction) {
    if (showTree) {
      treeViewer.updateTreeObjects(1, stateObs.getGameTick(), iteration, rootNode, stateObs, selectedAction);
    }
  }

  private void logMessage(final String message) {
    //logger.log(Level.INFO, message);
  }

  public void treeSearch(final int iterations,
                         final StateObservation initialState) {

/*    if (rootNode == null) {
      rootNode = new TreeNode(0, null, null);
    }*/

    rootNode = new TreeNode(0, null, null);

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
    }

    if (showTree) {
      updateTreeVisualization(initialState, 0, null);
    }
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
    // todo fix ids

    rootNode = rootNode.children.stream().filter(child -> child.previousAction == selectedAction).findFirst()
        .orElse(null);
    rootNode.parent = null;

  }

  public double rollout(final StateObservation initialState) {
    double initialScore = initialState.getGameScore();
    StateObservation copyState = initialState.copy();

    // contar jogadas e cortar antes do fim
    int advancementsInRollout = ROLLOUT_LOOK_AHEADS;

    while (!copyState.isGameOver() && advancementsInRollout > 0) {
      final ArrayList<Types.ACTIONS> availableActions = copyState.getAvailableActions();
      if (availableActions.size() < 1) {
        advancementsInRollout = 0;
      } else {
        final double currentScore = copyState.getGameScore();
        final Types.ACTIONS takeAction = availableActions.get(rand.nextInt(availableActions.size() - 1));
        copyState.advance(takeAction);
        advancementsInRollout--;
      }

    }

    double finalScore = copyState.getGameScore();
    double scoreDelta = finalScore - initialScore;

    int maxScore = 3;
    scoreDelta = scoreDelta / maxScore;

    final Types.WINNER gameWinner = copyState.getGameWinner();

    if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
      //scoreDelta = 1;
      scoreDelta = 1;
    } else if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
      scoreDelta = -1;
    }

    return scoreDelta;
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
      tempNode = new TreeNode(helper.getNodeId(node.id, action), node, action);
      node.children.add(tempNode);
    }
  }

  public TreeNode getMostVisitedChild(final TreeNode node) {
    return Collections.max(node.children, Comparator.comparing(c -> c.visits));
  }

  public TreeNode getChildWithHighestScore(final TreeNode node) {
    return Collections.max(node.children, Comparator.comparing(c -> c.value));
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

}
