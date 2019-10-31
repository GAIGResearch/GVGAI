package tracks.singlePlayer.florabranchi.agents;

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
import java.util.TreeMap;

import javax.swing.*;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.florabranchi.GameResulsTracker;
import tracks.singlePlayer.florabranchi.GameResults;
import tracks.singlePlayer.florabranchi.training.FeatureVectorController;
import tracks.singlePlayer.florabranchi.training.LearningAgentDebug;
import tracks.singlePlayer.florabranchi.training.OfflineTrainerResults;
import tracks.singlePlayer.florabranchi.training.TrainingLog;
import tracks.singlePlayer.florabranchi.training.TrainingWeights;

public class OfflineTrainerAgent extends AbstractPlayer {

  private static final String resultsPath = "./previousResults.txt";
  private static final String statisticsPath = "./offlineTrainingRecords.txt";

  private final TrainingLog log;

  private final LearningAgentDebug learningAgentDebug;

  private FeatureVectorController featureVectorController;

  protected Random randomGenerator = new Random();

  private GameResulsTracker gameTracker = new GameResulsTracker(statisticsPath);

  private Map<String, JLabel> labelMap = new HashMap<>();

  private static final double ALFA = 0.5;

  private static final double GAMMA = 0.9;

  // 10%
  private static final double EXPLORATION_EPSILON = 10;

  // Data
  public TrainingWeights trainingWeights;

  public OfflineTrainerResults previousResults;

  // State related data
  private Types.ACTIONS previousAction;
  private StateObservation previousState;
  private double previousScore;


  public OfflineTrainerAgent(final StateObservation stateObs,
                             final ElapsedCpuTimer elapsedTimer) {
    super();
    log = new TrainingLog(new ArrayList<>());
    featureVectorController = new FeatureVectorController();
    initializeTrainingWeightVector(featureVectorController.getSize());

    learningAgentDebug = new LearningAgentDebug(stateObs, previousResults);

  }

  @Override
  public Types.ACTIONS act(final StateObservation stateObs,
                           final ElapsedCpuTimer elapsedTimer) {

    log.write(String.format("Act for Game Tick %d", stateObs.getGameTick()));

    return getActionAndUpdateWeightVectorValues(stateObs, elapsedTimer);
  }

  @Override
  public void result(final StateObservation stateObs,
                     final ElapsedCpuTimer elapsedCpuTimer) {

    // last update - reward if won, negative if loss
    final Types.WINNER gameWinner = stateObs.getGameWinner();
    double reward = 5000;
    boolean won = false;
    if (gameWinner.equals(Types.WINNER.PLAYER_LOSES)) {
      reward = -reward;
      log.write("Player lost");
    } else {
      won = true;
      log.write("Player won");
    }

    updateRecord(won);
    updateAfterLastAction(reward, previousAction, previousState);

    logCurrentWeights();
    saveResults(won, stateObs.getGameScore());
    learningAgentDebug.closeJframe();
    super.result(stateObs, elapsedCpuTimer);
  }

  public void updateRecord(boolean won) {
    GameResults previousResults = this.gameTracker.readFromFile();
    if (previousResults == null) {
      previousResults = new GameResults(statisticsPath);
    }
    previousResults.updateResults(won);
    gameTracker.saveToFile(previousResults);
  }


  private void updateWeightVectorForAction(final Types.ACTIONS previousAction,
                                           final TreeMap<String, Double> featureVectorForState,
                                           final double delta) {

    // Get Weight vector for previous action (a) (W i a)
    final TreeMap<String, Double> relatedWeightVector = trainingWeights.getWeightVectorForAction(previousAction);

    for (Map.Entry<String, Double> weightMapEntry : relatedWeightVector.entrySet()) {

      final String featureType = weightMapEntry.getKey();

      // w = w + lambda * delta * f()
      final double updatedWeight = weightMapEntry.getValue() + (ALFA * delta * featureVectorForState.get(featureType));

      // Update
      trainingWeights.updateWeightVector(previousAction, featureType, updatedWeight);
    }
  }

  // need: a', s', a, s, r
  public void getActionAndUpdateWeightVectorValues(final double stateReward,
                                                   final Types.ACTIONS previousAction,
                                                   final StateObservation previousState,
                                                   final Types.ACTIONS currentAction,
                                                   final StateObservation currentState) {

    // Get feature values for previous state (s)
    final TreeMap<String, Double> initialFeatureVector = featureVectorController.extractFeatureVector(previousState);

    // delta = r + gamma (Qa'(s')) - Qa(s)
    double delta = stateReward + (GAMMA * getQValueForAction(currentState, currentAction)) - getQValueForAction(previousState, previousAction);

    // Update weights
    updateWeightVectorForAction(previousAction, initialFeatureVector, delta);

  }

  public void updateAfterLastAction(final double stateReward,
                                    final Types.ACTIONS previousAction,
                                    final StateObservation previousState) {


    // delta = r - Qa(s)
    double delta = stateReward - getQValueForAction(previousState, previousAction);

    // Get feature values for previous state (s)
    // fi(s, a)
    final TreeMap<String, Double> featureVector = featureVectorController.extractFeatureVector(previousState);

    updateWeightVectorForAction(previousAction, featureVector, delta);

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
  public Types.ACTIONS getActionAndUpdateWeightVectorValues(final StateObservation stateObs,
                                                            final ElapsedCpuTimer elapsedTimer) {

    // Reward = curr score - previous score
    double stateScore = stateObs.getGameScore();
    double reward = stateScore - previousScore;

    log.write(String.format("Updating weights with reward %s after action: ", reward));

    // First play
    if (previousAction == null) {
      final Types.ACTIONS randomAction = returnRandomAction(stateObs);

      // a
      previousAction = randomAction;
      // s
      previousState = stateObs;
      previousScore = 0;

      return randomAction;
    }

    // Select best action given current q values for (s') / exploration play
    final Types.ACTIONS selectedAction = selectBestPerceivedAction(stateObs);

    // need: s, a r, s', a'
    getActionAndUpdateWeightVectorValues(reward, previousAction, previousState, selectedAction, stateObs);

    final TreeMap<String, Double> featuresForCurrState = featureVectorController.extractFeatureVector(stateObs);

    if (learningAgentDebug.showJframe) {
      learningAgentDebug.writeResultsToUi(featuresForCurrState, selectedAction, trainingWeights);
    }

    // Update last values
    previousAction = selectedAction;
    previousState = stateObs;
    previousScore = stateObs.getGameScore();

    return selectedAction;
  }


  /**
   * Execute dot product between weights and features
   */
  public double getQValueForAction(final StateObservation state,
                                   final Types.ACTIONS action) {

    // Extract feature array
    final TreeMap<String, Double> featureAfterAction = featureVectorController.extractFeatureVector(state);

    // Get related weight vector
    final TreeMap<String, Double> weightVectorForAction = trainingWeights.getWeightVectorForAction(action);

    return dotProduct(featureAfterAction, weightVectorForAction);
  }

  public void saveResults(final boolean won,
                          final double score) {

    final int episode = previousResults.update(trainingWeights, won, score);
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

        // Update Q Values in UI
        if (learningAgentDebug.showJframe) {
          learningAgentDebug.updateQLabel(action, maxValue);
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
      previousResults = new OfflineTrainerResults(featureVectorSize);
      trainingWeights = new TrainingWeights(featureVectorSize);
      logCurrentWeights();
    } else {
      int previousEpisode = previousResults.getTotalGames();
      log.write(String.format("Loading previous results from episode %s", previousEpisode));
      trainingWeights = previousResults.getWeightVector();
      logCurrentWeights();
    }
  }

  private OfflineTrainerResults readFromFile() {
    OfflineTrainerResults pr1 = null;
    try {
      FileInputStream fi = new FileInputStream(resultsPath);
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

  private void saveToFile(final OfflineTrainerResults results) {

    try {

      FileOutputStream fileOut = new FileOutputStream(resultsPath);
      ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
      System.out.println(results.toString());
      objectOut.writeObject(results);
      objectOut.close();
      System.out.println("The Object  was succesfully written to a file");

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public double dotProduct(TreeMap<String, Double> a,
                           TreeMap<String, Double> b) {
    double sum = 0;
    for (String property : FeatureVectorController.getAvailableProperties()) {
      sum += a.get(property) * b.get(property);
    }

    return sum;
  }


}

