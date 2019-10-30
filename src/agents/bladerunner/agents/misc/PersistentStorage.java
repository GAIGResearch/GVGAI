package agents.bladerunner.agents.misc;

import java.util.HashMap;

import ontology.Types.ACTIONS;
import core.game.StateObservation;

public class PersistentStorage {

	/*
	 * HashMap of iTypeAttractivity for start situation
	 * 
	 * @note TODO: Maybe create List of AttractivityMaps for different game
	 * situations (e.g. avatar has found sword/has eaten mushroom/has a lot of
	 * honey)
	 */
	public static ITypeAttractivity iTypeAttractivity = null;

	/**
	 * an exploration reward map that is laid over the game-world to reward
	 * places that haven't been visited lately
	 */
	public static RewardMap rewMap = null;

	// keeps track of the reward at the start of the MCTS search
	public static double startingReward = 0;
	public static double numberOfBlockedMovables = 0;
	public static ACTIONS[] actions;
	public static double K = Math.sqrt(2);
	public static int MCTS_AVOID_DEATH_DEPTH = 2;
	
	public static int GameCounter = 0;
	
	public static StateObservation lastGameState= null;
	public static double lastWinLoseExpectation=0;

	// fix the MCTS_DEPTH to the starting DEPTH
	public static int MCTS_DEPTH_RUN = PersistentStorage.MCTS_DEPTH_FIX;

	/*
	 * running and fixed MCTS_DEPTH, first increments to counter the increment
	 * of the depth of the cut trees. The later stays fixed
	 */
	public static int MCTS_DEPTH_FIX = 3;
	
	// ## Parameters
	public static int ROLLOUT_DEPTH = 0;
	
	/**
	 * The adjacency map is a map containing positions you can move to at the current moment.
	 */
	public static AdjacencyMap adjacencyMap = null;
	
	public static HashMap<Integer, Integer> previousAvatarRessources = null;
}
