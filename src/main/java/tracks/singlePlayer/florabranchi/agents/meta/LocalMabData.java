package tracks.singlePlayer.florabranchi.agents.meta;

public class LocalMabData {
  public double marginalizedAvgScoreForParameter;

  public double timesParameterSelected;

  public LocalMabData(final double marginalizedAvgScoreForParameter, final double timesParameterSelected) {
    this.marginalizedAvgScoreForParameter = marginalizedAvgScoreForParameter;
    this.timesParameterSelected = timesParameterSelected;
  }

  public LocalMabData() {
  }
}
