package agents.MaastCTS2.playout;

import java.util.ArrayList;
import java.util.HashMap;

import agents.MaastCTS2.Globals;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.MctNode;
import agents.MaastCTS2.model.StateObs;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Random play-out strategy
 * 
 * <p> NOTE: This class  has not been used for a long time, and may not include all the special cases
 * and/or heuristics that have been added to NstPlayout in the meantime
 * 
 * @author Dennis Soemers
 *
 */
public class RandomPlayout implements IPlayoutStrategy {
	private int maxPlayoutDepth;

	public RandomPlayout(int maxPlayoutDepth) {
		this.maxPlayoutDepth = maxPlayoutDepth;
	}

	@Override
	public MctNode runPlayout(MctNode node, ElapsedCpuTimer elapsedTimer) {
		StateObservation state = node.getStateObs();
		StateObs stateObs;
		if(state == node.getSavedStateObs()){
			stateObs = new StateObs(state, true);	// last node of selection step has a closed-loop-style saved state
		}
		else{	// doing normal open-loop stuff
			stateObs = new StateObs(state, false);
		}
		
		int depth = 0;
		
		HashMap<Integer, Integer> previousResources = state.getAvatarResources();
		HashMap<Integer, Integer> nextResources;
		
		for (/**/; depth < maxPlayoutDepth; ++depth) {
			if (state.isGameOver()) {
				break;
			}
			
			double previousScore = state.getGameScore();
			int previousNumEvents = state.getHistoricEventsHistory().size();
			Vector2d previousAvatarPos = state.getAvatarPosition();
			Vector2d previousAvatarOrientation = state.getAvatarOrientation();
			
			ArrayList<ACTIONS> unexpandedActions = node.getUnexpandedActions();
			ACTIONS randomAction = unexpandedActions.remove(Globals.RNG.nextInt(unexpandedActions.size()));

			MctNode newNode = new MctNode(node, randomAction);
			stateObs = newNode.generateNewStateObs(stateObs, randomAction);
			state = stateObs.getStateObsNoCopy();
			
			node.addChild(newNode);
			node = newNode;
			
			nextResources = state.getAvatarResources();
			Globals.knowledgeBase.addEventKnowledge(previousScore, previousNumEvents, previousAvatarPos, 
													previousAvatarOrientation, randomAction, state, 
													previousResources, nextResources, true);
			previousResources = nextResources;
		}
		
		return node;
	}
	
	@Override
	public int getDesiredActionNGramSize() {
		return -1;
	}

	@Override
	public String getName() {
		return "RandomPlayout";
	}

	@Override
	public String getConfigDataString() {
		return "maxPlayoutDepth=" + this.maxPlayoutDepth;
	}

	@Override
	public void init(StateObservation so, ElapsedCpuTimer elapsedTimer, MctsController mctsController) {
		// no initialization necessary
	}

	@Override
	public boolean wantsActionStatistics(){
		return false;
	}
}
