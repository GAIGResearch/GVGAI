package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.HashMap;
import java.util.Map;

public class GameFeaturesHelper {

  static Map<Integer, GameFeatures> featuresByGame = new HashMap<>();

  // g1: aliens, butterflies, painter
  //
  //g2: camelRace, frogs, chase
  //
  //g3: jaws, seaquest, surviving zombies

  static {

    // flora usar id?
    // group 1 - aliens 1, butterfly 13, painter 70 first group are games easily beaten by most algorithms
    featuresByGame.put(0, new GameFeatures(true, false, false, false));
    featuresByGame.put(13, new GameFeatures(false, false, false, false));
    featuresByGame.put(70, new GameFeatures(true, false, false, false));

    // group 2 - camelRace 15 frogs 44 chase 18 second group can be mostly beaten
    featuresByGame.put(15, new GameFeatures(false, false, false, false));
    featuresByGame.put(44, new GameFeatures(false, false, true, false));
    featuresByGame.put(18, new GameFeatures(false, false, true, false));

    // group 3 - jaws 56, seaquest 77, surviving zombies 84 third group is particularly susceptible to MCTS controllers
    featuresByGame.put(56, new GameFeatures(false, true, false, false));
    featuresByGame.put(77, new GameFeatures(true, true, true, false));
    featuresByGame.put(84, new GameFeatures(false, true, true, true));

    // group 4 - brainmain 12, plants 61, eggomania difficult
    //for all
    featuresByGame.put(12, new GameFeatures(true, false, false, false));
    featuresByGame.put(61, new GameFeatures(false, false, true, false));
    featuresByGame.put(34, new GameFeatures(false, false, true, true));

  }

  public static Map<Integer, GameFeatures> getFeaturesByGame() {
    return featuresByGame;
  }

  public static GameFeatures getGameFeatures(final int gameId) {
    return featuresByGame.getOrDefault(gameId, null);
  }

  enum EGameFeatures {
    isDeterministic,
    canUse,
    canDie,
    isSurvival,
  }

  static class GameFeatures {
    public boolean isDeterministic;
    public boolean canUse;
    public boolean canDie;
    public boolean isSurvival; // there is timeout

    public GameFeatures(final boolean isDeterministic,
                        final boolean canUse,
                        final boolean canDie,
                        final boolean isSurvival) {
      this.isDeterministic = isDeterministic;
      this.canUse = canUse;
      this.canDie = canDie;
      this.isSurvival = isSurvival;
    }
  }


}
