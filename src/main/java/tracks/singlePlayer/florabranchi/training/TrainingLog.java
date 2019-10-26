package tracks.singlePlayer.florabranchi.training;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TrainingLog {

  private final static String filePath = "./tests/trainer_it%d_logs.txt";

  final List<String> trainingLog;

  public TrainingLog(final List<String> trainingLog) {
    this.trainingLog = trainingLog;
  }

  public void addLog(final String log) {
    trainingLog.add(log);
  }

  public void saveToFile(int iteration) {

    trainingLog.add(String.format("FINISHED ITERATION %d", iteration));

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
    System.out.println(log);
    trainingLog.add(log);
  }
}
