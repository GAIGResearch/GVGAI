package tracks.singlePlayer.florabranchi.trash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.agents.AbstractAgent;
import tracks.singlePlayer.florabranchi.trash.GameResults;
import tracks.singlePlayer.florabranchi.persistence.PersistenceController;
import tracks.singlePlayer.florabranchi.persistence.weights.OfflineTrainerResults;
import tracks.singlePlayer.florabranchi.persistence.weights.TrainingWeights;
import tracks.singlePlayer.florabranchi.training.FeatureVectorController;
import tracks.singlePlayer.florabranchi.training.LearningAgentDebug;
import tracks.singlePlayer.florabranchi.training.PossibleHarmfulSprite;

public class SarsaAgent extends AbstractAgent {

  public double ALFA = 0.3;
  public double GAMMA = 0.9;
  public double EXPLORATION_EPSILON = 10;

  protected final static Logger LOGGER = Logger.getLogger("GVGAI_BOT");

  private LearningAgentDebug learningAgentDebug;

  private FeatureVectorController featureVectorController;

  private PersistenceController persistenceController = new PersistenceController();

  protected Random randomGenerator = new Random();

  // Data
  public TrainingWeights trainingWeights;

  public OfflineTrainerResults previousResults;

  // State related data
  private Types.ACTIONS previousAction;
  private StateObservation previousState;
  private double previousScore;
  private int previousEnemyCount;

  public SarsaAgent(final StateObservation stateObs,
                    final ElapsedCpuTimer elapsedTimer) {
    super(stateObs, elapsedTimer);
    ALFA = propertyLoader.SARSA_ALFA;
    GAMMA = propertyLoader.SARSA_GAMMA;
    EXPLORATION_EPSILON = propertyLoader.SARSA_EPSILON;
    featureVectorController = new FeatureVectorController();
    initializeTrainingWeightVector(featureVectorController.getSize());

    if (!previousResults.getWeightVector().getPossibleHarmfulElements().isEmpty()) {
      featureVectorController.injectHarmfulList(previousResults.getWeightVector().getPossibleHarmfulElements());
    }

    if (displayDebug()) {
      learningAgentDebug = new LearningAgentDebug(stateObs, previousResults);
    }
  }

  protected boolean displayDebug() {
    return true;
  }

  protected String getPropertyPath() {
    return "sarsa.properties";
  }

  @Override
  public Types.ACTIONS act(final StateObservation stateObs,
                           final ElapsedCpuTimer elapsedTimer) {

    persistenceController.addLog(String.format("Act for Game Tick %d", stateObs.getGameTick()));

    return getActionAndUpdateWeightVectorValues(stateObs, elapsedTimer);
  }

  @Override
  public void result(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedCpuTimer) {

    // last update - reward if won, negative if loss
    final Types.WINNER gameWinner = stateObs.getGameWinner();
    double reward = 5000;
    boolean won = false;
    Integer harmfulSpriteId = null;
    PossibleHarmfulSprite possibleHarmfulSprite = null;

    if (gameWinner.equals(Types.WINNER.PLAYER_LOSES)) {
      reward = -reward;
      final Event last = stateObs.getEventsHistory().last();

      // if loss bullet is active
      final Map<Integer, Observation> integerListMap = featureVectorController.castAllObservationToMapOfIds(stateObs);
      harmfulSpriteId = last.passiveSpriteId;

      if (integerListMap.containsKey(harmfulSpriteId)) {
        LOGGER.info("Game finished due to possible harmful sprite");
        final Observation observations = integerListMap.get(harmfulSpriteId);
        possibleHarmfulSprite = new PossibleHarmfulSprite(observations.category, observations.itype);
      }

      LOGGER.info(String.format("Agent LOST - score: [%s]", stateObs.getGameScore()));
      persistenceController.addLog("Player lost");
    } else {
      won = true;
      LOGGER.info(String.format("Agent WON - score: [%s]", stateObs.getGameScore()));
      persistenceController.addLog("Player won");
    }

    final double finalScore = stateObs.getGameScore();

    updateRecord(won);
    updateAfterLastAction(reward, previousAction, previousState);

    logCurrentWeights();
    persistStatisticsAndWeights(won, finalScore, possibleHarmfulSprite);

    if (learningAgentDebug != null) {
      learningAgentDebug.closeJframe();
    }
    super.result(stateObs, elapsedCpuTimer);
  }

  public void updateRecord(boolean won) {
    GameResults previousResults = persistenceController.readPreviousGameResults();
    previousResults.updateResults(won);
    persistenceController.saveGameResults(previousResults);
  }


  private void updateWeightVectorForAction(final Types.ACTIONS previousAction,
                                           final TreeMap<String, Double> featureVectorForState,
                                           final double delta) {

    // Get Weight vector for previous action (a) (W i a)
    final TreeMap<String, Double> relatedWeightVector = trainingWeights.getWeightVectorForAction(previousAction);

    for (Map.Entry<String, Double> weightMapEntry : relatedWeightVector.entrySet()) {

      final String featureType = weightMapEntry.getKey();

      // w = w + lambda * delta * f()
      final double updatedWeight = weightMapEntry.getValue() + (ALFA * delta * featureVectorForState.get(featureType));

      // Update
      trainingWeights.updateWeightVector(previousAction, featureType, updatedWeight);
    }
  }

  // need: a', s', a, s, r
  public void getActionAndUpdateWeightVectorValues(final double stateReward,
                                                   final Types.ACTIONS previousAction,
                                                   final StateObservation previousState,
                                                   final Types.ACTIONS currentAction,
                                                   final StateObservation currentState) {

    // Get feature values for previous state (s)
    final TreeMap<String, Double> initialFeatureVector = featureVectorController.extractFeatureVector(previousState);

    // delta = r + gamma (Qa'(s')) - Qa(s)
    double delta = stateReward + (GAMMA * getQValueForAction(currentState, currentAction)) - getQValueForAction(previousState, previousAction);

    // Update weights
    updateWeightVectorForAction(previousAction, initialFeatureVector, delta);

  }

  public void updateAfterLastAction(final double stateReward,
                                    final Types.ACTIONS previousAction,
                                    final StateObservation previousState) {


    // delta = r - Qa(s)
    double delta = stateReward - getQValueForAction(previousState, previousAction);

    // Get feature values for previous state (s)
    // fi(s, a)
    final TreeMap<String, Double> featureVector = featureVectorController.extractFeatureVector(previousState);

    updateWeightVectorForAction(previousAction, featureVector, delta);

  }


  public Types.ACTIONS returnRandomAction(final StateObservation stateObservation) {

    if (stateObservation.getAvailableActions().isEmpty()) {
      return Types.ACTIONS.ACTION_NIL;
    }

    int index = randomGenerator.nextInt(stateObservation.getAvailableActions().size());
    return stateObservation.getAvailableActions().get(index);
  }


  /**
   * Uses default game score to update values
   */
  public Types.ACTIONS getActionAndUpdateWeightVectorValues(final StateObservation stateObs,
                                                            final ElapsedCpuTimer elapsedTimer) {

    // Reward = curr score - previous score
    double stateScore = stateObs.getGameScore();
    double reward = stateScore - previousScore;

    // try to give more points for killing enemies

    final int enemiesNow = countEnemies(stateObs);
    boolean killedAlien = previousEnemyCount < enemiesNow;
    if (killedAlien) {
      reward += 1;
    }


    // First play
    if (previousAction == null) {
      final Types.ACTIONS currentAction = selectBestPerceivedAction(stateObs);

      // a
      previousAction = currentAction;
      // s
      previousState = stateObs;
      previousScore = 0;

      return currentAction;
    }

    // Select best action given current q values for (s') / exploration play
    final Types.ACTIONS selectedAction = selectBestPerceivedAction(stateObs);

    // need: s, a r, s', a'
    getActionAndUpdateWeightVectorValues(reward, previousAction, previousState, selectedAction, stateObs);

    final TreeMap<String, Double> featuresForCurrState = featureVectorController.extractFeatureVector(stateObs);

    if (learningAgentDebug != null && learningAgentDebug.showJframe) {
      learningAgentDebug.writeResultsToUi(featuresForCurrState, selectedAction, trainingWeights, previousResults.getEpisodeTotalScoreMap());
    }

    // Update last values
    previousAction = selectedAction;
    previousState = stateObs;
    previousScore = stateObs.getGameScore();
    previousEnemyCount = enemiesNow;


    return selectedAction;
  }

  private int countEnemies(final StateObservation stateObs) {
    final ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions();
    int count = 0;
    if (npcPositions != null && npcPositions.length > 0) {
      count = npcPositions[0].size();
    }
    return count;
  }


  /**
   * Execute dot product between weights and features
   */
  public double getQValueForAction(final StateObservation state,
                                   final Types.ACTIONS action) {

    // Extract feature array
    final TreeMap<String, Double> featureAfterAction = featureVectorController.extractFeatureVector(state);

    // Get related weight vector
    final TreeMap<String, Double> weightVectorForAction = trainingWeights.getWeightVectorForAction(action);

    return dotProduct(featureAfterAction, weightVectorForAction);
  }

  public void persistStatisticsAndWeights(final boolean won,
                                          final double score,
                                          final PossibleHarmfulSprite harmfulSprite) {

    persistenceController.updateScoreProgressionLog(score);
    final int episode = previousResults.update(trainingWeights, won, score);

    if (harmfulSprite != null) {
      previousResults.addNewHarmfulSprite(harmfulSprite);
    }

    persistenceController.saveScoreProgression(episode);
    persistenceController.persistWeights(previousResults);
  }


  public Types.ACTIONS selectBestPerceivedAction(final StateObservation stateObservation) {

    if (stateObservation.getAvailableActions().isEmpty()) {
      persistenceController.addLog("Selecting NIL action to calculate last weight vector");
      return Types.ACTIONS.ACTION_NIL;
    }

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      final Types.ACTIONS randomAction = returnRandomAction(stateObservation);
      persistenceController.addLog(String.format("Exploring random action %s", randomAction));
      return returnRandomAction(stateObservation);
    }

    double maxValue = -Double.MAX_VALUE;
    Types.ACTIONS bestAction = Types.ACTIONS.ACTION_NIL;

    List<Types.ACTIONS> duplicatedActions = new ArrayList<>();


    for (Types.ACTIONS action : stateObservation.getAvailableActions()) {
      final double actionExpectedReward = getQValueForAction(stateObservation, action);
      if (actionExpectedReward >= maxValue) {

        if (actionExpectedReward != maxValue) {
          duplicatedActions = new ArrayList<>();
        }

        duplicatedActions.add(action);

        maxValue = actionExpectedReward;
        bestAction = action;

        // Update Q Values in UI
        if (learningAgentDebug != null && learningAgentDebug.showJframe) {
          learningAgentDebug.updateQLabel(action, maxValue);
        }
      }
    }

    // Return random if draw
    if (duplicatedActions.size() > 1) {

      Collections.shuffle(duplicatedActions);
      final Types.ACTIONS selectedActon = duplicatedActions.get(randomGenerator.nextInt(duplicatedActions.size() - 1));
      persistenceController.addLog(String.format("Best play: draw for score %s, selected: %s", maxValue, selectedActon));
      return selectedActon;
    }

    persistenceController.addLog(String.format("Best play: %s - score - %.2f", bestAction, maxValue));
    return bestAction;
  }


  public void logCurrentWeights() {
    final TreeMap<Types.ACTIONS, TreeMap<String, Double>> weightVectorMap = trainingWeights.getWeightVectorMap();
    weightVectorMap.forEach((key, value) -> {
      persistenceController.addLog(String.format("Weight Vector for %s", key));
      value.forEach((key1, value1) -> persistenceController.addLog(String.format("%s - %s", key1, value1)));
    });
  }


  public void initializeTrainingWeightVector(int featureVectorSize) {

    if (previousResults != null) {
      persistenceController.addLog("Previous results still in memory");
      logCurrentWeights();
      return;
    }

    // read file
    previousResults = persistenceController.readPreviousWeights();

    // if could not load initialize
    if (previousResults == null) {
      persistenceController.addLog("No previous results, creating new Weights");
      previousResults = new OfflineTrainerResults(featureVectorSize);
      trainingWeights = new TrainingWeights(featureVectorSize);
      logCurrentWeights();
    } else {
      int previousEpisode = previousResults.getTotalGames();
      persistenceController.addLog(String.format("Loading previous results from episode %s", previousEpisode));
      trainingWeights = previousResults.getWeightVector();
      logCurrentWeights();
    }
  }


  public double dotProduct(TreeMap<String, Double> a,
                           TreeMap<String, Double> b) {
    double sum = 0;
    for (String property : FeatureVectorController.getAvailableProperties()) {
      sum += a.get(property) * b.get(property);
    }

    return sum;
  }


}

