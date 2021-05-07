package tracks.singlePlayer.florabranchi.agents.meta;

public class GameFeatures {

  public int gameId;
  public boolean isDeterministic;
  public boolean canUse;
  public boolean canDie;
  public boolean isSurvival; // there is timeout

  public GameFeatures(final int gameId,
                      final boolean isDeterministic,
                      final boolean canUse,
                      final boolean canDie,
                      final boolean isSurvival) {
    this.isDeterministic = isDeterministic;
    this.canUse = canUse;
    this.canDie = canDie;
    this.isSurvival = isSurvival;
  }
}
