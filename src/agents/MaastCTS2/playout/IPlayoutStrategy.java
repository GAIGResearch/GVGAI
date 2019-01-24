package agents.MaastCTS2.playout;

import tools.ElapsedCpuTimer;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.MctNode;
import agents.MaastCTS2.test.IPrintableConfig;
import core.game.StateObservation;

public interface IPlayoutStrategy extends IPrintableConfig {
	/**
	 * 
	 * @param node
	 * @return the final node of the playout
	 */
	public MctNode runPlayout(MctNode node, ElapsedCpuTimer elapsedTimer);

	/**
	 * optional method that is called when the agent is initialized.
	 * 
	 * @param so
	 * @param elapsedTimer
	 */
	public void init(StateObservation so, ElapsedCpuTimer elapsedTimer, MctsController mctsController);
	
	/**
	 * Should be implemented to return the maximum desired size of n-grams of actions
	 * to collect statistics for.
	 * 
	 * <p> If a value < 1 is returned, will not collect statistics for any n-grams
	 * 
	 * @return
	 */
	public int getDesiredActionNGramSize();
	
	/**
	 * Should be implemented to return true iff the selection strategy requires
	 * action statistics to be backpropagated (for example, MAST)
	 * 
	 * @return
	 */
	public boolean wantsActionStatistics();
}
