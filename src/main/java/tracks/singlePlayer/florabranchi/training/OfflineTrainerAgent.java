package tracks.singlePlayer.florabranchi.training;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.*;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

public class OfflineTrainerAgent extends AbstractPlayer {

  private static final String resultsPath = "./previousResults.txt";
  private final TrainingLog log;

  private JPanel panel;

  private FeatureVectorController featureVectorController;

  protected Random randomGenerator = new Random();

  private Map<String, JLabel> labelMap = new HashMap<>();

  private static final String propertyLabelName = "_VALUE_LABEL";
  private static final String weightVectorLabelName = "_WEIGHT_LABEL";
  private static final String qValueLabelName = "_Q_VALUE";

  // deterministico - mais alto
  private static final double ALFA = 0.3;

  private static final double GAMMA = 0.9;

  // 10%
  private static final double EXPLORATION_EPSILON = 10;

  // Data

  public TrainingWeights trainingWeights;

  public OffilneTrainerResults previousResults;

  // State related data
  private Types.ACTIONS previousAction;
  private StateObservation previousState;
  private double previousScore;
  private JFrame frame;

  public OfflineTrainerAgent(final StateObservation stateObs,
                             final ElapsedCpuTimer elapsedTimer) {
    super();
    featureVectorController = new FeatureVectorController();
    initializeTrainingWeightVector(featureVectorController.getSize());

    log = new TrainingLog(new ArrayList<>());
    createJFrame(stateObs);
  }

  public void createJFrame(final StateObservation stateObs) {
    frame = new JFrame("Weight Vector Debug");
    panel = new JPanel();
    panel.setLayout(null);
    frame.add(panel);

    frame.setSize(800, 600);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);
    frame.setLayout(null);

    createUI(stateObs);
  }

  public void closeJframe() {
    frame.dispose();
  }

  @Override
  public void result(final StateObservation stateObs, final ElapsedCpuTimer elapsedCpuTimer) {
    saveResults();
    closeJframe();
    super.result(stateObs, elapsedCpuTimer);
  }

  public static void main(String[] args) {


    // saving test
    //OfflineTrainerAgent trainer = new OfflineTrainerAgent();

    //OfflineTrainingEpisode episode = new OfflineTrainingEpisode(0, new WeightVector());
    //OffilneTrainerResults results = new OffilneTrainerResults(Collections.singletonList(episode));
    //trainer.saveToFile(results);

    //final double[] doubles = trainer.readFromFile();

  }


  // need: a', s', a, s, r
  public void updateWeightVectorValues(final double stateReward,
                                       final Types.ACTIONS previousAction,
                                       final StateObservation previousState,
                                       final Types.ACTIONS currentAction,
                                       final StateObservation currentState) {


    // Get Weight vector for previous action
    final double[] weightVectorForPreviousAction = castToDoublesArray(trainingWeights.getWeightVectorForAction(previousAction));

    // Get current feature values
    final TreeMap<String, Double> initialFeatureVector = featureVectorController.extractFeatureVector(previousState);
    double[] featureVectorInPreviousState = castToDoublesArray(initialFeatureVector);

    // Get Weight vector for new action
    final double[] weightVectorForCurrentAction = castToDoublesArray(trainingWeights.getWeightVectorForAction(currentAction));

    // Extract feature values
    final TreeMap<String, Double> featureVectorAfterAction = featureVectorController.extractFeatureVector(currentState);
    double[] featureVectorInCurrentState = castToDoublesArray(featureVectorAfterAction);

    for (int weightIndex = 0; weightIndex < weightVectorForPreviousAction.length - 1; weightIndex++) {

      double initialW = weightVectorForPreviousAction[weightIndex];

      // delta = r + gamma (Qa'(s')) - Qa(s)
      double delta = stateReward + GAMMA * (dotProduct(weightVectorForPreviousAction, featureVectorInCurrentState))
          - dotProduct(weightVectorForCurrentAction, featureVectorInPreviousState);

      // w = w + lambda * delta
      final double updatedWeight = initialW + (ALFA * delta * featureVectorInPreviousState[weightIndex]);
      weightVectorForPreviousAction[weightIndex] = updatedWeight;

      // Update global weights
      trainingWeights.updateWeightVector(currentAction, weightIndex, updatedWeight);
    }

  }


  public Types.ACTIONS returnRandomAction(final StateObservation stateObservation) {
    int index = randomGenerator.nextInt(stateObservation.getAvailableActions().size());
    return stateObservation.getAvailableActions().get(index);
  }

  public double[] castToDoublesArray(List<Double> weightList) {
    double[] featureVector = initializeArrayWithExpectedSize();

    int i = 0;
    for (Double entry : weightList) {
      featureVector[i] = entry;
      i++;
    }
    return featureVector;
  }

  public double[] castToDoublesArray(Map<String, Double> propertyMap) {
    double[] featureVector = initializeArrayWithExpectedSize();

    int i = 0;
    for (Map.Entry<String, Double> entry : propertyMap.entrySet()) {
      featureVector[i] = entry.getValue();
      i++;
    }
    return featureVector;
  }

  private double[] initializeArrayWithExpectedSize() {
    int expectedSize = featureVectorController.getSize();
    double[] featureVector = new double[expectedSize];
    for (int j = 0; j < expectedSize; j++) {
      featureVector[j] = 0d;
    }
    return featureVector;
  }


  @Override
  public Types.ACTIONS act(final StateObservation stateObs,
                           final ElapsedCpuTimer elapsedTimer) {

    log.write(String.format("Act for Game Tick %d", stateObs.getGameTick()));
    // First play
    if (previousAction == null) {
      final Types.ACTIONS randomAction = returnRandomAction(stateObs);

      // Update last values
      previousAction = randomAction;
      previousState = stateObs;
      previousScore = 0;

      return randomAction;
    }

    // Select best action given current weight vector
    final Types.ACTIONS selectedAction = selectBestPerceivedAction(stateObs);

    // Reward = curr score - previous score
    double stateScore = stateObs.getGameScore();

    double reward = stateScore - previousScore;
    System.out.println("reward: " + reward);

    // need: a', s', a, s, r
    updateWeightVectorValues(reward, previousAction, previousState, selectedAction, stateObs);

    final TreeMap<String, Double> featuresForCurrState = featureVectorController.extractFeatureVector(stateObs);

    writeResultsToUi(featuresForCurrState, selectedAction);

    // Update last values
    previousAction = selectedAction;
    previousState = stateObs;
    previousScore = stateScore;

    return selectedAction;
  }

  public double getQValueForAction(final StateObservation initialState,
                                   final Types.ACTIONS action) {

    // Extract feature array
    TreeMap<String, Double> featureAfterAction = featureVectorController.extractFeatureVector(initialState);
    double[] featureValuesForState = castToDoublesArray(featureAfterAction);

    // Get related weight vector
    final List<Double> weightVectorForAction = trainingWeights.getWeightVectorForAction(action);
    double[] weightArray = castToDoublesArray(weightVectorForAction);

    return dotProduct(featureValuesForState, weightArray);
  }

  public void saveResults() {

    final int episode = previousResults.addNewEpisode(trainingWeights);
    log.saveToFile(episode);
    saveToFile(previousResults);
  }


  public Types.ACTIONS selectBestPerceivedAction(final StateObservation stateObservation) {

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      System.out.println("Exploring random action");
      return returnRandomAction(stateObservation);
    }

    double maxValue = -Double.MAX_VALUE;
    Types.ACTIONS bestAction = Types.ACTIONS.ACTION_NIL;

    List<Types.ACTIONS> duplicatedActions = new ArrayList<>();
    for (Types.ACTIONS action : stateObservation.getAvailableActions()) {
      final double actionExpectedReward = getQValueForAction(stateObservation, action);
      if (actionExpectedReward >= maxValue) {

        if (actionExpectedReward != maxValue) {
          duplicatedActions = new ArrayList<>();
        }

        duplicatedActions.add(action);

        maxValue = actionExpectedReward;
        bestAction = action;

        final JLabel jLabel = labelMap.get(buildQvalueKey(action));
        jLabel.setText(String.format("%.4f", maxValue));
      }
    }

    // Return random if draw
    if (duplicatedActions.size() > 1) {

      Collections.shuffle(duplicatedActions);
      final Types.ACTIONS selectedActon = duplicatedActions.get(randomGenerator.nextInt(duplicatedActions.size() - 1));
      System.out.println(String.format("Best play: draw for score %s, selected: %s", maxValue, selectedActon));
      return selectedActon;
    }

    System.out.println(String.format("Best play: %s - score - %.2f", bestAction, maxValue));
    return bestAction;
  }

  public void createUI(final StateObservation stateObs) {

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

    JLabel hValueLabel = new JLabel("FEATURE VALUE", JLabel.CENTER);
    hValueLabel.setBounds(elementX + lineWidth, elementY, lineWidth, lineHeight);


    final List<Types.ACTIONS> avallableGameActions = stateObs.getAvailableActions();

    int multiplier = 2;
    for (Types.ACTIONS action : avallableGameActions) {
      JLabel HFeatureValue = new JLabel(String.format("WEIGHT %s", action));
      HFeatureValue.setBounds(elementX + (multiplier * lineWidth), elementY, lineWidth, lineHeight);
      panel.add(HFeatureValue);
      multiplier++;
    }

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


        multiplier = 2;
        for (Types.ACTIONS action : avallableGameActions) {
          JLabel weightVectorValue = new JLabel(String.valueOf(0), JLabel.CENTER);
          weightVectorValue.setBounds(elementX + multiplier * lineWidth, elementY + (currElement * lineHeight), lineWidth, lineHeight);
          multiplier++;
          panel.add(weightVectorValue);
          labelMap.put(buildWeightVectorKey(entry, action), weightVectorValue);
        }

        // Update label map
        labelMap.put(buildPropertyKey(entry), valueLabel);
        panel.add(propertyLabel);
        panel.add(valueLabel);

        currElement++;
      }
    }

    int qValuesDisplacementY = 50;
    int qValuesLineSize = 300;

    // Create Q values debug
    for (Types.ACTIONS action : avallableGameActions) {

      JLabel newLabel = new JLabel(String.format("Q VALUE FOR %s", action), JLabel.CENTER);
      newLabel.setBounds(initialXDisplacement, qValuesDisplacementY + elementY + (currElement * lineHeight), qValuesLineSize, lineHeight);

      JLabel weightVectorValue = new JLabel(String.valueOf(0), JLabel.CENTER);
      weightVectorValue.setBounds(initialXDisplacement + qValuesLineSize, qValuesDisplacementY + elementY + (currElement * lineHeight), lineWidth, lineHeight);

      multiplier++;
      panel.add(weightVectorValue);
      panel.add(newLabel);

      labelMap.put(buildQvalueKey(action), weightVectorValue);
      currElement++;
    }

    panel.repaint();

  }

  public String buildPropertyKey(final String property) {
    return String.format("%s%s", property, propertyLabelName);
  }

  public String buildWeightVectorKey(final String property,
                                     final Types.ACTIONS action) {
    return String.format("%s%s%s", property, action, weightVectorLabelName);
  }

  public String buildQvalueKey(final Types.ACTIONS action) {
    return String.format("%s%s", action, qValueLabelName);
  }

  public void writeResultsToUi(final TreeMap<String, Double> featureVectorAfterAction,
                               final Types.ACTIONS selectedAction) {

    final List<Double> weightVectorForAction = trainingWeights.getWeightVectorForAction(selectedAction);
    final double[] weightVector = castToDoublesArray(weightVectorForAction);

    if (featureVectorAfterAction != null) {

      int i = 0;
      for (Map.Entry<String, Double> entry : featureVectorAfterAction.entrySet()) {

        JLabel propertyRelatedLabel = labelMap.getOrDefault(buildPropertyKey(entry.getKey()), null);
        if (propertyRelatedLabel != null) {
          propertyRelatedLabel.setText(entry.getValue().toString());
        }

        JLabel weightVectorRelatedLabel = labelMap.getOrDefault(buildWeightVectorKey(entry.getKey(), selectedAction), null);
        if (weightVectorRelatedLabel != null) {
          weightVectorRelatedLabel.setText(String.format("%.4f", weightVector[i]));
        }
        i++;

      }
    }

    panel.repaint();
  }


  public void initializeTrainingWeightVector(int featureVectorSize) {

    if (previousResults != null) {
      return;
    }

    // read file
    previousResults = readFromFile();

    // if could not load initialize
    if (previousResults == null) {
      previousResults = new OffilneTrainerResults(new ArrayList<>());
      trainingWeights = new TrainingWeights(featureVectorSize);
    } else {
      trainingWeights = previousResults.getEpsiodes().get(previousResults.getEpsiodes().size() - 1).getWeightVector();
    }
  }

  private OffilneTrainerResults readFromFile() {
    OffilneTrainerResults pr1 = null;
    try {
      FileInputStream fi = new FileInputStream(resultsPath);
      ObjectInputStream oi = new ObjectInputStream(fi);

      // Read objects
      pr1 = (OffilneTrainerResults) oi.readObject();
      return pr1;

    } catch (FileNotFoundException fileNotFoundException) {
      return null;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return pr1;
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

  public static double dotProduct(double[] a, double[] b) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += a[i] * b[i];
    }
    return sum;
  }


}

