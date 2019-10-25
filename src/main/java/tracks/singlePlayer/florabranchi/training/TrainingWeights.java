package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import ontology.Types;

public class TrainingWeights implements Serializable {

  private TreeMap<Types.ACTIONS, ArrayList<Double>> weightVectorMap;

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

    for (final Types.ACTIONS avallableGameAction : avallableGameActions) {
      weightVectorMap.put(avallableGameAction, new ArrayList<>(Collections.nCopies(featureSize, 0d)));
    }
  }

  public List<Double> getWeightVectorForAction(final Types.ACTIONS actions) {
    return weightVectorMap.get(actions);
  }

  public TrainingWeights(final TreeMap<Types.ACTIONS, ArrayList<Double>> weightVectorMap) {
    this.weightVectorMap = weightVectorMap;
  }

  public void updateWeightVector(final Types.ACTIONS action,
                                 final int index,
                                 final double newValue) {

    final ArrayList<Double> doubles = weightVectorMap.get(action);

    if (doubles.size() <= index) {
      doubles.add(newValue);
    } else {
      doubles.set(index, newValue);
    }
  }
}
