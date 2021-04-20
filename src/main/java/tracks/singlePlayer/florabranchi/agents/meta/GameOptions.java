package tracks.singlePlayer.florabranchi.agents.meta;

public class GameOptions {

  int treeSearchSize;
  int simulationDepth;
  boolean reuseTree;
  boolean rawGameScore;
  boolean lossAvoidance;
  boolean expandAllNodes;
  boolean safetyPreprunning;

  int rawScoreWeight;
  int totalResourcesWeight;
  int resourceScoreWeight;
  int explorationScoreWeight;
  int movablesScoreWeight;
  int portalScoreWeight;


}
