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

  private static final String AVATAR_HEALTH = "AVATAR_HEALTH";
  private static final String GAME_STATE = "GAME_STATE";
  private static final String GAME_TICK = "GAME_TICK";
  private static final String GAME_SCORE = "GAME_SCORE";
  private static final String TOTAL_ENEMIES = "TOTAL_ENEMIES";
  private static final String TOTAL_FRIENDLY_NPC = "TOTAL_FRIENDLY_NPC";
  private static final String WORLD_HEIGHT = "WORLD_HEIGHT";
  private static final String WORLD_WIDTH = "WORLD_WIDTH";
  private static final String AVAILABLE_RESOURCES = "AVAILABLE_RESOURCES";
  private static final String PLAYER_RESOURCES = "PLAYER_RESOURCES";

  public static TreeSet<String> availableProperties = new TreeSet<>();

  public static Set<String> getAvailableProperties() {
    return availableProperties;
  }

  public TreeMap<String, Double> getPropertyValueMap() {
    return propertyValueMap;
  }

  private TreeMap<String, Double> propertyValueMap = new TreeMap<>();

  private static final String PLAYER_X = "PLAYER_X";

  private static final String PLAYER_Y = "PLAYER_Y";

  private static final String CREATED_SPRITES = "CREATED_SPRITES";

  private static final String CLOSEST_SPRITE_X = "CLOSEST_SPRITE_X";

  private static final String CLOSEST_SPRITE_Y = "CLOSEST_SPRITE_Y";

  static {
    availableProperties.add(GAME_STATE);
    availableProperties.add(GAME_SCORE);
    availableProperties.add(TOTAL_ENEMIES);
    availableProperties.add(TOTAL_FRIENDLY_NPC);
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
  }

  public TreeMap<String, Double> extractFeatureVector(StateObservation stateObservation) {

    // Add default values in case of unavalable information
    TreeMap<String, Double> propertyMap = new TreeMap<>();
    availableProperties.forEach(entry -> propertyMap.put(entry, 0d));

    // World related Fixed properties
    double worldHeight = stateObservation.getWorldDimension().getHeight();
    double worldWidth = stateObservation.getWorldDimension().getWidth();

    addToPropertyMap(propertyMap, WORLD_HEIGHT, worldHeight, 1000);
    addToPropertyMap(propertyMap, WORLD_WIDTH, worldWidth, 1000);

    int avatarHealth = stateObservation.getAvatarHealthPoints();
    int totalAvatarHealth = stateObservation.getAvatarLimitHealthPoints();
    double avatarRemainingLife = avatarHealth == 0 ? 100 : (double) totalAvatarHealth / avatarHealth;
    addToPropertyMap(propertyMap, AVATAR_HEALTH, avatarRemainingLife, totalAvatarHealth);

    int gameTick = stateObservation.getGameTick();
    addToPropertyMap(propertyMap, GAME_TICK, (double) gameTick, 1000);

    double gameScore = stateObservation.getGameScore();
    addToPropertyMap(propertyMap, GAME_SCORE, gameScore, 1000);


    final ArrayList<Observation>[] npcPositions = stateObservation.getNPCPositions();
    if (npcPositions != null) {

      if (npcPositions.length > 0 && npcPositions[0] != null) {
        addToPropertyMap(propertyMap, TOTAL_ENEMIES, (double) npcPositions[0].size(), 50);
      }

      if (npcPositions.length > 1 && npcPositions[1] != null) {
        addToPropertyMap(propertyMap, TOTAL_FRIENDLY_NPC, (double) npcPositions[1].size(), 50);
      }
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

    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    addToPropertyMap(propertyMap, PLAYER_X, avatarPosition.x, worldWidth);
    addToPropertyMap(propertyMap, PLAYER_Y, avatarPosition.y, worldHeight);


/*    // Get Avatar created stuff
    final ArrayList<Observation>[] fromAvatarSpritesPositions = stateObservation.getFromAvatarSpritesPositions(avatarPosition);
    if (fromAvatarSpritesPositions != null) {

      int createdSprintes = fromAvatarSpritesPositions.length;
      addToPropertyMap(propertyMap, CREATED_SPRITES, createdSprintes, 10);
      if (fromAvatarSpritesPositions.length > 0) {
        final Observation closestSprite = fromAvatarSpritesPositions[0].get(fromAvatarSpritesPositions[0].size() - 1);
        addToPropertyMap(propertyMap, CLOSEST_SPRITE_X, closestSprite.position.x, worldWidth);
        addToPropertyMap(propertyMap, CLOSEST_SPRITE_Y, closestSprite.position.y, worldHeight);
      }

    }*/

    return propertyMap;
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
