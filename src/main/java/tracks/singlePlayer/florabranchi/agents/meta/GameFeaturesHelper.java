package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.HashMap;
import java.util.Map;

public class GameFeaturesHelper {

  static Map<Integer, tracks.singlePlayer.florabranchi.agents.meta.GameFeatures> featuresByGame = new HashMap<>();

  // g1: aliens, butterflies, painter
  //
  //g2: camelRace, frogs, chase
  //
  //g3: jaws, seaquest, surviving zombies

  static {

    // flora usar id?
    // group 1 - aliens 1, butterfly 13, painter 70 first group are games easily beaten by most algorithms
    featuresByGame.put(0, new GameFeatures(0, true, false, false, false));
    featuresByGame.put(13, new GameFeatures(13, false, false, false, false));
    featuresByGame.put(70, new GameFeatures(70, true, false, false, false));

    // group 2 - camelRace 15 frogs 44 chase 18 second group can be mostly beaten
    featuresByGame.put(15, new GameFeatures(15, false, false, false, false));
    featuresByGame.put(44, new GameFeatures(44, false, false, true, false));
    featuresByGame.put(18, new GameFeatures(18, false, false, true, false));

    // group 3 - jaws 56, seaquest 77, surviving zombies 84 third group is particularly susceptible to MCTS controllers
    featuresByGame.put(56, new GameFeatures(56, false, true, false, false));
    featuresByGame.put(77, new GameFeatures(77, true, true, true, false));
    featuresByGame.put(84, new GameFeatures(84, false, true, true, true));

    // group 4 - brainmain 12, plants 61, eggomania difficult
    //for all
    featuresByGame.put(12, new GameFeatures(12, true, false, false, false));
    featuresByGame.put(61, new GameFeatures(61, false, false, true, false));
    featuresByGame.put(34, new GameFeatures(34, false, false, true, true));

  }

  public static Map<Integer, tracks.singlePlayer.florabranchi.agents.meta.GameFeatures> getFeaturesByGame() {
    return featuresByGame;
  }

  public static GameFeatures getGameFeatures(final int gameId) {
    return featuresByGame.getOrDefault(gameId, null);
  }

  enum EGameFeatures {
    IS_DETERMINISTIC,
    CAN_USE,
    CAN_DIE,
    IS_SURVIVAL,
  }


}
