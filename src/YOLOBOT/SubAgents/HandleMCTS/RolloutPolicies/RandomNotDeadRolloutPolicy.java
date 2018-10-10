package YOLOBOT.SubAgents.HandleMCTS.RolloutPolicies;

import java.util.ArrayList;

import ontology.Types.ACTIONS;
import YOLOBOT.YoloState;
import YOLOBOT.Util.Wissensdatenbank.YoloKnowledge;

public class RandomNotDeadRolloutPolicy extends RolloutPolicy {

	RandomRolloutPolicy randomPolicy;
	double epsilon = 0.9;
	
	public RandomNotDeadRolloutPolicy() {
		randomPolicy = new RandomRolloutPolicy();
	}
	

	@Override
	public ArrayList<ACTIONS> possibleNextActions(YoloState state,
			ArrayList<ACTIONS> forbiddenAction, boolean forceNotEpsilon) {
		
		ArrayList<ACTIONS> validActions = randomPolicy.possibleNextActions(state, forbiddenAction, forceNotEpsilon);
		
		if(forbiddenAction != null)
			validActions.removeAll(forbiddenAction);
		
		if(validActions.isEmpty() || (!forceNotEpsilon && Math.random()>epsilon)){
			//If no action seems valid or Random:	Choose from all
			return  randomPolicy.possibleNextActions(state, forbiddenAction, forceNotEpsilon);
		}else{
			return validActions;
		}
		
	}
}
