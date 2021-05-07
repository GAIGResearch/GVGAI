package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.StringJoiner;
import java.util.TreeMap;

public class MabData {

  public double averageReward;

  public int timesSelected;

  public double marginalizedAvgScoreForoParameter;

  public double timesParameterSelected;

  public TreeMap<String, Double> getWeightVectorMap() {
    return weightVectorMap;
  }

  // map of feature to weight
  // features are game infos
  private TreeMap<String, Double> weightVectorMap;

  public MabData() {
    weightVectorMap = new TreeMap<>();

    TreeMap<GameFeaturesHelper.EGameFeatures, Double> emptyPropertyMap = new TreeMap<>();
    final GameFeaturesHelper.EGameFeatures[] propertyValueMap = GameFeaturesHelper.EGameFeatures.values();
    for (final GameFeaturesHelper.EGameFeatures property : propertyValueMap) {
      emptyPropertyMap.put(property, 0d);
    }
  }

  public void updateWeightVector(final String property,
                                 final double newValue) {
    weightVectorMap.put(property, newValue);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MabData.class.getSimpleName() + "[", "]")
        .add("averageReward=" + averageReward)
        .add("timesSelected=" + timesSelected)
        .add("marginalizedAvgScoreForoParameter=" + marginalizedAvgScoreForoParameter)
        .add("timesParameterSelected=" + timesParameterSelected)
        .toString();
  }
}
