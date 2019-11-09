package agents.AtheneAI.search.bfs;

import java.util.ArrayList;

import ontology.Types.ACTIONS;
import core.game.StateObservation;

public class BFSNode {
	protected final StateObservation state;
	protected final int depth;
	protected String trace;
	protected final ArrayList<ACTIONS> availableActions;

	public BFSNode(StateObservation state, int depth,
			String trace, ArrayList<ACTIONS> availableActions) {
		this.state = state;
		this.depth = depth;
		this.trace = trace;
		this.availableActions = availableActions;
	}
}
