package agents.Return42.algorithms;

import agents.Return42.knowledgebase.KnowledgeBase;
import agents.Return42.knowledgebase.KnowledgeBaseUpdatingStateObservation;
import core.game.StateObservation;
import core.player.AbstractPlayer;

public abstract class KnowledgebasedAgent extends AbstractPlayer {

	protected final KnowledgeBase knowledge;
	
	public KnowledgebasedAgent( KnowledgeBase knowledge ) {
		this.knowledge = knowledge;
	}
	
	public StateObservation learnFromActions( StateObservation state ) {
		return new KnowledgeBaseUpdatingStateObservation(state, knowledge.getObserver());
	}
}
