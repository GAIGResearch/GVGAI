package agents.AtheneAI.search.waysolving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import agents.AtheneAI.util.GameTypeChecker;
import agents.AtheneAI.util.Util;

/**
 * Player you can use to get actions to reach a certain object.
 * 
 * NOT USED CURRENTLY.
 * 
 * TODO: maybe use somewhere to reach exit in the last few ticks?!
 */
public class AStarPlayer {
	A_Star aStar;

	// Knowledge of the player
	private HashMap<Integer, List<Integer>> unreachableObsIDs;
	private int blockSize;

	// current observations/positions
	private Observation currentTargetObs;
	private int[] currentTarget; // in blocks (not pixel)
	private int[] myPosition; // in blocks (not pixel)
	private ACTIONS myDirection;

	// true to get detailed information printed
	private boolean debug = true;

	public AStarPlayer(StateObservation state) {
		unreachableObsIDs = new HashMap<Integer, List<Integer>>();
		blockSize = state.getBlockSize();

		currentTargetObs = null;
		currentTarget = new int[] { -1, -1 };
		myPosition = new int[] { -1, -1 };
		myDirection = ACTIONS.ACTION_NIL;
	}

	/**
	 * Returns a new action plan (i.e. a list of actions to go to reach an
	 * interesting object). In this method AStarPlayer autonomously determines what
	 * is the most interesting object.
	 */
	public List<ACTIONS> getNewActionPlan(StateObservation state, ElapsedCpuTimer timer, int agentType) {
		aStar = new A_Star(state, timer, 10);
		List<ACTIONS> actionPlan = new LinkedList<ACTIONS>();

		// when to check? what to do when runs out?
		long remaining = timer.remainingTimeMillis();

		// info about game state
		updatePlayer(state);
		boolean doubleMoves = GameTypeChecker.doubleMovesNeeded(state);

		// determine where to go next
		whereToGoNext(state);
		if (currentTarget[0] < 0 || currentTarget[1] < 0) {
			return actionPlan;
		}

		// try to find a path
		if (debug)
			System.out.println("trying to find path from (" + myPosition[0] + "," + myPosition[1] + ") to ("
					+ currentTarget[0] + "," + currentTarget[1] + ")");
		if (aStar.solve(myPosition[0], myPosition[1], currentTarget[0], currentTarget[1], false, false)) {

			// get the actions from start to end
			List<ACTIONS> actions = aStar.getActionSequence(doubleMoves, myDirection);

			if (debug) {
				System.out.print("A* found a path: ");
				for (ACTIONS a : actions) {
					System.out.print(a + "->");
				}
				System.out.println("");
			}

		} else {
			if (debug)
				System.out.println("A* found no path");
			this.setObservationUnreachable(currentTargetObs.itype, currentTargetObs.obsID);
		}

		return actionPlan;
	}

	/**
	 * Determines the most interesting object in the given gamestate.
	 * 
	 * TODO: take possible exits?
	 */
	private void whereToGoNext(StateObservation stateObs) {
		Observation bestObservation = null;

		ArrayList<Observation>[] portals = stateObs.getResourcesPositions(stateObs.getAvatarPosition());

		if (portals != null) {
			for (ArrayList<Observation> portalType : portals) {
				if (portalType != null && !portalType.isEmpty()) {
					for (Observation portal : portalType) {
						if (// !isObservationUnreachable(portal.itype, portal.obsID) &&
							// knowledge.getBenefit(portal.itype) > 0
						true) {
							bestObservation = portal;
						}
					}
				}
			}
		}

		// values indicating that nothing is found
		currentTarget[0] = -1;
		currentTarget[1] = -1;
		currentTargetObs = null;

		if (bestObservation != null) {
			if (debug)
				System.out.println("nearest interesting observation is itype=" + bestObservation.itype + ", obsID="
						+ bestObservation.obsID);
			currentTarget[0] = (int) bestObservation.position.x / blockSize;
			currentTarget[1] = (int) bestObservation.position.y / blockSize;
			currentTargetObs = bestObservation;

		} else {
			if (debug)
				System.out.println("nothing interesting found to go");
		}
	}

	/**
	 * Returns true if an immovable is unreachable, otherwise false.
	 */
	private boolean isObservationUnreachable(int typeID, int obsID) {
		if (unreachableObsIDs.containsKey(typeID)) {
			return unreachableObsIDs.get(typeID).contains(obsID);
		} else {
			return false;
		}
	}

	/**
	 * Adds an immovable's unique obsID to the list of unreachable immovables.
	 */
	private void setObservationUnreachable(int typeID, int obsID) {
		if (debug)
			System.out.println("setting observation unreachable: itype=" + typeID + ", obsID=" + obsID);
		if (unreachableObsIDs.containsKey(typeID)) {
			unreachableObsIDs.get(typeID).add(obsID);
		} else {
			List<Integer> l = new LinkedList<Integer>();
			l.add(obsID);
			unreachableObsIDs.put(typeID, l);
		}
	}

	/**
	 * Updates player position and direction
	 */
	private void updatePlayer(StateObservation state) {
		Vector2d avPos = state.getAvatarPosition();
		myPosition[0] = (int) avPos.x / blockSize;
		myPosition[1] = (int) avPos.y / blockSize;
		myDirection = Util.getDirection(state.getAvatarOrientation());
	}
}
