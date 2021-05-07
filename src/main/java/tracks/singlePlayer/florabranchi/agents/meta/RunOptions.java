package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.Serializable;

public class RunOptions implements Serializable {

  public String game;

  public int level;

  public int totalGames;

  public int[] scores;

  public int wins;

  public double wr;

  //public GameOptions gameOptions;

  public RunOptions() {
  }

}
