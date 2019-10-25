package tracks.singlePlayer.florabranchi.training;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OffilneTrainerResults implements Serializable {

  private static final long serialVersionUID = 1L;

  List<OfflineTrainingEpisode> epsiodes;

  public OffilneTrainerResults(final List<OfflineTrainingEpisode> epsiodes) {
    this.epsiodes = epsiodes;
  }

  public int addNewEpisode(final TrainingWeights results) {
    if (epsiodes == null) {
      epsiodes = new ArrayList<>();
    }

    int index = epsiodes.size();

    epsiodes.add(new OfflineTrainingEpisode(index, results));
    return index;
  }

  public List<OfflineTrainingEpisode> getEpsiodes() {
    return epsiodes;
  }

  public void setEpsiodes(final List<OfflineTrainingEpisode> epsiodes) {
    this.epsiodes = epsiodes;
  }
}
