package agents.AtheneAI;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.AtheneAI.heuristics.Heatmap;
import agents.AtheneAI.heuristics.Knowledge;
import agents.AtheneAI.mcts.MCTSHeatmapPlayer;
import agents.AtheneAI.search.bfs.BFSPlayer;
import agents.AtheneAI.search.ehc.EHCPlayer;
import agents.AtheneAI.search.randomWalk.RandomWalkPlayer;
import agents.AtheneAI.util.GameTypeChecker;

/**
 * 
 */
public class Agent extends AbstractPlayer {

	// General
	public static ACTIONS[] actions;
	public static int NUM_ACTIONS;
	public static double REWARD_DISCOUNT = 1.00;
	public static int REAL_SCORE_FACTOR = 10000;
	boolean deterministic = false;
	boolean catchingGame = false;
	public static boolean hasOnlyDynamicMovables = false;
	public static HashSet<Integer> ignoredSprites;

	// MCTS
	public static int MCTS_ITERATIONS = 100;
	public static int ROLLOUT_DEPTH = 10;
	public static double K = 0.8; // Math.sqrt(2);

	// Heatmap
	public Heatmap map;
	public double SECTOR_MIN_SIZE = 1;
	public double SECTOR_MAX_SIZE = 6;
	public static int HEATMAP_SCORE_FACTOR = 10;
	public static int HEATMAP_DECAY = 3;
	public static int HEATMAP_DELAY = 10;
	public static int HEATMAP_REBOUND = 1;
	public static int HEATMAP_MAX_SCORE = 10;
	protected MCTSHeatmapPlayer mctsHeatmapPlayer;

	// Random Walk
	private RandomWalkPlayer randomWalkPlayer;
	private static int RANDOMWALK_DEPTH = 50;

	// Best-First-Search
	private BFSPlayer bfsPlayer;
	private boolean doBFS;
	public static ArrayDeque<ACTIONS> goalTrace;
	private ArrayDeque<ACTIONS> intermediateTrace;
	public static int MAX_BFS_ITERATIONS = 1700; // maximum bfsIterations

	// EHC & OLMCTS
	private EHCPlayer ehcPlayer;
	protected MCTSHeatmapPlayer mctsPlayer;

	// Iteration counters
	public static int overallIterations;
	public static int bfsIterations; // current bfsIterations
	public static int randomWalkIterations;
	public static double lastScore;
	public static double TEMPERATURE;

	/**
	 * Public constructor with state observation and time due.
	 * 
	 * @param so
	 *            state observation of the current game.
	 * @param elapsedTimer
	 *            Timer for the controller creation.
	 */
	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {

		hasOnlyDynamicMovables = false;
		doBFS = true;

		overallIterations = 0;
		bfsIterations = 0;
		randomWalkIterations = 0;
		lastScore = 0;
		TEMPERATURE = 2;

		// detect game type
		catchingGame = GameTypeChecker.isCatchingGame(so.copy());
		ignoredSprites = new HashSet<Integer>();
		deterministic = GameTypeChecker.isGameDeterministic(so.copy(), elapsedTimer);
		System.out.println("Ignore Sprites of the following types in BFS: " + ignoredSprites.toString());

		ArrayList<ACTIONS> act = (hasOnlyDynamicMovables) ? so.getAvailableActions(true) : so.getAvailableActions();
		actions = new ACTIONS[act.size()];
		for (int i = 0; i < actions.length; ++i) {
			actions[i] = act.get(i);
		}
		NUM_ACTIONS = actions.length;

		// collect knowledge about sprites with simulation
		// knowledge = new Knowledge(so, elapsedTimer,
		// twoDimensionalWalkingGame);
		if (!deterministic)
			Knowledge.initKnowledge(so, elapsedTimer, catchingGame);

		// Heatmap
		SECTOR_MAX_SIZE *= so.getBlockSize();
		SECTOR_MIN_SIZE *= so.getBlockSize();
		map = new Heatmap(SECTOR_MAX_SIZE, SECTOR_MIN_SIZE, HEATMAP_DELAY, HEATMAP_DECAY, HEATMAP_REBOUND,
				HEATMAP_MAX_SCORE, so);

		// instantiate the needed agents for the detected game type
		if (catchingGame) {

			System.out.println(" game for MCTS & EHC detected");
			ehcPlayer = new EHCPlayer(act);
			mctsPlayer = new MCTSHeatmapPlayer(new Random(), map);

		} else {

			if (deterministic) {
				System.out.println(" game for BFS detected");
				bfsPlayer = new BFSPlayer();
				goalTrace = new ArrayDeque<ACTIONS>();
				intermediateTrace = new ArrayDeque<ACTIONS>();
				randomWalkPlayer = new RandomWalkPlayer(map);

				// use remaining time to already start with BFS
				bfsPlayer.solve(so, elapsedTimer, 50l);
			} else {
				System.out.println(" game for MCTS with heatmap detected");
				mctsHeatmapPlayer = new MCTSHeatmapPlayer(new Random(), map);
			}
		}
	}

	/**
	 * Picks an action. This function is called every game step to request an action
	 * from the player.
	 * 
	 * @param stateObs
	 *            Observation of the current state.
	 * @param elapsedTimer
	 *            Timer when the action returned is due.
	 * @return An action for the current state
	 */
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

		// if (true) return ACTIONS.ACTION_NIL;

		overallIterations++;
		if (catchingGame) {

			ArrayList<Integer> goodMovablesIds = Knowledge.getGoodMovables(stateObs);
			if (!goodMovablesIds.isEmpty()) {

				// determine next action with EHC
				return ehcPlayer.getBestAction(stateObs, elapsedTimer, goodMovablesIds);

			} else {

				// determine next action with OLMCTS (no heatmap)
				mctsPlayer.init(stateObs);
				int action = mctsPlayer.run(elapsedTimer);
				return actions[action];
			}

		} else if (!deterministic) {

			// update the distance heuristic regulary for the current gamestate
			// after that call Knowledge.getDistanceHeuristicValue(Vector2d
			// position)
			// if ((overallIterations + 1) % 50 == 0){
			// System.out.println("updating distance heuristic");
			// Knowledge.updateDistanceHeuristic(stateObs);
			// return ACTIONS.ACTION_NIL;
			// }

			// determine next action with MCTS and Heatmap
			map.addVisit(stateObs.getAvatarPosition());
			mctsHeatmapPlayer.init(stateObs);
			int action = mctsHeatmapPlayer.run(elapsedTimer);
			return actions[action];

		} else {
			map.addVisit(stateObs.getAvatarPosition());

			/*
			 * check if there's still meaningful time for BFS, otherwise switch to random
			 * walk
			 */
			if (overallIterations > (1950 - bfsPlayer.bestNodeTrace.size()) && doBFS) {
				System.out.println("Permanently switching to RandomWalk due to time constraints");
				if (intermediateTrace.isEmpty()) {
					intermediateTrace = new ArrayDeque<ACTIONS>(bfsPlayer.getIntermediateResult());
				}
				doBFS = false;
			}

			// goalTrace has elements -> choose those actions
			if (!goalTrace.isEmpty()) {
				return goalTrace.pop();
			} else if (doBFS) {
				// still time for BFS
				if (intermediateTrace.isEmpty()) {
					if (bfsIterations <= MAX_BFS_ITERATIONS && !bfsPlayer.disabled) {
						if (!bfsPlayer.reachedGoalState) {
							bfsIterations++;
							bfsPlayer.solve(stateObs, elapsedTimer, 4l);
							if (bfsPlayer.reachedGoalState) {
								bfsIterations = 0;
								bfsPlayer.reset();
							}
							if (!goalTrace.isEmpty())
								return goalTrace.pop();
							else
								return ACTIONS.ACTION_NIL;
						}
					}
					/*
					 * BFS iteration limit reached, try intermediate result and reset BFS.
					 */
					else if (bfsIterations > MAX_BFS_ITERATIONS) {
						System.out.println("Iteration " + overallIterations + ": Trying intermediate walk");
						intermediateTrace = new ArrayDeque<ACTIONS>(bfsPlayer.getIntermediateResult());
						bfsIterations = 0;
						bfsPlayer.reset();
					}
				}
				if (!intermediateTrace.isEmpty())
					return intermediateTrace.pop();
				else
					return ACTIONS.ACTION_NIL;
			}
			if (!intermediateTrace.isEmpty())
				return intermediateTrace.pop();
			/*
			 * BFS gets disabled if it's too ineffective. Does RandomWalks instead, once a
			 * positive score is reached try BFS again.
			 */
			if (bfsPlayer.disabled) {
				if (randomWalkIterations < 200) {
					randomWalkIterations++;
					return randomWalkPlayer.solveStatic(stateObs, elapsedTimer, RANDOMWALK_DEPTH);
				} else {
					if (doBFS && stateObs.getGameScore() > 0) {
						bfsPlayer.disabled = false;
						bfsIterations = 0;
						randomWalkIterations = 0;
						System.out.println("Trying BFS again");
					}
					return randomWalkPlayer.solveStatic(stateObs, elapsedTimer, RANDOMWALK_DEPTH);
				}
			}
			return randomWalkPlayer.solveStatic(stateObs, elapsedTimer, RANDOMWALK_DEPTH);
		}
	}
}
