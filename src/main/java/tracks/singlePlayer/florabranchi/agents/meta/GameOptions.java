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

  public void act(EMetaActions change) {

    switch (change) {
      case NIL:
        break;
      case TURN_TREE_REUSE_OFF:
        reuseTree = false;
        break;
      case TURN_TREE_REUSE_ON:
        reuseTree = true;
        break;
      case RAW_SCORE_ON:
        rawGameScore = true;
        break;
      case RAW_SCORE_OFF:
        rawGameScore = false;
        break;
      default:
        break;
    }


  }


}
