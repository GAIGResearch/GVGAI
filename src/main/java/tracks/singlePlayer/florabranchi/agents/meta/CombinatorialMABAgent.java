package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import tracks.singlePlayer.florabranchi.database.BanditArmsData;
import tracks.singlePlayer.florabranchi.database.BanditArmsDataDAO;
import tracks.singlePlayer.florabranchi.database.BanditsArmDTO;
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

  MabParameters previousAction;

  MultiArmedNaiveSampler sampler;

  BanditArmsDataDAO banditArmsDataDAO = new BanditArmsDataDAO();

  public CombinatorialMABAgent() {

    try {
      propertyLoader = new PropertyLoader(getPropertyPath());

    } catch (IOException ex) {
      LOGGER.severe("Error loading properties");
    }

    ALFA = propertyLoader.SARSA_ALFA;
    GAMMA = propertyLoader.SARSA_GAMMA;
    EXPLORATION_EPSILON = propertyLoader.SARSA_EPSILON;

    BanditsArmDTO banditArmsData = banditArmsDataDAO.getMetaWeights(1);
    BanditArmsData newData = new BanditArmsData(banditArmsData.object.armDataList, banditArmsData.object.localArmData);
    if (banditArmsData != null) {
      sampler = new MultiArmedNaiveSampler(newData);
    } else {
      sampler = new MultiArmedNaiveSampler();
      banditArmsDataDAO.saveBandit(new BanditsArmDTO(newData));
    }

    gameOptionFeatureController = new GameOptionFeatureController();

    if (previousAction == null) {
      System.out.println("first play");
      final MabParameters currentAction = selectBestPerceivedAction();

      System.out.println(currentAction);

      // a
      previousAction = currentAction;
      setProperties(currentAction);
    }

  }

  private void setProperties(MabParameters result) {
    PropertyLoader.EARLY_INITIALIZATION = result.getParameter(EMetaParameters.EARLY_INITIALIZATION);
    PropertyLoader.RAW_GAME_SCORE = result.getParameter(EMetaParameters.RAW_GAME_SCORE);
    PropertyLoader.MACRO_ACTIONS = result.getParameter(EMetaParameters.MACRO_ACTIONS);
    PropertyLoader.SELECT_HIGHEST_SCORE_CHILD = result.getParameter(EMetaParameters.SELECT_HIGHEST_SCORE_CHILD);
    PropertyLoader.TREE_REUSE = result.getParameter(EMetaParameters.TREE_REUSE);

    System.out.println("Properties set" + result);
  }

  public MabParameters act(final double reward) {

    // First play
    if (previousAction == null) {
      System.out.println("first play");
      final MabParameters currentAction = selectBestPerceivedAction();

      System.out.println(currentAction);

      // a
      previousAction = currentAction;
      // s;
      return currentAction;
    }

    // update expected reward
    sampler.updateMabData(previousAction, reward);

    sampler.updateBanditArms();
    banditArmsDataDAO.saveBandit(new BanditsArmDTO(sampler.banditArmsData));

    // Select best action given current q values for (s') / exploration play
    final MabParameters selectedAction = selectBestPerceivedAction();

    // Update last values
    previousAction = selectedAction;
    System.out.println("next mab:" + selectedAction);
    return selectedAction;
  }

  protected boolean displayDebug() {
    return true;
  }

  protected String getPropertyPath() {
    return "sarsa.properties";
  }

  public MabParameters returnRandomAction() {
    final MabParameters mabParameters = sampler.addRandomSample();
    return mabParameters;
  }

  public MabParameters selectBestPerceivedAction() {

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      System.out.println("selecting exploration play --------------------------------");
      return returnRandomAction();
    }

    System.out.println("selecting exploitation play -------------------------------");
    double maxValue = -Double.MAX_VALUE;

    final MabParameters mabParameters = sampler.exploitMabs();
    return mabParameters;
  }


}


