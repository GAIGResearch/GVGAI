package tracks.singlePlayer.florabranchi.persistence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import tracks.singlePlayer.florabranchi.trash.GameResults;
import tracks.singlePlayer.florabranchi.persistence.weights.OfflineTrainerResults;

public class PersistenceController {

  private final static String trainerLogPath = "./tests/trainer_it%d_logs.txt";

  private final static String gameTrackerPath = "./tests/gameTracker.txt";

  private final static String trainingResultCsvPath = "./tests/score_progression_csv";

  private final static String weightProgressionCsvPath = "./tests/weight_progression_csv";

  private static final String persistedWeightsPath = "./tests/offlineTrainingRecords.txt";

  final boolean saveToLog = true;

  private ScoreProgressionLog scoreProgressionLog = new ScoreProgressionLog(trainingResultCsvPath);

  private WeightUpdateHistoryLog weightUpdateHistoryLog = new WeightUpdateHistoryLog(weightProgressionCsvPath);

  protected TrainingLog trainingLog = new TrainingLog(trainerLogPath);

  public static void main(String[] args) {

    PersistenceController persistenceController = new PersistenceController();

    persistenceController.updateScoreProgressionLog(10);
  }

  public PersistenceController() {

    // Score progression
    scoreProgressionLog.readFile();

    // Weight progression
    weightUpdateHistoryLog.readFile();

    // Training log
    trainingLog.readFile();
  }

  public OfflineTrainerResults readPreviousWeights() {
    OfflineTrainerResults pr1 = null;
    try {
      FileInputStream fi = new FileInputStream(persistedWeightsPath);
      ObjectInputStream oi = new ObjectInputStream(fi);

      // Read objects
      pr1 = (OfflineTrainerResults) oi.readObject();
      return pr1;

    } catch (FileNotFoundException fileNotFoundException) {
      return null;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return pr1;
  }

  public void persistWeights(final OfflineTrainerResults results) {

    try {

      FileOutputStream fileOut = new FileOutputStream(persistedWeightsPath);
      ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
      System.out.println(results.toString());
      objectOut.writeObject(results);
      objectOut.close();
      System.out.println("The Object  was succesfully written to a file");

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  public void addLog(final String log) {
    trainingLog.addLog(log);
  }

  public void updateScoreProgressionLog(double gameScore) {
    final int newIteration = scoreProgressionLog.addEpisode(gameScore);
    saveScoreProgression(newIteration);
  }

  public void saveScoreProgression(final int iteration) {

    if (!saveToLog) return;
    scoreProgressionLog.saveToFile(iteration);
  }

  public GameResults readPreviousGameResults() {
    GameResults pr1 = null;
    try {
      FileInputStream fi = new FileInputStream(gameTrackerPath);
      ObjectInputStream oi = new ObjectInputStream(fi);

      // Read objects
      pr1 = (GameResults) oi.readObject();
      return pr1;

    } catch (FileNotFoundException fileNotFoundException) {
      return new GameResults(gameTrackerPath);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return pr1;
  }

  public void saveGameResults(final GameResults results) {

    try {

      FileOutputStream fileOut = new FileOutputStream(gameTrackerPath);
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
