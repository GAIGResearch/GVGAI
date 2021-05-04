
package tracks.singlePlayer.florabranchi.agents;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

public class BaseMCTSAgent extends ParametrizedMonteCarloTreeAgent {

  public BaseMCTSAgent(final StateObservation stateObs, final ElapsedCpuTimer elapsedTimer) {
    super(stateObs, elapsedTimer);
  }

  @Override
  public void result(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedCpuTimer) {

  }

  @Override
  protected String getPropertyPath() {
    return "base.mcts.properties";
  }
}

