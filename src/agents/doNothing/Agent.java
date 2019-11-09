package agents.doNothing;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;

public class Agent extends AbstractPlayer {
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
	
    }
    
    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
	return ACTIONS.ACTION_NIL;
    }
}
