package tracks.singlePlayer.florabranchi.agents.meta;

public class GameOptions {

  public int gameId;

  public boolean reuseTree;
  public boolean rawGameScore;
  public boolean macroActions;

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
      case MACRO_ACTIONS_ON:
        macroActions = true;
        break;
      case MACRO_ACTIONS_OFF:
        macroActions = false;
        break;
      default:
        break;
    }


  }


}
