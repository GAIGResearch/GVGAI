package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import ontology.Types;

public class TrainingWeights implements Serializable {

  private TreeMap<Types.ACTIONS, TreeMap<String, Double>> weightVectorMap;

  private static List<Types.ACTIONS> avallableGameActions = new ArrayList<>();

  static {
    avallableGameActions.add(Types.ACTIONS.ACTION_NIL);
    avallableGameActions.add(Types.ACTIONS.ACTION_LEFT);
    avallableGameActions.add(Types.ACTIONS.ACTION_RIGHT);
    avallableGameActions.add(Types.ACTIONS.ACTION_UP);
    avallableGameActions.add(Types.ACTIONS.ACTION_DOWN);
    avallableGameActions.add(Types.ACTIONS.ACTION_USE);
    avallableGameActions.add(Types.ACTIONS.ACTION_ESCAPE);
  }

  public static List<Types.ACTIONS> getAvallableGameActions() {
    return avallableGameActions;
  }

  public TrainingWeights(final int featureSize) {
    weightVectorMap = new TreeMap<>();

    TreeMap<String, Double> emptyPropertyMap = new TreeMap<>();
    final Set<String> propertyValueMap = FeatureVectorController.getAvailableProperties();
    for (final String property : propertyValueMap) {
      emptyPropertyMap.put(property, 0d);
    }

    for (final Types.ACTIONS avallableGameAction : avallableGameActions) {
      weightVectorMap.put(avallableGameAction, new TreeMap<>(emptyPropertyMap));
    }
  }

  public TreeMap<String, Double> getWeightVectorForAction(final Types.ACTIONS actions) {
    return weightVectorMap.get(actions);
  }

  public TrainingWeights(final TreeMap<Types.ACTIONS, TreeMap<String, Double>> weightVectorMap) {
    this.weightVectorMap = weightVectorMap;
  }

  public void updateWeightVector(final Types.ACTIONS action,
                                 final String property,
                                 final double newValue) {

    final TreeMap<String, Double> doubles = weightVectorMap.get(action);
    doubles.put(property, newValue);
  }
}
