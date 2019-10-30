package agents.AtheneAI.search.dfs;

import core.game.StateObservation;
import ontology.Types;

public class DFSNode {
	protected final StateObservation state;
	protected final DFSNode parent;
	protected final int depth;
	protected final Types.ACTIONS lastAction;

	public DFSNode(StateObservation state, DFSNode parent, int depth, Types.ACTIONS lastAction) {
		this.state = state;
		this.parent = parent;
		this.depth = depth;
		this.lastAction = lastAction;
	}
}
