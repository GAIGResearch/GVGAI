package agents.MaastCTS2.playout;

import java.util.ArrayList;
import java.util.HashMap;

import agents.MaastCTS2.Agent;
import agents.MaastCTS2.Globals;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.ActionLocation;
import agents.MaastCTS2.model.MctNode;
import agents.MaastCTS2.model.Score;
import agents.MaastCTS2.model.StateObs;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Playout strategy using Move-Average Sampling Technique (MAST)
 * 
 * <p> NOTE: This class  has not been used for a long time, and may not include all the special cases
 * and/or heuristics that have been added to NstPlayout in the meantime
 *
 * @author Dennis Soemers
 */
public class MastPlayout implements IPlayoutStrategy {
	
	/** Probability that we play a random action (epsilon-greedy selection of action with highest score) */
	private double epsilon;
	/** The maximum depth that our playouts should reach */
	private int maxPlayoutDepth;
	
	public MastPlayout(int maxPlayoutDepth, double epsilon){
		this.maxPlayoutDepth = maxPlayoutDepth;
		this.epsilon = epsilon;
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
			
			ACTIONS actionToPlay = ACTIONS.ACTION_NIL;
			ArrayList<ACTIONS> unexpandedActions = node.getUnexpandedActions();
			int avatarCell = node.getLastAvatarCell();
			
			if(Globals.RNG.nextDouble() < epsilon){
				// play random action with probability epsilon
				actionToPlay = unexpandedActions.remove(Globals.RNG.nextInt(unexpandedActions.size()));
			}
			else{
				// play ''best'' action with probability (1 - epsilon)
				MctsController mcts = (MctsController) Agent.controller;
				double bestAvgScore = Double.NEGATIVE_INFINITY;
				int bestActionIdx = -1;
				for(int idx = 0; idx < unexpandedActions.size(); ++idx){
					ActionLocation action = new ActionLocation(unexpandedActions.get(idx), avatarCell);
					Score actionScore = mcts.getActionScore(action);
					
					double avgScore;
					if(actionScore.timesVisited == 0.0){
						// if we've never played this action yet, we'll use the max score (to reward exploration of unknown actions)
						avgScore = mcts.MAX_SCORE;
					}
					else{
						avgScore = actionScore.score / actionScore.timesVisited;
					}
					
					avgScore += Globals.smallNoise();
					
					if(avgScore > bestAvgScore){
						bestAvgScore = avgScore;
						bestActionIdx = idx;
					}
				}
				
				actionToPlay = unexpandedActions.remove(bestActionIdx);
			}

			MctNode newNode = new MctNode(node, actionToPlay);
			stateObs = newNode.generateNewStateObs(stateObs, actionToPlay);
			state = stateObs.getStateObsNoCopy();
			
			node.addChild(newNode);
			node = newNode;
			
			nextResources = state.getAvatarResources();
			Globals.knowledgeBase.addEventKnowledge(previousScore, previousNumEvents, previousAvatarPos, 
													previousAvatarOrientation, actionToPlay, state,
													previousResources, nextResources, true);
			previousResources = nextResources;
		}
		
		return node;
	}
	
	@Override
	public int getDesiredActionNGramSize(){
		return -1;
	}

	@Override
	public String getName() {
		return "MAST Playout";
	}

	@Override
	public String getConfigDataString() {
		return "maxPlayoutDepth=" + maxPlayoutDepth +
				", epsilon=" + epsilon;
	}

	@Override
	public void init(StateObservation so, ElapsedCpuTimer elapsedTimer, MctsController mctsController) {
		// no initialization necessary
	}

	@Override
	public boolean wantsActionStatistics(){
		return true;
	}

}
