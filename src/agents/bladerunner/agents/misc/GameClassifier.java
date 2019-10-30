package agents.bladerunner.agents.misc;

import agents.bladerunner.Agent;
import core.game.StateObservation;
import ontology.Types;

public class GameClassifier {

	/**
	 * Number of test steps we run in the game classifier.
	 */
	public static final int testingSteps = 80;

	/**
	 * The game classification categories.
	 *
	 */
	public enum GameType {
		STOCHASTIC, DETERMINISTIC, NOT_DETERMINED
	}

	/**
	 * Our chosen game category
	 */
	static GameType gameType;

	public static GameType determineGameType(StateObservation so) {
		gameType = GameType.NOT_DETERMINED;

		// // Stochasticity 1
		// // Main problems: Some movement does not happend during the first 10
		// // steps.
		// // Advance a bit to check if stochastic
		// StateObservation testState1 = so.copy();
		// StateObservation testState2 = so.copy();
		// for (int ii = 0; ii < 10; ii++) {
		// testState1.advance(Types.ACTIONS.ACTION_NIL);
		// testState2.advance(Types.ACTIONS.ACTION_NIL);
		//
		// // I believe the advance method is more costly than the equiv
		// // method.
		// if (!testState1.equiv(testState2)) {
		// gameType = GameType.STOCHASTIC;
		// break;
		// }
		// }
		// gameType = GameType.DETERMINISTIC;

		// // Stochasticity 2
		// // Main problems: Some moving objects are not NPCs.
		// // Checks if there are Non player characters
		// StateObservation testState2 = so.copy();
		// for (int ii = 0; ii < 10; ii++) {
		// testState2.advance(Types.ACTIONS.ACTION_NIL);
		//
		// // I believe the advance method is more costly than the equiv
		// // method.
		// if (testState2.getNPCPositions() != null
		// && testState2.getNPCPositions().length > 0) {
		// gameType = GameType.STOCHASTIC;
		// break;
		// }
		// }
		// gameType = GameType.DETERMINISTIC;

		// Stochasticity 3
		if (hasMovement(so, testingSteps)) {
			gameType = GameType.STOCHASTIC;
		} else {
			gameType = GameType.DETERMINISTIC;
		}

		if (gameType == GameType.STOCHASTIC) {
			if (Agent.isVerbose) {
				System.out.println("CLASSIFIER::Game seems to be stochastic");
			}
		} else if (gameType == GameType.DETERMINISTIC) {
			PersistentStorage.MCTS_DEPTH_RUN += 20;
			if (Agent.isVerbose) {
				System.out
						.println("CLASSIFIER::Game seems to be deterministic");
			}
		}
		return gameType;

	}

	public static GameType getGameType() {
		return gameType;
	}

	/**
	 * Test if the game has movement in it.
	 * 
	 * @param so
	 *            The state observation.
	 * @param testingSteps
	 *            The number of testing steps we advance the so.
	 * @return If the game has movement or not.
	 */
	public static boolean hasMovement(StateObservation so, int testingSteps) {
		// get initial hash
		int initialHash = ObservationTools.getHash(so);

		// second hash
		int advancedHash = 0;
		if (Agent.isVerbose) {
			System.out.println("Initial hash: " + initialHash);
		}

		// check if hash changes as we advance the forward model
		for (int k = 0; k < testingSteps; k++) {

			// advance the forward model
			so.advance(Types.ACTIONS.ACTION_NIL);

			// get the hash and compare
			advancedHash = ObservationTools.getHash(so);
			if (initialHash != advancedHash) {
				if (Agent.isVerbose) {
					System.out.println(advancedHash + " is different.");
				}
				return true;
			}
		}
		return false;

	}

}
