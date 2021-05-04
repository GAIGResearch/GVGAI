package tracks.singlePlayer.florabranchi.agents.meta;

import java.io.Serializable;

public enum EMetaActions implements Serializable {
  // vetor com 3 flags
  //[a, b, c]

  // slot
  // [1] 0 0 0

  // ver artigo


  NIL,
  TURN_TREE_REUSE_OFF,
  TURN_TREE_REUSE_ON,
  RAW_SCORE_ON,
  RAW_SCORE_OFF,
  MACRO_ACTIONS_ON,
  MACRO_ACTIONS_OFF,
}
