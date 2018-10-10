package YOLOBOT.SubAgents.HandleMCTS.RolloutPolicies;

import java.util.ArrayList;

import ontology.Types.ACTIONS;
import YOLOBOT.YoloState;

public abstract class RolloutPolicy {

	public ACTIONS nextAction(YoloState state, ArrayList<ACTIONS> forbiddenActions) {
		return nextAction(state, forbiddenActions, false);
	}
	public ACTIONS nextAction(YoloState state, ArrayList<ACTIONS> forbiddenActions, boolean forceNotEpsilon){
		ArrayList<ACTIONS> validActions = possibleNextActions(state, forbiddenActions, forceNotEpsilon);
		validActions = possibleNextActions(state, forbiddenActions, true);
		return validActions.get((int)(validActions.size()*Math.random()));
	}

	public abstract ArrayList<ACTIONS> possibleNextActions(YoloState state, ArrayList<ACTIONS> forbiddenAction, boolean forceNotEpsilon);

}
