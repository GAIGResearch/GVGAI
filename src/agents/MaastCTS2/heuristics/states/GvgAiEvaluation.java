package agents.MaastCTS2.heuristics.states;

import agents.MaastCTS2.Globals;
import core.competition.CompetitionParameters;
import core.game.StateObservation;
import ontology.Types.WINNER;

/**
 * Same evaluation function as used by the sample MCTS controller of GVGAI
 * 
 * @author Dennis Soemers
 */
public class GvgAiEvaluation implements IPlayoutEvaluation {
	
	public static double evaluate(StateObservation stateObs){
		double score = stateObs.getGameScore();
		
		if (stateObs.isGameOver()) {		// game over
			if (stateObs.getGameWinner() == WINNER.PLAYER_WINS) {
				// game won
				score += Globals.HUGE_ENDGAME_SCORE;
			} 
			else if (stateObs.getGameWinner() == WINNER.PLAYER_LOSES) {
				// game lost
				if(stateObs.getGameTick() == CompetitionParameters.MAX_TIMESTEPS){
					// loss based on time is preferable over an early loss
					score -= Globals.HUGE_ENDGAME_SCORE * 0.8;
				}
				else{
					score -= Globals.HUGE_ENDGAME_SCORE;
				}
			}
		}
		
		return score;
	}

	@Override
	public double scorePlayout(StateObservation stateObs) {
		return GvgAiEvaluation.evaluate(stateObs);
	}

	@Override
	public String getConfigDataString() {
		return "";
	}

	@Override
	public String getName() {
		return "GvgAiEvaluation";
	}

}
