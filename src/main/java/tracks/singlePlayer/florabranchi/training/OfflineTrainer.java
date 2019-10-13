package tracks.singlePlayer.florabranchi.training;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;

import core.game.StateObservation;
import ontology.Types;

public class OfflineTrainer {

  private static final String resultsPath = "./resultsLogs.txt";

  public static void main(String[] args) {

    OfflineTrainer trainer = new OfflineTrainer();

    OfflineTrainingEpisode episode = new OfflineTrainingEpisode(0, new WeightVector());
    OffilneTrainerResults results = new OffilneTrainerResults(Collections.singletonList(episode));
    trainer.saveToFile(results);

    final double[] doubles = trainer.readFromFile();

  }

  public static double dotProduct(double[] a, double[] b) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += a[i] * b[i];
    }
    return sum;
  }

  public OfflineTrainer() {
    final FeatureVector featureVector = new FeatureVector();
    initializeWeightVector(featureVector.getSize());
  }

  public Types.ACTIONS selectAction(final StateObservation initialState) {

    // q black magic


    return Types.ACTIONS.ACTION_NIL;
  }

  public double[] weightVector;
  public double[] featureVector;

  public void initializeWeightVector(int featureVectorSize) {

    // read file
    weightVector = readFromFile();

    // if could not load initialize
    if (weightVector.length == 0) {
      for (int i = 0; i < featureVectorSize; i++) {
        weightVector[i] = 0;
      }
    }

  }

  private double[] readFromFile() {
    OffilneTrainerResults pr1 = null;
    try {
      FileInputStream fi = new FileInputStream(resultsPath);
      ObjectInputStream oi = new ObjectInputStream(fi);

      // Read objects
      pr1 = (OffilneTrainerResults) oi.readObject();
      final OfflineTrainingEpisode latestTraining = pr1.getEpsiodes().get(0);

      // get latest
      int i = 0;
      double[] primitiveArray = new double[latestTraining.weightVector.size()];
      for (int weight : latestTraining.weightVector) {
        primitiveArray[i] = weight;
        i++;
      }
      return primitiveArray;

    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  private void saveToFile(final OffilneTrainerResults results) {

    try {

      FileOutputStream fileOut = new FileOutputStream(resultsPath);
      ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
      objectOut.writeObject(results);
      objectOut.close();
      System.out.println("The Object  was succesfully written to a file");

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private double[] castWeightVector(final WeightVector weightVector) {
    double[] primitiveArray = new double[weightVector.size()];
    if (!weightVector.isEmpty()) {
      int i = 0;
      for (Integer weight : weightVector) {
        primitiveArray[i] = weight;
        i++;
      }
    }
    return primitiveArray;
  }


}

