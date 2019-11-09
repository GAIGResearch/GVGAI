package agents.bladerunner.agents;

import core.player.AbstractPlayer;

/**
 * General Agent properties and methods that we want in every agent.
 * 
 * @author Benjamin Ellenberger
 *
 */
public abstract class GameAgent extends AbstractPlayer {

	/**
	 * Perform actions to clean up some memory in case we are running out of it.
	 * You have to implement this in your Agent.
	 */
	public abstract void clearMemory();

}
