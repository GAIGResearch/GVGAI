package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

import tracks.singlePlayer.florabranchi.trash.GameResults;
import tracks.singlePlayer.florabranchi.database.DatabaseClient;
import tracks.singlePlayer.florabranchi.database.MetaWeightsDAO;
import tracks.singlePlayer.florabranchi.persistence.PersistenceController;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;
import tracks.singlePlayer.florabranchi.training.LearningAgentDebug;

public class MetaMCTSAgent {

  public static double maxDouble = Math.pow(10, 6);
  public static double minDouble = Math.pow(10, -6);

  public double ALFA;
  public double GAMMA;
  public double EXPLORATION_EPSILON;

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

  MetaWeightsDAO metaWeightsDAO = new MetaWeightsDAO(new DatabaseClient());

  public MetaMCTSAgent() {

    try {
      propertyLoader = new PropertyLoader(getPropertyPath());

    } catch (IOException ex) {
      LOGGER.severe("Error loading properties");
    }

    ALFA = propertyLoader.SARSA_ALFA;
    GAMMA = propertyLoader.SARSA_GAMMA;
    EXPLORATION_EPSILON = propertyLoader.SARSA_EPSILON;

    GameOptions gameOptions = new GameOptions();

    gameOptionFeatureController = new GameOptionFeatureController();
    initializeTrainingWeightVector();

    metaWeights = metaWeightsDAO.getMetaWeights(1);

  }

  protected boolean displayDebug() {
    return true;
  }

  protected String getPropertyPath() {
    return "sarsa.properties";
  }


  public double result(final GameOptions gameOptions,
                       final boolean won,
                       final int score,
                       final int iteration) {


    // last update - reward if won, negative if loss
    double reward = 5000;

    if (!won) {
      reward = -reward;

      LOGGER.info(String.format("Agent LOST - score: [%s]", iteration));
      //persistenceController.addLog("Player lost");
    } else {
      LOGGER.info(String.format("Agent WON - score: [%s]", iteration));
      //persistenceController.addLog("Player won");
    }

    reward += score;
    reward -= iteration;

    //updateRecord(won);
    //updateAfterLastAction(reward, previousAction, gameOptions);

    logCurrentWeights();
    metaWeightsDAO.save(metaWeights);

    if (learningAgentDebug != null) {
      learningAgentDebug.closeJframe();
    }
    //super.result(stateObs, elapsedCpuTimer);
    System.out.println("Reward: \n" + reward);
    return reward;
  }

  public void updateRecord(boolean won) {
    GameResults previousResults = persistenceController.readPreviousGameResults();
    previousResults.updateResults(won);
    persistenceController.saveGameResults(previousResults);
  }


  @SuppressWarnings("DuplicatedCode")
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
    final TreeMap<String, Double> initialFeatureVector = gameOptionFeatureController.extractFeatureVector(previousState.gameId);

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
    final TreeMap<String, Double> featureVector = gameOptionFeatureController.extractFeatureVector(previousState.gameId);

    updateWeightVectorForAction(previousAction, featureVector, delta);
  }

  public EMetaActions returnRandomAction() {
    int index = randomGenerator.nextInt(MetaWeights.avallableGameActions.size());
    return MetaWeights.avallableGameActions.get(index);
  }

  public EMetaActions getActionAndUpdateWeightVectorValues(final GameOptions gameOptions,
                                                           final boolean won,
                                                           final double reward) {

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

    final TreeMap<String, Double> featuresForCurrState = gameOptionFeatureController.extractFeatureVector(gameOptions.gameId);

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
    final TreeMap<String, Double> featureAfterAction = gameOptionFeatureController.extractFeatureVector(options.gameId);

    // Get related weight vector
    final TreeMap<String, Double> weightVectorForAction = metaWeights.getWeightVectorMap().get(action);

    return dotProduct(featureAfterAction, weightVectorForAction);
  }

  public void persistStatisticsAndWeights(final boolean won,
                                          final double score) {

    final MetaWeights metaWeights = metaWeightsDAO.getMetaWeights(1);
    metaWeightsDAO.save(metaWeights);

    //persistenceController.updateScoreProgressionLog(score);
    //final int episode = previousResults.update(trainingWeights, won, score);

    //persistenceController.saveScoreProgression(episode);
    //persistenceController.persistWeights(previousResults);
  }


  public EMetaActions selectBestPerceivedAction(final GameOptions options) {

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      final EMetaActions randomAction = returnRandomAction();
      persistenceController.addLog(String.format("Exploring random action %s", randomAction));
      return returnRandomAction();
    }

    double maxValue = -Double.MAX_VALUE;
    EMetaActions bestAction = EMetaActions.NIL;

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


  public void initializeTrainingWeightVector() {

    final long idForNow = 1;

    if (previousResults != null) {
      persistenceController.addLog("Previous results still in memory");
      logCurrentWeights();
      return;
    }

    // read file
    MetaWeights previousResults = metaWeightsDAO.getMetaWeights(idForNow);

    // if could not load initialize
    if (previousResults == null) {
      persistenceController.addLog("No previous results, creating new Weights");
      previousResults = new MetaWeights();
      this.metaWeights = new MetaWeights();
      logCurrentWeights();
    } else {
      System.out.println("Loaded DB previous weiohts.");
      this.metaWeights = previousResults;
      logCurrentWeights();
    }
  }


  public double dotProduct(TreeMap<String, Double> a,
                           TreeMap<String, Double> b) {
    double sum = 0;
    for (String property : GameOptionFeatureController.getAvailableProperties()) {
      sum += a.get(property) * b.get(property);
    }
    return sum;
  }


}


