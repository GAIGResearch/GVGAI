package agents.hillClimber;

import java.util.ArrayList;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.Heuristics.WinScoreHeuristic;

public class Agent extends AbstractPlayer {
	private int maxLength = 10;
	private ArrayList<ACTIONS> actions;
	private Random random;

	public Agent(StateObservation stateObs, ElapsedCpuTimer elpasedTimer) {
		actions = stateObs.getAvailableActions();
		random = new Random();
	}

	private ArrayList<ACTIONS> getRandomNeighbour(ArrayList<ACTIONS> listActions) {
		ArrayList<ACTIONS> newList = (ArrayList<ACTIONS>) listActions.clone();
		int randomIndex = random.nextInt(maxLength);
		newList.set(randomIndex, actions.get(random.nextInt(actions.size())));

		return newList;
	}

	private double calculateFitness(StateObservation stateObs, ArrayList<ACTIONS> listActions) {
		WinScoreHeuristic h = new WinScoreHeuristic(stateObs);
		StateObservation newState = stateObs.copy();

		for (ACTIONS a : listActions) {
			newState.advance(a);
		}

		return h.evaluateState(newState);
	}

	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		ArrayList<ACTIONS> currentActions = new ArrayList<ACTIONS>();
		for (int i = 0; i < maxLength; i++) {
			currentActions.add(actions.get(random.nextInt(actions.size())));
		}

		double worstTime = 10;
		double avgTime = 10;
		double totalTime = 0;
		double numberOfTime = 0;

		while (elapsedTimer.remainingTimeMillis() > avgTime && elapsedTimer.remainingTimeMillis() > worstTime) {
			ArrayList<ACTIONS> newList = getRandomNeighbour(currentActions);
			if (calculateFitness(stateObs, currentActions) < calculateFitness(stateObs, newList)) {
				currentActions = newList;
			}
		}

		return currentActions.get(0);
	}

}
