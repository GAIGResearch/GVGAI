package tracks.singlePlayer.florabranchi.training;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.*;

import core.game.StateObservation;
import ontology.Types;

public class LearningAgentDebug {

  public boolean showJframe = false;
  //public boolean showJframe = true;

  private Map<String, JLabel> labelMap = new HashMap<>();

  private static final String propertyLabelName = "_VALUE_LABEL";
  private static final String weightVectorLabelName = "_WEIGHT_LABEL";
  private static final String qValueLabelName = "_Q_VALUE";

  String HTMLlabelStr = "<html> %s </html>";

  private JPanel panel;

  private JFrame frame;

  public LearningAgentDebug(final StateObservation stateObs,
                            final OfflineTrainerResults previousResult) {

    if (showJframe) {
      createJFrame(stateObs, previousResult);
    }
  }


  public void updateQLabel(final Types.ACTIONS action,
                           final Object maxValue) {
    final JLabel jLabel = labelMap.get(buildQvalueKey(action));
    jLabel.setText(String.format("%.4f", maxValue));
  }

  public void createUI(final StateObservation stateObs,
                       final OfflineTrainerResults previousResult) {

    final List<Types.ACTIONS> avallableGameActions = stateObs.getAvailableActions();

    final int lineHeight = 20; //y
    final int lineWidth = 200; //x

    // INFO LINES DEBUG
    final int initialXDisplacement = 10;
    final int initialYDisplacement = 20;
    int headerElements = 2;

    // Q DEBUG
    int spacingBetweenHeaderAndQ = 0;
    final int initialQDisplacementX = 10;
    final int initialQDisplacementY = initialYDisplacement + spacingBetweenHeaderAndQ + (headerElements * lineHeight);
    final int qValuesLineSize = 200;

    // PROPERTY DEBUG
    int spacingBetweenQAndProperties = 100;
    int initialPropertiesDisplacementX = 10;
    int initialPropertiedDisplacementY = spacingBetweenQAndProperties + initialQDisplacementY + (avallableGameActions.size() + 1) * lineHeight;

    int currElement = 0;

    // Debug Header

    JLabel title = new JLabel(String.format(HTMLlabelStr, "Learning Agent Debug"), JLabel.CENTER);
    title.setBounds(initialXDisplacement, initialYDisplacement, lineWidth, lineHeight);
    panel.add(title);
    currElement++;

    JLabel totalGames = new JLabel(String.format("Total Games: %s", previousResult.totalGames), JLabel.CENTER);
    totalGames.setBounds(initialXDisplacement, initialYDisplacement + (currElement * lineHeight), lineWidth, lineHeight);
    panel.add(totalGames);
    currElement++;

    JLabel totalWins = new JLabel(String.format("Total Wins: %s", previousResult.wins), JLabel.CENTER);
    totalWins.setBounds(initialXDisplacement, initialYDisplacement + (currElement * lineHeight), lineWidth, lineHeight);
    panel.add(totalWins);
    currElement++;

    JLabel avgScore = new JLabel(String.format("Average score: %s", previousResult.getTotalScore() /
        previousResult.getTotalGames()), JLabel.CENTER);
    avgScore.setBounds(initialXDisplacement, initialYDisplacement + (currElement * lineHeight), lineWidth, lineHeight);
    panel.add(avgScore);
    currElement++;

    int elementX;
    int elementY;

    // Q Values

    JLabel currentQValuesLabel = new JLabel("CURRENT Q VALUES", JLabel.CENTER);
    currentQValuesLabel.setBounds(initialQDisplacementX, initialQDisplacementY + (currElement * lineHeight), qValuesLineSize, lineHeight);
    panel.add(currentQValuesLabel);
    currElement++;

    // Create Q values debug
    for (Types.ACTIONS action : avallableGameActions) {

      JLabel newLabel = new JLabel(String.format("Q VALUE FOR %s ", action), JLabel.CENTER);
      newLabel.setBounds(initialQDisplacementX, initialQDisplacementY + (currElement * lineHeight), qValuesLineSize, lineHeight);

      JLabel weightVectorValue = new JLabel(String.valueOf(0), JLabel.CENTER);
      weightVectorValue.setBounds(initialQDisplacementX + qValuesLineSize, initialQDisplacementY + (currElement * lineHeight), qValuesLineSize, lineHeight);

      panel.add(weightVectorValue);
      panel.add(newLabel);

      labelMap.put(buildQvalueKey(action), weightVectorValue);
      currElement++;
    }


    // Properties
    currElement = 0;
    elementX = initialPropertiesDisplacementX;

    int multiplier = 2;
    // Add column for each game action
    for (Types.ACTIONS action : avallableGameActions) {
      JLabel HFeatureValue = new JLabel(String.format("WEIGHT %s", action), JLabel.CENTER);
      HFeatureValue.setBounds(elementX + (multiplier * lineWidth), initialPropertiedDisplacementY, lineWidth, lineHeight);
      panel.add(HFeatureValue);
      multiplier++;
    }

    JLabel hPropertyLabel = new JLabel("PROPERTY", JLabel.CENTER);
    hPropertyLabel.setBounds(elementX, initialPropertiedDisplacementY, lineWidth, lineHeight);

    JLabel hValueLabel = new JLabel("FEATURE VALUE", JLabel.CENTER);
    hValueLabel.setBounds(elementX + lineWidth, initialPropertiedDisplacementY, lineWidth, lineHeight);

    panel.add(hPropertyLabel);
    panel.add(hValueLabel);

    currElement++;

    // Populate available properties
    final Set<String> propertyValueMap = FeatureVectorController.getAvailableProperties();

    if (propertyValueMap != null) {

      for (String entry : propertyValueMap) {

        // Update position
        elementY = initialPropertiedDisplacementY + currElement;


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
                               final Types.ACTIONS selectedAction,
                               final TrainingWeights trainingWeights) {

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

  public void createJFrame(final StateObservation stateObs,
                           final OfflineTrainerResults previousResult) {
    frame = new JFrame("Weight Vector Debug");
    panel = new JPanel();
    panel.setLayout(null);
    frame.add(panel);

    frame.setSize(1200, 1000);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);
    frame.setLayout(null);

    createUI(stateObs, previousResult);
  }


  public void closeJframe() {
    if (frame != null) {
      frame.dispose();
    }
  }


}
