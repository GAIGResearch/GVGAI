package tracks.singlePlayer.florabranchi.src;

import core.game.StateObservation;
import ontology.Types;
import tracks.singlePlayer.tools.Heuristics.StateHeuristic;


public class GameStateHeuristic extends StateHeuristic {

  double initialNpcCounter = 0;

  public GameStateHeuristic() {

  }

  public double evaluateState(StateObservation stateObs) {

    double score = 0;
    if (stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS) {
      score = 1000000;
    } else if (stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
      score = -1000000;
    }
    return stateObs.getGameScore();
  }


}


