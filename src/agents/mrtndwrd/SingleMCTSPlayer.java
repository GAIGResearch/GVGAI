package agents.mrtndwrd;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:13
 */
public class SingleMCTSPlayer
{
	/**
	 * Root of the tree.
	 */
	private SingleTreeNode rootNode;

	/**
	 * Random generator.
	 */
	private Random random;

	/**
	 * Creates the MCTS player with a sampleRandom generator object.
	 * @param random sampleRandom generator object.
	 */
	public SingleMCTSPlayer(Random random)
	{
		this.random = random;
	}

	/**
	 * Inits the tree with the new observation state in the root.
	 * @param gameState current state of the game.
	 */
	public void init(StateObservation gameState, ArrayList<Option> possibleOptions, HashSet<Integer> optionObsIDs, Option currentOption)
	{
		//Set the game observation to a new root node.
		rootNode = new SingleTreeNode(possibleOptions, optionObsIDs, random, currentOption);
		rootNode.state = gameState;
	}

	/**
	 * Runs MCTS to decide the action to take. It does not reset the tree.
	 * @param elapsedTimer Timer when the action returned is due.
	 * @return the action to execute in the game.
	 */
	public int run(ElapsedCpuTimer elapsedTimer)
	{
		//Do the search within the available time.
		rootNode.mctsSearch(elapsedTimer);

		//Determine the best action to take and return it.
		int action = rootNode.bestAction();
		return action;
	}

	public String printRootNode()
	{
		return rootNode.toString();
	}
}
