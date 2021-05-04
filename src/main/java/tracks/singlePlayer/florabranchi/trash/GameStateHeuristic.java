package tracks.singlePlayer.florabranchi.trash;

import core.game.StateObservation;
import ontology.Types;
import tracks.singlePlayer.tools.Heuristics.StateHeuristic;


public class GameStateHeuristic extends StateHeuristic {

  double initialNpcCounter = 0;

  double enemyDeathReward = 200;

  public GameStateHeuristic() {

  }

  public double evaluateState(StateObservation stateObs,
                              StateObservation updatedState) {

    int killedEnemies = 1;
    //int killedEnemies = stateObs.getNPCPositions().length - updatedState.getNPCPositions().length;
    System.out.println(String.format("Dead enemies: %s", killedEnemies));
    final double v = killedEnemies * enemyDeathReward;

    return v + evaluateState(stateObs);
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


