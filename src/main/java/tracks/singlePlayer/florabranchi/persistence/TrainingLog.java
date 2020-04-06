package tracks.singlePlayer.florabranchi.persistence;


import java.io.Serializable;

public class TrainingLog extends AbstractLog implements Serializable {

  public TrainingLog(final String filePath) {
    super(filePath);
  }
}
