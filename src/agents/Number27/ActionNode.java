/**
 * Node used by the BFS
 * 
 * @author Florian Kirchgessner (Alias: Number27)
 * @version customGA3
 */

package agents.Number27;

import java.util.LinkedList;

import core.game.StateObservation;

public class ActionNode {
	private StateObservation currentState;
	private LinkedList<Integer> actionHistory;
	private LinkedList<Integer> unexploredActions;
	
	
	public ActionNode(StateObservation stateObs, LinkedList<Integer> actionHistory, int numActions) {
		this.actionHistory = actionHistory;
		currentState = stateObs;
		unexploredActions = new LinkedList<Integer>();
		
		for(int i = 0; i < numActions; i++) {
			unexploredActions.add(i);
		}
	}
	
	
	public boolean check() {
		if(unexploredActions.size() == 0) {
			return true;
		}
		return false;
	}
	
	
	public int getUnexploredAction() {
		return unexploredActions.remove();
	}
	
	
	public StateObservation getCurrentState() {
		return currentState;
	}


	public LinkedList<Integer> getActionHistory() {
		return actionHistory;
	}
	
	
	public void clear() {
		actionHistory.clear();
		unexploredActions.clear();
		currentState = null;
		actionHistory = null;
		unexploredActions = null;
	}
}
