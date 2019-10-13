package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;
import java.util.List;

public class OffilneTrainerResults implements Serializable {

  List<OfflineTrainingEpisode> epsiodes;

  public OffilneTrainerResults(final List<OfflineTrainingEpisode> epsiodes) {
    this.epsiodes = epsiodes;
  }

  public List<OfflineTrainingEpisode> getEpsiodes() {
    return epsiodes;
  }

  public void setEpsiodes(final List<OfflineTrainingEpisode> epsiodes) {
    this.epsiodes = epsiodes;
  }
}
