package tracks.singlePlayer.florabranchi.agents.meta;

public class RunOptions {

  public String game;

  public int level;

  public int totalGames;

  public int[] scores;

  public int wins;

  public double wr;

  public GameOptions gameOptions;

  public RunOptions() {
  }

  public RunOptions(final String game,
                    final int level,
                    final GameOptions gameOptions) {
    this.game = game;
    this.level = level;
    this.gameOptions = gameOptions;
  }
}
