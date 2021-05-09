package tracks.singlePlayer.florabranchi.agents.meta;

public class GlobalMabData {

  public double totalRewards;

  public int timesSelected;

  public double getAverageReward() {
    return timesSelected == 0 ? 0 : totalRewards / timesSelected;
  }

}
