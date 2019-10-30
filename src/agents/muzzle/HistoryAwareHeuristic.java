package agents.muzzle;

import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import tracks.singlePlayer.tools.Heuristics.StateHeuristic;

/**
 * Created by kryo on 13.05.16.
 */
public class HistoryAwareHeuristic extends StateHeuristic {

	private static final double HUGE_NEGATIVE = -100.0;
	private static final double HUGE_POSITIVE = 100.0;

	double initialNpcCounter = 0;

	StateObservation lastObservation = null;
	double lastScore;
	Vector2d pos;

	private static final double EXPLORATION_FACTOR = 4;

	public HistoryAwareHeuristic(StateObservation stateObs) {
		lastObservation = stateObs;
		pos = stateObs.getAvatarPosition();
		lastScore = stateObs.getGameScore();
	}

	public double evaluateState(StateObservation stateObs) {
		boolean gameOver = stateObs.isGameOver();
		Types.WINNER win = stateObs.getGameWinner();
		double rawScore = 15 * (stateObs.getGameScore() - lastScore);

		Vector2d avatarPosition = stateObs.getAvatarPosition();

		if (gameOver && win == Types.WINNER.PLAYER_LOSES)
			return HUGE_NEGATIVE;

		if (gameOver && win == Types.WINNER.PLAYER_WINS)
			return HUGE_POSITIVE;

		/*
		 * if (stateObs.getAvatarLastAction() == Types.ACTIONS.ACTION_LEFT &&
		 * stateObs.getAvatarLastAction() == Types.ACTIONS.ACTION_RIGHT &&
		 * stateObs.getAvatarLastAction() == Types.ACTIONS.ACTION_DOWN &&
		 * stateObs.getAvatarLastAction() == Types.ACTIONS.ACTION_UP &&
		 * stateObs.getAvatarLastAction() == lastObservation.getAvatarLastAction()) {
		 * rawScore += 0.5; }
		 * 
		 * if(lastObservation.getAvatarPosition().equals(stateObs.getAvatarPosition())
		 * ||
		 * lastObservation.getAvatarOrientation().equals(stateObs.getAvatarOrientation()
		 * ) ) { rawScore -= 0.5; }
		 */
		if (EXPLORATION_FACTOR > 1) {
			pos = (pos.mul(EXPLORATION_FACTOR - 1).add(stateObs.getAvatarPosition())).mul(1.0 / EXPLORATION_FACTOR);
			double delta = pos.dist(stateObs.getAvatarPosition());

			rawScore += Math.max(10, delta);
		}

		if (stateObs.getAvatarHealthPoints() < lastObservation.getAvatarHealthPoints()) {
			rawScore -= 3;
		}
		ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions();
		ArrayList<Observation>[] resourcePositions = stateObs.getResourcesPositions();

		if (resourcePositions != null && resourcePositions.length > 0) {
			for (ArrayList<Observation> os : resourcePositions) {
				for (Observation o : os) {
					rawScore -= (o.position.dist(stateObs.getAvatarPosition()) / 10.0);
					// System.out.println("resource");

				}
			}
		} else if (portalPositions != null && portalPositions.length > 0) {
			for (ArrayList<Observation> os : portalPositions) {
				for (Observation o : os) {
					rawScore -= (o.position.dist(stateObs.getAvatarPosition()) / 10.0);
					// System.out.println("portal");
				}
			}
		}
		lastScore = stateObs.getGameScore();
		return rawScore;

	}
}
