package NovTea;

import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import core.game.StateObservation;
import core.player.AbstractPlayer;

public class Agent extends AbstractPlayer{

	private StateGraph stateGraph;

	
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
    	stateGraph = new StateGraph();
    }
	
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		stateGraph.setNewRoot(stateObs);
        ACTIONS action = stateGraph.simulate(elapsedTimer);
        if(action == null) {
        	return ACTIONS.ACTION_NIL;
        }
        return action;
	}

}
