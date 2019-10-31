package tracks.singlePlayer.florabranchi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class GameResulsTracker {

  final String resultsPath;

  public GameResulsTracker(final String resultsPath) {
    this.resultsPath = resultsPath;
  }

  public GameResults readFromFile() {
    GameResults pr1 = null;
    try {
      FileInputStream fi = new FileInputStream(resultsPath);
      ObjectInputStream oi = new ObjectInputStream(fi);

      // Read objects
      pr1 = (GameResults) oi.readObject();
      return pr1;

    } catch (FileNotFoundException fileNotFoundException) {
      return null;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return pr1;
  }

  public void saveToFile(final GameResults results) {

    try {

      FileOutputStream fileOut = new FileOutputStream(resultsPath);
      ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
      objectOut.writeObject(results);
      objectOut.close();
      System.out.println(results.toString());
      System.out.println("The Object  was succesfully written to a file");

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


}
