package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;

public class OfflineTrainingEpisode implements Serializable {
  int trainingCount;

  WeightVector weightVector;

  public int getTrainingCount() {
    return trainingCount;
  }

  public void setTrainingCount(final int trainingCount) {
    this.trainingCount = trainingCount;
  }

  public WeightVector getWeightVector() {
    return weightVector;
  }

  public void setWeightVector(final WeightVector weightVector) {
    this.weightVector = weightVector;
  }

  public OfflineTrainingEpisode(final int trainingCount,
                                final WeightVector weightVector) {
    this.trainingCount = trainingCount;
    this.weightVector = weightVector;
  }
}
