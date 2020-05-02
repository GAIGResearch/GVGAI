package tracks.singlePlayer.florabranchi.agents;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

public class MCTSVisualsAgent extends MonteCarloTreeAgent {

  public MCTSVisualsAgent(final StateObservation stateObs, final ElapsedCpuTimer elapsedTimer) {
    super(stateObs, elapsedTimer);
  }

  @Override
  protected boolean displayDebug() {
    return false;
  }

  @Override
  protected String getPropertyPath() {
    return "mcts.visuals.properties";
  }
}
