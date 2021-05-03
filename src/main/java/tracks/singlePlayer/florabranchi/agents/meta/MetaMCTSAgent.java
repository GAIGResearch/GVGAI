package tracks.singlePlayer.florabranchi.agents.meta;

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
import tracks.singlePlayer.florabranchi.GameResults;
import tracks.singlePlayer.florabranchi.persistence.PersistenceController;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;
import tracks.singlePlayer.florabranchi.persistence.weights.OfflineTrainerResults;
import tracks.singlePlayer.florabranchi.training.FeatureVectorController;
import tracks.singlePlayer.florabranchi.training.LearningAgentDebug;
import tracks.singlePlayer.florabranchi.training.PossibleHarmfulSprite;

public class MetaMCTSAgent {

  public double ALFA = 0.3;
  public double GAMMA = 0.9;
  public double EXPLORATION_EPSILON = 10;

  public PropertyLoader propertyLoader;

  protected final static Logger LOGGER = Logger.getLogger("MetaMCTSAgent");

  private LearningAgentDebug learningAgentDebug;

  private GameOptionFeatureController gameOptionFeatureController;

  private PersistenceController persistenceController = new PersistenceController();

  protected Random randomGenerator = new Random();

  // Data
  public MetaWeights metaWeights;

  public MetaWeights previousResults;

  // State related data
  private EMetaActions previousAction;
  private GameOptions previousState;
  private double previousScore;
  private int previousEnemyCount;

  public MetaMCTSAgent() {
    ALFA = propertyLoader.SARSA_ALFA;
    GAMMA = propertyLoader.SARSA_GAMMA;
    EXPLORATION_EPSILON = propertyLoader.SARSA_EPSILON;

    GameOptions gameOptions = new GameOptions();

    gameOptionFeatureController = new GameOptionFeatureController(gameOptions);
    //initializeTrainingWeightVector(featureVectorController.getSize());

  }

  protected boolean displayDebug() {
    return true;
  }

  protected String getPropertyPath() {
    return "sarsa.properties";
  }

  public EMetaActions act(final GameOptions gameOptions,
                          final boolean won,
                          final int score,
                          final int iteration) {

    persistenceController.addLog(String.format("Act for Game Tick %d", iteration));

    return getActionAndUpdateWeightVectorValues(gameOptions, won, score);
  }

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

      LOGGER.info(String.format("Agent LOST - score: [%s]", stateObs.getGameScore()));
      persistenceController.addLog("Player lost");
    } else {
      won = true;
      LOGGER.info(String.format("Agent WON - score: [%s]", stateObs.getGameScore()));
      persistenceController.addLog("Player won");
    }

    final double finalScore = stateObs.getGameScore();

    updateRecord(won);
    //updateAfterLastAction(reward, previousAction, previousState);

    logCurrentWeights();
    persistStatisticsAndWeights(won, finalScore, possibleHarmfulSprite);

    if (learningAgentDebug != null) {
      learningAgentDebug.closeJframe();
    }
    //super.result(stateObs, elapsedCpuTimer);
  }

  public void updateRecord(boolean won) {
    GameResults previousResults = persistenceController.readPreviousGameResults();
    previousResults.updateResults(won);
    persistenceController.saveGameResults(previousResults);
  }


  private void updateWeightVectorForAction(final EMetaActions previousAction,
                                           final TreeMap<String, Double> featureVectorForState,
                                           final double delta) {

    // Get Weight vector for previous action (a) (W i a)
    final TreeMap<String, Double> relatedWeightVector = metaWeights.getWeightVectorMap().get(previousAction);

    for (Map.Entry<String, Double> weightMapEntry : relatedWeightVector.entrySet()) {

      final String featureType = weightMapEntry.getKey();

      // w = w + lambda * delta * f()
      final double updatedWeight = weightMapEntry.getValue() + (ALFA * delta * featureVectorForState.get(featureType));

      // Update
      metaWeights.updateWeightVector(previousAction, featureType, updatedWeight);
    }
  }

  // need: a', s', a, s, r
  public void getActionAndUpdateWeightVectorValues(final double stateReward,
                                                   final EMetaActions previousAction,
                                                   final GameOptions previousState,
                                                   final EMetaActions currentAction,
                                                   final GameOptions currentState) {

    // Get feature values for previous state (s)
    final TreeMap<String, Double> initialFeatureVector = gameOptionFeatureController.extractFeatureVector(previousState);

    // delta = r + gamma (Qa'(s')) - Qa(s)
    double delta = stateReward + (GAMMA * getQValueForAction(currentState, currentAction)) - getQValueForAction(previousState, previousAction);

    // Update weights
    updateWeightVectorForAction(previousAction, initialFeatureVector, delta);

  }

  public void updateAfterLastAction(final double stateReward,
                                    final EMetaActions previousAction,
                                    final GameOptions previousState) {


    // delta = r - Qa(s)
    double delta = stateReward - getQValueForAction(previousState, previousAction);

    // Get feature values for previous state (s)
    // fi(s, a)
    final TreeMap<String, Double> featureVector = gameOptionFeatureController.extractFeatureVector(previousState);

    updateWeightVectorForAction(previousAction, featureVector, delta);
  }


  public EMetaActions returnRandomAction() {
    int index = randomGenerator.nextInt(MetaWeights.avallableGameActions.size());
    return MetaWeights.avallableGameActions.get(index);
  }


  public EMetaActions getActionAndUpdateWeightVectorValues(final GameOptions gameOptions,
                                                           final boolean won,
                                                           final int reward) {

    // Reward = curr score - previous score
    double stateScore = won ? 1000 + reward : -1000;

    // First play
    if (previousAction == null) {
      final EMetaActions currentAction = selectBestPerceivedAction(gameOptions);

      // a
      previousAction = currentAction;
      // s
      previousState = gameOptions;
      previousScore = 0;

      return currentAction;
    }

    // Select best action given current q values for (s') / exploration play
    final EMetaActions selectedAction = selectBestPerceivedAction(gameOptions);

    // need: s, a r, s', a'
    getActionAndUpdateWeightVectorValues(reward, previousAction, previousState, selectedAction, gameOptions);

    final TreeMap<String, Double> featuresForCurrState = gameOptionFeatureController.extractFeatureVector(gameOptions);

/*    if (learningAgentDebug != null && learningAgentDebug.showJframe) {
      learningAgentDebug.writeResultsToUi(featuresForCurrState, selectedAction, trainingWeights, previousResults.getEpisodeTotalScoreMap());
    }*/

    // Update last values
    previousAction = selectedAction;
    previousState = gameOptions;
    previousScore = stateScore;

    return selectedAction;
  }

  public double getQValueForAction(final GameOptions options,
                                   final EMetaActions action) {

    // Extract feature array
    final TreeMap<String, Double> featureAfterAction = gameOptionFeatureController.extractFeatureVector(options);

    // Get related weight vector
    final TreeMap<String, Double> weightVectorForAction = metaWeights.getWeightVectorMap().get(action);

    return dotProduct(featureAfterAction, weightVectorForAction);
  }

  public void persistStatisticsAndWeights(final boolean won,
                                          final double score,
                                          final PossibleHarmfulSprite harmfulSprite) {

    //persistenceController.updateScoreProgressionLog(score);
    //final int episode = previousResults.update(trainingWeights, won, score);

    //persistenceController.saveScoreProgression(episode);
    //persistenceController.persistWeights(previousResults);
  }


  public EMetaActions selectBestPerceivedAction(final GameOptions options) {
/*

    if (stateObservation.getAvailableActions().isEmpty()) {
      persistenceController.addLog("Selecting NIL action to calculate last weight vector");
      return Types.ACTIONS.ACTION_NIL;
    }
*/

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      final EMetaActions randomAction = returnRandomAction();
      persistenceController.addLog(String.format("Exploring random action %s", randomAction));
      return returnRandomAction();
    }

    double maxValue = -Double.MAX_VALUE;
    EMetaActions bestAction = EMetaActions.ACTION_NIL;

    List<EMetaActions> duplicatedActions = new ArrayList<>();


    for (EMetaActions action : MetaWeights.avallableGameActions) {
      final double actionExpectedReward = getQValueForAction(options, action);
      if (actionExpectedReward >= maxValue) {

        if (actionExpectedReward != maxValue) {
          duplicatedActions = new ArrayList<>();
        }

        duplicatedActions.add(action);

        maxValue = actionExpectedReward;
        bestAction = action;

/*        // Update Q Values in UI
        if (learningAgentDebug != null && learningAgentDebug.showJframe) {
          learningAgentDebug.updateQLabel(action, maxValue);
        }*/
      }
    }

    // Return random if draw
    if (duplicatedActions.size() > 1) {

      Collections.shuffle(duplicatedActions);
      final EMetaActions selectedActon = duplicatedActions.get(randomGenerator.nextInt(duplicatedActions.size() - 1));
      persistenceController.addLog(String.format("Best play: draw for score %s, selected: %s", maxValue, selectedActon));
      return selectedActon;
    }

    persistenceController.addLog(String.format("Best play: %s - score - %.2f", bestAction, maxValue));
    return bestAction;
  }


  public void logCurrentWeights() {
    final TreeMap<EMetaActions, TreeMap<String, Double>> weightVectorMap = metaWeights.getWeightVectorMap();
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
    ///previousResults = persistenceController.readPreviousWeights();

    // if could not load initialize
    if (previousResults == null) {
      persistenceController.addLog("No previous results, creating new Weights");
      previousResults = new OfflineTrainerResults(featureVectorSize);
      metaWeights = new MetaWeights();
      logCurrentWeights();
    } else {
      int previousEpisode = previousResults.getTotalGames();
      persistenceController.addLog(String.format("Loading previous results from episode %s", previousEpisode));
      metaWeights = previousResults.getWeightVector();
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


