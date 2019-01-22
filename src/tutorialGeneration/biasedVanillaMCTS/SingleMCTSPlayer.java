import java.util.Random;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;


/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:13
 */
public class SingleMCTSPlayer
{

	
    /**
     * Root of the tree.
     */
    public SingleTreeNode m_root;

    /**
     * Random generator.
     */
    public Random m_rnd;

    public int num_actions;
    public Types.ACTIONS[] actions;

    public boolean improved;
    public static SingleTreeNode currentNode;
    

    public boolean done = false;
    public SingleMCTSPlayer(Random a_rnd, int num_actions, Types.ACTIONS[] actions, boolean improved)
    {
        this.num_actions = num_actions;
        this.actions = actions;
        this.improved = improved;
        m_rnd = a_rnd;
    }

    /**
     * Inits the tree with the new observation state in the root.
     * @param a_gameState current state of the game.
     */
    public void init(StateObservation a_gameState)
    {
        //Set the game observation to a newly root node.
        //System.out.println("learning_style = " + learning_style);
        m_root = new SingleTreeNode(m_rnd, num_actions, actions);
        m_root.rootState = a_gameState;
        SingleMCTSPlayer.currentNode = m_root;
    }

    /**
     * Runs MCTS to decide the action to take. It does not reset the tree.
     * @param elapsedTimer Timer when the action returned is due.
     * @return the action to execute in the game.
     */
    public void run()
    {	        
    	m_root.mctsSearch(improved);
    	
    }

}
