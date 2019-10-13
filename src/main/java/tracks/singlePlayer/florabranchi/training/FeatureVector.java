package tracks.singlePlayer.florabranchi.training;

import core.game.StateObservation;

public class FeatureVector {

  int totalEnemies;

  int totalNpcs;

  public FeatureVector extractFeatureVector(StateObservation stateObservation) {
    totalEnemies = extract(stateObservation, "enemies");
    return this;
  }

  private int extract(final StateObservation stateObservation,
                      final String enemies) {
    return 0;
  }

  public int getSize() {
    return 0;
  }
}
