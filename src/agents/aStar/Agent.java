package agents.aStar;

import java.util.ArrayList;
import java.util.PriorityQueue;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;
import tools.ElapsedCpuTimer;
import agents.Heuristics.SimpleStateHeuristic;

public class Agent extends AbstractPlayer {

	private ArrayList<ACTIONS> actions;

	public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		actions = stateObs.getAvailableActions();
	}

	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		double worstTime = 10;
		double avgTime = 10;
		double totalTime = 0;
		double numberOfTime = 0;

		PriorityQueue<Node> queue = new PriorityQueue<Node>();
		queue.add(new Node(stateObs, null, ACTIONS.ACTION_NIL));
		Node currentNode = null;

		while (!queue.isEmpty() && elapsedTimer.remainingTimeMillis() > avgTime
				&& elapsedTimer.remainingTimeMillis() > worstTime) {
			ElapsedCpuTimer time = new ElapsedCpuTimer();

			currentNode = queue.remove();
			if (currentNode.stateObs.getGameWinner() == WINNER.PLAYER_WINS) {
				break;
			}
			if (currentNode.stateObs.getGameWinner() == WINNER.PLAYER_LOSES) {
				continue;
			}

			for (ACTIONS a : actions) {
				StateObservation newState = currentNode.stateObs.copy();
				newState.advance(a);
				queue.add(new Node(newState, currentNode, a));
			}

			totalTime += time.elapsedMillis();
			numberOfTime += 1;
			avgTime = totalTime / numberOfTime;
		}

		return currentNode.getAction();
	}

	private class Node implements Comparable<Node> {
		public StateObservation stateObs;
		public Node parent;
		public ACTIONS action;
		public double depth;
		public double cost;
		public double heuristic;

		public Node(StateObservation stateObs, Node parent, ACTIONS act) {
			this.stateObs = stateObs;
			this.parent = parent;
			this.action = act;
			this.depth = 1;
			if (this.parent != null) {
				this.depth += this.parent.depth;
			}
			this.cost = 100 / depth;
			SimpleStateHeuristic heuristic = new SimpleStateHeuristic(stateObs);
			this.heuristic = heuristic.evaluateState(stateObs);
		}

		public ACTIONS getAction() {
			if (this.parent == null) {
				return action;
			}
			if (this.parent.parent == null) {
				return this.action;
			}

			return parent.getAction();
		}

		@Override
		public int compareTo(Node n) {
			if (this.cost + this.heuristic > n.cost + n.heuristic) {
				return -1;
			}

			if (this.cost + this.heuristic <= n.cost + n.heuristic) {
				return 1;
			}

			return 0;
		}
	}
}
