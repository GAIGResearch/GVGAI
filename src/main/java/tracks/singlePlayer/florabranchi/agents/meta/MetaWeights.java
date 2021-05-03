package tracks.singlePlayer.florabranchi.agents.meta;

import static tracks.singlePlayer.florabranchi.agents.meta.EMetaActions.TREE_REUSE_OFF;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.logging.Logger;

public class MetaWeights implements Serializable {


  private final static Logger LOGGER = Logger.getLogger("MetaWeights");

  private TreeMap<EMetaActions, TreeMap<String, Double>> weightVectorMap;

  public static List<EMetaActions> avallableGameActions = new ArrayList<>();

  static {

    avallableGameActions.add(TREE_REUSE_OFF);

    // what are metabot actions?? "naive"
    // opt a - mais simples
    // bool isBrainman isAliens
    // 0 0
    // 0 1

    // opt 2 -> game characteristic flags
    // 0 0 0
    // 1 0 0
    // 1 1 0 ....etc


  }

  public TreeMap<EMetaActions, TreeMap<String, Double>> getWeightVectorMap() {
    return weightVectorMap;
  }

  public MetaWeights() {
    weightVectorMap = new TreeMap<>();

    TreeMap<String, Double> emptyPropertyMap = new TreeMap<>();
    final Set<String> propertyValueMap = GameOptionFeatureController.getAvailableProperties();
    for (final String property : propertyValueMap) {
      emptyPropertyMap.put(property, 0d);
    }

    for (final EMetaActions metaActions : avallableGameActions) {
      weightVectorMap.put(metaActions, new TreeMap<>(emptyPropertyMap));
    }
  }

  public void updateWeightVector(final EMetaActions action,
                                 final String property,
                                 final double newValue) {

    final TreeMap<String, Double> doubles = weightVectorMap.get(action);
    doubles.put(property, newValue);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MetaWeights.class.getSimpleName() + "[", "]")
        .add("weightVectorMap=" + weightVectorMap)
        .toString();
  }
}
