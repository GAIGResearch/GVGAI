package agents.MaastCTS2;

import java.awt.Graphics2D;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.MaastCTS2.controller.IController;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.heuristics.states.GvgAiEvaluation;
import agents.MaastCTS2.heuristics.states.IPlayoutEvaluation;
import agents.MaastCTS2.move_selection.IMoveSelectionStrategy;
import agents.MaastCTS2.move_selection.MaxAvgScore;
import agents.MaastCTS2.playout.IPlayoutStrategy;
import agents.MaastCTS2.playout.NstPlayout;
import agents.MaastCTS2.selection.ISelectionStrategy;
import agents.MaastCTS2.selection.ol.ProgressiveHistory;

public class Agent extends AbstractPlayer {
	public static IController controller;

	/**
	 * constructor for competition
	 * 
	 * @param so
	 * @param elapsedTimer
	 */
	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		MctsController.TIME_BUFFER_MILLISEC = 8;	// shorter time buffer because better hardware on official competition server
		controller = new MctsController(new ProgressiveHistory(0.6, 1.0), new NstPlayout(10, 0.5, 7.0, 3), 
				new MaxAvgScore(), new GvgAiEvaluation(), true, true, true, true, true, 0.6, 3, true, false);
		controller.init(so, elapsedTimer);
	}

	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer,
			ISelectionStrategy selectionStrategy, IPlayoutStrategy playoutStrategy, 
			IMoveSelectionStrategy moveSelectionStrategy, IPlayoutEvaluation playoutEval,
			boolean initBreadthFirst, boolean noveltyBasedPruning, boolean exploreLosses,
			boolean knowledgeBasedEval, boolean detectDeterministicGames, boolean treeReuse,
			double treeReuseGamma, int maxNumSafetyChecks, boolean alwaysKB, boolean noTreeReuseBFTI) {
		controller = new MctsController(selectionStrategy, playoutStrategy, moveSelectionStrategy, 
										playoutEval, initBreadthFirst, noveltyBasedPruning, exploreLosses,
										knowledgeBasedEval, treeReuse, treeReuseGamma, maxNumSafetyChecks, alwaysKB, noTreeReuseBFTI);
		controller.init(so, elapsedTimer);
	}
	
	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer, IController controller){
		Agent.controller = controller;
		controller.init(so,  elapsedTimer);
	}

	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		Globals.knowledgeBase.update(stateObs);
		return controller.chooseAction(stateObs, elapsedTimer);
	}
	
	@Override
	public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer){
		controller.result(stateObservation, elapsedCpuTimer);
    }
	
	@Override
	public void draw(Graphics2D g){
		if(Globals.DEBUG_DRAW){
			Globals.knowledgeBase.draw(g);
		}
	}

}
