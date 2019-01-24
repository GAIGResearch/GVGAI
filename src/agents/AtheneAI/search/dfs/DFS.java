package agents.AtheneAI.search.dfs;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.AtheneAI.Agent;

public final class DFS {

	private Random randomGenerator;

	private double bestScoreSeen;
	private boolean reachedGoalState;
	private LinkedList<ACTIONS> goalTrace;
	private ArrayDeque<DFSNode> nodesSave;

	// debugging variables
	private int nodesEvaluated;

	public DFS() {
		randomGenerator = new Random();
		bestScoreSeen = Double.NEGATIVE_INFINITY;
		reachedGoalState = false;
		goalTrace = new LinkedList<ACTIONS>();
		nodesSave = new ArrayDeque<DFSNode>();
		nodesEvaluated = 0;
	}

	/**
	 * 
	 * @param root
	 * @param elapsedTimer
	 * @param reset
	 * @param maxDepth
	 * @param currentIteration
	 * @return
	 */
	protected final LinkedList<ACTIONS> solve(StateObservation root, ElapsedCpuTimer elapsedTimer, boolean reset,
			int maxDepth) {

		LinkedList<ACTIONS> highestValueTrace = new LinkedList<ACTIONS>();
		reachedGoalState = false;

		if (reset) {
			bestScoreSeen = Double.NEGATIVE_INFINITY;
			goalTrace = new LinkedList<ACTIONS>();
			nodesSave = new ArrayDeque<DFSNode>();
		}

		highestValueTrace = runDFS(maxDepth, new DFSNode(root.copy(), null, 0, root.getAvatarLastAction()),
				elapsedTimer);

		if (reachedGoalState) {
			System.out.println("Trace to Win: " + highestValueTrace.toString());
			return highestValueTrace;
		}
		/*
		 * TODO: Handling of intermediate results in else block.
		 */

		return new LinkedList<ACTIONS>();
	}

	/**
	 * 
	 * @param depth
	 * @param root
	 * @param elapsedTimer
	 * @return
	 */
	private final LinkedList<ACTIONS> runDFS(int depth, DFSNode root, ElapsedCpuTimer elapsedTimer) {
		ArrayDeque<DFSNode> nodes = nodesSave;
		if (nodes.isEmpty())
			nodes.push(root);

		while (!nodes.isEmpty() && reachedGoalState == false
		// && elapsedTimer.remainingTimeMillis() > 3
		) {
			DFSNode node = nodes.pop();
			// StateObservation state = node.state;
			// System.out.println("Node.depth " + node.depth + " Path: "
			// + actionsToReachNode);
			if (node.depth < depth) {
				/*
				 * Even this basic check for removing useless actions costs so much performance
				 * that it's probably wiser to skip it (see chooseRandomAction implementation)
				 */
				// List<ACTIONS> possibleActions = getSensibleActions(state);

				// #### CHOOSE METHOD OF EXPANSION ####
				// Choose one, otherwise wrong results.

				// ADDING ONE RANDOM ACTION
				chooseRandomAction(nodes, node, Agent.actions);

				// ADDING BEST CHILD
				// chooseBestAction(nodes, node, possibleActions);

				// ADDING ALL CHILDREN ( infeasible )
				// chooseAllActions(nodes, node, possibleActions);

			}

			/*
			 * If not all possible actions are taken into account ( e.g. branching factor 1
			 * with random actions ), pushing the root to the stack of nodes enables us to
			 * search multiple times for a solution within runDFS.
			 * 
			 * TODO: Check for correctness, debugging shows a lot of cases where not even
			 * one action is taken after adding the root.
			 */
			if (nodes.isEmpty() && reachedGoalState == false && elapsedTimer.remainingTimeMillis() > 5) {
				nodes.push(new DFSNode(root.state.copy(), null, 0, root.state.getAvatarLastAction()));
			}
		}

		// debugging
		// System.out.println(nodesEvaluated);

		/*
		 * #### USED IF ONLY FULL TRACES TO GOALS ARE RETURNED. ####
		 */
		if (reachedGoalState) {
			nodesSave = new ArrayDeque<DFSNode>();
			return goalTrace;
		} else {
			nodesSave = nodes;
			return new LinkedList<ACTIONS>();
		}

		/*
		 * #### USED IF INTERMEDIATE TRACES ARE RETURNED AS WELL. ####
		 * 
		 * TODO: Still needs integration + switching between returning single actions
		 * and whole traces.
		 */
		// if (!reachedGoalState) {
		// nodesSave = nodes;
		// return concept of best action trace or single action so far
		// } else {
		// nodesSave = new Stack<Node>();
		// actionsSave = new LinkedList<ACTIONS>();
		// return actionsToReachNode;
		// }
	}

	/**
	 * Given a state, returns all actions from that state which make sense.
	 * 
	 * @return a list of sensible actions
	 */
	private final List<ACTIONS> getSensibleActions(StateObservation state) {
		List<ACTIONS> possibleActions = state.getAvailableActions();

		// TODO: Improve, still very basic ( include hashing )
		double playerX = state.getAvatarPosition().x;
		double playerY = state.getAvatarPosition().y;
		if (playerX == 0) {
			possibleActions.remove(ACTIONS.ACTION_LEFT);
		} else if (playerX + state.getBlockSize() == state.getWorldDimension().width) {
			possibleActions.remove(ACTIONS.ACTION_RIGHT);
		}
		if (playerY == 0) {
			possibleActions.remove(ACTIONS.ACTION_UP);
		} else if (playerY + state.getBlockSize() == state.getWorldDimension().height) {
			possibleActions.remove(ACTIONS.ACTION_DOWN);
		}

		return possibleActions;
	}

	/**
	 * Given an array of possible actions and a node, it chooses one random action
	 * and adds the resulting childNode to a stack of nodes.
	 */
	private final void chooseRandomAction(ArrayDeque<DFSNode> nodesStack, DFSNode currentNode,
			ACTIONS[] possibleActions) {
		if (possibleActions.length > 0) {
			int index = randomGenerator.nextInt(possibleActions.length);
			ACTIONS a = possibleActions[index];

			StateObservation stateCopy = currentNode.state.copy();
			stateCopy.advance(a);
			DFSNode childNode = new DFSNode(stateCopy, currentNode, currentNode.depth + 1, a);
			nodesStack.push(childNode);

			double stateValue = value(stateCopy);
			nodesEvaluated++;
			if (reachedGoalState) {
				/*
				 * Found goal state, create LinkedList of all actions from the original root to
				 * the goal state.
				 */
				goalTrace = buildGoalTrace(childNode);
			}
			if (stateValue > bestScoreSeen) {
				bestScoreSeen = stateValue;
			}
		}
	}

	/**
	 * Given a list of possible actions and a node, it chooses the action with the
	 * highest value and adds the resulting childNode to a stack of nodes.
	 */
	private final void chooseBestAction(ArrayDeque<DFSNode> nodesStack, DFSNode currentNode,
			List<ACTIONS> possibleActions) {
		DFSNode childNode = null;
		double bestChild = Double.NEGATIVE_INFINITY;
		while (!possibleActions.isEmpty()) {
			int index = randomGenerator.nextInt(possibleActions.size());
			ACTIONS a = possibleActions.get(index);
			possibleActions.remove(index);
			StateObservation stateCopy = currentNode.state.copy();
			stateCopy.advance(a);
			double stateValue = value(stateCopy);
			nodesEvaluated++;
			if (reachedGoalState) {
				goalTrace = buildGoalTrace(childNode);
			}
			if (stateValue > bestChild) {
				childNode = new DFSNode(stateCopy, currentNode, currentNode.depth + 1, a);
				bestChild = stateValue;
				if (bestChild > bestScoreSeen) {
					bestScoreSeen = stateValue;
				}
			}
		}
		if (childNode != null) {
			nodesStack.push(childNode);
		}
	}

	/**
	 * Given a list of possible actions and a node, it chooses all action and adds
	 * the resulting childNodes to a stack of nodes.
	 */
	private final void chooseAllActions(ArrayDeque<DFSNode> nodesStack, DFSNode currentNode,
			List<ACTIONS> possibleActions) {
		while (!possibleActions.isEmpty()) {
			int index = randomGenerator.nextInt(possibleActions.size());
			ACTIONS a = possibleActions.get(index);
			possibleActions.remove(index);
			StateObservation stateCopy = currentNode.state.copy();
			stateCopy.advance(a);
			DFSNode childNode = new DFSNode(stateCopy, currentNode, currentNode.depth + 1, a);
			double stateValue = value(stateCopy);
			nodesEvaluated++;
			if (reachedGoalState) {
				goalTrace = buildGoalTrace(childNode);
			}
			if (stateValue > bestScoreSeen) {
				bestScoreSeen = stateValue;
			}
			nodesStack.push(childNode);
		}
	}

	/**
	 * Determines the value of a state. Only very basic heuristic for DFS, quantity
	 * over quality.
	 * 
	 * @param a_gameState
	 * @param map
	 * @return a basic estimate of the value of a_gameState
	 */
	private final double value(StateObservation a_gameState) {

		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();
		double rawScore = Agent.REAL_SCORE_FACTOR * a_gameState.getGameScore();

		if (gameOver && win == Types.WINNER.PLAYER_LOSES)
			rawScore += -100000d;

		if (gameOver && win == Types.WINNER.PLAYER_WINS)
			rawScore += 100000d;

		return rawScore;
	}

	/**
	 * Creates a trace of actions to reach a goal state.
	 *
	 * @param childNode
	 *            the goal state
	 * @return a path from the root of the search tree to the goal state
	 */
	private final LinkedList<ACTIONS> buildGoalTrace(DFSNode childNode) {
		LinkedList<ACTIONS> trace = new LinkedList<ACTIONS>();
		DFSNode currentNode = childNode;
		while (currentNode != null) {
			trace.addFirst(currentNode.lastAction);
			currentNode = currentNode.parent;
		}

		return trace;
	}
}