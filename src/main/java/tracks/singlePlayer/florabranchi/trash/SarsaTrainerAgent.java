package tracks.singlePlayer.florabranchi.trash;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.trash.SarsaAgent;

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
    return "depr/sarsa.trainer.properties";
  }
}
