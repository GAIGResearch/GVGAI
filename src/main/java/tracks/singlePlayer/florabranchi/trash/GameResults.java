package tracks.singlePlayer.florabranchi.trash;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class GameResults implements Serializable {

  int totalGames;

  int victories;

  int losses;

  final String resultsPath;

  public GameResults(final String resultsPath) {
    this.resultsPath = resultsPath;
  }

  @Override
  public String toString() {
    return String.format("GAMES %d: - WIN : %d - LOSS - %d - WR %f", totalGames, victories, losses, (double) victories / totalGames);
  }

  public void updateResults(boolean won) {
    if (won) victories++;
    else losses++;
    totalGames++;
  }


}

