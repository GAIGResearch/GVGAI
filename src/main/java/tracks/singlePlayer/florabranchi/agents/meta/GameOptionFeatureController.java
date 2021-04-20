package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public class GameOptionFeatureController {

  private final static Logger LOGGER = Logger.getLogger("GVGAI_BOT");

  private GameOptions gameOptions;

  private static final String TREE_REUSE = "TREE_REUSE";
  private static final String LOSS_AVOIDANCE = "LOSS_AVOIDANCE";
  private static final String RAW_GAME_SCORE = "RAW_GAME_SCORE";
  private static final String EXPAND_ALL_CHILD_NODES = "EXPAND_ALL_CHILD_NODES";
  private static final String SAFETY_PREPRUNNING = "SAFETY_PREPRUNNING";

  private static final String RAW_SCORE_WEIGHT = "RAW_SCORE_WEIGHT";
  private static final String TOTAL_RESOURCES_SCORE_WEIGHT = "TOTAL_RESOURCES_SCORE_WEIGHT";
  private static final String RESOURCE_SCORE_WEIGHT = "RESOURCE_SCORE_WEIGHT";
  private static final String EXPLORATION_SCORE_WEIGHT = "EXPLORATION_SCORE_WEIGHT";
  private static final String MOVABLES_SCORE_WEIGHT = "MOVABLES_SCORE_WEIGHT";
  private static final String PORTALS_SCORE_WEIGHT = "PORTALS_SCORE_WEIGHT";

  public static TreeSet<String> availableProperties = new TreeSet<>();

  public static TreeSet<String> getAvailableProperties() {
    return availableProperties;
  }

  static {
    availableProperties.add(TREE_REUSE);
    availableProperties.add(LOSS_AVOIDANCE);
    availableProperties.add(RAW_GAME_SCORE);
    availableProperties.add(EXPAND_ALL_CHILD_NODES);
    availableProperties.add(SAFETY_PREPRUNNING);

    availableProperties.add(RAW_SCORE_WEIGHT);
    availableProperties.add(TOTAL_RESOURCES_SCORE_WEIGHT);
    availableProperties.add(RESOURCE_SCORE_WEIGHT);
    availableProperties.add(EXPLORATION_SCORE_WEIGHT);
    availableProperties.add(MOVABLES_SCORE_WEIGHT);
    availableProperties.add(PORTALS_SCORE_WEIGHT);
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


    return propertyMap;

  }

  private static String buildPropertyName(String objectType,
                                          String propertyType) {
    return String.format(propertyType, objectType);
  }


}
