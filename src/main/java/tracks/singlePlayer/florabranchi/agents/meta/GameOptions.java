package tracks.singlePlayer.florabranchi.agents.meta;

public class GameOptions {

  public boolean reuseTree;

  public boolean lossAvoidance;
  public boolean expandAllNodes;
  public boolean safetyPreprunning;

  // exploração produnda /rasa
  public boolean shallowRollout;

  public boolean rawGameScore;

  // total de ações =  2^flags


}
