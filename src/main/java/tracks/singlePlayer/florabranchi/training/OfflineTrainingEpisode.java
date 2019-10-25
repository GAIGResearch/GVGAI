package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;

public class OfflineTrainingEpisode implements Serializable {

  int trainingCount;

  TrainingWeights weightVector;

  public int getTrainingCount() {
    return trainingCount;
  }

  public void setTrainingCount(final int trainingCount) {
    this.trainingCount = trainingCount;
  }

  public TrainingWeights getWeightVector() {
    return weightVector;
  }

  public void setWeightVector(final TrainingWeights weightVector) {
    this.weightVector = weightVector;
  }

  public OfflineTrainingEpisode(final int trainingCount,
                                final TrainingWeights weightVector) {
    this.trainingCount = trainingCount;
    this.weightVector = weightVector;
  }
}
