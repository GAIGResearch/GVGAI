package YOLOBOT.SubAgents.HandleMCTS.RolloutPolicies;

import java.util.ArrayList;

import ontology.Types.ACTIONS;
import YOLOBOT.YoloState;
import YOLOBOT.SubAgents.HandleMCTS.MCTHandler;

public class EpsilonGreedyBestFirstRolloutPolicy extends RolloutPolicy {

	private RandomRolloutPolicy randomPolicy;
	private HeuristicRolloutPolicy aStarPolicy;
	private double bestFirstProbability;
	
	public EpsilonGreedyBestFirstRolloutPolicy(double bestFirstProbability) {
		randomPolicy = new RandomRolloutPolicy();
		aStarPolicy = new HeuristicRolloutPolicy();
		this.bestFirstProbability = bestFirstProbability;
	}
	
	public EpsilonGreedyBestFirstRolloutPolicy() {
		this(0);

	}

	@Override
	public ArrayList<ACTIONS> possibleNextActions(YoloState state,
			ArrayList<ACTIONS> forbiddenAction, boolean forceNotEpsilon) {
		if(forceNotEpsilon || Math.random() < bestFirstProbability){
			return aStarPolicy.possibleNextActions(state, forbiddenAction, forceNotEpsilon);
		}else{
			if(MCTHandler.DEBUG_TRACE)
				System.out.print("[e]");
			return randomPolicy.possibleNextActions(state, forbiddenAction, forceNotEpsilon);
		}
	}

}
