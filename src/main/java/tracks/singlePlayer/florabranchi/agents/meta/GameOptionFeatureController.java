package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public class GameOptionFeatureController {

  private final static Logger LOGGER = Logger.getLogger("GVGAI_BOT");

  // game state features
  private static final String IS_DETERMINISTIC = "IS_DETERMINISTIC";
  private static final String CAN_USE = "CAN_USE";
  private static final String CAN_DIE = "CAN_DIE";
  private static final String IS_SURVIVAL = "IS_SURVIVAL";

  public static TreeSet<String> availableProperties = new TreeSet<>();

  public static TreeSet<String> getAvailableProperties() {
    return availableProperties;
  }

  static {
    availableProperties.add(IS_DETERMINISTIC);
    availableProperties.add(CAN_USE);
    availableProperties.add(CAN_DIE);
    availableProperties.add(IS_SURVIVAL);
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

  public GameOptionFeatureController() {

  }

  public TreeMap<String, Double> extractFeatureVector(final int gameId) {

    final GameFeatures gameFeatures = GameFeaturesHelper.getGameFeatures(gameId);

    // Add default values in case of unavalable information
    TreeMap<String, Double> propertyMap = new TreeMap<>();
    availableProperties.forEach(entry -> propertyMap.put(entry, 0d));
    propertyMap.put(IS_DETERMINISTIC, gameFeatures.isDeterministic ? 1d : 0);
    propertyMap.put(CAN_USE, gameFeatures.canUse ? 1d : 0);
    propertyMap.put(CAN_DIE, gameFeatures.canDie ? 1d : 0);
    propertyMap.put(IS_SURVIVAL, gameFeatures.isSurvival ? 1d : 0);

    return propertyMap;

  }

  public GameFeatures extractGameOptions(final int gameId) {

    return GameFeaturesHelper.getGameFeatures(gameId);

  }

  private static String buildPropertyName(String objectType,
                                          String propertyType) {
    return String.format(propertyType, objectType);
  }


}
