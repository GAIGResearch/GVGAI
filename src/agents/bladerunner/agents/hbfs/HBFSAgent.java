package agents.bladerunner.agents.hbfs;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.TreeSet;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import agents.bladerunner.Agent;
import agents.bladerunner.agents.GameAgent;
import agents.bladerunner.agents.misc.ObservationTools;

// Heuristic Breadth First Search
// 
// - Paths are scored by a heuristic:
//   - It is a weighted sum of 
//   	+ depth
//   	- how many events have been created 
//   	- 1.75^(how many different tile interactions have been seen)
//   	- how did the total number of tiles change (positive for decrease)
//   	(depth has a positive weight, the other 3 weights are negative)
// - Paths with minimal values of the heuristic are considered for expansion
//   (Paths under consideration are stored in the pipe)
//  
// - Loops are prevented by keeping a hash set of visited states (visited)
//   Hash codes for the StateObservation are computed via 
//	 Rotating hash for sequences of small values:
//   http://burtleburtle.net/bob/hash/doobs.html
//   (heuristics and hashing in BFSNode)
//
// - Both pipe and rejection Set are cleared once they reach a limit number of 
//   elements to prevent stalling and eventual out of memory errors
// 
// - Increase memory available to java: add VM Arguments -Xmx4096m and -Xms1024m  in eclipse run configuration dialog (run button)
//
//   Puzzle Style Games: * (see HBFSRunner) work with HBFS, heuristic parameters wT = -3; wL = -2;
// - When running two different games in a row, errors can occur. The controller gets reset, so it is currently unclear why this happens. 
//   As a workaround Run only blocks of the same game only.
// - In some games the forward model does not seem to work properly. E.g. in BOLOADVENTURES (level 1), 
//   an initial move to the left is not reflected in the updated StateObservation (see comments in HBFSAgent.initializeBfs(StateObservation so))
// @author Sepp Kollmorgen
//
//

public class HBFSAgent extends GameAgent {

	public static final int STATE_PLANNING = 1;
	public static final int STATE_ACTING = 2;
	public static final int STATE_IDLE = 3;
	public static final int STATE_OTHER = 4;

	public static final int prime = 179426549; // 4583; // 4583; 7927; 13163; 18097;

	public static int MAX_PIPE_LENGTH = 3000;
	public static int MAX_REJECTION_SET_SIZE = 10000;
	// public static int INITIAL_REJECTION_SET_CAPACITY = 4000;
	public static int CARRY_OVER_PIPE_LENGTH_HEAD = (int) Math.round(MAX_PIPE_LENGTH * 0.06);
	public static int CARRY_OVER_PIPE_LENGTH_BODY = (int) Math.round(MAX_PIPE_LENGTH * 0.04);;

	public static final int callReportFrequency = 10000;

	public static final double wLoad = -2; // -2; // -4
	public static final double wPosition = 0;
	public static final double wTileDiversity = -3; // -2
	public static final double wEvents = -0.1;
	public static final double wDepth = 1;
	public static final double wTransforms = -1;

	public static final int INITIALIZATION_REMTIME = 25;
	public static final int ACTION_REMTIME = 10;
	public static final int INITIALIZATION_ITEMS_PER_ROUND = 1;
	public static final int ACTION_ITEMS_PER_ROUND = 1;
	public static final boolean IS_VERY_VERBOSE = false;
	public static final boolean TRACK_HASHING = false;
	public static final boolean RESPECT_AGENT_ORIENTATION = true; // true works better for brain man
	public static final boolean REPSECT_AGENT_SPEED = false;
	public static final int reportFrequency = 100;
	public static final int MAX_TICKS = 1800;
	public static final int MAX_TICKS_2nd_TIMEOUT = 1950;

	public static int NUM_ACTIONS;
	public static Types.ACTIONS[] ACTIONS;
	public static int rootLoad = -1;
	public static HashMap<Integer, Integer> rootObservationList = null;
	public static double correspondingScore = Double.NEGATIVE_INFINITY;
	public static double maxScoreDifference = Double.NEGATIVE_INFINITY;
	public static int compareCalls = 0;
	public static int equalCalls = 0;
	public static int hashCollisions = 0;
	public static int hashesEqual = 0;
	public static List<Integer> hashList = new LinkedList<Integer>();

	public int controllerState = STATE_PLANNING;
	public Stack<Types.ACTIONS> actionSequence = null;

	public PriorityQueue<HBFSNode> pipe = null;
	// public HashSet<HBFSNode> visited = null;
	public TreeSet<Integer> visited = null;
	public HBFSNode hbfsRoot = null;
	public HBFSNode hbfsSolution = null;

	public int stats_rejects = 0;
	public int stats_nonUseful = 0;
	public int turnAroundSpeed = -1;
	public int pipeEmptyEvents = 0;
	public boolean hasTimedOut = false;

	private void initializeHbfs(StateObservation so) {
		if (Agent.isVerbose) {
			System.out.println("HBFS::##Initializing HBFS...");
		}
		System.gc();

		// testForwardModel(so);
		controllerState = STATE_OTHER;

		pipe = new PriorityQueue<HBFSNode>(MAX_PIPE_LENGTH);
		// visited = new HashSet<HBFSNode>(INITIAL_REJECTION_SET_CAPACITY);
		visited = new TreeSet<Integer>();

		// reset protocol statistics
		stats_rejects = 0;
		stats_nonUseful = 0;
		turnAroundSpeed = -1;
		pipeEmptyEvents = 0;
		HBFSAgent.maxScoreDifference = Double.NEGATIVE_INFINITY;
		HBFSAgent.correspondingScore = Double.NEGATIVE_INFINITY;
		HBFSAgent.rootLoad = -1;
		HBFSAgent.equalCalls = 0;
		HBFSAgent.compareCalls = 0;

		hbfsRoot = new HBFSNode(so, null, null, 0);
		rootObservationList = ObservationTools.getObsList(so);

		HBFSNode.setRootLoad(hbfsRoot.getLoad());
		// HBFSNode.displayStateObservation(so);

		if (hbfsRoot.so.isGameOver()) {
			throw new IllegalStateException();
		}

		pipe.add(hbfsRoot);
		visited.add(hbfsRoot.hashCode());

		controllerState = STATE_PLANNING;
	}

	public void testForwardModel(StateObservation so) {
		System.out.println("HBFS::##Testing Forward Model...");

		StateObservation s0 = so;
		int[] es = new int[ACTIONS.length];
		int[] es2 = new int[ACTIONS.length];
		int[] d = new int[ACTIONS.length];
		Stack<StateObservation> s = new Stack<StateObservation>();
		for (int k = 0; k < ACTIONS.length; k++) {
			so = s0.copy();
			so.advance(ACTIONS[k]);
			if (s0.getAvatarPosition().equals(so.getAvatarPosition())) {
				// no effect on position
				es[k]++;
				// repeat action
				so = so.copy();
				so.advance(ACTIONS[k]);
				// so.advance(Types.ACTIONS.ACTION_NIL);

				if (s0.getAvatarPosition().equals(so.getAvatarPosition())) {
					es2[k]++;
				} else {
					s.push(so);
				}
			} else {
				s.push(so);
			}
			d[k] = es[k] - es2[k];
			System.out.println(
					"HBFS::" + ACTIONS[k] + " | ineffective on repeat: " + es2[k] + " | ineffective on 1st: " + es[k]);

		}
		for (StateObservation so2 : s) {
			testForwardModel(so2);
		}

	}

	private void cleanHbfs() {
		pipe.clear();
		visited.clear();
		hbfsRoot = null;
		rootObservationList = null;
		hbfsSolution = null;
		actionSequence = null;
		pipe = null;
		visited = null;
		// System.gc();
	}

	@SuppressWarnings("unused")
	private boolean performHbfs(ElapsedCpuTimer elapsedTimer, int remTime) {

		if (pipe.isEmpty()) {
			controllerState = STATE_OTHER;
			if (Agent.isVerbose) {
				System.out.println("HBFS::performBfs was called on empty pipe. Changing to STATE_OTHER.");
			}
			return false;
		}

		HBFSNode current = pipe.remove();

		for (Types.ACTIONS a : ACTIONS) {
			StateObservation soCopy = current.so.copy();
			soCopy.advance(a);

			if (elapsedTimer.remainingTimeMillis() < remTime) {
				pipe.add(current); // could get stuck, but usually at least one node can be fully processed.
				break;
			}

			if (soCopy.isGameOver()) {
				if (soCopy.getGameWinner() == Types.WINNER.PLAYER_WINS) {
					hbfsSolution = new HBFSNode(soCopy, a, current, current.depth + 1);
					hbfsSolution.getEventScore();
					return true;
				}
			} else {

				if (visited.size() > MAX_REJECTION_SET_SIZE) {
					visited.clear();
					if (Agent.isVerbose) {
						System.out.print("RSc.");
					}
					// System.gc();
				}

				if (pipe.size() > MAX_PIPE_LENGTH) {
					resetPipe();
					// System.gc();
				}

				HBFSNode m = new HBFSNode(soCopy, a, current, current.depth + 1);

				if (visited.add(m.hashCode())) {
					pipe.add(m);
					// visited.add(m);
				} else {
					stats_rejects++;
				}

				m = null;
			}
		}

		if (pipe.isEmpty()) {
			// Pipe is seeded with children of current and current itself
			if (Agent.isVerbose) {
				System.out.println("\nHBFS::#Pipe unexpectedly empty. Reseeding and clearing rejection set.");
			}
			visited.clear();
			for (Types.ACTIONS a : ACTIONS) {
				StateObservation soCopy = current.so.copy();
				soCopy.advance(a);
				HBFSNode m = new HBFSNode(soCopy, a, current, current.depth + 1);
				visited.add(m.hashCode());
				pipe.add(m);
			}
			visited.add(current.hashCode());
			pipe.add(current);
			pipeEmptyEvents += 1;
		} else {
		}

		if (Agent.isVerbose && HBFSAgent.IS_VERY_VERBOSE) {
			current.displayActionSequence();
			displayAgentState(current);
		}
		return false;
	}

	private void resetPipe() {
		Stack<HBFSNode> backup = new Stack<HBFSNode>();
		for (int k = 0; k < CARRY_OVER_PIPE_LENGTH_HEAD; k++) {
			backup.push(pipe.remove());
		}
		if (CARRY_OVER_PIPE_LENGTH_BODY > 0) {
			int nth = pipe.size() / CARRY_OVER_PIPE_LENGTH_BODY;
			int n = 0;
			for (HBFSNode node : pipe) {
				n++;
				if (n % nth == 1) {
					backup.push(node);
				}

			}
		}
		pipe.clear();
		pipe.addAll(backup);
		if (Agent.isVerbose) {
			System.out.print("Pc.");
		}
	}

	/**
	 * Public constructor with state observation and time due.
	 * 
	 * @param so
	 *            state observation of the current game.
	 * @param elapsedTimer
	 *            Timer for the controller creation.
	 */
	public HBFSAgent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		// Get the actions in a static array.
		if (Agent.isVerbose) {
			System.out.println("HBFS::##Creating HBFSAgent...");
		}
		ArrayList<Types.ACTIONS> act = so.getAvailableActions();
		ACTIONS = new Types.ACTIONS[act.size()];
		for (int i = 0; i < ACTIONS.length; ++i) {
			ACTIONS[i] = act.get(i);
		}
		NUM_ACTIONS = ACTIONS.length;

		initializeHbfs(so);

		boolean hasTerminated = false;
		while (!hasTerminated && elapsedTimer.remainingTimeMillis() > INITIALIZATION_REMTIME
				&& controllerState == STATE_PLANNING) {
			hasTerminated = performHbfs(elapsedTimer, INITIALIZATION_REMTIME);
		}
		if (controllerState != STATE_PLANNING) {
			if (Agent.isVerbose) {
				System.out.println("HBFS::#Controller State: controllerState");
			}
		}
	}

	public void displayAgentState() {
		displayAgentState(null);
	}

	public void displayAgentState(HBFSNode node) {
		if (node == null)
			node = pipe.peek();
		if (node == null) {
			if (Agent.isVerbose) {
				System.out.println("HBFS::#Pipe Empty");
			}
			return;
		}
		if (Agent.isVerbose) {
			System.out.println();
			System.out.format(
					"HBFS::Pipe:%5d|R.Set:%5d|Rejects:%6d|Depth:%3d|Events:%3d|E.Score:%3.2f|D.Score:%3.2f|L.Score:%3.2f|T.Score:%3.2f|Score:%3.2f|B.Delta:%3.2f|C.Score:%3.2f|Speed:%3d",
					pipe.size(), visited.size(), stats_rejects, node.depth, node.so.getHistoricEventsHistory().size(),
					node.getEventScore(), node.getTileDiversityScore(), node.getLoadScore(), node.getTransformScore(),
					node.getScore(), HBFSAgent.maxScoreDifference, HBFSAgent.correspondingScore, turnAroundSpeed);
		}
	}

	/**
	 * Picks an action. This function is called every game step to request an action
	 * from the player.
	 * 
	 * @param so
	 *            Observation of the current state.
	 * @param elapsedTimer
	 *            Timer when the action returned is due.
	 * @return An action for the current state
	 */
	public Types.ACTIONS act(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		switch (controllerState) {
		case STATE_ACTING:

			if (actionSequence.isEmpty()) {
				if (Agent.isVerbose) {
					if (HBFSAgent.IS_VERY_VERBOSE) {
						HBFSNode.displayStateObservation(so);
					}
					System.out.println("HBFS::--Action Stack Empty.");
				}
				controllerState = STATE_IDLE;
				cleanHbfs(); // free handles to allow the garbage collector to
								// start cleaning.
				return Types.ACTIONS.ACTION_NIL;
			}
			if (Agent.isVerbose) {
				if (HBFSAgent.IS_VERY_VERBOSE) {
					HBFSNode.displayStateObservation(so);
				}
				System.out.println("HBFS::--Performing Action: " + actionSequence.peek());
			}
			return actionSequence.pop();

		case STATE_PLANNING:

			if (so.getGameTick() % reportFrequency == 1) {
				displayAgentState();
			}
			boolean hasTerminated = false;
			turnAroundSpeed = 0;
			while (!hasTerminated && elapsedTimer.remainingTimeMillis() > ACTION_REMTIME
					&& controllerState == STATE_PLANNING) {
				hasTerminated = performHbfs(elapsedTimer, ACTION_REMTIME);
				turnAroundSpeed += 1;
			}
			if (hasTerminated) {
				controllerState = STATE_ACTING;
				actionSequence = hbfsSolution.getActionSequence();
				if (Agent.isVerbose) {
					System.out.println("\nHBFS::#Solution Found. ACTING Phase...");
					System.out.println("Best Sequence Length: " + actionSequence.size());
				}
			}
			if (hasTimedOut && so.getGameTick() > MAX_TICKS_2nd_TIMEOUT
					|| (!hasTimedOut && so.getGameTick() > MAX_TICKS)) {
				controllerState = STATE_ACTING;
				hbfsSolution = pipe.peek();
				actionSequence = hbfsSolution.getActionSequence();
				if (Agent.isVerbose) {
					System.out.println("\nHBFS::#Timeout! ACTING Phase...");
					System.out.println("Timeout Sequence Length: " + actionSequence.size());
				}
				hasTimedOut = true;
			}
			if (pipeEmptyEvents > 1) {
				controllerState = STATE_ACTING;
				actionSequence = new Stack<Types.ACTIONS>();
				actionSequence.push(ACTIONS[(int) Math.floor(Math.random() * 4)]);
				if (Agent.isVerbose) {
					System.out.println("\nHBFS::#Pipe Constantly Empty! Performing some move. ACTING Phase...");
					System.out.println("HBFS::Random Sequence Length: " + actionSequence.size());
				}
			}
			return Types.ACTIONS.ACTION_NIL;

		case STATE_IDLE:
		case STATE_OTHER:
			if (!so.isGameOver()) {
				if (Agent.isVerbose) {
					System.out.println("\nHBFS::#Controller IDLE but game continues. Restarting PLANNING Phase...");
				}
				initializeHbfs(so);
				controllerState = STATE_PLANNING;
			}
			return Types.ACTIONS.ACTION_NIL;
		default:
			throw new IllegalStateException();
		}
	}

	public void clearMemory() {
		if (Agent.isVerbose) {
			System.out.println("\nHBFS::#Clearing Memory.");
		}
		visited.clear();
		resetPipe();
		if (Agent.isVerbose) {

			System.out.print("RSc.");
		}
	}

	public static void saveHashlist() {
		try {
			FileWriter fos = new FileWriter("hashList.data");
			PrintWriter dos = new PrintWriter(fos);
			// loop through all your data and print it to the file
			for (int q : hashList) {
				dos.println(q);
			}
			dos.close();
			fos.close();
		} catch (Exception e) {
			System.out.println("Couldn't write hash list.");

		}
	}

	public static void displayHashingDiagnostics() {
		System.out.println("HBFS::Hashing Diagnostics: " + HBFSAgent.equalCalls + " equal calls; "
				+ HBFSAgent.hashCollisions + "/" + HBFSAgent.hashesEqual + " hash collisions/hashes equal"
				+ "; collision fraction: " + (double) HBFSAgent.hashCollisions / (double) HBFSAgent.hashesEqual);
	}

}
