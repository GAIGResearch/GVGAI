package agents.TomVodo;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

import java.util.Random;

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
    public SingleTreeNode m_root = null;

    /**
     * Random generator.
     */
    public Random m_rnd;


    public SingleMCTSPlayer(Random a_rnd)
    {
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
        if((m_root == null)||(m_root.nextRootNode == null)) {
            m_root = new SingleTreeNode(m_rnd);
        }
        else{
            m_root = m_root.nextRootNode;
            m_root.parent = null;
            m_root.m_depth_offset = m_root.m_depth;

            m_root.nVisits = m_root.nVisits * Math.pow(m_root.par_forgettingRate_search, m_root.currentSearchID - m_root.lastEvaluationSearch);
            m_root.lastEvaluationSearch = m_root.currentSearchID;
        }

        m_root.rootState = a_gameState;
    }

    /**
     * Runs MCTS to decide the action to take. It does not reset the tree.
     * @param elapsedTimer Timer when the action returned is due.
     * @return the action to execute in the game.
     */
    public int run(ElapsedCpuTimer elapsedTimer)
    {
        //Do the search within the available time.
        m_root.mctsSearch(elapsedTimer);

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();
        //int action = m_root.bestAction();
        return action;
    }

}
