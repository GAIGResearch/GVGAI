package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

  private GameOptionFeatureController gameOptionFeatureController;

  protected Random randomGenerator = new Random();

  // State related data
  public Map<MabParameters, MabData> mabsData = new HashMap<>();
  public Map<MabParameters, MabData> previousData = new HashMap<>();
  MabParameters previousAction;

  MultiArmedNaiveSampler sampler = new MultiArmedNaiveSampler();
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

    // load
    mabsData = new HashMap<>();
    previousData = new HashMap<>();

    sampler.loadMabs();

  }

  protected boolean displayDebug() {
    return true;
  }

  protected String getPropertyPath() {
    return "sarsa.properties";
  }


  public double result(final MabParameters gameOptions,
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
    //metaWeightsDAO.save(metaWeights);

    //super.result(stateObs, elapsedCpuTimer);
    System.out.println("Reward: \n" + reward);
    return reward;
  }

  public void updateRecord(boolean won) {
    // GameResults previousResults = persistenceController.readPreviousGameResults();
    //previousResults.updateResults(won);
    //persistenceController.saveGameResults(previousResults);
  }


  @SuppressWarnings("DuplicatedCode")
  private void updateWeightVectorForAction(final MabParameters previousAction,
                                           final TreeMap<String, Double> featureVectorForState,
                                           final double delta) {

    // Get Weight vector for previous action (a) (W i a)
    final MabData mabData = mabsData.get(previousAction);
    final TreeMap<String, Double> relatedWeightVector = mabData.getWeightVectorMap();

    for (Map.Entry<String, Double> weightMapEntry : relatedWeightVector.entrySet()) {

      final String featureType = weightMapEntry.getKey();

      // w = w + lambda * delta * f()
      final double updatedWeight = weightMapEntry.getValue() + (ALFA * delta * featureVectorForState.get(featureType));

      // Update
      mabData.updateWeightVector(featureType, updatedWeight);
    }
  }

  // need: a', s', a, s, r
  public void updateAndGetNewMab(final double stateReward,
                                 final MabParameters previousAction,
                                 final GameOptions previousState,
                                 final MabParameters currentAction,
                                 final GameOptions currentState) {

    // Get feature values for previous state (s)
    final TreeMap<String, Double> initialFeatureVector = gameOptionFeatureController.extractFeatureVector(previousState.gameId);

    // delta = r + gamma (Qa'(s')) - Qa(s)
    double delta = stateReward + (GAMMA * getQValueForAction(currentState, currentAction)) - getQValueForAction(previousState, previousAction);

    // Update weights
    updateWeightVectorForAction(previousAction, initialFeatureVector, delta);

  }

  public void updateAfterLastAction(final double stateReward,
                                    final MabParameters previousAction,
                                    final GameOptions previousState) {


    // delta = r - Qa(s)
    double delta = stateReward - getQValueForAction(previousState, previousAction);

    // Get feature values for previous state (s)
    // fi(s, a)
    final TreeMap<String, Double> featureVector = gameOptionFeatureController.extractFeatureVector(previousState.gameId);

    updateWeightVectorForAction(previousAction, featureVector, delta);
  }

  public MabParameters returnRandomAction() {

    return null;
    // return random mab
    //int index = randomGenerator.nextInt(MetaWeights.availableParameters.size());
    //return MetaWeights.availableParameters.get(index);
  }

  public MabParameters updateAndGetNewMab(final MabParameters gameOptions,
                                          final boolean won,
                                          final double reward) {

    // Reward = curr score - previous score
    double stateScore = won ? 1000 + reward : -1000;

    // First play
    if (previousAction == null) {
      final MabParameters currentAction = selectBestPerceivedAction(gameOptions);

      // a
      previousAction = currentAction;
      // s
      //previousState = gameOptions;
      //previousScore = 0;

      return currentAction;
    }

    // Select best action given current q values for (s') / exploration play
    //final EMetaParameters selectedAction = selectBestPerceivedAction(gameOptions);

    // need: s, a r, s', a'
   // updateAndGetNewMab(reward, previousAction, previousState, selectedAction, gameOptions);

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
                                   final MabParameters action) {

    // Extract feature array
    final TreeMap<String, Double> featureAfterAction = gameOptionFeatureController.extractFeatureVector(options.gameId);

    // Get related weight vector
    final TreeMap<String, Double> weightVectorForAction = mabsData.get(action).getWeightVectorMap();

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


  public MabParameters selectBestPerceivedAction(final GameOptions options) {

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      final MabParameters randomAction = returnRandomAction();
      //persistenceController.addLog(String.format("Exploring random action %s", randomAction));
      return returnRandomAction();
    }

    double maxValue = -Double.MAX_VALUE;
    MabParameters bestAction = null;

    List<MabParameters> duplicatedActions = new ArrayList<>();


    for (MabParameters action : mabsData.keySet()) {
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
      final MabParameters selectedActon = duplicatedActions.get(randomGenerator.nextInt(duplicatedActions.size() - 1));
      //persistenceController.addLog(String.format("Best play: draw for score %s, selected: %s", maxValue, selectedActon));
      return selectedActon;
    }

    //persistenceController.addLog(String.format("Best play: %s - score - %.2f", bestAction, maxValue));
    return bestAction;
  }


  public void logCurrentWeights() {
    final TreeMap<MabParameters, TreeMap<String, Double>> weightVectorMap = (TreeMap<MabParameters, TreeMap<String, Double>>) mabsData.values().stream().map(MabData::getWeightVectorMap);
    weightVectorMap.forEach((key, value) -> {
      // persistenceController.addLog(String.format("Weight Vector for %s", key));
      //value.forEach((key1, value1) -> persistenceController.addLog(String.format("%s - %s", key1, value1)));
    });
  }


  public void initializeTrainingWeightVector() {

    final long idForNow = 1;

    if (mabsData != null) {
      //persistenceController.addLog("Previous results still in memory");
      logCurrentWeights();
      return;
    }

    // read file
    //MetaWeights previousData = metaWeightsDAO.getMetaWeights(idForNow);

    // if could not load initialize
    if (previousData == null) {
      //persistenceController.addLog("No previous results, creating new Weights");
      previousData = new HashMap<>();
      this.mabsData = new HashMap<>();
      logCurrentWeights();
    } else {
      System.out.println("Loaded DB previous weiohts.");
      //this.metaWeights = previousResults;
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


