package tracks.singlePlayer.florabranchi.agents;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import tracks.singlePlayer.florabranchi.agents.meta.EMetaParameters;
import tracks.singlePlayer.florabranchi.agents.meta.MabParameters;
import tracks.singlePlayer.florabranchi.agents.meta.MultiArmedNaiveSampler;
import tracks.singlePlayer.florabranchi.database.BanditArmsDataDAO;
import tracks.singlePlayer.florabranchi.database.BanditsArmDTO;
import tracks.singlePlayer.florabranchi.database.CombinatorialMabAgentResult;
import tracks.singlePlayer.florabranchi.database.CombinatorialMabAgentResultDAO;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

// This agent considers that the inputs (state) is always the same
public class CombinatorialMABAgent {

  public static String gameName;

  public double ALFA = 0.3;
  public double GAMMA = 0.9;
  public double EXPLORATION_EPSILON = 20;

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

    banditsArmDTO = banditArmsDataDAO.getBanditArmsDataForGame(PropertyLoader.GAME_NAME);
    if (banditsArmDTO != null) {
      sampler = new MultiArmedNaiveSampler(banditsArmDTO.object);
    } else {
      banditsArmDTO = new BanditsArmDTO();
      banditsArmDTO.game = PropertyLoader.GAME_NAME;

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
    PropertyLoader.LOSS_AVOIDANCE = result.getParameter(EMetaParameters.LOSS_AVOIDANCE);

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

    baseMonteCarloResult.avgNodesExplored = PropertyLoader.AVERAGE_NODES;
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
      finalReward = 0;
    } else {
      finalReward += 100;
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

  public MabParameters selectBestPerceivedAction() {

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      System.out.println("selecting exploration play --------------------------------");
      return sampler.exploreMabs();
    }

    System.out.println("selecting exploitation play -------------------------------");

    return sampler.exploitMabs();
  }


}


