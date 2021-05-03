package tracks.singlePlayer.florabranchi.database;

import java.io.Serializable;

import tools.com.google.gson.annotations.SerializedName;
import tracks.singlePlayer.florabranchi.agents.meta.MetaWeights;
import tracks.singlePlayer.florabranchi.agents.meta.RunOptions;

public class SavedMetaWeights implements Serializable {

  int currentVersion = 0;

  RunOptions usedOptions;

  MetaWeights metaWeights;

  public SavedMetaWeights(final RunOptions usedOptions) {
    this.usedOptions = usedOptions;
  }

}
