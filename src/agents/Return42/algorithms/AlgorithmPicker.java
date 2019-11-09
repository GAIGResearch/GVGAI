package agents.Return42.algorithms;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import tools.ElapsedCpuTimer;
import agents.Return42.algorithms.deterministic.CombinedDeterministicAgent;
import agents.Return42.algorithms.deterministic.NonDeterministicRandomAgent;
import agents.Return42.knowledgebase.KnowledgeBase;

/**
 * Responsible for selecting an algorithm (= agent) which is best for solving the current level.
 */
public class AlgorithmPicker {

    public AbstractPlayer pickAlgorithm( KnowledgeBase knowledge, StateObservation state, ElapsedCpuTimer timer ) {
    	GameClassification classification = knowledge.getGameClassification();
    	
    	if (classification.isDeterministic() && !classification.hasSelfMovingObjects()) {
    		return new CombinedDeterministicAgent( knowledge, state, timer );
    	} else {
    		return new NonDeterministicRandomAgent(knowledge, state, timer);
    	}
    	/*
        if (classification.isDeterministic() && classification.hasSelfMovingObjects() ) {
            System.out.println("self moving movables --> use ga");
            return new agents.Return42.algorithms.pattern.Agent(state, timer);
        } else if ( classification.isDeterministic() ) {
            System.out.println( "deterministic --> use solver + random" );
            return new CombinedDeterministicAgent( knowledge, state, timer );
        } else {
            System.out.println("non deterministic --> use ga");
            return new agents.Return42.algorithms.pattern.Agent( state, timer );
        }*/
    }
}
