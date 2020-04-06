package tracks.singlePlayer.florabranchi.persistence.weights;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import tracks.singlePlayer.florabranchi.training.PossibleHarmfulSprite;

public class OfflineTrainerResults implements Serializable {

  class HarmfulObjectData {

    public int category;
    public int type;
  }

  private static final long serialVersionUID = 1L;

  public TrainingWeights weightVector;

  public int totalGames;

  public int wins;

  public double totalScore;

  public Map<Integer, Double> episodeTotalScoreMap = new HashMap<>();

  public int getTotalGames() {
    return totalGames;
  }

  public int getWins() {
    return wins;
  }

  public void addNewHarmfulSprite(final PossibleHarmfulSprite spriteId) {
    weightVector.addNewHarmfulElement(spriteId);
  }

  public double getTotalScore() {
    return totalScore;
  }

  public Map<Integer, Double> getEpisodeTotalScoreMap() {
    return episodeTotalScoreMap;
  }


  @Override
  public String toString() {
    return new StringJoiner(", ", OfflineTrainerResults.class.getSimpleName() + "[", "]")
        .add("totalGames=" + totalGames)
        .add("wins=" + wins)
        .add("episodeTotalScoreMap=" + episodeTotalScoreMap)
        .toString();
  }


  public TrainingWeights getWeightVector() {
    return weightVector;
  }

  public void setWeightVector(final TrainingWeights weightVector) {
    this.weightVector = weightVector;
  }

  public OfflineTrainerResults(final int featureVectorSize) {
    weightVector = new TrainingWeights(featureVectorSize);
  }

  public int update(final TrainingWeights trainingWeights,
                    final boolean won,
                    final double score) {
    totalGames++;

    totalScore += score;

    episodeTotalScoreMap.put(totalGames, score);

    if (won) {
      wins++;
    }

    this.weightVector = trainingWeights;
    return totalGames;
  }
}
