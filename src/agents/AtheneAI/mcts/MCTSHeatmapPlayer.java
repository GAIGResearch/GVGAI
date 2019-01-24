package agents.AtheneAI.mcts;

import java.util.Random;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;
import agents.AtheneAI.heuristics.Heatmap;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 07/11/13 Time: 17:13
 */
public class MCTSHeatmapPlayer {

	/**
	 * Root of the tree.
	 */
	public SingleTreeNode m_root;

	/**
	 * Random generator.
	 */
	public Random m_rnd;

	public Heatmap map;

	public MCTSHeatmapPlayer(Random a_rnd, Heatmap heatmap) {
		m_rnd = a_rnd;
		map = heatmap;
	}

	/**
	 * Inits the tree with the new observation state in the root.
	 * 
	 * @param a_gameState
	 *            current state of the game.
	 */
	public void init(StateObservation a_gameState) {
		// Set the game observation to a newly root node.
		// System.out.println("learning_style = " + learning_style);
		m_root = new SingleTreeNode(m_rnd);
		m_root.rootState = a_gameState;
	}

	public void progress(StateObservation a_gameState, int action) {
		for (SingleTreeNode child : m_root.children) {
			if (child.childIdx == action) {
				m_root = m_root.children[action];
				m_root.rootState = a_gameState;
				return;
			}
		}
		this.init(a_gameState);
	}

	/**
	 * Runs MCTS to decide the action to take. It does not reset the tree.
	 * 
	 * @param elapsedTimer
	 *            Timer when the action returned is due.
	 * @return the action to execute in the game.
	 */
	public int run(ElapsedCpuTimer elapsedTimer) {
		// Do the search within the available time.
		m_root.mctsSearch(elapsedTimer, map);

		// Determine the best action to take and return it.
		int action = m_root.mostVisitedAction();
		// int action = m_root.bestAction();
		return action;
	}

	public double valueOfBestAction(int action) {
		return m_root.children[m_root.bestAction()].totValue;
	}

}
