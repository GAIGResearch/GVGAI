package agents.AtheneAI.search.bfs;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.AtheneAI.Agent;
import agents.AtheneAI.util.MurmurHash;

public final class BFSPlayer {

	private Random randomGenerator;

	// true if BFS shouldn't be used in Agent
	public boolean disabled;
	public boolean reachedGoalState;
	// contains the action trace for a goalstate if present
	private ArrayDeque<ACTIONS> goalTrace;
	// contains the action trace with the highest value
	public ArrayDeque<ACTIONS> bestNodeTrace;
	// PQ with all open BFSNodes
	private int bestTraceLength;
	private static int traceOffset = 0;
	private PriorityQueue<BFSNode> open;
	// hashes of all BFSNodes in open
	private HashSet<Integer> openHash;
	// hashes of all closed BFSNodes
	private HashSet<Integer> closed;

	// time
	private double totalTimePassed;
	private double oldTotalTimePassed;
	private long totalGCTimePassed;
	private long oldTotalGCTimePassed;
	private static long MIN_REMAINING_TIME;

	// debugging variables
	private int nodesAdded;

	// highest value seen so far in the game
	public static double bestValue;
	// true if the score didn't increase during the iteration
	private boolean noScoreChange;
	private int noScoreChangeCounter;

	// the cost of taking one action for the value function
	private static double stepCost;

	static class BFSNodeComparator implements Comparator<BFSNode> {
		public int compare(BFSNode msg1, BFSNode msg2) {
			return f(msg1).compareTo(f(msg2));
		}
	}

	public BFSPlayer() {
		randomGenerator = new Random();
		reset();
	}

	/**
	 * @param root
	 * @param elapsedTimer
	 * @param reset
	 * @param maxDepth
	 * @param currentIteration
	 * @return
	 */
	public final void solve(StateObservation root, ElapsedCpuTimer elapsedTimer, long minRemainingTime) {

		MIN_REMAINING_TIME = minRemainingTime;
		adjustStepCostAdvanced();

		reachedGoalState = false;
		noScoreChange = true;

		long currentTime = System.currentTimeMillis();
		if (oldTotalTimePassed == 0)
			oldTotalTimePassed = currentTime;

		totalTimePassed = totalTimePassed + (currentTime - oldTotalTimePassed);
		oldTotalTimePassed = currentTime;

		if (Agent.bfsIterations > 100) {
			checkTimeSpentOnGC();
		}

		if (open.isEmpty()) {
			BFSNode rootNode = new BFSNode(root.copy(), 0, "", root.getAvailableActions());
			open.add(rootNode);
			openHash.add(hash(rootNode.state));
		}

		runBFS(elapsedTimer);

		if (noScoreChange) {
			noScoreChangeCounter++;
		} else if (bestValue > 0) {
			noScoreChangeCounter = 0;
		}
	}

	/**
	 * @param depth
	 * @param root
	 * @param elapsedTimer
	 * @return
	 */
	private final void runBFS(ElapsedCpuTimer elapsedTimer) {

		while (elapsedTimer.remainingTimeMillis() > MIN_REMAINING_TIME) {
			if (open.isEmpty()) {
				return;
			}

			// retrieve node n with best score
			BFSNode n = open.poll();

			// expand best node
			boolean finished = chooseAllActions(n, elapsedTimer);

			if (finished) {
				Integer hash = hash(n.state);
				closed.add(hash);
				openHash.remove(hash);
			} else {
				open.add(n);
			}
		}
	}

	/**
	 * Given a list of possible actions and a node, it chooses all action and adds
	 * the resulting childNodes to the open queue.
	 * 
	 * @param elapsedTimer
	 */
	private final boolean chooseAllActions(BFSNode n, ElapsedCpuTimer elapsedTimer) {
		ArrayList<ACTIONS> possibleActions = n.availableActions;

		while (!possibleActions.isEmpty()) {

			int index = randomGenerator.nextInt(possibleActions.size());
			ACTIONS a = possibleActions.get(index);

			StateObservation s = n.state.copy();
			s.advance(a);

			Integer hash = hash(s);

			if (!closed.contains(hash) && !openHash.contains(hash)) {
				if (elapsedTimer.remainingTimeMillis() <= MIN_REMAINING_TIME) {
					/*
					 * not enough time for the following operations, better return at a later point
					 */
					return false;
				}
				possibleActions.remove(index);
				BFSNode succ = new BFSNode(s, n.depth + 1, n.trace + actionToString(a), s.getAvailableActions());
				nodesAdded++;

				if (isGoalState(s)) {
					reachedGoalState = true;
					double value = s.getGameScore();

					// goal trumps everything, even is gameScore is lower
					if (goalTrace.isEmpty()) {
						goalTrace = stringToTrace(succ.trace);
						Agent.goalTrace = new ArrayDeque<ACTIONS>(goalTrace);
						bestValue = value;
						bestTraceLength = succ.depth + traceOffset;
					} else {
						if (value > bestValue) {
							goalTrace = stringToTrace(succ.trace);
							Agent.goalTrace = new ArrayDeque<ACTIONS>(goalTrace);
							bestValue = value;
							bestTraceLength = succ.depth + traceOffset;
						} else if (value == bestValue && (succ.depth + traceOffset) < bestTraceLength) {
							goalTrace = stringToTrace(succ.trace);
							Agent.goalTrace = new ArrayDeque<ACTIONS>(goalTrace);
							bestTraceLength = succ.depth + traceOffset;
						}
					}
				} /*
					 * no GoalState found, check if score is higher or path is shorter with
					 * identical score
					 */
				else {
					double value = s.getGameScore();
					if (value > bestValue) {
						System.out.println("Best Value: " + value + "  Length: " + (succ.depth + traceOffset));
						bestNodeTrace = stringToTrace(succ.trace);
						bestValue = value;
						bestTraceLength = succ.depth + traceOffset;
						noScoreChange = false;
					} else if (value == bestValue && (succ.depth + traceOffset) < bestTraceLength) {
						System.out.println("Best Value: " + value + "  Length: " + (succ.depth + traceOffset)
								+ " Shorter Solution");
						bestNodeTrace = stringToTrace(succ.trace);
						bestTraceLength = succ.depth + traceOffset;
					}

					nodesAdded++;
					openHash.add(hash);
					open.add(succ);
				}
			} else {
				possibleActions.remove(index);
			}
		}
		n = null;
		return true;
	}

	private String actionToString(ACTIONS a) {
		switch (a) {
		case ACTION_LEFT:
			return "l";
		case ACTION_DOWN:
			return "d";
		case ACTION_RIGHT:
			return "r";
		case ACTION_UP:
			return "u";
		case ACTION_USE:
			return "p";
		case ACTION_NIL:
			return "n";
		default:
			return "";
		}
	}

	private ACTIONS charToAction(char c) {
		switch (c) {
		case 'l':
			return ACTIONS.ACTION_LEFT;
		case 'd':
			return ACTIONS.ACTION_DOWN;
		case 'r':
			return ACTIONS.ACTION_RIGHT;
		case 'u':
			return ACTIONS.ACTION_UP;
		case 'p':
			return ACTIONS.ACTION_USE;
		default:
			return ACTIONS.ACTION_NIL;
		}
	}

	private ArrayDeque<ACTIONS> stringToTrace(String trace) {
		ArrayDeque<ACTIONS> a = new ArrayDeque<ACTIONS>(trace.length());
		for (int i = 0, n = trace.length(); i < n; i++) {
			a.add(charToAction(trace.charAt(i)));
		}
		return a;
	}

	/**
	 * Evaluation function f for BFS, determines the value of a BFSNode. Takes score
	 * and path cost into consideration.
	 * 
	 * @param n
	 * @return a basic estimate of the value of a_gameState
	 */
	private final static Double f(BFSNode n) {

		StateObservation s = n.state;
		boolean gameOver = s.isGameOver();
		Types.WINNER win = s.getGameWinner();

		double rawScore = -10 * s.getGameScore();
		rawScore += (n.depth + traceOffset) * stepCost;

		if (gameOver && win == Types.WINNER.PLAYER_LOSES)
			rawScore += 100000d;

		if (gameOver && win == Types.WINNER.PLAYER_WINS)
			rawScore += -100000d;

		return rawScore;
	}

	private boolean isGoalState(StateObservation a_gameState) {

		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();

		return (gameOver && win == Types.WINNER.PLAYER_WINS);
	}

	public ArrayDeque<ACTIONS> getIntermediateResult() {
		return (bestNodeTrace == null) ? new ArrayDeque<ACTIONS>() : bestNodeTrace;
	}

	public void reset() {
		System.out.println("Resetting BFSPlayer");

		// Data
		goalTrace = new ArrayDeque<ACTIONS>();
		BFSNodeComparator bfsCmp = new BFSNodeComparator();
		open = new PriorityQueue<BFSNode>(1000, bfsCmp);
		openHash = new HashSet<Integer>();
		closed = new HashSet<Integer>();
		traceOffset = (bestNodeTrace == null) ? 0 : (traceOffset + bestNodeTrace.size());
		bestNodeTrace = new ArrayDeque<ACTIONS>();
		bestTraceLength = 0;

		// Control Flow
		reachedGoalState = false;
		noScoreChange = true;
		disabled = false;
		noScoreChangeCounter = 0;
		bestValue = Double.NEGATIVE_INFINITY;
		Agent.MAX_BFS_ITERATIONS = 1700;

		// GC
		totalTimePassed = 0;
		oldTotalTimePassed = 0;
		totalGCTimePassed = 0;
		oldTotalGCTimePassed = 0;

		// Step Cost
		stepCost = 0.5d;

		// Debugging
		nodesAdded = 0;
	}

	private Integer hash(StateObservation s) {
		StringBuilder sb = new StringBuilder(2048);

		sb.append(s.getAvatarPosition().x).append("|").append(s.getAvatarPosition().y).append("|");

		ArrayList<Observation>[] movables = s.getMovablePositions();
		if (movables != null) {
			for (ArrayList<Observation> a : movables) {
				if (!a.isEmpty() && !Agent.ignoredSprites.contains(new Integer(a.get(0).itype))) {
					for (Observation o : a) {
						sb.append(o.position).append(o.itype);
					}
				}
			}
		}

		/*
		 * TODO: More intelligent way to only hash immovables that make sense. The
		 * surrounding area is also counted as immovables, so the basic check size<25 in
		 * GameTypeChecker should remove all those cases ( but might result in a game
		 * where sth. with size>=25 should have been hashed.
		 */
		ArrayList<Observation>[] immovables = s.getImmovablePositions();
		if (immovables != null) {
			for (ArrayList<Observation> a : immovables) {
				if (!a.isEmpty() && !Agent.ignoredSprites.contains(new Integer(a.get(0).itype))) {
					for (Observation o : a) {
						sb.append(o.position).append(o.itype);
					}
				}
			}
		}

		ArrayList<Observation>[] ressources = s.getResourcesPositions();
		if (ressources != null) {
			for (ArrayList<Observation> a : ressources) {
				for (Observation o : a) {
					sb.append(o.position).append(o.itype);
				}
			}
		}

		Map<Integer, Integer> avatarRessources = s.getAvatarResources();
		for (Integer key : avatarRessources.keySet()) {
			sb.append(key).append(avatarRessources.get(key));
		}

		// System.out.println("Murmur String: " + sb.toString());
		// System.out.println("Murmur Hash: " + hashCode);
		return new Integer(MurmurHash.hash32(sb.toString()));
	}

	private static long getGarbageCollectionTime() {
		long collectionTime = 0;
		for (GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
			collectionTime += garbageCollectorMXBean.getCollectionTime();
		}
		return collectionTime;
	}

	private void checkTimeSpentOnGC() {
		long gcTime = getGarbageCollectionTime();
		if (oldTotalGCTimePassed == 0)
			oldTotalGCTimePassed = gcTime;

		totalGCTimePassed = totalGCTimePassed + (gcTime - oldTotalGCTimePassed);
		oldTotalGCTimePassed = gcTime;

		// debugGCChecking();
		if ((totalGCTimePassed / totalTimePassed) > 0.65) {
			System.out.println("Aborting BFS due to GC exception concerns!");
			Agent.bfsIterations = Agent.MAX_BFS_ITERATIONS + 1;
		}
	}

	private void debugGCChecking() {
		System.out.println("Total GC Time Spent: " + totalGCTimePassed);
		System.out.println("Procentual GC Time Spent: " + (totalGCTimePassed / totalTimePassed) * 100 + "%");
	}

	private void adjustStepCost() {

		if (Agent.bfsIterations > 100 && bestTraceLength == 1 && bestValue == 0 && stepCost > 0) {
			System.out.println("Trying stepCost of 0");
			stepCost = 0d;
			BFSNodeComparator bfsCmp = new BFSNodeComparator();
			PriorityQueue<BFSNode> save = open;
			open = new PriorityQueue<BFSNode>(1000, bfsCmp);
			open.addAll(save);
			save.clear();
			noScoreChangeCounter++;
		}

		if (noScoreChangeCounter > 80 && bestValue > 0) {
			// adjust bestValue
			if (bestTraceLength >= traceOffset) {
				stepCost = stepCost + (0.05 * ((10 * bestValue - (stepCost * bestTraceLength)) / bestTraceLength));
				noScoreChangeCounter = 0;
				BFSNodeComparator bfsCmp = new BFSNodeComparator();
				PriorityQueue<BFSNode> save = open;
				int pqSize = (open.size() > 0) ? open.size() : 1000;
				open = new PriorityQueue<BFSNode>(pqSize, bfsCmp);
				open.addAll(save);
				save.clear();
			}
		}
	}

	private void adjustStepCostAdvanced() {

		if (!noScoreChange) {
			double newStepCost = ((bestValue * 10) / (bestTraceLength * 1.1));
			if (newStepCost > 0.5) {
				System.out.println("Complete reset because of new bestValue");
				stepCost = newStepCost;
				BFSNodeComparator bfsCmp = new BFSNodeComparator();
				int pqSize = (open.size() > 0) ? open.size() : 1000;
				open = new PriorityQueue<BFSNode>(pqSize, bfsCmp);
				openHash.clear();
				closed.clear();
			}
			// every 50 iterations without any score change, possibly mix up PQ
			// with a new heuristic function
		} else if (noScoreChangeCounter % 50 == 49 && stepCost > 0.5d) {
			if (stepCost >= 1d)
				stepCost /= 2d;
			else
				stepCost = 0.5d;
			System.out.println("Mix up PQ");
			BFSNodeComparator bfsCmp = new BFSNodeComparator();
			PriorityQueue<BFSNode> save = open;
			int pqSize = (open.size() > 0) ? open.size() : 1000;
			open = new PriorityQueue<BFSNode>(pqSize, bfsCmp);
			open.addAll(save);
			save.clear();
		} else if (noScoreChangeCounter == 250) {
			noScoreChangeCounter = 0;
			stepCost = (bestNodeTrace.size() > 1) ? ((bestValue * 10) / (bestTraceLength * 1.1)) : 0;
			System.out.println("Complete reset because of 150 iterations without change");
			BFSNodeComparator bfsCmp = new BFSNodeComparator();
			int pqSize = (open.size() > 0) ? open.size() : 1000;
			open = new PriorityQueue<BFSNode>(pqSize, bfsCmp);
			openHash.clear();
			closed.clear();
		}
	}

	private double getClosestMovable(StateObservation s) {
		ArrayList<Observation>[] a = s.getMovablePositions(s.getAvatarPosition());
		double closestMovable = Double.MAX_VALUE;
		if (a != null) {
			for (int i = 0; i < a.length; i++) {
				if (!a[i].isEmpty() && a[i].get(0).sqDist < closestMovable) {
					closestMovable = a[i].get(0).sqDist;
				}
			}
		}
		return closestMovable;
	}
}