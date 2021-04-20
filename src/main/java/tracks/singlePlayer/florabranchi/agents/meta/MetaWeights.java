package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import ontology.Types;

public class MetaWeights {

  private final static Logger LOGGER = Logger.getLogger("MetaWeights");

  private TreeMap<Types.ACTIONS, TreeMap<String, Double>> weightVectorMap;

  private static List<Types.ACTIONS> avallableGameActions = new ArrayList<>();

  private final String IS_DETERMINISTIC = "IS_det";

  static {
    // what are metabot actions?? "naive"


    avallableGameActions.add(Types.ACTIONS.ACTION_NIL);
    avallableGameActions.add(Types.ACTIONS.ACTION_LEFT);
    avallableGameActions.add(Types.ACTIONS.ACTION_RIGHT);
    avallableGameActions.add(Types.ACTIONS.ACTION_UP);
    avallableGameActions.add(Types.ACTIONS.ACTION_DOWN);
    avallableGameActions.add(Types.ACTIONS.ACTION_USE);
    avallableGameActions.add(Types.ACTIONS.ACTION_ESCAPE);


    // opt a - mais simples
    // bool isBrainman isAliens
    // 0 0
    // 0 1




    // opt 2 -> game characteristic flags
    // 0 0 0
    // 1 0 0
    // 1 1 0 ....etc





  }

  public TreeMap<Types.ACTIONS, TreeMap<String, Double>> getWeightVectorMap() {
    return weightVectorMap;
  }

  public MetaWeights(final int featureSize) {
    weightVectorMap = new TreeMap<>();

    TreeMap<String, Double> emptyPropertyMap = new TreeMap<>();
    final Set<String> propertyValueMap = GameOptionFeatureController.getAvailableProperties();
    for (final String property : propertyValueMap) {
      emptyPropertyMap.put(property, 0d);
    }

    for (final Types.ACTIONS avallableGameAction : avallableGameActions) {
      weightVectorMap.put(avallableGameAction, new TreeMap<>(emptyPropertyMap));
    }
  }
}
