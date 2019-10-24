package tracks.singlePlayer.florabranchi.training;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

public class OfflineTrainerAgent extends AbstractPlayer {

  private static final String resultsPath = "./resultsLogs.txt";
  private final JPanel panel;

  private FeatureVectorController featureVectorController;

  private Map<String, JLabel> labelMap = new HashMap<>();

  private static final String propertyLabelName = "_VALUE_LABEL";

  public static void main(String[] args) {


    // saving test
    //OfflineTrainerAgent trainer = new OfflineTrainerAgent();

    OfflineTrainingEpisode episode = new OfflineTrainingEpisode(0, new WeightVector());
    OffilneTrainerResults results = new OffilneTrainerResults(Collections.singletonList(episode));
    //trainer.saveToFile(results);

    //final double[] doubles = trainer.readFromFile();

  }

  public static double dotProduct(double[] a, double[] b) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += a[i] * b[i];
    }
    return sum;
  }

  public OfflineTrainerAgent(final StateObservation stateObs,
                             final ElapsedCpuTimer elapsedTimer) {
    super();
    final FeatureVectorController featureVector = new FeatureVectorController();
    initializeWeightVector(featureVector.getSize());

    final JFrame frame = new JFrame("Weight Vector Debug");
    panel = new JPanel();
    panel.setLayout(null);
    frame.add(panel);

    frame.setSize(400, 600);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);
    frame.setLayout(null);

    createUI();

  }

  @Override
  public Types.ACTIONS act(final StateObservation stateObs,
                           final ElapsedCpuTimer elapsedTimer) {


    featureVectorController = new FeatureVectorController();
    featureVectorController.extractFeatureVector(stateObs);
    writeResultsToUi();

    final WeightVector doubles = featureVectorController.castToWeightVector();
    double[] arrayVector = doubles.castToArray();

    // black magic here

    return Types.ACTIONS.ACTION_NIL;
  }

  public void createUI() {

    int initialXDisplacement = 10;
    int initialYDisplacement = 10;
    int lineHeight = 20; //y
    int lineWidth = 150; //x

    int currElement = 0;

    // Add Header
    int elementX = initialXDisplacement + currElement;
    int elementY = initialYDisplacement + currElement;


    JLabel hPropertyLabel = new JLabel("PROPERTY", JLabel.CENTER);
    hPropertyLabel.setBounds(elementX, elementY, lineWidth, lineHeight);

    JLabel hValueLabel = new JLabel("VALUE", JLabel.CENTER);
    hValueLabel.setBounds(elementX + lineWidth, elementY, lineWidth, lineHeight);

    panel.add(hPropertyLabel);
    panel.add(hValueLabel);

    currElement++;

    final Set<String> propertyValueMap = FeatureVectorController.getAvailableProperties();

    if (propertyValueMap != null) {

      for (String entry : propertyValueMap) {

        // Update position
        elementY = initialYDisplacement + currElement;


        JLabel propertyLabel = new JLabel(entry, JLabel.CENTER);
        propertyLabel.setBounds(elementX, elementY + (currElement * lineHeight), lineWidth, lineHeight);

        JLabel valueLabel = new JLabel(String.valueOf(0), JLabel.CENTER);
        valueLabel.setBounds(elementX + lineWidth, elementY + (currElement * lineHeight), lineWidth, lineHeight);

        // Update label map
        labelMap.put(buildPropertyKey(entry), valueLabel);

        panel.add(propertyLabel);
        panel.add(valueLabel);

        currElement++;
      }
    }

    panel.repaint();

  }

  public String buildPropertyKey(final String property) {
    return String.format("%s%s", property, propertyLabelName);
  }

  public void writeResultsToUi() {

    final Map<String, Double> propertyValueMap = featureVectorController.getPropertyValueMap();

    if (propertyValueMap != null) {

      for (Map.Entry<String, Double> entry : propertyValueMap.entrySet()) {

        JLabel propertyRelatedLabel = labelMap.getOrDefault(buildPropertyKey(entry.getKey()), null);
        if (propertyRelatedLabel != null) {
          propertyRelatedLabel.setText(entry.getValue().toString());
        }
      }
    }

    panel.repaint();
  }

  public double[] weightVector;
  public double[] featureVector;

  public void initializeWeightVector(int featureVectorSize) {

    // read file
    //weightVector = readFromFile();

    if (weightVector == null) {
      weightVector = new double[featureVectorSize];
    }

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
      for (double weight : latestTraining.weightVector) {
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


}

