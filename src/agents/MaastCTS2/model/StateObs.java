package agents.MaastCTS2.model;

import core.game.StateObservation;

/**
 * Wrapper class around StateObservation objects.
 * Will automatically create a copy of a StateObservation before exposing it if necessary
 * 
 * @author Dennis Soemers
 *
 */
public class StateObs {
	
	private final StateObservation stateObs;
	private final boolean shouldCopy;

	public StateObs(StateObservation stateObs, boolean shouldCopy){
		this.stateObs = stateObs;
		this.shouldCopy = shouldCopy;
	}
	
	/**
	 * Returns the wrapped StateObservation object. Returns a copy if this is considered to be necessary
	 * 
	 * @return
	 */
	public StateObservation getStateObs(){
		if(shouldCopy){
			return stateObs.copy();
		}
		else{
			return stateObs;
		}
	}
	
	/**
	 * Returns a non-copied version of the wrapped StateObservation object. This method
	 * should only be used if it is not planned to call advance() on the returned object
	 * 
	 * @return
	 */
	public StateObservation getStateObsNoCopy(){
		return stateObs;
	}
	
	public boolean shouldCopy(){
		return shouldCopy;
	}
}
