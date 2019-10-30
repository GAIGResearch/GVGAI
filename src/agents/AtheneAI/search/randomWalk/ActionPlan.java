package agents.AtheneAI.search.randomWalk;

import java.util.ArrayDeque;

import ontology.Types.ACTIONS;

public class ActionPlan {

	public ArrayDeque<ACTIONS> actions;
	protected double gameScore;
	protected int heatmapScore;
	

	public ActionPlan() {
		actions = new ArrayDeque<ACTIONS>();
		gameScore = Double.NEGATIVE_INFINITY;
	}

	public ActionPlan(ArrayDeque<ACTIONS> actions, double gameScore, int heatmapScore) {
		this.actions = actions;
		this.gameScore = gameScore;
		this.heatmapScore = heatmapScore;
	}
}
