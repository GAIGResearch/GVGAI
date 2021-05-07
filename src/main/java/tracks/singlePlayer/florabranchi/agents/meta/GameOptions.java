package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.HashMap;
import java.util.Map;

public class GameOptions {

  Map<String, EGameParameters> options = new HashMap<>;


  // total de ações =  2^flags


  MabParameters toMab() {
    MabParameters mabParameters = new MabParameters();
    mabParameters.addParameter(EMetaParameters.TREE_REUSE, treeReuse);
    mabParameters.addParameter(EMetaParameters.RAW_GAME_SCORE, rawGameScore);
    mabParameters.addParameter(EMetaParameters.MACRO_ACTIONS, macroActions);
    mabParameters.addParameter(EMetaParameters.LOSS_AVOIDANCE, lossAvoidance);
    mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, earlyInitialization);
    mabParameters.addParameter(EMetaParameters.SELECT_HIGHEST_SCORE_CHILD, selectHighestScoreChild);
    return mabParameters;
  }


}
