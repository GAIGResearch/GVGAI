package tracks.singlePlayer.florabranchi.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import core.game.Observation;
import core.game.StateObservation;

public class FeatureVectorController {


  private static final String AVATAR_HEALTH = "AVATAR_HEALTH";
  private static final String GAME_STATE = "GAME_STATE";
  private static final String GAME_TICK = "GAME_TICK";
  private static final String TOTAL_ENEMIES = "TOTAL_ENEMIES";
  private static final String TOTAL_FRIENDLY_NPC = "TOTAL_FRIENDLY_NPC";


  static TreeSet<String> availableProperties = new TreeSet<>();

  public static Set<String> getAvailableProperties() {
    return availableProperties;
  }

  public Map<String, Double> getPropertyValueMap() {
    return propertyValueMap;
  }

  private Map<String, Double> propertyValueMap = new HashMap<>();

  static {
    availableProperties.add(GAME_STATE);
    availableProperties.add(TOTAL_ENEMIES);
    availableProperties.add(TOTAL_FRIENDLY_NPC);
    availableProperties.add(AVATAR_HEALTH);
    availableProperties.add(GAME_TICK);
  }

  public void extractFeatureVector(StateObservation stateObservation) {

    int avatarHealth = stateObservation.getAvatarHealthPoints();
    propertyValueMap.put(AVATAR_HEALTH, (double) avatarHealth);

    int gameTick = stateObservation.getGameTick();
    propertyValueMap.put(GAME_TICK, (double) gameTick);

    final ArrayList<Observation>[] npcPositions = stateObservation.getNPCPositions();
    if (npcPositions != null) {

      if (npcPositions[0] != null) {
        propertyValueMap.put(TOTAL_ENEMIES, (double) npcPositions[0].size());
      }


    }


  }

  public Double getFeatureValue(final String property) {
    return propertyValueMap.getOrDefault(property, 0d);
  }

  public WeightVector castToWeightVector() {
    WeightVector featureVector = new WeightVector();
    availableProperties.forEach(property -> featureVector.add(getFeatureValue(property)));
    return featureVector;

  }


  public int getSize() {
    return 0;
  }
}
