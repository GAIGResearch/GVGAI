package agents.AtheneAI.search.randomWalk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.AtheneAI.Agent;
import agents.AtheneAI.heuristics.Heatmap;

public final class RandomWalkPlayer {

	private Random randomGenerator;

	private ArrayList<ActionPlan> randomWalks;
	private ArrayList<ActionPlan> oneStepPlans;
	private boolean goalFound;
	private ArrayDeque<ACTIONS> goalActions;
	private Heatmap map;

	// debugging variables
	protected static int nodesEvaluated;

	public RandomWalkPlayer(Heatmap map2) {
		randomGenerator = new Random();
		goalFound = false;
		this.map = map2;
		randomWalks = new ArrayList<ActionPlan>();
		oneStepPlans = new ArrayList<ActionPlan>();
		goalActions = new ArrayDeque<ACTIONS>();
		nodesEvaluated = 0;
	}

	public ACTIONS solveStatic(StateObservation state, ElapsedCpuTimer elapsedTimer, int newIters) {
		ACTIONS nextAction = ACTIONS.ACTION_NIL;
		if (!goalFound) {
			oneStepPlans.clear();
			filterRandomWalks(state.getAvatarLastAction());
			// int b = randomWalks.size();
			createOneStepPlans(state.copy());
			// advanceRemainingRandomWalks(newIters, elapsedTimer);
			createNewRandomWalks(state, newIters, elapsedTimer);
			// System.out.println("RandomWalks created: " + (randomWalks.size()
			// - b));
			nextAction = findBestActionPlan().actions.getFirst();
		}
		// System.out.println(nodesEvaluated);
		if (goalFound)
			return goalActions.pop();
		else
			return nextAction;
	}

	public ACTIONS solveDynamic(StateObservation state, ElapsedCpuTimer elapsedTimer, int newIters) {
		ACTIONS nextAction = ACTIONS.ACTION_NIL;
		oneStepPlans.clear();
		randomWalks.clear();
		createOneStepPlans(state.copy());
		// advanceRemainingRandomWalks(newIters, elapsedTimer);
		createNewRandomWalks(state, newIters, elapsedTimer);
		nextAction = findBestActionPlan().actions.getFirst();
		// System.out.println(nodesEvaluated);
		return nextAction;
	}

	/**
	 * Filters all random walks. All plans whose first action is equal to the given
	 * action remain, but their first action is removed. All other plans are
	 * discarded.
	 * 
	 * @param lastAction
	 *            the last avatar action that was taken in the state
	 * @return a filtered list of random walks
	 */
	private void filterRandomWalks(ACTIONS lastAction) {
		LinkedList<ActionPlan> plansToRemove = new LinkedList<ActionPlan>();
		for (ActionPlan ap : randomWalks) {
			if (ap.actions.size() > 1) {
				if (lastAction != ACTIONS.ACTION_NIL && ap.actions.getFirst() == lastAction) {
					ap.actions.pop();
				}
			} else {
				plansToRemove.add(ap);
			}
		}
		randomWalks.removeAll(plansToRemove);
		plansToRemove = null;
	}

	/**
	 * Given a state, this method creates all possible one-step plans. Only stores
	 * one-step plans that didn't alter the state of the game apart from the avatar
	 * position ( moved boxes etc. ).
	 */
	private void createOneStepPlans(StateObservation state) {
		ArrayList<ACTIONS> possibleActions = (Agent.hasOnlyDynamicMovables) ? state.getAvailableActions(true)
				: state.getAvailableActions();
		while (!possibleActions.isEmpty()) {
			int index = randomGenerator.nextInt(possibleActions.size());
			ACTIONS action = possibleActions.get(index);
			possibleActions.remove(index);
			StateObservation stateSave = state.copy();
			nodesEvaluated++;
			stateSave.advance(action);
			if (action == ACTIONS.ACTION_NIL
					|| (!gameStateChanged(state, stateSave) && avatarPositionChanged(state, stateSave))) {
				ArrayDeque<ACTIONS> actionPlan = new ArrayDeque<ACTIONS>();
				actionPlan.add(action);
				oneStepPlans
						.add(new ActionPlan(actionPlan, value(stateSave), map.getScore(stateSave.getAvatarPosition())));
			}
		}
	}

	/**
	 * As long as there's still enough time remaining, do random walks and store the
	 * resulting action plans.
	 * 
	 * @param newIterations
	 *            how many iterations the random walk consists of
	 */
	private void createNewRandomWalks(StateObservation state, int newIterations, ElapsedCpuTimer elapsedTimer) {

		while (elapsedTimer.remainingTimeMillis() > 3l) {

			StateObservation stateSave = state.copy();
			ArrayDeque<ACTIONS> actionPlan = new ArrayDeque<ACTIONS>();
			for (int i = 0; i < newIterations && elapsedTimer.remainingTimeMillis() > 2l; i++) {
				int index = randomGenerator.nextInt(Agent.actions.length);
				ACTIONS action = Agent.actions[index];
				actionPlan.add(action);
				nodesEvaluated++;
				stateSave.advance(action);
			}
			randomWalks.add(new ActionPlan(actionPlan, value(stateSave), map.getScore(stateSave.getAvatarPosition())));
		}
	}

	/**
	 * Given all action plans so far it returns the plan with the highest score.
	 */
	private ActionPlan findBestActionPlan() {
		double bestScore = Double.NEGATIVE_INFINITY;
		ActionPlan bestPlan = new ActionPlan();
		for (ActionPlan ap : randomWalks) {
			// System.out.println("Random Walk: " + ap.actions + " Score: "
			// + ap.gameScore);
			if (ap.gameScore >= bestScore) {
				bestPlan = ap;
				bestScore = ap.gameScore;
			}
		}
		for (ActionPlan ap : oneStepPlans) {
			// System.out.println("OneStepPlanScore for move "
			// + ap.actions.getFirst() + " : " + ap.gameScore
			// + " Best Score: " + bestScore);
			if (ap.gameScore > bestScore
					|| (ap.gameScore == bestScore && ap.actions.size() < bestPlan.actions.size())) {
				bestPlan = ap;
				bestScore = ap.gameScore;
			}
		}
		if (goalFound) {
			goalActions = bestPlan.actions;
		}
		// System.out.println("Best Plan: " + bestPlan.actions.toString()
		// + " with Score: " + bestPlan.gameScore);
		return bestPlan;
	}

	/**
	 * Determines whether the two given states show any structural changes (
	 * different positions of movable objects etc. )
	 * 
	 * @return true if both gameStates are identical ( apart avatar positions )
	 */
	private boolean gameStateChanged(StateObservation _this, StateObservation _that) {
		ArrayList<Observation>[] _thisMovablePositions = _this.getMovablePositions();
		ArrayList<Observation>[] _thatMovablePositions = _that.getMovablePositions();

		return (!Agent.hasOnlyDynamicMovables && !(Arrays.deepEquals(_thisMovablePositions, _thatMovablePositions)));
	}

	private boolean avatarPositionChanged(StateObservation _this, StateObservation _that) {
		return (_this.getAvatarPosition().x != _that.getAvatarPosition().x
				|| _this.getAvatarPosition().y != _that.getAvatarPosition().y);
	}

	/**
	 * Determines the value of a state. Only very basic heuristic for RandomWalk to
	 * evaluate the score of an ActionPlan, quantity over quality.
	 * 
	 * @param a_gameState
	 * @return a basic estimate of the value of a_gameState
	 */
	private final double value(StateObservation a_gameState) {

		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();
		double rawScore = a_gameState.getGameScore();

		if (gameOver && win == Types.WINNER.PLAYER_LOSES)
			rawScore -= 100000d;

		if (gameOver && win == Types.WINNER.PLAYER_WINS) {
			rawScore += 100000d;
			goalFound = true;
		}
		return rawScore;
	}
}
