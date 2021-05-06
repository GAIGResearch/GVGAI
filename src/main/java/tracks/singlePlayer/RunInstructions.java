package tracks.singlePlayer;

import java.util.ArrayList;
import java.util.List;

public class RunInstructions {

  public List<RunInstruction> runInstructionList = new ArrayList<>();

  void addInstruction(final RunInstruction runInstruction) {
    runInstructionList.add(runInstruction);
  }


  static class RunInstruction {
    public String gamePath;
    public String gameName;
    public String levelPath;
    public int levelId;
    public int episodes;

    public RunInstruction(final String gamePath, final String gameName,
                          final String levelPath, final int levelId, final int episodes) {
      this.gamePath = gamePath;
      this.gameName = gameName;
      this.levelPath = levelPath;
      this.levelId = levelId;
      this.episodes = episodes;
    }
  }
}
