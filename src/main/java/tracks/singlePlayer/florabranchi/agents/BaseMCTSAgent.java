
package tracks.singlePlayer.florabranchi.agents;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.database.BaseMonteCarloResult;
import tracks.singlePlayer.florabranchi.database.BaseMonteCarloResultDAO;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

public class BaseMCTSAgent extends ParametrizedMonteCarloTreeAgent {

  BaseMonteCarloResultDAO dao = new BaseMonteCarloResultDAO();


  public BaseMCTSAgent(final StateObservation stateObs, final ElapsedCpuTimer elapsedTimer) {
    super(stateObs, elapsedTimer);
    gameName = PropertyLoader.GAME_NAME;
  }

  @Override
  public Types.ACTIONS act(final StateObservation stateObs, final ElapsedCpuTimer elapsedTimer) {
    return super.act(stateObs, elapsedTimer);
  }

  @Override
  public void result(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedCpuTimer) {

    gameName = PropertyLoader.GAME_NAME;

    showTree = PropertyLoader.SHOW_TREE;
    SHALLOW_ROLLOUT = PropertyLoader.SHALLOW_ROLLOUT;
    ROLLOUT_DEPTH = SHALLOW_ROLLOUT ? 1 : 10;

    final BaseMonteCarloResult baseMonteCarloResult = new BaseMonteCarloResult();
    baseMonteCarloResult.agent = PropertyLoader.AGENT;
    baseMonteCarloResult.gameName = gameName;
    baseMonteCarloResult.gameLevel = PropertyLoader.LEVEL;
    baseMonteCarloResult.finalScore = stateObs.getGameScore();
    baseMonteCarloResult.won = stateObs.getGameWinner().equals(Types.WINNER.PLAYER_WINS);
    baseMonteCarloResult.avgNodesExplored = (int) totalNodes.stream().mapToInt(val -> val).average().orElse(0);
    baseMonteCarloResult.rawGameScore = PropertyLoader.RAW_GAME_SCORE;
    baseMonteCarloResult.macroActions = PropertyLoader.MACRO_ACTIONS;
    baseMonteCarloResult.lossAvoidance = PropertyLoader.LOSS_AVOIDANCE;
    baseMonteCarloResult.earlyInitialization = PropertyLoader.EARLY_INITIALIZATION;
    baseMonteCarloResult.selectHighestScoreChild = PropertyLoader.SELECT_HIGHEST_SCORE_CHILD;
    dao.save(baseMonteCarloResult);
  }


  @Override
  protected String getPropertyPath() {
    return "base.mcts.properties";
  }
}

