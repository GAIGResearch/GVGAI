package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import tracks.singlePlayer.florabranchi.database.BanditArmsDataDAO;
import tracks.singlePlayer.florabranchi.database.BanditsArmDTO;
import tracks.singlePlayer.florabranchi.database.CombinatorialMabAgentResult;
import tracks.singlePlayer.florabranchi.database.CombinatorialMabAgentResultDAO;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

// This agent considers that the inputs (state) is always the same
public class CombinatorialMABAgent {

  public static String gameName;

  public static double minDouble = Math.pow(10, -3);

  public double ALFA;
  public double GAMMA;
  public double EXPLORATION_EPSILON;

  public PropertyLoader propertyLoader;

  protected final static Logger LOGGER = Logger.getLogger("MetaMCTSAgent");

  protected Random randomGenerator = new Random();

  MabParameters previousAction;

  MultiArmedNaiveSampler sampler;

  BanditArmsDataDAO banditArmsDataDAO = new BanditArmsDataDAO();

  CombinatorialMabAgentResultDAO combinatorialMabAgentResultDAO = new CombinatorialMabAgentResultDAO();

  BanditsArmDTO banditsArmDTO;

  protected String getPropertyPath() {
    return "sarsa.properties";
  }

  public CombinatorialMABAgent() {

    try {
      propertyLoader = new PropertyLoader(getPropertyPath());

    } catch (IOException ex) {
      LOGGER.severe("Error loading properties");
    }

    ALFA = propertyLoader.SARSA_ALFA;
    GAMMA = propertyLoader.SARSA_GAMMA;
    EXPLORATION_EPSILON = propertyLoader.SARSA_EPSILON;

    banditsArmDTO = banditArmsDataDAO.getBanditArmsData(1);
    if (banditsArmDTO != null) {
      sampler = new MultiArmedNaiveSampler(banditsArmDTO.object);
    } else {
      banditsArmDTO = new BanditsArmDTO();
      sampler = new MultiArmedNaiveSampler();
      banditArmsDataDAO.saveBandit(banditsArmDTO);
    }

    if (previousAction == null) {
      System.out.println("first play");
      final MabParameters currentAction = selectBestPerceivedAction();

      System.out.println("First status:");
      System.out.println(currentAction);

      // a
      previousAction = currentAction;
      updateGlobalProperties(currentAction);
    }

  }

  private void updateGlobalProperties(MabParameters result) {
    PropertyLoader.EARLY_INITIALIZATION = result.getParameter(EMetaParameters.EARLY_INITIALIZATION);
    PropertyLoader.RAW_GAME_SCORE = result.getParameter(EMetaParameters.RAW_GAME_SCORE);
    PropertyLoader.MACRO_ACTIONS = result.getParameter(EMetaParameters.MACRO_ACTIONS);
    PropertyLoader.SELECT_HIGHEST_SCORE_CHILD = result.getParameter(EMetaParameters.SELECT_HIGHEST_SCORE_CHILD);
    PropertyLoader.TREE_REUSE = result.getParameter(EMetaParameters.TREE_REUSE);

    System.out.println("Properties set " + result);
  }

  public void result(final double score,
                     final boolean won) {

    gameName = PropertyLoader.GAME_NAME;
    final CombinatorialMabAgentResult baseMonteCarloResult = new CombinatorialMabAgentResult();
    baseMonteCarloResult.agent = CombinatorialMabAgentResult.class.getSimpleName();
    baseMonteCarloResult.gameName = gameName;
    baseMonteCarloResult.gameLevel = PropertyLoader.LEVEL;
    baseMonteCarloResult.finalScore = score;
    baseMonteCarloResult.won = won;
    baseMonteCarloResult.treeReuse = PropertyLoader.TREE_REUSE;
    baseMonteCarloResult.rawGameScore = PropertyLoader.RAW_GAME_SCORE;
    baseMonteCarloResult.macroActions = PropertyLoader.MACRO_ACTIONS;
    baseMonteCarloResult.lossAvoidance = PropertyLoader.LOSS_AVOIDANCE;
    baseMonteCarloResult.earlyInitialization = PropertyLoader.EARLY_INITIALIZATION;
    baseMonteCarloResult.selectHighestScoreChild = PropertyLoader.SELECT_HIGHEST_SCORE_CHILD;
    combinatorialMabAgentResultDAO.save(baseMonteCarloResult);
  }

  public MabParameters act(final double reward,
                           final boolean won) {

    double finalReward = reward;
    if (!won) {
      finalReward = minDouble;
    } else {
      finalReward += 1000;
    }

    // update expected reward
    sampler.updateMabData(previousAction, finalReward);
    banditsArmDTO.object = sampler.banditArmsData;
    banditArmsDataDAO.updateBandit(banditsArmDTO);

    // Select best action given current q values for (s') / exploration play
    final MabParameters selectedAction = selectBestPerceivedAction();

    // Update last values
    previousAction = selectedAction;
    System.out.println("next mab:" + selectedAction);

    updateGlobalProperties(selectedAction);
    return selectedAction;
  }

  public MabParameters returnRandomAction() {
    return sampler.addRandomSample();
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


