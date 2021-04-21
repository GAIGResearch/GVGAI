package tracks.singlePlayer.florabranchi.agents;

import static tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper.getAverageDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper;
import tracks.singlePlayer.florabranchi.tree.Node;
import tracks.singlePlayer.florabranchi.tree.TreeController;
import tracks.singlePlayer.florabranchi.tree.TreeHelper;

/**
 * Flora Branchi (florabranchi@gmail.com) September 2019
 */
public class ParametrizedMonteCarloTreeAgent extends AbstractAgent {

  private final static Logger logger = Logger.getLogger(TreeController.class.getName());

  /**
   * Random generator for the agent.
   */
  protected Random randomGenerator;

  /**
   * List of available actions for the agent
   */
  protected ArrayList<ACTIONS> actions;

  protected boolean showTree;

  // flora(todo) add time
  public int TREE_SEARCH_SIZE;
  public int SIMULATION_DEPTH;

  // Enhancements
  public boolean TREE_REUSE;
  public boolean LOSS_AVOIDANCE;

  public boolean EXPAND_ALL_CHILD_NODES;
  public boolean SAFETY_PREPRUNNING;

  public boolean RAW_GAME_SCORE;

  // Heuristic Weights
  public int RAW_SCORE_WEIGHT;
  public int TOTAL_RESOURCES_SCORE_WEIGHT;
  public int RESOURCE_SCORE_WEIGHT;
  public int EXPLORATION_SCORE_WEIGHT;
  public int MOVABLES_SCORE_WEIGHT;
  public int PORTALS_SCORE_WEIGHT;

  // todo
  // breadth first initialization
  // macroactions
  // reversal penalty


  //UCB1
  //private final static double C = 1 / Math.sqrt(2);
  private final static double C = Math.sqrt(2);

  private final TreeHelper helper;

  private final int maxDistance;

  private ACTIONS lastAction = null;

  public Node rootNode;

  private Random rand = new Random();

  private static final double WINNER_SCORE = Math.pow(10, 6);
  private static final double LOSS_SCORE = Math.pow(10, -6);

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
    RAW_GAME_SCORE = propertyLoader.RAW_GAME_SCORE;
    EXPAND_ALL_CHILD_NODES = propertyLoader.EXPAND_ALL_CHILD_NODES;
    SAFETY_PREPRUNNING = propertyLoader.SAFETY_PREPRUNNING;

    // weights
    RAW_SCORE_WEIGHT = propertyLoader.RAW_SCORE_WEIGHT;
    TOTAL_RESOURCES_SCORE_WEIGHT = propertyLoader.TOTAL_RESOURCES_SCORE_WEIGHT;
    RESOURCE_SCORE_WEIGHT = propertyLoader.RESOURCE_SCORE_WEIGHT;
    EXPLORATION_SCORE_WEIGHT = propertyLoader.EXPLORATION_SCORE_WEIGHT;
    MOVABLES_SCORE_WEIGHT = propertyLoader.MOVABLES_SCORE_WEIGHT;
    PORTALS_SCORE_WEIGHT = propertyLoader.PORTALS_SCORE_WEIGHT;

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

      if (newRoot.isPresent()) {
        rootNode = newRoot.get();
        rootNode.parent = null;

        // flora(todo) how to prune state if different from expected?
        // Build new root is state is different
        if (!stateObs.equals(rootNode.currentGameState)) {
          newRoot.orElseGet(() -> new Node(null, null, stateObs));
        }

      } else {
        rootNode = newRoot.orElseGet(() -> new Node(null, null, stateObs));
      }

    } else {
      rootNode = new Node(null, null, stateObs);
    }

    int iterations = TREE_SEARCH_SIZE;
    boolean skipTreeUpdate = false;
    double initialScore = rootNode.currentGameState.getGameScore();
    for (int i = 0; i < iterations; i++) {

      Node selectedNode = parametrizedSelection();

      //Pair<Double, Node> rolloutResults;
      double simulationReward = 0;

      // Expansion - Expand node if not terminal
      if (!selectedNode.currentGameState.isGameOver()) {

        expansion(selectedNode, selectedNode.currentGameState.getAvailableActions());

        // Rollout random children or self if no children

        if (!selectedNode.children.isEmpty()) {
          selectedNode = selectedNode.children.get(rand.nextInt(selectedNode.children.size()));
        }

        logMessage(String.format("Rollouting selected node %s", selectedNode.id));
        simulationReward = rollout(selectedNode, initialScore);

        if (simulationReward == LOSS_SCORE) {
          skipTreeUpdate = true;
        }

        logMessage(String.format("Simulation Reward: %s", simulationReward));

      } else {

        StateObservation currentGameState = selectedNode.currentGameState;

        // Selected node is game over
        if (currentGameState.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
          simulationReward = WINNER_SCORE;
        } else if (currentGameState.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
          simulationReward = LOSS_SCORE;

          if (SAFETY_PREPRUNNING) {
            skipTreeUpdate = true;
          }
        }
      }
      if (!skipTreeUpdate) {
        // Backpropagation

        // todo(flora) add discount factor
        backup(selectedNode, simulationReward);
      }
    }

    final Node bestChild = getChildWithHighestScore(rootNode);
    return bestChild.previousAction;
  }

  public Node parametrizedSelection() {

    Node selectedNode = rootNode;

    if (EXPAND_ALL_CHILD_NODES) {
      while (!selectedNode.children.isEmpty()) {
        selectedNode = getBestChild(selectedNode);
      }
    } else {

      while (!isExpandable(selectedNode)) {

        if (selectedNode.children.isEmpty()) {
          logMessage(String.format("Selected node %s with no children", selectedNode.id));
          return selectedNode;
        }

        // fix - best child = unvisited action that is not here
        // Get node with higher UCB
        selectedNode = getBestChild(selectedNode);
      }
    }

    logMessage(String.format("Selected node %s", selectedNode.id));
    return selectedNode;
  }

  private void logMessage(final String message) {
    //logger.log(Level.INFO, message);
  }

  private boolean isExpandable(final Node node) {
    return node.children.size() < node.currentGameState.getAvailableActions().size();
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

  public void expansion(final Node parentNode,
                        final ArrayList<Types.ACTIONS> actions) {
    logMessage(String.format("Expanding children of node %s", parentNode.id));

    if (EXPAND_ALL_CHILD_NODES) {
      for (Types.ACTIONS action : actions) {
        parentNode.children.add(buildChildNode(parentNode, action));
      }
    } else {
      final ACTIONS selectedAction = actions.get(rand.nextInt(actions.size()));
      parentNode.children.add(buildChildNode(parentNode, selectedAction));
    }
  }

  public Node buildChildNode(final Node parent,
                             final Types.ACTIONS action) {
    final StateObservation newNodeState = parent.currentGameState.copy();
    newNodeState.advance(action);
    return new Node(parent, action, newNodeState);
  }


  public double getRawGameStateScore(final StateObservation copyState,
                                     final double initialScore) {

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
        RAW_SCORE_WEIGHT * scoreDelta
            + TOTAL_RESOURCES_SCORE_WEIGHT * resources
            + (RESOURCE_SCORE_WEIGHT * resourceScore)
            + (EXPLORATION_SCORE_WEIGHT * exporationScore)
            + (MOVABLES_SCORE_WEIGHT * movableScore)
            + (PORTALS_SCORE_WEIGHT * portalScore);

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

  public Double rollout(final Node node,
                        final double initialScore) {

    final StateObservation currentState = node.currentGameState.copy();
    int advancementsInRollout = SIMULATION_DEPTH;

    //Node currentNode = node;
    while (!currentState.isGameOver() && advancementsInRollout > 0) {

      final ArrayList<Types.ACTIONS> availableActions = currentState.getAvailableActions();
      // If terminal state, break loop
      if (availableActions.size() < 1) {
        advancementsInRollout = 0;
      } else {

        final Types.ACTIONS takeAction = availableActions.get(rand.nextInt(availableActions.size()));
        currentState.advance(takeAction);

/*        // add nodes to tree
        final Node newNode = buildChildNode(node, takeAction);
        node.children.add(newNode);
        currentNode = newNode;*/

        advancementsInRollout--;
      }
    }

    double reward = 0;
    if (RAW_GAME_SCORE) {
      reward = getRawGameStateScore(currentState, initialScore);
    } else {
      reward = getStateScore(currentState, initialScore);
    }

    return reward;
    //return new Pair<Double, Node>(reward, currentNode);
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
