package tracks.singlePlayer.florabranchi.persistence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Abstract implementation that saves lists of strings.
 */
abstract public class AbstractLog implements Serializable {

  protected final String filePath;

  boolean saveToLog;

  protected List<String> trainingLog = new ArrayList<>();

  public AbstractLog(final String filePath) {
    this.filePath = filePath;
  }

  public void addLog(String log) {
    trainingLog.add(log);
  }

  public AbstractLog readFile() {

    try {
      Scanner s = new Scanner(new File(filePath));
      ArrayList<String> list = new ArrayList<String>();
      while (s.hasNext()) {
        list.add(s.next());
      }
      this.trainingLog = list;
      s.close();
      return this;
    } catch (IOException e) {
      return null;
    }


  }

  public void saveToFile(int iteration) {

    if (!saveToLog) return;


    try {
      FileWriter writer = new FileWriter(String.format(filePath, iteration));
      final String results = String.join("\n", trainingLog);
      writer.write(results);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void write(final String log) {
    //System.out.println(log);
    trainingLog.add(log);
  }
}
