package agents.MaastCTS2.playout;

import java.util.ArrayList;
import java.util.HashMap;

import agents.MaastCTS2.Agent;
import agents.MaastCTS2.Globals;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.ActionLocation;
import agents.MaastCTS2.model.ActionNGram;
import agents.MaastCTS2.model.MctNode;
import agents.MaastCTS2.model.Score;
import agents.MaastCTS2.model.StateObs;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Playout strategy using N-Gram Selection Technique
 *
 * @author Dennis Soemers
 */
public class NstPlayout implements IPlayoutStrategy {
	
	/** Probability that we play a random action (epsilon-greedy selection of action with highest score) */
	private final double epsilon;
	/** The minimum number of times that an n-gram must have been visited for its statistics to be used (k parameter) */
	private final double minNGramVisitCount;
	/** The maximum size of n-grams to consider */
	private final int maxNGramSize;
	
	/** The maximum depth that our playouts should reach */
	private final int maxPlayoutDepth;
	
	public NstPlayout(int maxPlayoutDepth, double epsilon, double minNGramVisitCount, int maxNGramSize){
		this.maxPlayoutDepth = maxPlayoutDepth;
		this.epsilon = epsilon;
		this.minNGramVisitCount = minNGramVisitCount;
		this.maxNGramSize = maxNGramSize;
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
					
					double sumAvgScores = 0.0;
					int numNGramsConsidered = 0;
					if(actionScore.timesVisited == 0.0){
						// if we've never played this action yet, we'll use the max score (to reward exploration of unknown actions)
						sumAvgScores += mcts.MAX_SCORE;
						++numNGramsConsidered;
					}
					else{
						sumAvgScores += actionScore.score / actionScore.timesVisited;
						++numNGramsConsidered;
						
						ArrayList<ActionLocation> actionSequence = new ArrayList<ActionLocation>(maxNGramSize);
						actionSequence.add(action);
						MctNode currentActionNode = node;
						
						while(currentActionNode.getParent() != null && actionSequence.size() < maxNGramSize){
							actionSequence.add(currentActionNode.getActionLocationFromParent());
							ActionLocation[] nGram = new ActionLocation[actionSequence.size()];
							
							for(int i = 0; i < actionSequence.size(); ++i){
								nGram[i] = actionSequence.get(actionSequence.size() - 1 - i);
							}
							
							Score actionNGramScore = mcts.getActionNGramScore(new ActionNGram(nGram));
							if(actionNGramScore.timesVisited < minNGramVisitCount){
								// don't have enough samples anymore to take into account this n-gram or any bigger ones
								// that this one is a part of
								break;
							}
							
							sumAvgScores += actionNGramScore.score / actionNGramScore.timesVisited;
							++numNGramsConsidered;
						}
					}
					
					double avgScore = sumAvgScores / numNGramsConsidered;
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
		return maxNGramSize;
	}

	@Override
	public String getName() {
		return "NST Playout";
	}

	@Override
	public String getConfigDataString() {
		return "maxPlayoutDepth=" + maxPlayoutDepth +
				", epsilon=" + epsilon +
				", n=" + maxNGramSize +
				", k=" + minNGramVisitCount;
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
