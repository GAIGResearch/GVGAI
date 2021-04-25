package tracks.singlePlayer.florabranchi.tree;

import static tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper.getAverageDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.logging.Logger;

import core.game.StateObservation;
import javafx.util.Pair;
import ontology.Types;
import tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper;

public class TreeController {

  private final static Logger logger = Logger.getLogger(TreeController.class.getName());

  //UCB1
  //private final static double C = 1 / Math.sqrt(2);
  private final static double C = Math.sqrt(2);

  private final TreeHelper helper;
  private final int height;
  private final int width;
  private final int maxDistance;

  public TreeNode rootNode;

  private Random rand = new Random();

 // private TreeViewer treeViewer;

  private boolean showTree;

  public int ROLLOUT_LOOK_AHEADS;
  private static final double WINNER_SCORE = Double.MAX_VALUE;
  private static final double LOSS_SCORE = Double.MIN_VALUE;

  private final int[][] visitCount;

  public TreeController(StateObservation initialState,
                        boolean showTree,
                        int rolloutLookAheads) {

    this.showTree = showTree;
    if (showTree) {
      //treeViewer = new TreeViewer(initialState);
    }
    ROLLOUT_LOOK_AHEADS = rolloutLookAheads;
    helper = new TreeHelper(initialState.getAvailableActions());

    height = StateEvaluatorHelper.getHeight(initialState);
    width = StateEvaluatorHelper.getWidth(initialState);
    visitCount = new int[width][height];

    for (int i = 0; i < width - 1; i++) {
      for (int j = 0; j < height - 1; j++) {
        visitCount[i][j] = 0;
      }
    }

    maxDistance = height * width;

  }

  public void updateTreeVisualization(final StateObservation stateObs,
                                      final int iteration,
                                      final Types.ACTIONS selectedAction) {
    if (showTree) {
     // treeViewer.updateTreeObjects(1, stateObs.getGameTick(), iteration, rootNode, stateObs, selectedAction);
    }
  }

  private void logMessage(final String message) {
    //logger.log(Level.INFO, message);
  }

  public void treeSearch(final int iterations,
                         final StateObservation initialState) {

    rootNode = new TreeNode(0, null, null);

    treePolicy(iterations, initialState);

    if (showTree) {
      updateTreeVisualization(initialState, 0, null);
    }
  }

  public void jenJerry(final StateObservation initialState) {
    rootNode = new TreeNode(0, null, null);

    int children = initialState.getAvailableActions().size();

    expand(rootNode, initialState.getAvailableActions());

    for (int i = 0; i < children; i++) {
      // Simulation with random children
      TreeNode selectedNode = rootNode.children.get(i);
      StateObservation mostPromisingNodeState = initialState.copy();
      mostPromisingNodeState.advance(selectedNode.previousAction);
      final double simulationReward = rollout(mostPromisingNodeState);
      backup(selectedNode, simulationReward);
    }

  }

  public void treePolicy(final int iterations,
                         final StateObservation initialState) {

    for (int i = 0; i < iterations; i++) {

      //System.out.println("ITERATION " + i);

      // Selection - Get most promising node
      final Pair<TreeNode, StateObservation> mostPromisingNodePair = selection(initialState);

      TreeNode mostPromisingNode = mostPromisingNodePair.getKey();
      StateObservation mostPromisingNodeState = mostPromisingNodePair.getValue();

      TreeNode selectedNode = mostPromisingNode;

      double simulationReward;

      // Expansion - Expand node if not terminal
      if (!mostPromisingNodeState.isGameOver()) {
        expand(mostPromisingNode, mostPromisingNodeState.getAvailableActions());

        // Simulation with random children
        selectedNode = mostPromisingNode.children.get(rand.nextInt(mostPromisingNode.children.size()));
        mostPromisingNodeState.advance(selectedNode.previousAction);
        logMessage(String.format("Rollouting selected node %s", selectedNode.id));
        simulationReward = rollout(mostPromisingNodeState);
        logMessage(String.format("Simulation Reward: %s", simulationReward));

      } else {
        simulationReward = LOSS_SCORE;
      }

      // Backpropagation
      backup(mostPromisingNode, simulationReward);
    }

    if (showTree) {
      updateTreeVisualization(initialState, 0, null);
    }
  }


  public Pair<TreeNode, StateObservation> selection(final StateObservation initialState) {
    TreeNode selectedNode = rootNode;
    StateObservation newState = initialState.copy();

    // Go through tree until leaf node is found
    while (!selectedNode.children.isEmpty()) {
      // Get node with higher UCB
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

  public double getGameScore(final StateObservation copyState, final
  double initialScore) {

    final Types.WINNER gameWinner = copyState.getGameWinner();

    if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
      return WINNER_SCORE;
    } else if (copyState.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
      return LOSS_SCORE;
    }

    double finalScore = copyState.getGameScore();
    return finalScore - initialScore;
  }

  public double getStateScore(final StateObservation copyState, final
  double initialScore) {

    final Types.WINNER gameWinner = copyState.getGameWinner();

    if (gameWinner.equals(Types.WINNER.PLAYER_WINS)) {
      return WINNER_SCORE;
    } else if (gameWinner.equals(Types.WINNER.PLAYER_LOSES)) {
      return LOSS_SCORE;
    }

    final int avatarX = (int) copyState.getAvatarPosition().x;
    final int avatarY = (int) copyState.getAvatarPosition().y;

    double totalScore = 0;

    final StateEvaluatorHelper.ObservableData npcData = StateEvaluatorHelper.getNpcData(copyState);
    final StateEvaluatorHelper.ObservableData portalsData = StateEvaluatorHelper.getPortalsData(copyState);
    final StateEvaluatorHelper.ObservableData movablesData = StateEvaluatorHelper.getMovablesData(copyState);
    final StateEvaluatorHelper.ObservableData resourcesData = StateEvaluatorHelper.getResourcesData(copyState);

    // todo get distance to closest enemy
    final Double avgNpcDist = getAverageDistance(npcData);

    final double distClosestResource = distanceToClosestObservable(avatarX, avatarY, resourcesData);
    final double distClosestPortal = distanceToClosestObservable(avatarX, avatarY, portalsData);
    final double distClosestMovable = distanceToClosestObservable(avatarX, avatarY, movablesData);
    final double distClosestNpc = distanceToClosestObservable(avatarX, avatarY, npcData);


    visitCount[avatarX][avatarY] = visitCount[avatarX][avatarY] + 1;

    double finalScore = copyState.getGameScore();
    double scoreDelta = finalScore - initialScore;

    double exporationScore = ((double) 1 - visitCount[avatarX][avatarY]) / maxDistance;
    double resourceScore = distClosestResource == 0 ? 0 : (1 - distClosestResource) / maxDistance;
    //double resourceScore = (1 - distClosestResource) / maxDistance;
    double movableScore = distClosestMovable / maxDistance;

    // should npc score be removed? quando é inimigo, atrapalha

    double npcScore = distClosestNpc / maxDistance;
    double portalScore = distClosestPortal == 0 ? 0 : (1 - distClosestPortal) / maxDistance;
    //double portalScore = (1 - distClosestPortal) / maxDistance;

    final int resources = copyState.getAvatarResources().size();

    final double score =
        1 * scoreDelta
            + 1 * resources
            + (2 * resourceScore)
            + (5 * exporationScore)
            + (1 * movableScore)
            + (2 * portalScore);

    return score;
  }

  public double distanceToClosestObservable(final int avatarX,
                                            final int avatarY,
                                            final StateEvaluatorHelper.ObservableData closestObservable) {

    if (closestObservable.closerObject == null) {
      return 0;

    }
    return calculateDistanceBetweenPoints(closestObservable.closerObject.position.x,
        closestObservable.closerObject.position.y,
        avatarX,
        avatarY);
  }

  public double calculateDistanceBetweenPoints(
      double x1,
      double y1,
      double x2,
      double y2) {
    return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
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
        final Types.ACTIONS takeAction = availableActions.get(rand.nextInt(availableActions.size()));
        copyState.advance(takeAction);
        advancementsInRollout--;
      }

    }


    //return getGameScore(copyState, initialScore);
    return getStateScore(copyState, initialScore);
  }

  public void backup(final TreeNode selectedNode,
                     final double updatedValue) {

    selectedNode.visits++;
    selectedNode.value = selectedNode.value + updatedValue;
    logMessage(String.format("Node %s has been visited %d times", selectedNode.id, selectedNode.visits));
    logMessage(String.format("Node %s value %s", selectedNode.id, selectedNode.value / selectedNode.visits));
    if (selectedNode.parent != null) {
      backup(selectedNode.parent, updatedValue);
    }
  }

  public void expand(final TreeNode node,
                     final ArrayList<Types.ACTIONS> actions) {
    TreeNode tempNode = node;
    logMessage(String.format("Expanding children of node %s", node.id));
    for (Types.ACTIONS action : actions) {
      tempNode = new TreeNode(helper.getNodeId(node.id, action), node, action);
      node.children.add(tempNode);
    }
  }

  public TreeNode getMostVisitedChild(final TreeNode node) {

    if (allValuesEqual(node)) {
      return node.children.get(rand.nextInt(node.children.size()));
    }

    return Collections.max(node.children, Comparator.comparing(c -> c.visits));
  }

  public TreeNode getChildWithHighestScore(final TreeNode node) {

    if (allValuesEqual(node)) {
      return node.children.get(rand.nextInt(node.children.size()));
    }

    return Collections.max(node.children, Comparator.comparing(c -> c.value));
  }

  public boolean allValuesEqual(final TreeNode node) {

    // outros casos de empate
    // retornar lista de nodos com valor parecido
    // n1 .4 n2 .4 n3 .3
    // lista com valores maximos iguais


    Double value = null;
    for (TreeNode child : node.children) {
      if (value != null && child.value != value) {
        return false;
      } else if (value == null) {
        value = child.value;
      }
    }
    //System.out.print("All values equal \n");
    return true;
  }

  public TreeNode getBestChild(final TreeNode node) {

    if (allValuesEqual(node)) {
      return node.children.get(rand.nextInt(node.children.size()));
    }

    return Collections.max(node.children, Comparator.comparing(c -> getNodeUpperConfidenceBound(c, node.visits)));
  }

  public double getNodeUpperConfidenceBound(final TreeNode node,
                                            final int parentVisits) {

    if (node.visits == 0) {
      return Double.MAX_VALUE;
    }
    return (node.value / node.visits) + 2 * C * Math.sqrt((2 * (Math.log(parentVisits)) / node.visits));
  }

}
