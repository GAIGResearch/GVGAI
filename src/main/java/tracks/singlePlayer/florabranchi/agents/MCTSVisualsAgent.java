package tracks.singlePlayer.florabranchi.agents;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

public class MCTSVisualsAgent extends MonteCarloTreeAgent {

  public MCTSVisualsAgent(final StateObservation stateObs, final ElapsedCpuTimer elapsedTimer) {

    super(stateObs, elapsedTimer);
    showTree = propertyLoader.SHOW_TREE;
  }

  @Override
  protected boolean displayDebug() {
    return propertyLoader.SHOW_TREE;
  }

  @Override
  protected String getPropertyPath() {
    return "mcts.visuals.properties";
  }
}
