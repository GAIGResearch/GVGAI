package agents.MaastCTS2.heuristics.states;

import agents.MaastCTS2.test.IPrintableConfig;
import core.game.StateObservation;

public interface IPlayoutEvaluation extends IPrintableConfig {
	public double scorePlayout(StateObservation stateObs);
}
