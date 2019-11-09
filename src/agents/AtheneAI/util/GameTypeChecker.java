package agents.AtheneAI.util;

import java.util.ArrayList;
import java.util.List;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import agents.AtheneAI.Agent;

public final class GameTypeChecker {

	/**
	 * Determines whether a game is deterministic or not.
	 * 
	 * @param so
	 *            the game state to analyze
	 * @param elapsedTimer
	 *            Timer when the action returned is due.
	 * @return true if game is deterministic, false if game is stochastic
	 */
	public final static boolean isGameDeterministic(StateObservation so, ElapsedCpuTimer elapsedTimer) {

		ElapsedCpuTimer localTimer = new ElapsedCpuTimer();

		StateObservation soInit = so.copy();
		/*
		 * Small initialization with 5x ACTION_NIL for steady-state. Is the game already
		 * over after this? Yes -> Stochastic Approach
		 */
		for (int i = 0; i < 5; i++) {
			soInit.advance(ACTIONS.ACTION_NIL);
			if (!soInit.isGameOver()) {
				so.advance(ACTIONS.ACTION_NIL);
			} else {
				System.out.println(" stochastic_quick_game");
				System.out.println(" Check for determinisation took: " + localTimer.elapsedMillis() + "ms");
				return false;
			}
		}

		/*
		 * State after initialization is saved and taken as a starting point for further
		 * simulations.
		 */
		StateObservation soSave = so.copy();

		/*
		 * Does the game have NPCs? Yes -> Stochastic Approach
		 */
		if (soSave.getNPCPositions() != null) {
			System.out.println(" stochastic_has_npcs");
			System.out.println(" Check for determinisation took: " + localTimer.elapsedMillis() + "ms");
			return false;
		}

		for (int i = 0; i < 150; i++) {
			so.advance(ACTIONS.ACTION_NIL);
			/*
			 * Is the game over within the first 25 iterations? Yes -> Stochastic Approach.
			 */
			if (i < 25 && so.isGameOver()) {
				System.out.println(" stochastic_quick_game");
				System.out.println(" Check for determinisation took: " + localTimer.elapsedMillis() + "ms");
				return false;
			}
			/*
			 * Does the gamestate change even if we apply ACTION_NIL? Yes -> Stochastic
			 * Approach
			 */
			if (i % 10 == 0) {
				if (!equiv(so, soSave)) {
					System.out.println(" stochastic_not_equiv");
					System.out.println(" Check for determinisation took: " + localTimer.elapsedMillis() + "ms");
					return false;
				}
			}
			/*
			 * Does the game only have dynamic movables? Yes -> Stochastic Approach
			 */
			if (!Agent.hasOnlyDynamicMovables
					&& !hasConsistentMovable(so.getMovablePositions(), soSave.getMovablePositions())) {
				System.out.println(" stochastic_has_dynamic_movables");
				System.out.println(" Check for determinisation took: " + localTimer.elapsedMillis() + "ms");
				Agent.hasOnlyDynamicMovables = true;
				return false;
			}
			/*
			 * Flag dynamic movables in agent.
			 */
			if (i % 25 == 0) {
				findDynamicMovables(so.getMovablePositions(), soSave.getMovablePositions());
			}
			/*
			 * Flag useless immovables in agent.
			 */
			if (i % 25 == 0) {
				findUselessImmovables(so.getImmovablePositions());
			}
		}

		/*
		 * Everything we associate with stochastic games wasn't present. Therefore try a
		 * deterministic approach
		 */
		System.out.println(" deterministic");
		System.out.println(" Check for determinisation took: " + localTimer.elapsedMillis() + "ms");
		return true;
	}

	private static boolean equiv(StateObservation _this, StateObservation _that) {
		// First simple object-level checks.
		if (_this == _that)
			return true;

		// Game state checks.
		if (_this.getGameScore() != _that.getGameScore())
			return false;
		if (_this.getGameWinner() != _that.getGameWinner())
			return false;
		if (_this.isGameOver() != _that.isGameOver())
			return false;
		if (_this.getAvatarSpeed() != _that.getAvatarSpeed())
			return false;
		if (!_this.getAvatarPosition().equals(_that.getAvatarPosition()))
			return false;
		if (!_this.getAvatarOrientation().equals(_that.getAvatarOrientation()))
			return false;

		// Check NPC positions.
		ArrayList<Observation>[] _thisNPCPositions = _this.getNPCPositions();
		ArrayList<Observation>[] _thatNPCPositions = _that.getNPCPositions();

		if (!(_thisNPCPositions == null && _thatNPCPositions == null))
			return false;

		return true;
	}

	/**
	 * Determines whether a game can be solved with A* or not.
	 */
	public final static boolean isGameForAStar(StateObservation so) {

		if (so.getNPCPositions() == null) {
			if (so.getMovablePositions() != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether a game has only two walking directions.
	 */
	public final static boolean isCatchingGame(StateObservation so) {
		List<ACTIONS> actions = so.getAvailableActions();

		// only 2 walking directions??
		if (actions.size() <= 3) {
			return true;
		}
		return false;
	}

	/**
	 * Determines whether the agent needs to do double moves if the direction
	 * changes. E.g. go left -> look up -> go up.
	 */
	public final static boolean doubleMovesNeeded(StateObservation stateObs) {
		Vector2d initialPos = stateObs.getAvatarPosition();
		ACTIONS initialDirection = Util.getDirection(stateObs.getAvatarOrientation());
		StateObservation test = stateObs.copy();

		if (initialDirection == ACTIONS.ACTION_NIL) {
			return false;
		}

		// try every direction and check if position changes
		for (ACTIONS action : stateObs.getAvailableActions()) {
			if (action == initialDirection) {
				continue;
			}
			test.advance(action);
			if (!test.getAvatarPosition().equals(initialPos)) {
				return false;
			}
		}

		// try the direction where we initially looked at last!
		test.advance(initialDirection);
		if (!test.getAvatarPosition().equals(initialPos)) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether there is at least one movable that isn't dynamic.
	 */
	private final static boolean hasConsistentMovable(ArrayList<Observation>[] a, ArrayList<Observation>[] b) {
		if (a == null && b == null) {
			return true;
		} else if (a == null || b == null) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			// in case a and b itself aren't in the same order
			for (int j = 0; j < b.length; j++) {
				boolean consistent = true;
				if (a[i].size() != b[j].size()) {
					consistent = false;
				} else {
					for (int k = 0; k < a[i].size(); k++) {
						Observation aObs = a[i].get(k);
						boolean bContainsAObs = false;
						for (int l = 0; l < b[j].size(); l++) {
							Observation bObs = b[j].get(l);
							if (aObs.category == bObs.category && aObs.itype == bObs.itype
									&& aObs.position.x == bObs.position.x && aObs.position.y == bObs.position.y)
								bContainsAObs = true;
						}
						if (!bContainsAObs)
							consistent = false;
					}
					if (consistent) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Flags all movables that shouldn't be checked in BFS. A movable shouldn't be
	 * checked if its of dynamic nature, since it makes useful hashing impossible.
	 */
	private final static void findDynamicMovables(ArrayList<Observation>[] a, ArrayList<Observation>[] b) {
		if (a == null && b == null) {
			return;
		} else if (a == null || b == null) {
			return;
		}

		for (int i = 0; i < a.length; i++) {
			// in case a and b itself aren't in the same order
			if (i < b.length && a[i].size() != b[i].size()) {
				System.out.println("Ignore dynamic movable " + a[i].get(0).itype);
				Agent.ignoredSprites.add(new Integer(a[i].get(0).itype));
			} else {
				for (int k = 0; k < a[i].size(); k++) {
					Observation aObs = a[i].get(k);
					boolean bContainsAObs = false;
					for (int l = 0; l < b[i].size(); l++) {
						Observation bObs = b[i].get(l);
						if (aObs.category == bObs.category && aObs.itype == bObs.itype
								&& aObs.position.x == bObs.position.x && aObs.position.y == bObs.position.y)
							bContainsAObs = true;
					}
					if (!bContainsAObs) {
						System.out.println("Ignore dynamic movable " + a[i].get(k).itype);
						Agent.ignoredSprites.add(new Integer(a[i].get(k).itype));
					}
				}
			}
		}
	}

	/**
	 * Flags all immovables that shouldn't be checked in BFS. A immovable shouldn't
	 * be checked if there are too many of it ( and therefore probably useless ).
	 * 
	 * TODO: Improve check
	 */
	private final static void findUselessImmovables(ArrayList<Observation>[] a) {
		if (a == null)
			return;

		for (int i = 0; i < a.length; i++) {
			if (a[i].size() > 25) {
				Agent.ignoredSprites.add(new Integer(a[i].get(0).itype));
			}
		}
	}

	private static boolean areObservationsEqual(Observation _this, Observation _that) {
		if (_this.itype != _that.itype)
			return false;
		if (_this.position.x != _that.position.x)
			return false;
		if (_this.position.y != _that.position.y)
			return false;
		if (_this.category != _that.category)
			return false;
		return true;
	}
}
