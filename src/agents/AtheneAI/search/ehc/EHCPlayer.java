package agents.AtheneAI.search.ehc;

import java.util.ArrayList;
import java.util.List;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Chooses the next move by using Enforced Hill Climbing. Apply for games where
 * the agent only has two walking directions and there are movables falling down
 * to catch. The next move is chosen by minimizing the distance of the agent to
 * the movable that is nearest to the ground.
 * 
 * Agent is not applied, if there is evidence that movable could do any harm.
 * 
 * Ground can be on any side of the play field. It is determined, if ground is
 * horizontal or vertical.
 * 
 * TODO: it should be checked if movables are really falling down and not just
 * lying somewhere the agent can not reach them. For example we could recognize
 * if the distance to an object never decreases and then remember this object to
 * be unreachable. If there only unreachable objects MCTS should be applied.
 * 
 * TODO: In contrast to aliens in eggomania the agent with olmcts is not able to
 * kill the npc. Find out the reason for that and maybe we can adapt Simons
 * player. (However for eggomania we win anyway. And it would be rather bad if
 * we directly kill the npc because then we would catch less coins. But for
 * other games it might be a requirement for winning to kill the npc)
 * 
 * TODO: we should update Knowledge also here if we get to know that sth. is
 * good/bad!
 * 
 * Problem - disqualification in aliens, or even pacman (during the first step)
 */
public class EHCPlayer {

	private static int HORIZONTAL = 0;
	private static int VERTICAL = 1;
	private List<ACTIONS> actions;
	private int ground;
	private boolean debug = false;

	public EHCPlayer(List<ACTIONS> act) {
		actions = act;
		ground = learnGroundOrientation();
	}

	/**
	 * Returns the action considered to be best according to EHC. This means the
	 * best move is chosen by minimizing the distance of the agent to the movable
	 * that is nearest to the ground.
	 */
	public ACTIONS getBestAction(StateObservation state, ElapsedCpuTimer timer, ArrayList<Integer> goodMovablesIds) {
		int nearestDistanceToGround = Integer.MAX_VALUE;
		Observation obsNearestToGround = null;
		ACTIONS bestAction = ACTIONS.ACTION_NIL;

		// Get movables of current state
		ArrayList<Observation>[] movables = state.getMovablePositions();
		if (movables != null) {

			// find the movable which is nearest to the bottom and good
			for (ArrayList<Observation> movableType : movables) {
				if (movableType != null && !movableType.isEmpty()) {
					for (Observation movable : movableType) {
						// check if this movable is good (worth chasing after)
						if (goodMovablesIds != null && goodMovablesIds.contains(movable.itype)) {
							int tempDistance = distToGround(state.getAvatarPosition(), movable.position);

							if (tempDistance < nearestDistanceToGround) {
								nearestDistanceToGround = tempDistance;
								obsNearestToGround = movable;
							}
						}
					}
				}
			}

			if (obsNearestToGround != null) {

				if (debug)
					System.out.println("obsId = " + obsNearestToGround.obsID + " is " + nearestDistanceToGround
							+ "px away from ground");

				// find action minimizing the distance between agent and movable
				int nearestDistanceToAgent = Integer.MAX_VALUE;
				for (ACTIONS a : state.getAvailableActions(true)) {

					if (a != ACTIONS.ACTION_USE && a != ACTIONS.ACTION_ESCAPE) {
						StateObservation stateCopy = state.copy();
						stateCopy.advance(a);

						// NOTE: if the agent dies, its position is set to (-1, -1)!!
						int tempDistance = Integer.MAX_VALUE;
						if (!(stateCopy.isGameOver() && stateCopy.getGameWinner() == Types.WINNER.PLAYER_LOSES)) {
							// NOTE: by advancing the state the position in the movable object is NOT
							// updated, but it should work fine anyway
							tempDistance = dist(stateCopy.getAvatarPosition(), obsNearestToGround.position);
						}

						if (tempDistance < nearestDistanceToAgent) {
							nearestDistanceToAgent = tempDistance;
							bestAction = a;
						}
					}
				}

				if (debug)
					System.out.println("action = " + bestAction + " is best action, distance to agent is now "
							+ nearestDistanceToAgent + "px");
			}
		}

		return bestAction;
	}

	/**
	 * Returns Manhattan-Distance in pixel between two points.
	 */
	private int dist(Vector2d avatarPosition, Vector2d observationPosition) {
		return (int) (Math.abs(avatarPosition.x - observationPosition.x)
				+ Math.abs(avatarPosition.y - observationPosition.y));
	}

	/**
	 * Returns distance in pixel between two points along one axis. Axis is
	 * orthogonal to the ground.
	 */
	private int distToGround(Vector2d avatarPosition, Vector2d observationPosition) {
		if (ground == EHCPlayer.HORIZONTAL) {
			if (debug)
				System.out.println("avatar y = " + avatarPosition.y + ", mov y " + observationPosition.y);
			return (int) Math.abs((avatarPosition.y - observationPosition.y));

		} else
			return (int) Math.abs((avatarPosition.x - observationPosition.x));
	}

	/**
	 * Returns ground orientation. Assumes that the game is two dimensional
	 */
	private int learnGroundOrientation() {
		if (actions.contains(ACTIONS.ACTION_LEFT))
			return EHCPlayer.HORIZONTAL;
		else
			return EHCPlayer.VERTICAL;
	}

}
