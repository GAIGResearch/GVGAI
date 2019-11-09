package agents.MaastCTS2.selection.ol;

import java.util.ArrayList;
import java.util.HashMap;

import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import agents.MaastCTS2.Agent;
import agents.MaastCTS2.Globals;
import agents.MaastCTS2.KnowledgeBase;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.MctNode;
import agents.MaastCTS2.model.Score;
import agents.MaastCTS2.model.StateObs;
import agents.MaastCTS2.selection.ISelectionStrategy;

/**
 * Progressive History selection strategy
 * 
 * @author Dennis Soemers
 *
 */
public class ProgressiveHistory implements ISelectionStrategy {

	/** The exploration constant */
	private final double c;
	
	/** Constant that determines the influence of progressive bias */
	private final double w;

	public ProgressiveHistory(double c, double w) {
		this.c = c;
		this.w = w;
	}

	@Override
	public MctNode select(MctNode rootNode, ElapsedCpuTimer timer) {
		MctNode node = rootNode;
		StateObservation state = rootNode.getStateObs();
		
		HashMap<Integer, Integer> previousResources = state.getAvatarResources();
		HashMap<Integer, Integer> nextResources;
		
		StateObs stateObs = new StateObs(state, true);
		
		boolean firstStateGenerated = false;
		
		// use uct to select child
		while (!state.isGameOver() && node.isFullyExpanded() && !node.getChildren().isEmpty()) {
			double previousScore = state.getGameScore();
			int previousNumEvents = state.getHistoricEventsHistory().size();
			Vector2d previousAvatarPos = state.getAvatarPosition();
			Vector2d previousAvatarOrientation = state.getAvatarOrientation();
			
			node.preSelect(state);
			node = getNextNode(node);
			
			stateObs = node.generateNewStateObs(stateObs, node.getActionFromParent());
			state = stateObs.getStateObsNoCopy();
			
			if(!firstStateGenerated){
				firstStateGenerated = true;
				KnowledgeBase kb = Globals.knowledgeBase;
				int pheromoneStrength = kb.getPheromoneStrength(kb.positionToCell(state.getAvatarPosition()));
				
				if(pheromoneStrength > 0){
					MctsController.ONE_STEP_EVAL -= 0.005 * pheromoneStrength;
				}
				
				if(node.getActionFromParent() == ACTIONS.ACTION_USE){
					// slight punishment for using the USE action 
					// (only want to use this action when it serves an observable purpose)
					MctsController.ONE_STEP_EVAL -= 0.01;
				}
			}
			
			nextResources = state.getAvatarResources();
			Globals.knowledgeBase.addEventKnowledge(previousScore, previousNumEvents, previousAvatarPos, 
													previousAvatarOrientation, node.getActionFromParent(), state, 
													previousResources, nextResources, false);
			previousResources = nextResources;
		}
		
		return node;
	}

	private MctNode getNextNode(MctNode node) {	
		MctsController controller = (MctsController) Agent.controller;
		final double MIN_SCORE = controller.MIN_SCORE;
		final double MAX_SCORE = controller.MAX_SCORE;
		
		double n = node.getNumVisits();
		
		// using Math.max() here to avoid getting a negative value when 0.0 < n < 1.0
		// (which is possible due to tree decay)
		double log_n = Math.max(0.0, Math.log(n));
		
		ArrayList<MctNode> children = node.getChildren();
		int numChildren = children.size();
		
		MctNode bestNode = null;
		double bestVal = Double.NEGATIVE_INFINITY;
		
		//System.out.println("numChildren = " + numChildren);

		for(int i = 0; i < numChildren; ++i){
			MctNode child = children.get(i);
			double n_i = Math.max(child.getNumVisits(), 0.00001);
			
			double avgScore = child.getTotalScore() / n_i;
			avgScore = Globals.normalise(avgScore, MIN_SCORE, MAX_SCORE);
			
			// play less scared as time passes
			//if(avgScore < 0.0){
			//	avgScore *= (((double)CompetitionParameters.MAX_TIMESTEPS - controller.rootTick) / CompetitionParameters.MAX_TIMESTEPS);
			//}
			
			double uctVal;
			if(Globals.knowledgeBase.isGameDeterministic()/* && !controller.allowsTdBackups()*/){
				// mixmax for deterministic games
				double maxScore = Globals.normalise(child.getMaxScore(), MIN_SCORE, MAX_SCORE);
				uctVal = 0.75 * avgScore + 0.25 * maxScore + c * Math.sqrt(log_n / n_i);
			}
			else{
				uctVal = avgScore + c * Math.sqrt(log_n / n_i);
			}
			Score actionScore = controller.getActionScore(child.getActionLocationFromParent());
			double historyHeuristic;
			
			if(actionScore.timesVisited == 0.0){
				historyHeuristic = 1.0;
			}
			else{
				historyHeuristic = Globals.normalise(actionScore.getAverageScore(), controller.MIN_ACTION_SCORE, controller.MAX_ACTION_SCORE);
			}
			
			//System.out.println("historyHeuristic = " + historyHeuristic + " (normalized between " + controller.MIN_ACTION_SCORE + " and " + controller.MAX_ACTION_SCORE + ")");
			
			// the ''avgScore * n_i'' transforms the normalized average back into a normalized sum
			//System.out.println("progressiveBias = w / (n_i - (avgScore * n_i) + 1.0 = " + w + " / (" + n_i + " - (" + avgScore + " * " + n_i + ") + 1.0)");
			double progressiveBias = w / (n_i - (avgScore * n_i) + 1.0);
			
			double value = (uctVal + historyHeuristic * progressiveBias) + Globals.smallNoise();
			
			if(controller.allowsNoveltyBasedPruning() && !node.isInescapableLossFound()){
				if(!child.isNovel() && Globals.normalise(node.getTotalScore() / n, controller.MIN_SCORE, controller.MAX_SCORE) >= 0.5){
					value -= 100.0;		// this makes sure we basically never select non-novel nodes
				}
			}
			
			if(node.canBeImmediateLoss()){
				value -= 100.0;
			}

			if (value > bestVal) {
				bestVal = value;
				bestNode = child;
			}
		}
		
		return bestNode;
	}
	
	@Override
	public int getDesiredActionNGramSize(){
		return -1;
	}

	@Override
	public String getName() {
		return "progressive_history";
	}

	@Override
	public String getConfigDataString() {
		return "c=" + c + ", w=" + w;
	}

	@Override
	public void init(StateObservation so, ElapsedCpuTimer elapsedTimer) {
	}
	
	@Override
	public boolean wantsActionStatistics(){
		return true;
	}
	
}
