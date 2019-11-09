package agents.MaastCTS2.selection.ol;

import java.util.ArrayList;
import java.util.HashMap;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import agents.MaastCTS2.Agent;
import agents.MaastCTS2.Globals;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.MctNode;
import agents.MaastCTS2.model.StateObs;
import agents.MaastCTS2.selection.ISelectionStrategy;

/**
 * Standard (open-loop) UCT selection strategy
 * 
 * <p> NOTE: This class  has not been used for a long time, and may not include all the special cases
 * and/or heuristics that have been added to ProgressiveHistory in the meantime
 * 
 * @author Dennis Soemers
 *
 */
public class OlUctSelection implements ISelectionStrategy {

	/** The exploration constant */
	private double c;

	public OlUctSelection(double c) {
		this.c = c;
	}

	@Override
	public MctNode select(MctNode rootNode, ElapsedCpuTimer timer) {
		MctNode node = rootNode;
		StateObservation state = rootNode.getStateObs();
		
		HashMap<Integer, Integer> previousResources = state.getAvatarResources();
		HashMap<Integer, Integer> nextResources;
		
		StateObs stateObs = new StateObs(state, true);
		
		// use uct to select child
		while (!state.isGameOver() && node.isFullyExpanded() && !node.getChildren().isEmpty()) {
			double previousScore = state.getGameScore();
			int previousNumEvents = state.getHistoricEventsHistory().size();
			Vector2d previousAvatarPos = state.getAvatarPosition();
			Vector2d previousAvatarOrientation = state.getAvatarOrientation();
			
			node.preSelect(state);
			node = getNextNodeByUct(node);
			
			stateObs = node.generateNewStateObs(stateObs, node.getActionFromParent());
			state = stateObs.getStateObsNoCopy();
			
			nextResources = state.getAvatarResources();
			Globals.knowledgeBase.addEventKnowledge(previousScore, previousNumEvents, previousAvatarPos, 
													previousAvatarOrientation, node.getActionFromParent(), state, 
													previousResources, nextResources, false);
			previousResources = nextResources;
		}
		
		return node;
	}

	private MctNode getNextNodeByUct(MctNode node) {	
		MctsController controller = (MctsController) Agent.controller;
		final double MIN_SCORE = controller.MIN_SCORE;
		final double MAX_SCORE = controller.MAX_SCORE;
		
		double n = node.getNumVisits();
		double log_n = Math.max(0.0, Math.log(n));
		
		ArrayList<MctNode> children = node.getChildren();
		int numChildren = children.size();
		
		// initialize best node as the first child
		MctNode bestNode = null;
		double bestUctVal = Double.NEGATIVE_INFINITY;
		
		for(int i = 0; i < numChildren; ++i){
			MctNode child = children.get(i);
			double n_i = Math.max(child.getNumVisits(), 0.00001);
			
			double avgScore = child.getTotalScore() / n_i;
			avgScore = Globals.normalise(avgScore, MIN_SCORE, MAX_SCORE);
			
			double uctVal;
			if(Globals.knowledgeBase.isGameDeterministic()/* && !controller.allowsTdBackups()*/){
				// mixmax for deterministic games
				uctVal = 0.75 * avgScore + 0.25 * Globals.normalise(child.getMaxScore(), MIN_SCORE, MAX_SCORE) + c * Math.sqrt(log_n / n_i);
			}
			else{
				uctVal = avgScore + c * Math.sqrt(log_n / n_i);
			}
			uctVal += Globals.smallNoise();
			
			if(controller.allowsNoveltyBasedPruning() && !node.isInescapableLossFound()){
				if(!child.isNovel() && Globals.normalise(node.getTotalScore() / n, controller.MIN_SCORE, controller.MAX_SCORE) >= 0.5){
					uctVal -= 100.0;		// this makes sure we basically never select non-novel nodes
				}
			}

			if (uctVal > bestUctVal) {
				bestUctVal = uctVal;
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
		return "ol.UctSelection";
	}

	@Override
	public String getConfigDataString() {
		return "c=" + c;
	}

	@Override
	public void init(StateObservation so, ElapsedCpuTimer elapsedTimer) {
	}
	
	@Override
	public boolean wantsActionStatistics(){
		return false;
	}
}
