package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public class GameOptionFeatureController {

  private final static Logger LOGGER = Logger.getLogger("GVGAI_BOT");

  private GameOptions gameOptions;

  private static final String TREE_REUSE = "TREE_REUSE";
  private static final String RAW_GAME_SCORE = "RAW_GAME_SCORE";
  private static final String EXPAND_ALL_CHILD_NODES = "EXPAND_ALL_CHILD_NODES";


  public static TreeSet<String> availableProperties = new TreeSet<>();

  public static TreeSet<String> getAvailableProperties() {
    return availableProperties;
  }

  static {
    availableProperties.add(TREE_REUSE);
    availableProperties.add(RAW_GAME_SCORE);
    availableProperties.add(EXPAND_ALL_CHILD_NODES);
  }

  public double getFeatureNormalizedValue(final String property, double value,
                                          double max) {
    double min = 0;
    //System.out.println(property);
    //System.out.println(String.format("Value %s, Max %s", value, max));
    return (value - min) / (max - min);
  }

  public void addToPropertyMap(final TreeMap<String, Double> propertyMap,
                               final String property,
                               final double value,
                               final double maxValue) {
    final double featureNormalizedValue = getFeatureNormalizedValue(property, value, maxValue);
    if (featureNormalizedValue > 1) {
      System.out.printf("Value %s, Max %s%n", value, maxValue);
      System.out.println("------- ERROR FEATURE VALUE > 1 ");
      System.out.println(property);
      System.out.println(value);
      System.out.println(maxValue);
    }
    propertyMap.put(property, featureNormalizedValue);
  }

  public GameOptionFeatureController(final GameOptions gameOptions) {
    this.gameOptions = gameOptions;
  }

  public TreeMap<String, Double> extractFeatureVector(final GameOptions gameOptions) {

    // Add default values in case of unavalable information
    TreeMap<String, Double> propertyMap = new TreeMap<>();
    availableProperties.forEach(entry -> propertyMap.put(entry, 0d));
    propertyMap.put(TREE_REUSE, gameOptions.reuseTree ? 1d : 0);
    propertyMap.put(RAW_GAME_SCORE, gameOptions.rawGameScore ? 1d : 0);

    return propertyMap;

  }

  private static String buildPropertyName(String objectType,
                                          String propertyType) {
    return String.format(propertyType, objectType);
  }


}
