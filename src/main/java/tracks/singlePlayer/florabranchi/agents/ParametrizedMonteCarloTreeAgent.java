package tracks.singlePlayer.florabranchi.agents;

import static tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper.getAverageDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;
import tracks.singlePlayer.florabranchi.training.StateEvaluatorHelper;
import tracks.singlePlayer.florabranchi.tree.Node;
import tracks.singlePlayer.florabranchi.tree.TreeController;

/**
 * Flora Branchi (florabranchi@gmail.com) September 2019
 */
public class ParametrizedMonteCarloTreeAgent extends AbstractAgent {

  private final static Logger logger = Logger.getLogger(TreeController.class.getName());

  //UCB1
  //private final static double C = 1 / Math.sqrt(2);
  private final static double C = 0.6;

  /**
   * Random generator for the agent.
   */
  protected Random randomGenerator;

  /**
   * List of available actions for the agent
   */
  protected ArrayList<ACTIONS> actions;

  public static String gameName;

  public static boolean showTree;

  public static int TIME_LIMITATION_IN_MILLIS;
  public static int TREE_SEARCH_SIZE;
  public static int ROLLOUT_DEPTH;

  // Enhancements
  public static boolean TREE_REUSE;
  public static boolean LOSS_AVOIDANCE;

  public static boolean EXPAND_ALL_CHILD_NODES;
  public static boolean SAFETY_PREPRUNNING;
  public static boolean EARLY_INITIALIZATION;

  public static boolean TIME_LIMITATION;
  public static boolean SELECT_HIGHEST_SCORE_CHILD;
  public static boolean RAW_GAME_SCORE;
  public static boolean MACRO_ACTIONS;

  // Heuristic Weights
  public int RAW_SCORE_WEIGHT = 1;
  public int TOTAL_RESOURCES_SCORE_WEIGHT = 1;
  public int RESOURCE_SCORE_WEIGHT = 2;
  public int EXPLORATION_SCORE_WEIGHT = 5;
  public int MOVABLES_SCORE_WEIGHT = 1;
  public int PORTALS_SCORE_WEIGHT = 1;

  protected List<Integer> totalNodes = new ArrayList<>();

  private final int maxDistance;

  private ACTIONS lastAction = null;

  public Node rootNode;

  private Random rand = new Random();

  private static final double WINNER_SCORE = Math.pow(10, 6);
  private static final double LOSS_SCORE = Math.pow(10, -6);

  private final int[][] visitCount;

  private static final int MACRO_ACTIONS_DURATION = 5;

  private ACTIONS currentMacroAction;
  private int remainingMacroActionRepetitions;
  boolean resetAlgorithm = false;

  public static void reloadProperties() {

    // gameName = PropertyLoader.GAME_NAME;

    showTree = PropertyLoader.SHOW_TREE;

    TREE_SEARCH_SIZE = PropertyLoader.TREE_SEARCH_SIZE;
    ROLLOUT_DEPTH = PropertyLoader.ROLLOUT_DEPTH;

    TREE_REUSE = PropertyLoader.TREE_REUSE;
    MACRO_ACTIONS = PropertyLoader.MACRO_ACTIONS;
    TIME_LIMITATION_IN_MILLIS = PropertyLoader.TIME_LIMITATION_IN_MILLIS;
    TIME_LIMITATION = PropertyLoader.TIME_LIMITATION;
    SELECT_HIGHEST_SCORE_CHILD = PropertyLoader.SELECT_HIGHEST_SCORE_CHILD;
    LOSS_AVOIDANCE = PropertyLoader.LOSS_AVOIDANCE;
    RAW_GAME_SCORE = PropertyLoader.RAW_GAME_SCORE;
    EXPAND_ALL_CHILD_NODES = PropertyLoader.EXPAND_ALL_CHILD_NODES;
    SAFETY_PREPRUNNING = PropertyLoader.SAFETY_PREPRUNNING;
    EARLY_INITIALIZATION = PropertyLoader.EARLY_INITIALIZATION;
  }

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

    reloadProperties();

    randomGenerator = new Random();

    final int height = StateEvaluatorHelper.getHeight(stateObs);
    final int width = StateEvaluatorHelper.getWidth(stateObs);
    visitCount = new int[width][height];

    for (int i = 0; i < width - 1; i++) {
      for (int j = 0; j < height - 1; j++) {
        visitCount[i][j] = 0;
      }
    }

    maxDistance = height * width;

    final long startEarlyInit = getStartTime();
    if (EARLY_INITIALIZATION) {
      rootNode = buildNode(stateObs, null, null);
      searchWithTimeLimitation(stateObs, 1000, 50);
    }
    measureTime(startEarlyInit, "early initialization");

    System.out.println(rootNode);
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

    //System.out.println("SCORE: " + stateObs.getGameScore());
    //final long startTime = getStartTime();
    final ACTIONS selectedAction = monteCarloSearchParametrized(stateObs, elapsedTimer, lastAction);
    //measureTime(startTime, "act");
    lastAction = selectedAction;
    totalNodes.add(rootNode.visits);
    System.out.println(rootNode.visits);
    System.out.println(totalNodes.stream().mapToInt(val -> val).average().orElse(0));
    return selectedAction;
  }

  public ArrayList<ACTIONS> getAvailableActions(final StateObservation stateObservation) {
    final ArrayList<ACTIONS> availableActions = stateObservation.getAvailableActions();
    if (!availableActions.contains(ACTIONS.ACTION_NIL)) {
      availableActions.add(ACTIONS.ACTION_NIL);
    }
    return availableActions;
  }

  @Override
  public void result(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedCpuTimer) {

    reloadProperties();

  }

  public ACTIONS monteCarloSearchParametrized(final StateObservation stateObs,
                                              final ElapsedCpuTimer elapsedTimer,
                                              final ACTIONS previousAction) {


    if (MACRO_ACTIONS) {

      if (currentMacroAction == null) {
        //System.out.println("Setting first MA");
        rootNode = buildRootNode(stateObs);
        currentMacroAction = decideMacro(stateObs);
      } else {
        //System.out.printf("Current MA: %s Remaining reps: %d \n", currentMacroAction, remainingMacroActionRepetitions);

        for (int i = 0; i < remainingMacroActionRepetitions; i++) {
          stateObs.advance(currentMacroAction);
        }

        if (remainingMacroActionRepetitions > 0) {
          //System.out.println("Macro action is set. Remaining uses: " + remainingMacroActionRepetitions);
          if (resetAlgorithm) {
            //System.out.println("Reseting algorithm");
            rootNode = buildRootNode(stateObs);
            search(stateObs);
            resetAlgorithm = false;
          }
          search(stateObs);
        } else {
          System.out.println("0 actions left.");
          currentMacroAction = decideMacro(stateObs);
        }
      }

      remainingMacroActionRepetitions--;
      //System.out.println("Executing " + currentMacroAction);
      return currentMacroAction;

    } else {
      // Regular search

      if (TREE_REUSE && previousAction != null) {
        final Optional<Node> newRoot = rootNode.children.stream()
            .filter(child -> child.previousAction.equals(previousAction)).findFirst();

        if (newRoot.isPresent() && stateObs.equals(rootNode.currentGameState)) {
          rootNode = newRoot.get();
          rootNode.parent = null;

          // fix depth
          final List<Node> nodes = returnAllNodes(rootNode);
          nodes.forEach(node -> node.depth--);

          //todo add decay

        } else {
          rootNode = buildRootNode(stateObs);
        }
      } else {
        rootNode = buildRootNode(stateObs);
      }

      search(stateObs);
    }

    return nextMove();
  }

  private ACTIONS decideMacro(final StateObservation stateObs) {
    //System.out.println("Deciding new MacroAction.");
    search(stateObs);
    remainingMacroActionRepetitions = MACRO_ACTIONS_DURATION;
    resetAlgorithm = true;
    final ACTIONS nextMacro = nextMove();
    //System.out.println("New MacroAction: " + nextMacro);
    return nextMacro;
  }

  private ACTIONS nextMove() {
    if (SELECT_HIGHEST_SCORE_CHILD) {
      return getChildWithHighestScore(rootNode).previousAction;
    } else {
      return getMostVisitedChild(rootNode).previousAction;
    }
  }

  private void search(final StateObservation stateObs) {

    if (!TIME_LIMITATION) {
      searchWithIterationLimit(stateObs);
    } else {
      searchWithTimeLimitation(stateObs, TIME_LIMITATION_IN_MILLIS, 2);
    }
  }

  private Node buildRootNode(final StateObservation stateObs) {
    return new Node(stateObs);
  }

  public void searchWithIterationLimit(final StateObservation stateObs) {
    int iterations = TREE_SEARCH_SIZE;
    for (int i = 0; i < iterations; i++) {
      monteCarloSearch(stateObs);
    }
  }

  private Node buildNode(final StateObservation stateObs, final Node parent, final ACTIONS previousAction) {
    return new Node(parent, previousAction, stateObs);
  }

  public void searchWithTimeLimitation(final StateObservation stateObs,
                                       long remainingMillis,
                                       final int remainingMillisLimit) {

    final long initial = System.currentTimeMillis();

    //long remaining = TIME_LIMITATION_IN_MILLIS;
    // long remaining = elapsedTimer.remainingTimeMillis();

    double avgTimeTaken = 0;
    double acumTimeTaken = 0;
    int iterations = 0;

    List<Long> timePerTurn = new ArrayList<>();
    final long timeNow = getStartTime();
    while (remainingMillis > 2 * avgTimeTaken && remainingMillis > remainingMillisLimit) {

      ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
      long initialIt = System.currentTimeMillis();

      monteCarloSearch(stateObs);

      //System.out.println(elapsedTimerIteration.elapsedMillis() +nodes);
      iterations++;
      long itDelta = System.currentTimeMillis() - initialIt;
      acumTimeTaken += itDelta;
      avgTimeTaken = acumTimeTaken / iterations;
      remainingMillis -= itDelta;
      final long finish = getStartTime();
      timePerTurn.add(finish);
    }

    //System.out.printf("Time taken per turn: %s", timePerTurn);

    final long finalTime = System.currentTimeMillis();
    final long l = finalTime - initial;
    //System.out.println("time: " + l);
    //System.out.println("iterations: " + iterations + " ");
  }

  public void monteCarloSearch(final StateObservation stateObs) {

    boolean skipTreeUpdate = false;
    final double initialScore = stateObs.getGameScore();

    //final long selectionStartTime = getStartTime();

    // Tree policy
    // Selection and expansion
    Node selectedNode = treePolicy();
    //measureTime(selectionStartTime, "selection");

    double simulationReward = 0;
    //final long rolloutStartTime = getStartTime();
    logMessage(String.format("Rollouting selected node %s", selectedNode.id));
    simulationReward = rollout(selectedNode, initialScore);
    //measureTime(expansionStartTime, "rollout");

    logMessage(String.format("Simulation Reward: %s", simulationReward));

    // Backpropagation

    // todo(flora) add discount factor
    backup(selectedNode, simulationReward);
  }

  long getStartTime() {
    return System.nanoTime();
  }

  void measureTime(final long startTime, final String context) {
    long estimatedTime = System.nanoTime() - startTime;
    System.out.printf("[%s] elapsed time: %.2f \n", context, (float) (estimatedTime / (float) 1000000));
  }

  public Node treePolicy() {

    Node selectedNode = rootNode;

    while (!selectedNode.currentGameState.isGameOver()) {

      if (isExpandable(selectedNode)) {
        expansion(selectedNode, getAvailableActions(selectedNode.currentGameState));
        return selectedNode;
      }
      selectedNode = getBestChild(selectedNode);
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
    selectedNode.updateNodeReward(updatedValue);
    if (selectedNode.parent != null) {
      backup(selectedNode.parent, updatedValue);
    }
  }


  public static List<Node> returnAllNodes(final Node node) {
    List<Node> listOfNodes = new ArrayList<>();
    if (node != null) {
      listOfNodes.add(node);
      for (int i = 0; i < listOfNodes.size(); ++i) {
        Node n = listOfNodes.get(i);
        List<Node> children = n.children;
        if (children != null) {
          for (Node child : children) {
            if (!listOfNodes.contains(child)) {
              listOfNodes.add(child);
            }
          }
        }
      }
    }
    return listOfNodes;
  }

  public void expansion(final Node parentNode,
                        final ArrayList<Types.ACTIONS> actions) {
    logMessage(String.format("Expanding children of node %s", parentNode.id));

    if (EXPAND_ALL_CHILD_NODES) {
      for (Types.ACTIONS action : actions) {
        parentNode.children.add(buildChildNode(parentNode, action));
      }
    } else {
      final ArrayList<ACTIONS> remainingActions = new ArrayList<>(actions);
      remainingActions.removeAll(parentNode.children.stream().map(node -> node.previousAction).collect(Collectors.toList()));
      final ACTIONS selectedAction = remainingActions.get(rand.nextInt(remainingActions.size()));
      parentNode.children.add(buildChildNode(parentNode, selectedAction));
    }
  }

  public Node buildChildNode(final Node parent,
                             final Types.ACTIONS action) {
    final StateObservation newNodeState = parent.currentGameState.copy();
    newNodeState.advance(action);
    return buildNode(newNodeState, parent, action);
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
    int advancementsInRollout = ROLLOUT_DEPTH;

    //Node currentNode = node;
    while (!currentState.isGameOver() && advancementsInRollout > 0) {

      final ArrayList<Types.ACTIONS> availableActions = currentState.getAvailableActions();
      final Types.ACTIONS takeAction = availableActions.get(rand.nextInt(availableActions.size()));
      currentState.advance(takeAction);
      advancementsInRollout--;
    }

    double reward = 0;
    // Selected node is game over
    if (currentState.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
      return WINNER_SCORE;
    } else if (currentState.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
      return LOSS_SCORE;
    } else {

      if (RAW_GAME_SCORE) {
        reward = getRawGameStateScore(currentState, initialScore);
      } else {
        reward = getStateScore(currentState, initialScore);
      }
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

    return Collections.max(node.children, Comparator.comparing(c -> c.totalValue));
  }

  public boolean allValuesEqual(final Node node) {

    // outros casos de empate
    // retornar lista de nodos com valor parecido
    // n1 .4 n2 .4 n3 .3
    // lista com valores maximos iguais


    Double value = null;
    for (Node child : node.children) {
      if (value != null && child.totalValue != value) {
        return false;
      } else if (value == null) {
        value = child.totalValue;
      }
    }
    //System.out.print("All values equal \n");
    return true;
  }

  public Node getBestChild(final Node node) {

    if (allValuesEqual(node)) {
      return node.children.get(rand.nextInt(node.children.size()));
    }


    final Node max = Collections.max(node.children, Comparator.comparing(c -> getNodeUpperConfidenceBound(c, node.visits)));
    return max;
  }

  public double getNodeUpperConfidenceBound(final Node node,
                                            final int parentVisits) {

    if (node.visits == 0) {
      return Double.MAX_VALUE;
    }
    return node.currentValue + 2 * C * Math.sqrt((2 * (Math.log(parentVisits)) / node.visits));
  }
}
