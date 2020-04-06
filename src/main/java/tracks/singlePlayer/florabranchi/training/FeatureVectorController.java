package tracks.singlePlayer.florabranchi.training;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

public class FeatureVectorController {

  private List<PossibleHarmfulSprite> possibleHarmfulSprites = new ArrayList<>();

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
  private static final String RESOURCES = "MOVABLES";
  private static final String FROM_AVATAR = "MOVABLES";
  private static final String PORTALS = "PORTALS";

  private static final String TOTAL_OBSERVABLE = "TOTAL_%s";
  private static final String TOTAL_OBSERVABLE_DISTANCE = "TOTAL_%s_DST";
  private static final String CLOSEST_OBSERVABLE_X = "CLOSEST_%s_X";
  private static final String CLOSEST_OBSERVABLE_Y = "CLOSEST_%s_Y";
  private static final String AVERAGE_DISTANCE_TO_OBSERVABLE = "AVG_DST_TO_%s";

  private static final String DST_TO_CLOSEST_HARM = "DST_TO_CLOSEST_HARM";
  private static final String POSSIBLE_HARM_X_AXIS = "POSSIBLE_HARM_X_AXIS";
  private static final String POSSIBLE_HARM_Y_AXIS_TO_LEFT = "POSSIBLE_HARM_Y_AXIS_TO_LEFT";
  private static final String POSSIBLE_HARM_Y_AXIS_TO_RIGHT = "POSSIBLE_HARM_Y_AXIS_TO_RIGHT";

  private static final String CLOSEST_OBSERVABLE_TYPE = "CLOSEST_%s_TYPE";

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

  public void injectHarmfulList(final List<PossibleHarmfulSprite> list) {
    this.possibleHarmfulSprites = list;
  }

  private TreeMap<String, Double> propertyValueMap = new TreeMap<>();

  static {
    availableProperties.add(GAME_SCORE);
    availableProperties.add(AVATAR_HEALTH);
    availableProperties.add(GAME_TICK);
    availableProperties.add(WORLD_HEIGHT);
    availableProperties.add(WORLD_WIDTH);
    availableProperties.add(PLAYER_RESOURCES);
    availableProperties.add(PLAYER_X);
    availableProperties.add(PLAYER_Y);

    availableProperties.add(DST_TO_CLOSEST_HARM);
    availableProperties.add(POSSIBLE_HARM_X_AXIS);
    availableProperties.add(POSSIBLE_HARM_Y_AXIS_TO_RIGHT);
    availableProperties.add(POSSIBLE_HARM_Y_AXIS_TO_LEFT);

    addGeneratedProperties();
  }

  public static void addGeneratedProperties() {
    addGeneratedSpecificProperties(NPCS);
    addGeneratedSpecificProperties(MOVABLES);
    addGeneratedSpecificProperties(IMMOVABLES);
    addGeneratedSpecificProperties(RESOURCES);
    addGeneratedSpecificProperties(FROM_AVATAR);
    addGeneratedSpecificProperties(PORTALS);
  }

  public static void addGeneratedSpecificProperties(final String type) {
    availableProperties.add(buildPropertyName(type, TOTAL_OBSERVABLE));
    availableProperties.add(buildPropertyName(type, CLOSEST_OBSERVABLE_X));
    availableProperties.add(buildPropertyName(type, CLOSEST_OBSERVABLE_Y));
    //availableProperties.add(buildPropertyName(type, CLOSEST_OBSERVABLE_TYPE));
    availableProperties.add(buildPropertyName(type, AVERAGE_DISTANCE_TO_OBSERVABLE));
    availableProperties.add(buildPropertyName(type, TOTAL_OBSERVABLE_DISTANCE));
  }

  public void addObservableObjectProperties(final String type,
                                            final ArrayList<Observation>[] objects,
                                            final Vector2d avatarPosition,
                                            final TreeMap<String, Double> propertyMap,
                                            final double maxDistance,
                                            final double maxTotalDist) {
    final ObservableData observableData = getObservableData(objects, avatarPosition);
    addToPropertyMap(propertyMap, buildPropertyName(type, TOTAL_OBSERVABLE), observableData.totalEnemies, 500);
    addToPropertyMap(propertyMap, buildPropertyName(type, CLOSEST_OBSERVABLE_X), observableData.closerObject.position.x, maxDistance);
    addToPropertyMap(propertyMap, buildPropertyName(type, CLOSEST_OBSERVABLE_Y), observableData.closerObject.position.y, maxDistance);
    addToPropertyMap(propertyMap, buildPropertyName(type, TOTAL_OBSERVABLE_DISTANCE), observableData.totalDist, maxDistance);
    //addToPropertyMap(propertyMap, buildPropertyName(type, CLOSEST_OBSERVABLE_TYPE), observableData.closerObject.itype, 10);
    addToPropertyMap(propertyMap, buildPropertyName(type, AVERAGE_DISTANCE_TO_OBSERVABLE), observableData.totalDist / observableData.totalEnemies, maxDistance);
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

    double maxDistance = Math.sqrt(Math.pow(worldHeight, 2) * Math.pow(worldWidth, 2));
    double maxTotalDistance = worldHeight * worldWidth;

    int gameTick = stateObservation.getGameTick();
    addToPropertyMap(propertyMap, GAME_TICK, (double) gameTick, 2000);

    double gameScore = stateObservation.getGameScore();
    addToPropertyMap(propertyMap, GAME_SCORE, gameScore, 1000);

    // Avatar position
    final Vector2d avatarPosition = stateObservation.getAvatarPosition();
    addToPropertyMap(propertyMap, PLAYER_X, avatarPosition.x, worldWidth);
    addToPropertyMap(propertyMap, PLAYER_Y, avatarPosition.y, worldHeight);

    int avatarHealth = stateObservation.getAvatarHealthPoints();
    int totalAvatarHealth = stateObservation.getAvatarLimitHealthPoints();
    double avatarRemainingLife = avatarHealth == 0 ? 100 : (double) totalAvatarHealth / avatarHealth;
    addToPropertyMap(propertyMap, AVATAR_HEALTH, avatarRemainingLife, totalAvatarHealth);


    // NPCS
    final ArrayList<Observation>[] npcPositions = stateObservation.getNPCPositions();
    if (npcPositions != null && npcPositions.length > 0) {
      addObservableObjectProperties(NPCS, npcPositions, avatarPosition, propertyMap, maxDistance, maxTotalDistance);
    }

    // IMMOVABLES
    final ArrayList<Observation>[] immovablePositions = stateObservation.getImmovablePositions();
    if (immovablePositions != null) {
      addObservableObjectProperties(IMMOVABLES, immovablePositions, avatarPosition, propertyMap, maxDistance, maxTotalDistance);
    }

    // MOVABLES
    final ArrayList<Observation>[] movablePositions = stateObservation.getMovablePositions(avatarPosition);
    if (movablePositions != null) {
      addObservableObjectProperties(MOVABLES, movablePositions, avatarPosition, propertyMap, maxDistance, maxTotalDistance);

      // Check for harm movables
      final List<List<Observation>> observations = filterHarmObservablesByAvatarPosition(movablePositions, avatarPosition);

      if (!observations.isEmpty() && !observations.get(0).isEmpty()) {
        System.out.println("HARMMMMM");
        addToPropertyMap(propertyMap, POSSIBLE_HARM_X_AXIS, 1, 1);
      }

/*      if (!observations.isEmpty() && !observations.get(1).isEmpty()) {
        final Observation closestMovableHarmwWithSameY = observations.get(1).get(0);
        addToPropertyMap(propertyMap, DST_TO_CLOSEST_HARM, closestMovableHarmwWithSameY.position.dist(avatarPosition), maxDistance);

        if (avatarPosition.x > closestMovableHarmwWithSameY.position.x) {

          addToPropertyMap(propertyMap, POSSIBLE_HARM_Y_AXIS_TO_LEFT, 1, 1);
        } else {
          System.out.println("HARM IN right");
          addToPropertyMap(propertyMap, POSSIBLE_HARM_Y_AXIS_TO_RIGHT, 1, 1);
        }*/
      // }

    }

    // Get possible harm sprites


    // RESOURCES
    final ArrayList<Observation>[] resourcesPositions = stateObservation.getResourcesPositions();
    if (resourcesPositions != null) {
      addObservableObjectProperties(RESOURCES, resourcesPositions, avatarPosition, propertyMap, maxDistance, maxTotalDistance);
    }

    // FROM_AVATAR
    final ArrayList<Observation>[] fromAvatarSpritesPositions = stateObservation.getFromAvatarSpritesPositions(avatarPosition);
    if (fromAvatarSpritesPositions != null) {
      addObservableObjectProperties(FROM_AVATAR, fromAvatarSpritesPositions, avatarPosition, propertyMap, maxDistance, maxTotalDistance);
    }

    // PORTALS
    final ArrayList<Observation>[] portalsPositions = stateObservation.getPortalsPositions(avatarPosition);
    if (portalsPositions != null) {
      addObservableObjectProperties(PORTALS, portalsPositions, avatarPosition, propertyMap, maxDistance, maxTotalDistance);
    }

    // Player resources
    double totalPlayerResources = 0;
    final HashMap<Integer, Integer> avatarResources = stateObservation.getAvatarResources();
    if (avatarResources != null) {
      totalPlayerResources = avatarResources.size();
    }
    addToPropertyMap(propertyMap, PLAYER_RESOURCES, totalPlayerResources, 10);


    return propertyMap;
  }

  public List<List<Observation>> filterHarmObservablesByAvatarPosition(final ArrayList<Observation>[] observables,
                                                                       final Vector2d avatarPosition) {
    List<Observation> possibleHarmInX = new ArrayList<>();
    List<Observation> possibleHarmInY = new ArrayList<>();

    Arrays.stream(observables).forEach(
        list -> {
          list.forEach(
              element -> {
                if (possibleHarmfulSprites.contains(new PossibleHarmfulSprite(element.category, element.itype))) {

                  if (isPossibleThreatInX(avatarPosition, element.position, 10)) {
                    possibleHarmInX.add(element);
                  }

/*                  if (isPossibleThreatInY(avatarPosition, element.position, 10)) {
                    possibleHarmInY.add(element);
                  }*/
                }
              }
          );
        }
    );

    List<List<Observation>> results = new LinkedList<>();
    results.add(possibleHarmInX);
    //results.add(possibleHarmInY);

    return results;
  }

  private boolean isPossibleThreatInX(final Vector2d avatar,
                                      final Vector2d object,
                                      final int range) {

    double minValue = avatar.x - range;
    double maxValue = avatar.x + range;
    return object.x > minValue && object.x < maxValue;
  }

  private boolean isPossibleThreatInY(final Vector2d avatar,
                                      final Vector2d object,
                                      final int range) {
    double minValue = avatar.y - range;
    double maxValue = avatar.y + range;
    return object.y > minValue && object.y < maxValue;
  }


  public ObservableData getObservableData(final ArrayList<Observation>[] objectsList,
                                          final Vector2d avatarPosition) {
    double totalEnemies = 0;
    double maxDistance = 0;
    double minDistance = Integer.MAX_VALUE;
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

  public void addToPropertyMap(final TreeMap<String, Double> propertyMap,
                               final String property,
                               final double value,
                               final double maxValue) {
    if (value / maxValue > 1) {
      System.out.println("------- ERROR FEATURE VALUE > 1 ");
      System.out.println(property);
      System.out.println(value);
      System.out.println(maxValue);
    }
    propertyMap.put(property, value / maxValue);
  }

  public Double getFeatureValue(final String property) {
    return propertyValueMap.getOrDefault(property, 0d);
  }


  public int getSize() {
    return availableProperties.size();
  }

  public Map<Integer, List<Observation>> castAllObservationToMapOfTypes(final StateObservation stateObservation) {

    Map<Integer, List<Observation>> castedMap = new HashMap<>();
    final ArrayList<Observation>[][] observationGrid = stateObservation.getObservationGrid();
    for (int i = 0; i < observationGrid.length - 1; i++) {
      for (int j = 0; j < observationGrid[0].length - 1; j++) {
        final ArrayList<Observation> observations = observationGrid[i][j];
        observations.forEach(entry -> {
          addElementToMap(entry.category, entry, castedMap);
          if (possibleHarmfulSprites.contains(entry.itype)) {
            System.out.println("got it");
          }
        });
      }
    }

    return castedMap;
  }

  public Map<Integer, Observation> castAllObservationToMapOfIds(final StateObservation stateObservation) {

    Map<Integer, Observation> castedMap = new HashMap<>();
    final ArrayList<Observation>[][] observationGrid = stateObservation.getObservationGrid();
    for (int i = 0; i < observationGrid.length - 1; i++) {
      for (int j = 0; j < observationGrid[0].length - 1; j++) {
        final ArrayList<Observation> observations = observationGrid[i][j];
        observations.forEach(entry -> castedMap.put(entry.obsID, entry));
      }
    }

    return castedMap;
  }

  public void addElementToMap(final int category,
                              final Observation object,
                              final Map<Integer, List<Observation>> map) {
    if (map.containsKey(category)) {
      map.get(category).add(object);
    } else {
      List<Observation> newList = new ArrayList<>();
      newList.add(object);
      map.put(category, newList);
    }
  }


}
