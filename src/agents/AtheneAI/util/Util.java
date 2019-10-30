package agents.AtheneAI.util;

import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class Util {

	/**
	 * Returns the direction the avatar looks in or NIL if avatar has no direction.
	 */
	public static ACTIONS getDirection(Vector2d avatarOrientation) {
		ACTIONS result = ACTIONS.ACTION_NIL;

		if (avatarOrientation.x < 0) {
			result = ACTIONS.ACTION_LEFT;
		} else if (avatarOrientation.x > 0) {
			result = ACTIONS.ACTION_RIGHT;
		} else if (avatarOrientation.y < 0) {
			result = ACTIONS.ACTION_UP;
		} else if (avatarOrientation.y > 0) {
			result = ACTIONS.ACTION_DOWN;
		}

		return result;
	}

	/**
	 * Returns Manhattan-Distance in pixel between two points.
	 */
	public static int manhattanDistInPx(Vector2d startPosition, Vector2d targetPosition) {
		return (int) (Math.abs(startPosition.x - targetPosition.x) + Math.abs(startPosition.y - targetPosition.y));
	}

	/**
	 * Returns Manhattan-Distance in pixel between two points.
	 */
	public static int manhattanDistInBlocks(Vector2d startPosition, Vector2d targetPosition, int blockSize) {
		return (int) (Math.abs(startPosition.x / blockSize - targetPosition.x / blockSize)
				+ Math.abs(startPosition.y / blockSize - targetPosition.y / blockSize));
	}

	/**
	 * Returns Manhattan-Distance in blocks between two game fields.
	 */
	public static int manhattanDistInBlocks(int startX, int startY, int targetX, int targetY) {
		return (int) (Math.abs(startX - targetX) + Math.abs(startY - targetY));
	}

	/**
	 * Prints information about a game state.
	 * 
	 * @param state
	 *            the game state to print
	 */
	public static void printDebugGameState(StateObservation state) {

		System.out.print("Game State: ");
		printDebugObservationArray(state.getNPCPositions(), "npc");
		printDebugObservationArray(state.getImmovablePositions(), "immov");
		printDebugObservationArray(state.getMovablePositions(), "mov");
		printDebugObservationArray(state.getResourcesPositions(), "res");
		printDebugObservationArray(state.getPortalsPositions(), "portals");
		System.out.println("score = " + state.getGameScore());
	}

	/**
	 * Prints the number of different types of sprites available in the "positions"
	 * array. Between brackets, the number of observations of each type.
	 * 
	 * @param positions
	 *            array with observations.
	 * @param str
	 *            identifier to print
	 */
	public static void printDebugObservationArray(ArrayList<Observation>[] positions, String str) {
		if (positions != null && positions.length != 0) {
			System.out.print(str + ":" + positions.length + "(");
			for (int i = 0; i < positions.length; i++) {
				if (!positions[i].isEmpty())
					System.out.print(positions[i].get(0).itype + " (category=" + positions[i].get(0).category + "):"
							+ positions[i].size() + ",");
			}
			System.out.print("); ");
		} else
			System.out.print(str + ": 0; ");
	}

	/**
	 * using the known position of the agent and the fact, that agents belong to the
	 * 0 category, extract the agent Id from the grid (Maria)
	 */
	public static int getAgentType(StateObservation state) {
		int agentId = 0;
		Vector2d position = state.getAvatarPosition();

		// get all observations on the current position of the agent
		ArrayList<Observation> observations = state.getObservationGrid()[(int) position.x
				/ state.getBlockSize()][(int) position.y / state.getBlockSize()];
		for (Observation o : observations) {
			// agent belongs to the category 0
			if (o.category == 0) {
				return o.itype;
			}
		}
		return agentId;
	}

	public static boolean areMovablesInCurrentState(StateObservation state) {

		ArrayList<Observation>[] movables = state.getMovablePositions();
		if (movables != null) {
			for (ArrayList<Observation> movableType : movables) {
				if (movableType != null && !movableType.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}
}
