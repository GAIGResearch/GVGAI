package tracks.singlePlayer.florabranchi.mtcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import core.game.StateObservation;
import javafx.util.Pair;
import ontology.Types;

public class TreeController {

  private TreeNode rootNode;

  private Logger logger = Logger.getLogger(TreeController.class.getName());

  private double C = 1;

  private Random rand = new Random();

  private void logMessage(final String message) {
    logger.log(Level.INFO, message);
  }

  public void buildTree(int iterations,
                        final StateObservation initialState) {

    rootNode = new TreeNode(0, null, null);

    for (int i = 0; i < iterations; i++) {
      final Pair<TreeNode, StateObservation> policyResult = executeTreePolicy(initialState);
      final TreeNode selectedNode = policyResult.getKey();
      logMessage(String.format("Rollouting selected node %s", selectedNode.id));
      rollout(selectedNode, initialState);
    }
  }

  private Pair<TreeNode, StateObservation> executeTreePolicy(final StateObservation stateObservation) {

    TreeNode selectedNode = rootNode;
    final List<Types.ACTIONS> actionsToGetToNode = new ArrayList<>();
    StateObservation stateAfterNodeAction = stateObservation.copy();

    do {
      if (!isNodeFullyExpanded(selectedNode, stateAfterNodeAction.getAvailableActions())) {
        logMessage(String.format("Expanding children of node %s", selectedNode.id));
        stateAfterNodeAction = advanceStateToNodeState(stateObservation, actionsToGetToNode);
        final TreeNode nodeAfterExpansion = expand(selectedNode, stateAfterNodeAction);
        return new Pair<>(nodeAfterExpansion, stateAfterNodeAction);
      } else {
        selectedNode = getBestChild(selectedNode);
        actionsToGetToNode.add(selectedNode.previousAction);
        logMessage(String.format("Selected children %s", selectedNode.id));
      }
    } while (!isNodeFullyExpanded(selectedNode, stateAfterNodeAction.getAvailableActions()));
    return new Pair<>(selectedNode, stateAfterNodeAction);
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
    double stateReward = 0.01;
    double scoreDelta = initialScore - finalScore;
    stateReward += 0.01 * scoreDelta;

    final Types.WINNER gameWinner = copyState.getGameWinner();

    if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
      stateReward += 1;
    } else if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
      stateReward = 0;
    }

    updateTree(selectedNode, stateReward);
  }

  public void updateTree(final TreeNode selectedNode,
                         final double updatedValue) {
    logMessage(String.format("Node %s has been visited %d times", selectedNode.id, selectedNode.visits));
    selectedNode.visits++;
    selectedNode.value = selectedNode.value + updatedValue;
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
    Collections.shuffle(unexploredActions);

    final Types.ACTIONS selectedAction = unexploredActions.get(0);
    StateObservation newNodeState = currentState.copy();
    newNodeState.advance(selectedAction);

    logMessage(String.format("Selected action %s to expand node %s", selectedAction.toString(), node.id));
    TreeNode tempNode = new TreeNode(0, node, selectedAction);
    node.children.add(tempNode);
    return tempNode;
  }

  private boolean isNodeFullyExpanded(final TreeNode node,
                                      final ArrayList<Types.ACTIONS> nodeAvailableActions) {
    return node.children.size() == nodeAvailableActions.size();
  }

  private TreeNode getBestChild(TreeNode node) {

    //for (MCTNode n : childrenNodes) {
    //System.out.println(String.format("Node %d UCB %s ", n.getNodeId(), n.getNodeUpperConfidenceBound(getTimesVisited())));
    //}


    final TreeNode maxUcbNode = Collections.max(node.children, Comparator.comparing(c -> getNodeUpperConfidenceBound(c, node.visits)));
    //System.out.println(String.format("Max UCB: %f.2 for action %s in node %s", maxUcbNode.getNodeUpperConfidenceBound(getTimesVisited()),
    //    maxUcbNode.getActionToGetToNode().toString(), maxUcbNode.getNodeId()));
    return maxUcbNode;
  }

  public double getNodeUpperConfidenceBound(final TreeNode node,
                                            int parentVisits) {

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


  public Types.ACTIONS getBestFoundAction() {
    final TreeNode bestNode = getBestChild(rootNode);
    return bestNode.previousAction;
  }
}
