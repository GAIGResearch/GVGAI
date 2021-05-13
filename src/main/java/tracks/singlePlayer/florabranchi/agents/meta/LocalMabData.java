package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LocalMabData {

  public Map<Boolean, LocalMabInfo> localMabData = new HashMap<>();

  public static class LocalMabInfo implements Serializable {
    public double marginalizedAvgScoreForParameter;

    public double timesParameterSelected;

    public double getAverageReward() {
      return timesParameterSelected == 0 ? 0 : marginalizedAvgScoreForParameter / timesParameterSelected;
    }


    public LocalMabInfo(final double marginalizedAvgScoreForParameter,
                        final double timesParameterSelected) {
      this.marginalizedAvgScoreForParameter = marginalizedAvgScoreForParameter;
      this.timesParameterSelected = timesParameterSelected;
    }

    public LocalMabInfo() {
    }
  }

  public LocalMabData(final Map<Boolean, LocalMabInfo> localMabData) {
    this.localMabData = localMabData;
  }

  public LocalMabData() {
    localMabData.put(true, new LocalMabInfo());
    localMabData.put(false, new LocalMabInfo());
  }

}
