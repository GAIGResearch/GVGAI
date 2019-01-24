package tutorialGeneration.biasedOnetreeMCTS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import video.basics.GameEvent;
import ontology.Types;


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
    
    boolean oneTree= false;

    /**
     * Random generator.
     */
    public Random m_rnd;

    public int num_actions;
    public Types.ACTIONS[] actions;

    public boolean improved;
    public static SingleTreeNode currentNode;
    

    public boolean done = false;
    
    File expFile;
    File mainExperimentsFile;
    
    public SingleMCTSPlayer(Random a_rnd, int num_actions, Types.ACTIONS[] actions, boolean improved, File expFile, File mainExperimentsFile)
    {
        this.num_actions = num_actions;
        this.actions = actions;
        this.improved = improved;
        m_rnd = a_rnd;
        
        this.expFile = expFile;
        this.mainExperimentsFile = mainExperimentsFile;
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
    public int run(int number)
    {	        
    	if	(oneTree) {
    		m_root.mctsSearch(improved);
    	} else {
    		StateObservation a_gameState = m_root.rootState;
    		ArrayList<ACTIONS> moves = new ArrayList<ACTIONS>();
    		int count = 1;
    		while (!a_gameState.isGameOver() && a_gameState.getGameTick() < 1000) {
        		SingleTreeNode.deepest = 0;
        		SingleTreeNode.deepestNode = null;
	    	    init(a_gameState);
	    		m_root.numIterations = 5000;
	    	    m_root.mctsSearch(improved);
	    	    int action = m_root.mostVisitedAction();
	    	    Types.ACTIONS act = actions[action];
	    	    a_gameState.advance(act);
	            try { 
	            	  
	                // Open given file in append mode. 
	                BufferedWriter out = new BufferedWriter( 
	                       new FileWriter(expFile, true)); 
	                out.write(count + "," + act);
	                
	    			ArrayList<GameEvent> events = a_gameState.getFirstTimeEventsHistory();
	    			String ev = "";
	    			for (GameEvent event : events) {
	    				if(Integer.parseInt(event.gameTick) == count-1) {
	    					ev += "," + event.toString();
	    				}
	    			}
	    			ev += "\n";
	    			out.write(ev);
	                out.close(); 
	            } 
	            catch (IOException e) { 
	                System.out.println("exception occured" + e); 
	            }
	    	    
	    	    count++;
    		}
    		
    		if (a_gameState.getGameWinner() == Types.WINNER.PLAYER_WINS) {
    			System.out.println("Won game!");
    		} else {
    			System.out.println("Lost game...");
    		}
    		
            try { 
                // Open given file in append mode. 
                BufferedWriter out = new BufferedWriter( 
                       new FileWriter(expFile, true)); 

        		if (a_gameState.getGameWinner() == Types.WINNER.PLAYER_WINS) {
        			out.write("Won game!\n");
        		} else {
        			out.write("Lost game\n");
        		}
                out.close(); 
            } 
            catch (IOException e) { 
                System.out.println("exception occured" + e); 
            }
            
            try { 
                // Open given file in append mode. 
                BufferedWriter out = new BufferedWriter( 
                       new FileWriter(mainExperimentsFile, true)); 
        		if (a_gameState.getGameWinner() == Types.WINNER.PLAYER_WINS) {
        			out.write(number+",1," + a_gameState.getGameScore() +  "\n");
                    out.close(); 
        			return 1;
        		} else {
        			out.write(number+",0," + a_gameState.getGameScore() +  "\n");
                    out.close(); 
                    return 0;
        		}
            } 
            catch (IOException e) { 
                e.printStackTrace(); 
            }
    	}
		return 0;
    	
    }

}
