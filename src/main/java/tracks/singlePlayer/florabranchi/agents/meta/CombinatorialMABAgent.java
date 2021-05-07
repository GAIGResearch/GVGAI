package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

// This agent considers that the inputs (state) is always the same
public class CombinatorialMABAgent {

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

  MultiArmedNaiveSampler sampler = new MultiArmedNaiveSampler();

  public CombinatorialMABAgent() {

    try {
      propertyLoader = new PropertyLoader(getPropertyPath());

    } catch (IOException ex) {
      LOGGER.severe("Error loading properties");
    }

    ALFA = propertyLoader.SARSA_ALFA;
    GAMMA = propertyLoader.SARSA_GAMMA;
    EXPLORATION_EPSILON = propertyLoader.SARSA_EPSILON;

    gameOptionFeatureController = new GameOptionFeatureController();

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

    // First play
    if (previousAction == null) {
      final MabParameters currentAction = selectBestPerceivedAction();

      // a
      previousAction = currentAction;
      // s;
      return currentAction;
    }

    // update expected reward
    final MabData mabData = mabsData.get(previousAction);
    mabData.timesSelected++;
    mabData.totalRewards += reward;


    // Select best action given current q values for (s') / exploration play
    final MabParameters selectedAction = selectBestPerceivedAction();

    // Update last values
    previousAction = selectedAction;
    previousData = mabsData;
    return selectedAction;
  }

  protected boolean displayDebug() {
    return true;
  }

  protected String getPropertyPath() {
    return "sarsa.properties";
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

  public MabParameters selectBestPerceivedAction() {

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      System.out.println("selecting exploration play");
      return returnRandomAction();
    }

    System.out.println("selecting exploitation play");
    double maxValue = -Double.MAX_VALUE;
    MabParameters bestAction = null;

    List<MabParameters> duplicatedActions = new ArrayList<>();

    for (MabParameters action : mabsData.keySet()) {
      final MabData mabData = mabsData.get(action);
      final double actionExpectedReward = mabData.timesSelected == 0 ? 0
          : mabData.totalRewards / mabData.timesSelected;
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


}


