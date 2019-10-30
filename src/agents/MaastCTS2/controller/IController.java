package agents.MaastCTS2.controller;

import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import core.game.StateObservation;

/**
 * Interface for controllers to play GVGAI
 *
 * @author Dennis Soemers
 */
public interface IController {
	
	/**
	 * Method called upon construction of the agent, to perform any necessary initialization
	 * 
	 * @param so
	 * @param elapsedTimer
	 */
	public void init(StateObservation so, ElapsedCpuTimer elapsedTimer);
	
	/**
	 * Method called whenever an action needs to be taken. Should return the action to play in-game.
	 * 
	 * @param currentStateObs
	 * @param elapsedTimer
	 * @return
	 */
	public ACTIONS chooseAction(StateObservation currentStateObs, ElapsedCpuTimer elapsedTimer);
	
	/**
     * Function called when the game is over. This method must finish before CompetitionParameters.TEAR_DOWN_TIME,
     *  or the agent will be DISQUALIFIED
     * @param stateObservation the game state at the end of the game
     * @param elapsedCpuTimer timer when this method is meant to finish.
     */
	public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer);

}
