package tracks.singlePlayer.florabranchi.agents;

import static tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper.getAverageDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

import core.game.StateObservation;
import javafx.util.Pair;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper;
import tracks.singlePlayer.florabranchi.tree.Node;
import tracks.singlePlayer.florabranchi.tree.TreeHelper;

/**
 * Flora Branchi (florabranchi@gmail.com) September 2019
 */
public class ParametrizedMonteCarloTreeAgent extends AbstractAgent {

  /**
   * Random generator for the agent.
   */
  protected Random randomGenerator;

  /**
   * List of available actions for the agent
   */
  protected ArrayList<ACTIONS> actions;

  protected boolean showTree;

  public int TREE_SEARCH_SIZE;
  public int SIMULATION_DEPTH;

  // Enhancements
  public boolean TREE_REUSE;
  public boolean LOSS_AVOIDANCE;

  //UCB1
  //private final static double C = 1 / Math.sqrt(2);
  private final static double C = Math.sqrt(2);

  private final TreeHelper helper;

  private final int maxDistance;

  private ACTIONS lastAction = null;

  public Node rootNode;

  private Random rand = new Random();

  private static final double WINNER_SCORE = Double.MAX_VALUE;
  private static final double LOSS_SCORE = Double.MIN_VALUE;

  private final int[][] visitCount;

  /**
   * ' initialize all variables for the agent
   *
   * @param stateObs     Observation of the current state.
   * @param elapsedTimer Timer when the action returned is due.
   */
  public ParametrizedMonteCarloTreeAgent(final StateObservation stateObs,
                                         final ElapsedCpuTimer elapsedTimer) {
    super(stateObs, elapsedTimer);
    showTree = propertyLoader.SHOW_TREE;

    TREE_SEARCH_SIZE = propertyLoader.TREE_SEARCH_SIZE;
    SIMULATION_DEPTH = propertyLoader.SIMULATION_DEPTH;
    TREE_REUSE = propertyLoader.TREE_REUSE;
    LOSS_AVOIDANCE = propertyLoader.LOSS_AVOIDANCE;

    randomGenerator = new Random();
    actions = stateObs.getAvailableActions();

    helper = new TreeHelper(stateObs.getAvailableActions());

    final int height = StateEvaluatorHelper.getHeight(stateObs);
    final int width = StateEvaluatorHelper.getWidth(stateObs);
    visitCount = new int[width][height];

    for (int i = 0; i < width - 1; i++) {
      for (int j = 0; j < height - 1; j++) {
        visitCount[i][j] = 0;
      }
    }

    maxDistance = height * width;
  }

  @Override
  protected String getPropertyPath() {
    return "pmcts.visuals.properties";
  }


  /**
   * return Action given selected policy.
   */
  @Override
  public ACTIONS act(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedTimer) {

    System.out.println("SCORE: " + stateObs.getGameScore());
    final ACTIONS selectedAction = monteCarloSearchParametrized(stateObs, elapsedTimer, lastAction);
    lastAction = selectedAction;
    return selectedAction;
  }

  public ACTIONS monteCarloSearchParametrized(final StateObservation stateObs,
                                              final ElapsedCpuTimer elapsedTimer,
                                              final ACTIONS previousAction) {

    // todo time limit

    if (TREE_REUSE && previousAction != null) {
      final Optional<Node> newRoot = rootNode.children.stream()
          .filter(child -> child.previousAction.equals(previousAction)).findFirst();
      rootNode = newRoot.orElseGet(() -> new Node(null, null));
    } else {
      rootNode = new Node(null, null);
    }

    int iterations = TREE_SEARCH_SIZE;

    for (int i = 0; i < iterations; i++) {

      Pair<Node, StateObservation> selectedNodeInfo = parametrizedSelection(stateObs);

      StateObservation mostPromisingNodeState = selectedNodeInfo.getValue();
      Node selectedNode = selectedNodeInfo.getKey();


      double simulationReward;
      // Expansion - Expand node if not terminal
      if (!mostPromisingNodeState.isGameOver()) {
        expandAllChildren(selectedNode, mostPromisingNodeState.getAvailableActions());
        // Simulation with random children
        selectedNode = selectedNode.children.get(rand.nextInt(selectedNode.children.size()));

        simulationReward = rollout(mostPromisingNodeState);
      } else {

        // Selected node is game over

        simulationReward = LOSS_SCORE;
      }

      // Backpropagation
      backup(selectedNode, simulationReward);
    }

    final Node bestChild = getChildWithHighestScore(rootNode);
    return bestChild.previousAction;
  }

  public Pair<Node, StateObservation> parametrizedSelection(final StateObservation initialState) {

    Node selectedNode = rootNode;
    StateObservation newState = initialState.copy();

    // find left node
    // currently we add multiple children so this while works
    while (!selectedNode.children.isEmpty()) {

      // Get node with higher UCB
      selectedNode = getBestChild(selectedNode);
      newState.advance(selectedNode.previousAction);
    }
    return new Pair<>(selectedNode, newState);
  }

  public void parametrizedExpansion() {


  }

  private void logMessage(final String message) {
    //logger.log(Level.INFO, message);
  }


  public Pair<Node, StateObservation> selection(final StateObservation initialState) {
    Node selectedNode = rootNode;
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

  public void backup(final Node selectedNode,
                     final double updatedValue) {

    selectedNode.visits++;
    selectedNode.value = selectedNode.value + updatedValue;
    logMessage(String.format("Node %s has been visited %d times", selectedNode.id, selectedNode.visits));
    logMessage(String.format("Node %s value %s", selectedNode.id, selectedNode.value / selectedNode.visits));
    if (selectedNode.parent != null) {
      backup(selectedNode.parent, updatedValue);
    }
  }

  public void expandAllChildren(final Node node,
                                final ArrayList<Types.ACTIONS> actions) {
    Node tempNode = node;
    logMessage(String.format("Expanding children of node %s", node.id));
    for (Types.ACTIONS action : actions) {
      tempNode = new Node(node, action);
      node.children.add(tempNode);
    }
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
    int advancementsInRollout = SIMULATION_DEPTH;

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

  public Node getMostVisitedChild(final Node node) {

    if (allValuesEqual(node)) {
      return node.children.get(rand.nextInt(node.children.size()));
    }

    return Collections.max(node.children, Comparator.comparing(c -> c.visits));
  }

  public Node getChildWithHighestScore(final Node node) {

    if (allValuesEqual(node)) {
      return node.children.get(rand.nextInt(node.children.size()));
    }

    return Collections.max(node.children, Comparator.comparing(c -> c.value));
  }

  public boolean allValuesEqual(final Node node) {

    // outros casos de empate
    // retornar lista de nodos com valor parecido
    // n1 .4 n2 .4 n3 .3
    // lista com valores maximos iguais


    Double value = null;
    for (Node child : node.children) {
      if (value != null && child.value != value) {
        return false;
      } else if (value == null) {
        value = child.value;
      }
    }
    //System.out.print("All values equal \n");
    return true;
  }

  public Node getBestChild(final Node node) {

    if (allValuesEqual(node)) {
      return node.children.get(rand.nextInt(node.children.size()));
    }

    return Collections.max(node.children, Comparator.comparing(c -> getNodeUpperConfidenceBound(c, node.visits)));
  }

  public double getNodeUpperConfidenceBound(final Node node,
                                            final int parentVisits) {

    if (node.visits == 0) {
      return Double.MAX_VALUE;
    }
    return (node.value / node.visits) + 2 * C * Math.sqrt((2 * (Math.log(parentVisits)) / node.visits));
  }
}
