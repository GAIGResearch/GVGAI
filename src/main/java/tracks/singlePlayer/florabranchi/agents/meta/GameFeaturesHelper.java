package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.HashMap;
import java.util.Map;

public class GameFeaturesHelper {

  static Map<Integer, GameFeatures> featuresByGame = new HashMap<>();

  static {

    // camelRace 15
    featuresByGame.put(15, new GameFeatures(true, false, false, false));
    // butterflies 13
    featuresByGame.put(13, new GameFeatures(false, false, false, false));
    // painter 70
    featuresByGame.put(70, new GameFeatures(true, false, false, false));
    // frogs 44
    featuresByGame.put(44, new GameFeatures(false, false, true, false));
  }

  public static Map<Integer, GameFeatures> getFeaturesByGame() {
    return featuresByGame;
  }

  public static GameFeatures getGameFeatures(final int gameId) {
    return featuresByGame.getOrDefault(gameId, null);
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
