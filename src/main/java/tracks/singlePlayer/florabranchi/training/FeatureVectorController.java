package tracks.singlePlayer.florabranchi.training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

public class FeatureVectorController {

  class ObservableData {

    double maxDistance = 0;
    double minDistance = 0;
    double totalDist = 0;
    double totalEnemies;
    Observation furtherObject;
    Observation closerObject;

    public ObservableData(final double maxDistance,
                          final double minDistance,
                          final double totalEnemies,
                          final double totalDist,
                          final Observation furtherObject,
                          final Observation closerObject) {
      this.maxDistance = maxDistance;
      this.minDistance = minDistance;
      this.totalDist = totalDist;
      this.furtherObject = furtherObject;
      this.closerObject = closerObject;
      this.totalEnemies = totalEnemies;
    }
  }

  private static final String WORLD_HEIGHT = "WORLD_HEIGHT";
  private static final String WORLD_WIDTH = "WORLD_WIDTH";

  private static final String GAME_STATE = "GAME_STATE";
  private static final String GAME_TICK = "GAME_TICK";
  private static final String GAME_SCORE = "GAME_SCORE";

  private static final String PLAYER_RESOURCES = "PLAYER_RESOURCES";
  private static final String PLAYER_X = "PLAYER_X";
  private static final String PLAYER_Y = "PLAYER_Y";
  private static final String AVATAR_HEALTH = "AVATAR_HEALTH";


  // Observables
  private static final String NPCS = "NPC";
  private static final String IMMOVABLES = "IMMOVABLES";
  private static final String MOVABLES = "MOVABLES";

  private static final String TOTAL_OBSERVABLE = "TOTAL_%s";
  private static final String TOTAL_OBSERVABLE_DISTANCE = "TOTAL_%s_DST";
  private static final String CLOSEST_OBSERVABLE_TYPE = "CLOSEST_%s_TYPE";
  private static final String CLOSEST_OBSERVABLE_X = "CLOSEST_%s_X";
  private static final String CLOSEST_OBSERVABLE_Y = "CLOSEST_%s_Y";
  private static final String AVERAGE_DISTANCE_TO_OBSERVABLE = "AVG_DST_TO_%s";


  private static final String AVAILABLE_RESOURCES = "AVAILABLE_RESOURCES";

  private static final String CREATED_SPRITES = "CREATED_SPRITES";
  private static final String CLOSEST_SPRITE_X = "CLOSEST_SPRITE_X";
  private static final String CLOSEST_SPRITE_Y = "CLOSEST_SPRITE_Y";


  public static TreeSet<String> availableProperties = new TreeSet<>();

  public static Set<String> getAvailableProperties() {
    return availableProperties;
  }

  public TreeMap<String, Double> getPropertyValueMap() {
    return propertyValueMap;
  }

  private static String buildPropertyName(String objectType,
                                          String propertyType) {
    return String.format(propertyType, objectType);
  }

  private TreeMap<String, Double> propertyValueMap = new TreeMap<>();

  static {
    availableProperties.add(GAME_STATE);
    availableProperties.add(GAME_SCORE);

    availableProperties.add(AVATAR_HEALTH);
    availableProperties.add(GAME_TICK);
    availableProperties.add(WORLD_HEIGHT);
    availableProperties.add(WORLD_WIDTH);
    availableProperties.add(AVAILABLE_RESOURCES);
    availableProperties.add(PLAYER_RESOURCES);
    availableProperties.add(PLAYER_X);
    availableProperties.add(PLAYER_Y);
    availableProperties.add(CREATED_SPRITES);
    availableProperties.add(CLOSEST_SPRITE_X);
    availableProperties.add(CLOSEST_SPRITE_Y);

    addGeneratedProperties();
  }

  public static void addGeneratedProperties() {
    addGeneratedSpecificProperties(NPCS);
    addGeneratedSpecificProperties(MOVABLES);
    addGeneratedSpecificProperties(IMMOVABLES);
  }

  public static void addGeneratedSpecificProperties(final String type) {
    availableProperties.add(buildPropertyName(type, TOTAL_OBSERVABLE));
    availableProperties.add(buildPropertyName(type, CLOSEST_OBSERVABLE_X));
    availableProperties.add(buildPropertyName(type, CLOSEST_OBSERVABLE_Y));
    availableProperties.add(buildPropertyName(type, CLOSEST_OBSERVABLE_TYPE));
    availableProperties.add(buildPropertyName(type, AVERAGE_DISTANCE_TO_OBSERVABLE));
  }

  public void addObservableObjectProperties(final String type,
                                            final ArrayList<Observation>[] objects,
                                            final Vector2d avatarPosition,
                                            final TreeMap<String, Double> propertyMap,
                                            final double maxDistance) {
    final ObservableData observableData = getObservableData(objects, avatarPosition);
    addToPropertyMap(propertyMap, buildPropertyName(type, TOTAL_OBSERVABLE), observableData.totalEnemies, 50);
    addToPropertyMap(propertyMap, buildPropertyName(type, CLOSEST_OBSERVABLE_X), observableData.closerObject.position.x, maxDistance);
    addToPropertyMap(propertyMap, buildPropertyName(type, CLOSEST_OBSERVABLE_Y), observableData.closerObject.position.y, maxDistance);
    addToPropertyMap(propertyMap, buildPropertyName(type, CLOSEST_OBSERVABLE_TYPE), observableData.closerObject.itype, 10);
    addToPropertyMap(propertyMap, buildPropertyName(type, AVERAGE_DISTANCE_TO_OBSERVABLE),
        observableData.totalDist / observableData.totalDist, maxDistance);
  }

  public TreeMap<String, Double> extractFeatureVector(StateObservation stateObservation) {

    // Add default values in case of unavalable information
    TreeMap<String, Double> propertyMap = new TreeMap<>();
    availableProperties.forEach(entry -> propertyMap.put(entry, 0d));

    // World related Fixed properties
    double worldHeight = stateObservation.getWorldDimension().getHeight();
    double worldWidth = stateObservation.getWorldDimension().getWidth();

    // World properties
    addToPropertyMap(propertyMap, WORLD_HEIGHT, worldHeight, 1000);
    addToPropertyMap(propertyMap, WORLD_WIDTH, worldWidth, 1000);

    int gameTick = stateObservation.getGameTick();
    addToPropertyMap(propertyMap, GAME_TICK, (double) gameTick, 1000);

    double gameScore = stateObservation.getGameScore();
    addToPropertyMap(propertyMap, GAME_SCORE, gameScore, 1000);

    // Avatar position
    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    addToPropertyMap(propertyMap, PLAYER_X, avatarPosition.x, worldWidth);
    addToPropertyMap(propertyMap, PLAYER_Y, avatarPosition.y, worldHeight);


    // Get Avatar created sprites
    final ArrayList<Observation>[] fromAvatarSpritesPositions = stateObservation.getFromAvatarSpritesPositions(avatarPosition);
    if (fromAvatarSpritesPositions != null) {

      int createdSprintes = fromAvatarSpritesPositions.length;
      addToPropertyMap(propertyMap, CREATED_SPRITES, createdSprintes, 10);
      if (fromAvatarSpritesPositions.length > 0) {
        final Observation closestSprite = fromAvatarSpritesPositions[0].get(fromAvatarSpritesPositions[0].size() - 1);
        addToPropertyMap(propertyMap, CLOSEST_SPRITE_X, closestSprite.position.x, worldWidth);
        addToPropertyMap(propertyMap, CLOSEST_SPRITE_Y, closestSprite.position.y, worldHeight);
      }
    }

    int avatarHealth = stateObservation.getAvatarHealthPoints();
    int totalAvatarHealth = stateObservation.getAvatarLimitHealthPoints();
    double avatarRemainingLife = avatarHealth == 0 ? 100 : (double) totalAvatarHealth / avatarHealth;
    addToPropertyMap(propertyMap, AVATAR_HEALTH, avatarRemainingLife, totalAvatarHealth);

    // Observables parsing
    double maxDistance = worldWidth * worldHeight;

    // NPCS
    final ArrayList<Observation>[] npcPositions = stateObservation.getNPCPositions();
    if (npcPositions != null && npcPositions.length > 0) {
      addObservableObjectProperties(NPCS, npcPositions, avatarPosition, propertyMap, maxDistance);
    }

    // IMMOVABLES
    final ArrayList<Observation>[] immovablePositions = stateObservation.getImmovablePositions();
    if (immovablePositions != null) {
      addObservableObjectProperties(NPCS, immovablePositions, avatarPosition, propertyMap, maxDistance);
    }

    // MOVABLES
    final ArrayList<Observation>[] movablePositions = stateObservation.getMovablePositions();
    if (movablePositions != null) {
      addObservableObjectProperties(NPCS, movablePositions, avatarPosition, propertyMap, maxDistance);
    }

    double totalResourcs = 0;
    final ArrayList<Observation>[] resourcesPositions = stateObservation.getResourcesPositions();
    if (resourcesPositions != null) {
      totalResourcs = resourcesPositions.length;
    }
    addToPropertyMap(propertyMap, AVAILABLE_RESOURCES, totalResourcs, 10);

    double totalPlayerResources = 0;
    final HashMap<Integer, Integer> avatarResources = stateObservation.getAvatarResources();
    if (avatarResources != null) {
      totalPlayerResources = avatarResources.size();
    }
    addToPropertyMap(propertyMap, PLAYER_RESOURCES, totalPlayerResources, 10);


    return propertyMap;
  }


  public ObservableData getObservableData(final ArrayList<Observation>[] objectsList,
                                          final Vector2d avatarPosition) {
    double totalEnemies = 0;
    double maxDistance = 0;
    double minDistance = -1;
    double totalDist = 0;
    Observation furtherObject = objectsList[0].get(0);
    Observation closestObject = objectsList[0].get(0);

    for (final ArrayList<Observation> objects : objectsList) {
      for (Observation object : objects) {
        double distance = object.position.dist(avatarPosition);
        if (distance > maxDistance) {
          maxDistance = distance;
          furtherObject = object;
        }

        if (distance < minDistance) {
          minDistance = distance;
          closestObject = object;
        }

        totalDist += distance;
        totalEnemies++;
      }
    }

    return new ObservableData(maxDistance, minDistance, totalEnemies, totalDist, furtherObject, closestObject);
  }

  public void addToPropertyMap(TreeMap<String, Double> propertyMap,
                               final String property,
                               final double value,
                               final double maxValue) {
    propertyMap.put(property, value / maxValue);
  }

  public Double getFeatureValue(final String property) {
    return propertyValueMap.getOrDefault(property, 0d);
  }


  public int getSize() {
    return availableProperties.size();
  }


}
