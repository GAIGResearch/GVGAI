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

import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

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
  MabParameters currentAction;
  MabParameters previousAction;
  private GameFeatures previousState;
  private GameFeatures currentState;

  MultiArmedNaiveSampler sampler = new MultiArmedNaiveSampler();

  public MetaMCTSAgent() {

    try {
      propertyLoader = new PropertyLoader(getPropertyPath());

    } catch (IOException ex) {
      LOGGER.severe("Error loading properties");
    }

    ALFA = propertyLoader.SARSA_ALFA;
    GAMMA = propertyLoader.SARSA_GAMMA;
    EXPLORATION_EPSILON = propertyLoader.SARSA_EPSILON;

    gameOptionFeatureController = new GameOptionFeatureController();
    initializeTrainingWeightVector();

    // load
    mabsData = new HashMap<>();
    previousData = new HashMap<>();

    MabParameters mabParameters = new MabParameters();
    mabParameters.addParameter(EMetaParameters.TREE_REUSE, PropertyLoader.TREE_REUSE);
    mabParameters.addParameter(EMetaParameters.SELECT_HIGHEST_SCORE_CHILD, PropertyLoader.SELECT_HIGHEST_SCORE_CHILD);
    mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, PropertyLoader.EARLY_INITIALIZATION);
    mabParameters.addParameter(EMetaParameters.LOSS_AVOIDANCE, PropertyLoader.LOSS_AVOIDANCE);
    mabParameters.addParameter(EMetaParameters.RAW_GAME_SCORE, PropertyLoader.RAW_GAME_SCORE);
    mabsData.put(mabParameters, new MabData());

    sampler.loadMabs();
  }

  public MabParameters act(final double reward) {

    final GameFeatures currentState = gameOptionFeatureController.extractGameOptions(PropertyLoader.GAME);

    // First play
    if (previousAction == null) {
      final MabParameters currentAction = selectBestPerceivedAction(currentState);

      // a
      previousAction = currentAction;
      // s
      previousState = currentState;
      return currentAction;
    }

    // Select best action given current q values for (s') / exploration play
    final MabParameters selectedAction = selectBestPerceivedAction(currentState);

    // need: s, a r, s', a'
    updateAndGetNewMab(reward, previousAction, previousState, selectedAction, currentState);

    // Update last values
    previousAction = selectedAction;
    previousState = currentState;
    previousData = mabsData;
    return selectedAction;

  }

  protected boolean displayDebug() {
    return true;
  }

  protected String getPropertyPath() {
    return "sarsa.properties";
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
                                 final GameFeatures previousState,
                                 final MabParameters currentAction,
                                 final GameFeatures currentState) {

    // Get feature values for previous state (s)
    final TreeMap<String, Double> initialFeatureVector
        = gameOptionFeatureController.extractFeatureVector(PropertyLoader.GAME);

    // delta = r + gamma (Qa'(s')) - Qa(s)
    double delta = stateReward + (GAMMA * getQValueForAction(currentState, currentAction)) - getQValueForAction(previousState, previousAction);

    // Update weights
    updateWeightVectorForAction(previousAction, initialFeatureVector, delta);

  }

  public void updateAfterLastAction(final double stateReward,
                                    final MabParameters previousAction,
                                    final GameFeatures previousState) {


    // delta = r - Qa(s)
    double delta = stateReward - getQValueForAction(previousState, previousAction);

    // Get feature values for previous state (s)
    // fi(s, a)
    final TreeMap<String, Double> featureVector = gameOptionFeatureController.extractFeatureVector(previousState.gameId);

    updateWeightVectorForAction(previousAction, featureVector, delta);
  }

  public MabParameters returnRandomAction() {

    // check for local mabs still unexplored
    List<MabParameters> unexploredLocalMabs = new ArrayList<>();
    final Map<EMetaParameters, MabParameters> localMabs = sampler.localMabs;
    localMabs.values().forEach(localMab -> {
      if (!mabsData.containsKey(localMab)) {
        unexploredLocalMabs.add(localMab);
      }
    });
    if (!unexploredLocalMabs.isEmpty()) {
      final MabParameters mabParameters = unexploredLocalMabs.get(randomGenerator.nextInt(unexploredLocalMabs.size()));
      mabsData.put(mabParameters, new MabData());
      return mabParameters;
    }

    // Return random
    final MabParameters mabParameters = sampler.addRandomSample();
    mabsData.put(mabParameters, new MabData());
    return mabParameters;
  }

  public MabParameters updateAndGetNewMab(final boolean won,
                                          final double reward) {

    // Reward = curr score - previous score
    double stateScore = won ? 1000 + reward : -1000;

    final GameFeatures gameOptions = gameOptionFeatureController.extractGameOptions(PropertyLoader.GAME);

    // First play
    if (previousAction == null) {
      final MabParameters currentAction = selectBestPerceivedAction(gameOptions);

      // a
      previousAction = currentAction;
      // s
      previousState = gameOptions;
      //previousScore = 0;

      return currentAction;
    }

    // Select best action given current q values for (s') / exploration play
    final MabParameters selectedAction = selectBestPerceivedAction(gameOptions);

    // need: s, a r, s', a'
    updateAndGetNewMab(reward, previousAction, previousState, selectedAction, gameOptions);

    // Update last values
    previousAction = selectedAction;
    previousState = gameOptions;
    return selectedAction;
  }

  public double getQValueForAction(final GameFeatures options,
                                   final MabParameters action) {

    // Extract feature array
    final TreeMap<String, Double> featureAfterAction = gameOptionFeatureController.extractFeatureVector(options.gameId);

    // Get related weight vector
    final TreeMap<String, Double> weightVectorForAction = mabsData.get(action).getWeightVectorMap();

    return dotProduct(featureAfterAction, weightVectorForAction);
  }

  public void persistStatisticsAndWeights(final boolean won,
                                          final double score) {

    //final MetaWeights metaWeights = metaWeightsDAO.getMetaWeights(1);
    //metaWeightsDAO.save(metaWeights);

    //persistenceController.updateScoreProgressionLog(score);
    //final int episode = previousResults.update(trainingWeights, won, score);

    //persistenceController.saveScoreProgression(episode);
    //persistenceController.persistWeights(previousResults);
  }


  public MabParameters selectBestPerceivedAction(final GameFeatures options) {

    // Exploration parameter
    int rand = randomGenerator.nextInt(2);
    if (rand <= EXPLORATION_EPSILON) {
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
      }
    }

    // Return random if draw
    if (duplicatedActions.size() > 1) {
      Collections.shuffle(duplicatedActions);
      return duplicatedActions.get(randomGenerator.nextInt(duplicatedActions.size() - 1));
    }

    return bestAction;
  }

  public void initializeTrainingWeightVector() {

    final long idForNow = 1;

    if (mabsData != null) {
      System.out.println("Previous result still in memory");
      return;
    }

    // read file
    //MetaWeights previousData = metaWeightsDAO.getMetaWeights(idForNow);

    // if could not load initialize
    if (previousData == null) {
      //persistenceController.addLog("No previous results, creating new Weights");
      previousData = new HashMap<>();
      this.mabsData = new HashMap<>();
    } else {
      System.out.println("Loaded DB previous weiohts.");
      //this.metaWeights = previousResults;
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


