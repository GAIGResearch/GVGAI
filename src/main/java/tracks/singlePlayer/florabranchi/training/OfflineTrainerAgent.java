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

  boolean showJframe = true;

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
    log = new TrainingLog(new ArrayList<>());
    featureVectorController = new FeatureVectorController();
    initializeTrainingWeightVector(featureVectorController.getSize());

    if (showJframe) {
      createJFrame(stateObs);
    }
  }

  @Override
  public Types.ACTIONS act(final StateObservation stateObs,
                           final ElapsedCpuTimer elapsedTimer) {

    log.write(String.format("Act for Game Tick %d", stateObs.getGameTick()));

    return getActionAndupdateWeightVectorValues(stateObs, elapsedTimer);
  }

  @Override
  public void result(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedCpuTimer) {

    // last update - reward if won, negative if loss
    final Types.WINNER gameWinner = stateObs.getGameWinner();
    double reward = 20;
    if (gameWinner.equals(Types.WINNER.PLAYER_LOSES)) {
      reward = -50;
      log.write("Player lost");
    } else {
      log.write("Player won");
    }
    getActionAndupdateWeightVectorValues(stateObs, elapsedCpuTimer, reward);

    logCurrentWeights();
    saveResults();
    closeJframe();
    super.result(stateObs, elapsedCpuTimer);
  }

  public void createJFrame(final StateObservation stateObs) {
    frame = new JFrame("Weight Vector Debug");
    panel = new JPanel();
    panel.setLayout(null);
    frame.add(panel);

    frame.setSize(1200, 1000);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);
    frame.setLayout(null);

    createUI(stateObs);
  }

  public void closeJframe() {
    if (frame != null) {
      frame.dispose();
    }
  }

  // need: a', s', a, s, r
  public void getActionAndupdateWeightVectorValues(final double stateReward,
                                                   final Types.ACTIONS previousAction,
                                                   final StateObservation previousState,
                                                   final Types.ACTIONS currentAction,
                                                   final StateObservation currentState) {


    // Get Weight vector for previous action (a)
    final TreeMap<String, Double> weightVectorForPreviousAction = trainingWeights.getWeightVectorForAction(previousAction);

    // Get feature values for previous state (s)
    final TreeMap<String, Double> initialFeatureVector = featureVectorController.extractFeatureVector(previousState);

    // Get Weight vector for new action (a')
    final TreeMap<String, Double> weightVectorForCurrentAction = trainingWeights.getWeightVectorForAction(currentAction);

    // Extract feature values (s')
    final TreeMap<String, Double> featureVectorAfterAction = featureVectorController.extractFeatureVector(currentState);

    // delta = r + gamma (Qa'(s')) - Qa(s)
    double delta = stateReward + GAMMA * (dotProduct(weightVectorForCurrentAction, featureVectorAfterAction))
        - dotProduct(weightVectorForPreviousAction, initialFeatureVector);

    // Update weights
    for (Map.Entry<String, Double> weightMapEntry : weightVectorForPreviousAction.entrySet()) {

      double initialW = weightMapEntry.getValue();

      // w = w + lambda * delta * f()
      final double updatedWeight = initialW + (ALFA * delta * initialFeatureVector.getOrDefault(weightMapEntry.getKey(), 0d));

      // Update global weights
      trainingWeights.updateWeightVector(currentAction, weightMapEntry.getKey(), updatedWeight);
    }

  }


  public Types.ACTIONS returnRandomAction(final StateObservation stateObservation) {

    if (stateObservation.getAvailableActions().isEmpty()) {
      return Types.ACTIONS.ACTION_NIL;
    }

    int index = randomGenerator.nextInt(stateObservation.getAvailableActions().size());
    return stateObservation.getAvailableActions().get(index);
  }


  /**
   * Uses default game score to update values
   */
  public Types.ACTIONS getActionAndupdateWeightVectorValues(final StateObservation stateObs,
                                                            final ElapsedCpuTimer elapsedTimer) {

    // Reward = curr score - previous score
    double stateScore = stateObs.getGameScore();
    double reward = stateScore - previousScore;
    return getActionAndupdateWeightVectorValues(stateObs, elapsedTimer, reward);
  }

  public Types.ACTIONS getActionAndupdateWeightVectorValues(final StateObservation stateObs,
                                                            final ElapsedCpuTimer elapsedTimer,
                                                            final double reward) {

    log.write(String.format("Updating weights with reward %s after action: ", reward));
    if (previousAction == null) {
      final Types.ACTIONS randomAction = returnRandomAction(stateObs);

      // Update last values
      // a
      previousAction = randomAction;
      // s
      previousState = stateObs;
      previousScore = 0;

      return randomAction;
    }

    // Select best action given current weight vector (a')
    final Types.ACTIONS selectedAction = selectBestPerceivedAction(stateObs);

    // need: a, s, r, a', s1
    getActionAndupdateWeightVectorValues(reward, previousAction, previousState, selectedAction, stateObs);

    final TreeMap<String, Double> featuresForCurrState = featureVectorController.extractFeatureVector(stateObs);

    if (showJframe) {
      writeResultsToUi(featuresForCurrState, selectedAction);
    }

    // Update last values
    previousAction = selectedAction;
    previousState = stateObs;
    previousScore = stateObs.getGameScore();

    return selectedAction;
  }

  public double getQValueForAction(final StateObservation initialState,
                                   final Types.ACTIONS action) {

    // Extract feature array
    final TreeMap<String, Double> featureAfterAction = featureVectorController.extractFeatureVector(initialState);

    // Get related weight vector
    final TreeMap<String, Double> weightVectorForAction = trainingWeights.getWeightVectorForAction(action);

    return dotProduct(featureAfterAction, weightVectorForAction);
  }

  public void saveResults() {

    final int episode = previousResults.addNewEpisode(trainingWeights);
    log.saveToFile(episode);
    saveToFile(previousResults);
  }


  public Types.ACTIONS selectBestPerceivedAction(final StateObservation stateObservation) {

    if (stateObservation.getAvailableActions().isEmpty()) {
      log.write("Selecting NIL action to calculate last weight vector");
      return Types.ACTIONS.ACTION_NIL;
    }

    // Exploration parameter
    int rand = randomGenerator.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      final Types.ACTIONS randomAction = returnRandomAction(stateObservation);
      log.write(String.format("Exploring random action %s", randomAction));
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

        if (showJframe) {
          final JLabel jLabel = labelMap.get(buildQvalueKey(action));
          jLabel.setText(String.format("%.4f", maxValue));
        }
      }
    }

    // Return random if draw
    if (duplicatedActions.size() > 1) {

      Collections.shuffle(duplicatedActions);
      final Types.ACTIONS selectedActon = duplicatedActions.get(randomGenerator.nextInt(duplicatedActions.size() - 1));
      log.write(String.format("Best play: draw for score %s, selected: %s", maxValue, selectedActon));
      return selectedActon;
    }

    log.write(String.format("Best play: %s - score - %.2f", bestAction, maxValue));
    return bestAction;
  }

  public void createUI(final StateObservation stateObs) {

    int initialXDisplacement = 10;
    int initialYDisplacement = 10;
    int lineHeight = 20; //y
    int lineWidth = 200; //x

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

    final TreeMap<String, Double> weightVectorForAction = trainingWeights.getWeightVectorForAction(selectedAction);

    if (featureVectorAfterAction != null) {

      int i = 0;
      for (Map.Entry<String, Double> entry : featureVectorAfterAction.entrySet()) {

        JLabel propertyRelatedLabel = labelMap.getOrDefault(buildPropertyKey(entry.getKey()), null);
        if (propertyRelatedLabel != null) {
          propertyRelatedLabel.setText(String.format("%.4f", entry.getValue()));
        }

        JLabel weightVectorRelatedLabel = labelMap.getOrDefault(buildWeightVectorKey(entry.getKey(), selectedAction), null);
        if (weightVectorRelatedLabel != null) {
          weightVectorRelatedLabel.setText(String.format("%.4f", weightVectorForAction.get(entry.getKey())));
        }
        i++;

      }
    }

    panel.repaint();
  }

  public void logCurrentWeights() {
    final TreeMap<Types.ACTIONS, TreeMap<String, Double>> weightVectorMap = trainingWeights.getWeightVectorMap();
    weightVectorMap.forEach((key, value) -> {
      log.write(String.format("Weight Vector for %s", key));
      value.forEach((key1, value1) -> log.write(String.format("%s - %s", key1, value1)));
    });
  }


  public void initializeTrainingWeightVector(int featureVectorSize) {

    if (previousResults != null) {
      log.write("Previous results still in memory");
      logCurrentWeights();
      return;
    }

    // read file
    previousResults = readFromFile();

    // if could not load initialize
    if (previousResults == null) {
      log.write("No previous results, creating new Weights");
      previousResults = new OffilneTrainerResults(new ArrayList<>());
      trainingWeights = new TrainingWeights(featureVectorSize);
      logCurrentWeights();
    } else {
      int previousEpisode = previousResults.getEpsiodes().size() - 1;
      log.write(String.format("Loading previous results from episode %s", previousEpisode));
      trainingWeights = previousResults.getEpsiodes().get(previousEpisode).getWeightVector();
      logCurrentWeights();
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

  public double dotProduct(TreeMap<String, Double> a,
                           TreeMap<String, Double> b) {
    double sum = 0;
    for (String property : FeatureVectorController.getAvailableProperties()) {
      sum += a.getOrDefault(property, 0d) * b.getOrDefault(property, 0d);
    }

    return sum;
  }


}

