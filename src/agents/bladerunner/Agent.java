package agents.bladerunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import agents.bladerunner.agents.GameAgent;
import agents.bladerunner.agents.hbfs.HBFSAgent;
import agents.bladerunner.agents.mcts.MCTSAgent;
import agents.bladerunner.agents.misc.AdjacencyMap;
import agents.bladerunner.agents.misc.GameClassifier;
import agents.bladerunner.agents.misc.GameClassifier.GameType;
import agents.bladerunner.agents.misc.ITypeAttractivity;
import agents.bladerunner.agents.misc.PersistentStorage;
import agents.bladerunner.agents.misc.RewardMap;

/**
 * The main agent holding subagents.
 *
 */
public class Agent extends AbstractPlayer {

	/**
	 * The type of agent we intend to run.
	 */
	public enum AgentType {
		MCTS, BFS, MIXED
	}

	ArrayList<Observation>[][] grid;
	protected int blockSize; // only needed for the drawing

	public static final boolean isVerbose = true;

	/**
	 * The agent type we force the agent into.
	 */
	public AgentType forcedAgentType = AgentType.MIXED;

	/**
	 * Agents
	 */
	private static MCTSAgent mctsAgent = null;
	private static HBFSAgent hbfsAgent = null;
	private static GameAgent currentAgent = null;

	/**
	 * Agent switching properties
	 */
	private GameAgent previousAgent = null;
	private static int agentSwitchTicksRemaining = 0;

	/**
	 * Public constructor with state observation and time due.
	 * 
	 * @param so
	 *            state observation of the current game.
	 * @param elapsedTimer
	 *            Timer for the controller creation.
	 */
	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		// #################
		// PERSISTENT STORAGE
		// Get the actions in a static array.
		ArrayList<Types.ACTIONS> act = so.getAvailableActions();
		PersistentStorage.actions = new Types.ACTIONS[act.size()];
		for (int i = 0; i < PersistentStorage.actions.length; ++i) {
			PersistentStorage.actions[i] = act.get(i);
		}

		// initialize exploration reward map with 1
		PersistentStorage.rewMap = new RewardMap(so, 0.3);

		// set the MCTS_depth back
		PersistentStorage.MCTS_DEPTH_RUN = PersistentStorage.MCTS_DEPTH_FIX;

		PersistentStorage.startingReward = 0;

		// initialize the adjacency map with the current state observation
		PersistentStorage.adjacencyMap = new AdjacencyMap(so);

		PersistentStorage.previousAvatarRessources = new HashMap<>();

		// Classify game
		GameClassifier.determineGameType(so);

		// save some information over a set of games
		PersistentStorage.GameCounter++;

		if ((PersistentStorage.GameCounter - 1) % 5 == 0) {
			// initialize new iTypeAttractivities otherwise keep them
			int numActions = act.size();
			// initialize ItypeAttracivity object for starting situation
			PersistentStorage.iTypeAttractivity = new ITypeAttractivity(so, numActions);
			PersistentStorage.lastGameState = null;
			PersistentStorage.lastWinLoseExpectation = 0;
		} else {
			// Check the last game state for reasons of death or win
			if (PersistentStorage.lastGameState != null) {
				StateObservation lastState = PersistentStorage.lastGameState;

				Vector2d avPos = lastState.getAvatarPosition();
				int blockSize = lastState.getBlockSize();

				// here we assume somehow death
				ArrayList<Observation>[] npcPositions = null;
				npcPositions = lastState.getNPCPositions(avPos);
				if (npcPositions != null) {
					for (ArrayList<Observation> npcs : npcPositions) {
						if (npcs.size() > 0) {
							// only look at the closest rewarding/punishing npc
							Vector2d npcPos = npcs.get(0).position;
							// check is NPS was adjacent
							if (Math.sqrt(Math.pow(npcPos.x - avPos.x, 2) + Math.pow(npcPos.y - avPos.y, 2)) - 2 < Math
									.sqrt(2 * blockSize * blockSize)
									&& PersistentStorage.iTypeAttractivity.get(npcs.get(0).itype) < -0.5
									&& PersistentStorage.lastWinLoseExpectation < 0) {
								// I label the itype attractivy of those enemies as -2, since we died from them.
								PersistentStorage.iTypeAttractivity.put(npcs.get(0).itype, -2.0);
							}
						}
					}
				}
			}
		}

		// use time that is left to build a tree or do BFS
		if ((GameClassifier.getGameType() == GameType.STOCHASTIC || forcedAgentType == AgentType.MCTS)
				&& forcedAgentType != AgentType.BFS) {
			// Create the player.
			mctsAgent = new MCTSAgent(so, elapsedTimer, new Random());
			currentAgent = mctsAgent;
		} else {
			hbfsAgent = new HBFSAgent(so, elapsedTimer);
			currentAgent = hbfsAgent;
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
	public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;

		// this is just for the drawing. comment it out, if you don't need it
		// DrawingTools.updateObservation(stateObs);

		try {
			action = currentAgent.act(stateObs, elapsedTimer);
		} catch (OutOfMemoryError e) {
			currentAgent.clearMemory();
			// action = currentAgent.act(stateObs, elapsedTimer);
		}

		// if agent is switched to another one
		if (agentSwitchTicksRemaining > 0 && previousAgent != null) {
			agentSwitchTicksRemaining--;
		} else {
			switchBack();
		}

		return action;
	}

	/**
	 * Switch to another agent for a limited number of ticks.
	 * 
	 * @note TODO: Not yet working, the agents need to be both initialized.
	 * 
	 * @param type
	 *            The type of agent we want to switch to.
	 */
	public void switchAgent(AgentType type) {
		switch (type) {
		case BFS:
			if (hbfsAgent != null) {
				currentAgent = hbfsAgent;
			} else {
				throw new NullPointerException("HBFS Agent needs to be initialized.");
			}
			break;
		case MCTS:
			if (mctsAgent != null) {
				currentAgent = mctsAgent;
			} else {
				throw new NullPointerException("MCTS Agent needs to be initialized.");
			}
			break;
		case MIXED:
		default:
			break;
		}
	}

	/**
	 * Switch to another agent for a limited number of ticks.
	 * 
	 * @note TODO: Not yet working, the agents need to be both initialized.
	 * 
	 * @param type
	 *            The type of agent we want to switch to.
	 * @param ticks
	 *            The number of ticks the agent should be switched.
	 */
	public static void switchAgentForTicks(AgentType type, int ticks) {
		agentSwitchTicksRemaining = ticks;
		switch (type) {
		case BFS:
			if (hbfsAgent != null) {
				currentAgent = hbfsAgent;
			} else {
				throw new NullPointerException("HBFS Agent needs to be initialized.");
			}
			break;
		case MCTS:
			if (mctsAgent != null) {
				currentAgent = mctsAgent;
			} else {
				throw new NullPointerException("MCTS Agent needs to be initialized.");
			}
			break;
		case MIXED:
		default:
			break;
		}
	}

	/**
	 * Switch the agent back to the previous agent.
	 * 
	 * @note TODO: Not yet working, the agents need to be both initialized.
	 */
	public void switchBack() {
		if (previousAgent != null) {
			currentAgent = previousAgent;
			previousAgent = null;
		}
	}

	/**
	 * Gets the player the control to draw something on the screen. It can be used
	 * for debug purposes. The draw method of the agent is called by the framework
	 * (VGDLViewer) whenever it runs games visually Comment this out, when you do
	 * not need it We could draw anything!
	 * 
	 * @param g
	 *            Graphics device to draw to.
	 */
	/*
	 * public void draw(Graphics2D g) { DrawingTools.draw(g); }
	 */
	// if you want to use that, you also have to uncomment
	// DrawingTools.updateObservation() in the act method

}
