package tracks.singlePlayer.florabranchi.training;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.*;

import core.game.StateObservation;
import ontology.Types;
import tracks.singlePlayer.florabranchi.persistence.weights.OfflineTrainerResults;
import tracks.singlePlayer.florabranchi.persistence.weights.TrainingWeights;

public class LearningAgentDebug {

  //public boolean showJframe = false;
  public boolean showJframe = true;

  private Map<String, JLabel> labelMap = new HashMap<>();

  private static final String propertyLabelName = "_VALUE_LABEL";
  private static final String weightVectorLabelName = "_WEIGHT_LABEL";
  private static final String qValueLabelName = "_Q_VALUE";

  String HTMLlabelStr = "<html> %s </html>";

  private JPanel qPanel;
  private JPanel graphPanel;
  private JPanel debugValuesPanel;

  private JFrame frame;


  public LearningAgentDebug(final StateObservation stateObs,
                            final OfflineTrainerResults previousResult) {

    if (showJframe) {
      createJFrame(stateObs, previousResult);
    }
  }

  public void createJFrame(final StateObservation stateObs,
                           final OfflineTrainerResults previousResult) {

    frame = new JFrame("Weight Vector Debug");
    frame.setSize(1200, 1000);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);
    frame.setLayout(null);

    // Main panel
    JPanel mainPanel = (JPanel) frame.getContentPane();

    // Q Panel
    int qPanelx = 0;
    int qPaney = 0;
    int qPanelHeight = 300;
    int qPanelWidth = 600;

    qPanel = new JPanel();
    qPanel.setLayout(null);
    qPanel.setBounds(qPanelx, qPaney, qPanelWidth, qPanelHeight);
    //qPanel.setBackground(Color.LIGHT_GRAY);
    //qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.X_AXIS));

    // Graph panel
    int graphPanelY = 0;
    int graphPanelHeight = 300;
    int graphPanellWidth = 400;

    graphPanel = new JPanel();
    graphPanel.setBounds(qPanelWidth, graphPanelY, graphPanellWidth, graphPanelHeight);

    // Debug panel
    int debugPanelX = 0;
    int debugPaneHeight = 800;
    int debugPanelWidth = 1000;

    debugValuesPanel = new JPanel();
    debugValuesPanel.setLayout(null);
    debugValuesPanel.setBounds(debugPanelX, qPanelHeight, debugPanelWidth, debugPaneHeight);

    mainPanel.add(qPanel);
    mainPanel.add(graphPanel);
    mainPanel.add(debugValuesPanel);

    frame.setVisible(true);

    createUI(stateObs, previousResult);
  }


  public void updateQLabel(final Types.ACTIONS action,
                           final Object maxValue) {
    final JLabel jLabel = labelMap.get(buildQvalueKey(action));
    jLabel.setText(String.format("%.4f", maxValue));
  }

  public void createUI(final StateObservation stateObs,
                       final OfflineTrainerResults previousResult) {

    /// info panel
    // q panel
    // debug panel

    final List<Types.ACTIONS> avallableGameActions = stateObs.getAvailableActions();

    final int lineHeight = 20; //y
    final int lineWidth = 200; //x

    // INFO LINES DEBUG
    final int initialXDisplacement = 10;
    final int initialYDisplacement = 20;
    int headerElements = 2;

    // Q DEBUG
    int spacingBetweenHeaderAndQ = 10;
    final int initialQDisplacementX = 10;
    final int initialQDisplacementY = initialYDisplacement + spacingBetweenHeaderAndQ + (headerElements * lineHeight);
    final int qValuesLineSize = 200;

    // PROPERTY DEBUG
    int initialPropertiesDisplacementX = 10;
    int initialPropertiedDisplacementY = 0;

    int currElement = 0;

    // Build Debug Header

    JLabel title = new JLabel(String.format(HTMLlabelStr, "Learning Agent Debug"), JLabel.CENTER);
    title.setBounds(initialXDisplacement, initialYDisplacement, lineWidth, lineHeight);
    qPanel.add(title);
    currElement++;

    JLabel totalGames = new JLabel(String.format("Total Games: %s", previousResult.totalGames), JLabel.CENTER);
    totalGames.setBounds(initialXDisplacement, initialYDisplacement + (currElement * lineHeight), lineWidth, lineHeight);
    qPanel.add(totalGames);
    currElement++;

    JLabel totalWins = new JLabel(String.format("Total Wins: %s", previousResult.wins), JLabel.CENTER);
    totalWins.setBounds(initialXDisplacement, initialYDisplacement + (currElement * lineHeight), lineWidth, lineHeight);
    qPanel.add(totalWins);
    currElement++;

    JLabel avgScore = new JLabel(String.format("Average score: %.4f", previousResult.getTotalScore() /
        previousResult.getTotalGames()), JLabel.CENTER);
    avgScore.setBounds(initialXDisplacement, initialYDisplacement + (currElement * lineHeight), lineWidth, lineHeight);
    qPanel.add(avgScore);
    currElement++;

    int elementX;
    int elementY;

    // Build Q Values panel

    JLabel currentQValuesLabel = new JLabel("CURRENT Q VALUES", JLabel.CENTER);
    currentQValuesLabel.setBounds(initialQDisplacementX, initialQDisplacementY + (currElement * lineHeight), qValuesLineSize, lineHeight);
    qPanel.add(currentQValuesLabel);
    currElement++;

    // Create Q values debug
    for (Types.ACTIONS action : avallableGameActions) {

      JLabel newLabel = new JLabel(String.format("Q VALUE FOR %s ", action), JLabel.CENTER);
      newLabel.setBounds(initialQDisplacementX, initialQDisplacementY + (currElement * lineHeight), qValuesLineSize, lineHeight);

      JLabel weightVectorValue = new JLabel(String.valueOf(0), JLabel.CENTER);
      weightVectorValue.setBounds(initialQDisplacementX + qValuesLineSize, initialQDisplacementY + (currElement * lineHeight), qValuesLineSize, lineHeight);

      qPanel.add(weightVectorValue);
      qPanel.add(newLabel);

      labelMap.put(buildQvalueKey(action), weightVectorValue);
      currElement++;
    }

    // Build debugValuesPanel

    // Properties
    currElement = 0;
    elementX = initialPropertiesDisplacementX;

    int multiplier = 2;
    // Add column for each game action
    for (Types.ACTIONS action : avallableGameActions) {
      JLabel HFeatureValue = new JLabel(String.format("WEIGHT %s", action), JLabel.CENTER);
      HFeatureValue.setBounds(elementX + (multiplier * lineWidth), initialPropertiedDisplacementY, lineWidth, lineHeight);
      debugValuesPanel.add(HFeatureValue);
      multiplier++;
    }

    JLabel hPropertyLabel = new JLabel("PROPERTY", JLabel.CENTER);
    hPropertyLabel.setBounds(elementX, initialPropertiedDisplacementY, lineWidth, lineHeight);
    currElement++;

    JLabel hValueLabel = new JLabel("FEATURE VALUE", JLabel.CENTER);
    hValueLabel.setBounds(elementX + lineWidth, initialPropertiedDisplacementY, lineWidth, lineHeight);
    currElement++;

    debugValuesPanel.add(hPropertyLabel);
    debugValuesPanel.add(hValueLabel);

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
          debugValuesPanel.add(weightVectorValue);
          labelMap.put(buildWeightVectorKey(entry, action), weightVectorValue);
        }

        // Update label map
        labelMap.put(buildPropertyKey(entry), valueLabel);
        debugValuesPanel.add(propertyLabel);
        debugValuesPanel.add(valueLabel);

        currElement++;
      }
    }


    debugValuesPanel.repaint();
    qPanel.repaint();
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
                               final TrainingWeights trainingWeights,
                               final Map<Integer, Double> episodeTotalScoreMap) {

    // Fill Chart panel
    drawGraph(episodeTotalScoreMap);

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

    qPanel.repaint();
  }

  public void drawGraph(final Map<Integer, Double> rewardPerEpisodeMap) {


    final XYSeries series = new XYSeries("Score over Episodes");

    // Show only last n elements
    int elementsToShow = 300;
    final int size = rewardPerEpisodeMap.size();

    int firstElement = 0;

    if (elementsToShow > size) {
      firstElement = size - elementsToShow;
    } else {
      firstElement = size - elementsToShow;
    }

    final int finalFirstElement = firstElement;
    final Map<Integer, Double> collectedValues = rewardPerEpisodeMap.entrySet().stream()
        .filter(entry -> entry.getKey() >= finalFirstElement)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    collectedValues.forEach(series::add);

    final XYSeriesCollection data = new XYSeriesCollection(series);
    final JFreeChart chart = ChartFactory.createXYLineChart(
        "Previous Game Scores",
        "Episode",
        "Game score",
        data,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    );

    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(400, 300));
    graphPanel.add(chartPanel, BorderLayout.CENTER);
  }


  public void closeJframe() {
    if (frame != null) {
      frame.dispose();
    }
  }


}
