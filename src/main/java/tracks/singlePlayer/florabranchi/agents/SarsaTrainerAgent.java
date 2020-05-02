package tracks.singlePlayer.florabranchi.agents;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

public class SarsaTrainerAgent extends SarsaAgent {


  public SarsaTrainerAgent(final StateObservation stateObs,
                           final ElapsedCpuTimer elapsedTimer) {
    super(stateObs, elapsedTimer);
  }

  @Override
  protected boolean displayDebug() {
    return false;
  }

  @Override
  protected String getPropertyPath() {
    return "sarsa.trainer.properties";
  }
}
