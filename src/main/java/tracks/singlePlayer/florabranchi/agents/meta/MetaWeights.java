package tracks.singlePlayer.florabranchi.agents.meta;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.logging.Logger;

public class MetaWeights implements Serializable {

  public int currentVersion = 1;

  private final static Logger LOGGER = Logger.getLogger("MetaWeights");

  private TreeMap<EMetaParameters, TreeMap<String, Double>> weightVectorMap;

  public static List<EMetaParameters> availableParameters = new ArrayList<>();

  static {
    availableParameters.addAll(Arrays.asList(EMetaParameters.values()));
  }

  public TreeMap<EMetaParameters, TreeMap<String, Double>> getWeightVectorMap() {
    return weightVectorMap;
  }

  public MetaWeights() {


    for (final EMetaParameters metaActions : availableParameters) {
      weightVectorMap.put(metaActions, new TreeMap<>(emptyPropertyMap));
    }
  }



  @Override
  public String toString() {
    return new StringJoiner(", ", MetaWeights.class.getSimpleName() + "[", "]")
        .add("weightVectorMap=" + weightVectorMap)
        .toString();
  }
}
