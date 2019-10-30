package agents.Return42.algorithms.deterministic.puzzleSolver;

import agents.Return42.algorithms.deterministic.puzzleSolver.MovableSinkCorrelationFinder.MovableSinkCorrelation;
import agents.Return42.algorithms.deterministic.puzzleSolver.cache.LruStateCache;
import agents.Return42.algorithms.deterministic.puzzleSolver.cache.StateCache;
import agents.Return42.algorithms.deterministic.puzzleSolver.costFunction.CombinedCostFunction;
import agents.Return42.algorithms.deterministic.puzzleSolver.costFunction.ConstantCostFunction;
import agents.Return42.algorithms.deterministic.puzzleSolver.costFunction.PenalizeCornerCostFunction;
import agents.Return42.algorithms.deterministic.puzzleSolver.heuristic.CombinedHeuristic;
import agents.Return42.algorithms.deterministic.puzzleSolver.heuristic.DistanceBetweenMovablesAndSinksHeuristic;
import agents.Return42.algorithms.deterministic.puzzleSolver.heuristic.DistanceToNextMovableHeuristic;
import agents.Return42.algorithms.deterministic.puzzleSolver.heuristic.FavorWinHeuristic;
import agents.Return42.algorithms.deterministic.puzzleSolver.heuristic.NumberOfResourcesHeuristic;
import agents.Return42.algorithms.deterministic.puzzleSolver.heuristic.ScoreHeuristic;
import agents.Return42.algorithms.deterministic.puzzleSolver.simulation.AStarAdvanceFunction;
import agents.Return42.algorithms.deterministic.puzzleSolver.simulation.MissileAvoidingStateAdvancer;
import agents.Return42.knowledgebase.KnowledgeBase;
import agents.Return42.knowledgebase.observation.ScoreObserver;
import core.game.StateObservation;

/**
 * Created by Oliver on 03.05.2015.
 */
public class AStarFactory {

    public static AStar build( KnowledgeBase knowledge, StateObservation state, int maxIterations ) {
    	MovableSinkCorrelation correlation = MovableSinkCorrelationFinder.searchForCorrelation( state );
    	
        CombinedCostFunction costFunction = new CombinedCostFunction(
                new ConstantCostFunction( pickCostPerStep( knowledge ) )
        );

        CombinedHeuristic heuristic = new CombinedHeuristic(
                new ScoreHeuristic(),
                new FavorWinHeuristic(),
                new NumberOfResourcesHeuristic()
        );

        if (correlation.didFindCorrelation()) {
        	heuristic.addHeuristic( new DistanceBetweenMovablesAndSinksHeuristic( correlation.getMovableType(), correlation.getSinkType(), true ) );
        	heuristic.addHeuristic( new DistanceToNextMovableHeuristic( correlation.getMovableType() ) );
            costFunction.addCostFunction( new PenalizeCornerCostFunction( state, correlation.getMovableType() ) );
        }

        StateCache cache = new LruStateCache();
        StateBuilder builder = new StateBuilder(state);
        AStarAdvanceFunction advancer = new MissileAvoidingStateAdvancer( state, knowledge );
        AStarStateHasher hasher = new AStarStateHasher( knowledge );
        
        return new AStar( maxIterations, state, costFunction, heuristic, cache, builder, advancer, hasher );
	}

	private static double pickCostPerStep(KnowledgeBase knowledge) {
		ScoreObserver scoreObserver = knowledge.getScoreObserver();
		
		// make an educated guess if we know nothing
		if ( scoreObserver.getNumberOfObservedTicks() < 1000 )
			return 0.1;
		else {
			double scorePerStep = scoreObserver.getMaxScorePerStep();
			return Math.max( 0.01, scorePerStep );
		}
	}
}
