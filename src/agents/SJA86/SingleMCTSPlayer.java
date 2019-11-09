package agents.SJA86;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:13
 */
public class SingleMCTSPlayer
{
	public boolean badRun;
	public int badRunCount = 0;
	public int alternateRunCount = 0;
	public static int switchThreshold = 100;
	public static int alternateRun = 50;
	public boolean alternate_on = false;
    public Vector2d my_pos = null;
	public StateObservation stateObs = null;
	
	/**
     * Root of the tree.
     */
    public SingleTreeNode m_root;

    /**
     * Random generator.
     */
    public Random m_rnd;

    /**
     * Creates the MCTS player with a sampleRandom generator object.
     * @param a_rnd sampleRandom generator object.
     */
    public SingleMCTSPlayer(Random a_rnd)
    {
        m_rnd = a_rnd;
        m_root = new SingleTreeNode(a_rnd);
    }

    /**
     * Inits the tree with the new observation state in the root.
     * @param a_gameState current state of the game.
     */
    public void init(StateObservation a_gameState)
    {
        //Set the game observation to a newly root node.
        m_root = new SingleTreeNode(m_rnd);
		m_root.state = a_gameState;
		stateObs = a_gameState;
    }

    /**
     * Runs MCTS to decide the action to take. It does not reset the tree.
     * @param elapsedTimer Timer when the action returned is due.
     * @return the action to execute in the game.
     */
    public int run(ElapsedCpuTimer elapsedTimer)
    {
		if (badRunCount > switchThreshold) {
			alternate_on = true;
			my_pos = stateObs.getAvatarPosition();
			badRunCount = 0;
		}
		
		
		if (alternateRunCount > alternateRun) {
			alternate_on = false;
			alternateRunCount = 0;
		}
		//Do the search within the available time.
        m_root.mctsSearch(elapsedTimer, alternate_on, my_pos);
		 

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();
        
		//Was it a bad run (was the tree too balanced)?
		if (!alternate_on) {
			badRun = m_root.run_status();
			if (badRun){badRunCount++;}
			else{badRunCount = 0;}
		}
		if (alternate_on) {alternateRunCount++;}
		//int action = m_root.bestAction();
        return action;
    }

}
