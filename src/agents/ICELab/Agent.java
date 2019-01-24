package agents.ICELab;


import java.util.ArrayList;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.ICELab.BFS.BFS;
import agents.ICELab.OpenLoopRLBiasMCTS.AnyTime;
import agents.ICELab.OpenLoopRLBiasMCTS.Memory;
import agents.ICELab.OpenLoopRLBiasMCTS.Node;
import agents.ICELab.OpenLoopRLBiasMCTS.NodePool;
import agents.ICELab.OpenLoopRLBiasMCTS.Utils;
import agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch.DBS;
import agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch.RLBiasMCTS;
import agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch.TreeSearch;

//2016/5/18é�—ï¿½
//gvgai_2016Frameworkç€µæƒ§ç¹™
public class Agent extends AbstractPlayer{

	private JudgeGameType GameType;
	public static boolean USE_BFS = false;

	//use BFS
    public BFS best;
    public static int Max_GameTick;
	//
	//use OLRLBMCTS
    public static Random  random  = new Random();
    public static AnyTime anyTime = new AnyTime();
    public static Memory memory;
    public Node gameStart;
    public Node origin;
    public TreeSearch search;
    private boolean isInitializing = true;
    //
	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		
		GameInfo.init(so);
//      GameType = new JudgeGameType(so);
      if(GameInfo.isDetermin){
        	USE_BFS=true;
            ArrayList<Types.ACTIONS> act = so.getAvailableActions();
            StateObservation temp = so.copy();
       	 	for(int j = 0; ;j++){
       	 		temp.advance(ACTIONS.ACTION_NIL);
       	 		if(temp.isGameOver()){
       	 			Max_GameTick = j;
       	 			break;
       	 		}
       	 	}
      	 	best = new BFS(so);
      	 	MCTSinit(so);
         	//search = new RLBiasMCTS(origin);
      	 	best.search(so,elapsedTimer);
       	 	Runtime.getRuntime().gc();
        }else{
        	System.out.println("USE_OLRLBMCTS");
//      	 	best = new BFS(so);
       	 	//OLRLBMCTSåˆ�æœŸåŒ–
       	 	System.out.print("begin init...");
       	 	MCTSinit(so);
    		anyTime.beginInit(elapsedTimer);
    		memory = new Memory(GameInfo.avatarType);
    		// choose our search algorithm
    		//search = new MCTS(origin);
    		search = new DBS(origin);
//    		search = new RLBiasMCTS(origin);
    		// fill search tree
    		search.search();

    		isInitializing = false;
         	search = new RLBiasMCTS(origin);
         	System.out.println(" ...exit init");
        }
        //*/
    }
	public boolean isInitializing(){
		return isInitializing;
	}
	//MCTSåˆ�æœŸè¨­å®š
	public void MCTSinit(StateObservation so){
		Utils.initLogger();
		gameStart = NodePool.get();
		gameStart.state = so;
		gameStart.avatarPos = so.getAvatarPosition();
		gameStart.nVisits = 1;
		gameStart.depth = 0;
		origin = gameStart;

	}

    public Types.ACTIONS act(StateObservation so, ElapsedCpuTimer timer)
    {
    	if(USE_BFS){
    		if (so.getGameTick() % 20 == 0) 
    		{
				Runtime.getRuntime().gc();
			}
    		return best.Run(so,timer);
    	}
    	else
    	{
    		anyTime.begin(timer);
            if (so.getGameTick() % 10 == 0)
             	Agent.memory.report();
            origin.state = so;
            origin.depth = 0;
            if (origin.prev != null) {
            	origin.prev.depth = -1;
            }
            // fill search tree
            search.search();

            // select action
            Node selected = origin.select();
            Types.ACTIONS action = selected.action;

            // release garbage & roll search tree
            selected.isDestroyable = false;
            origin.release();
            origin = selected;

            // roll search algorithm
            search.roll(origin);

            Utils.logger.info(action + "\n");

            return action;
            //*/
            //return ACTIONS.ACTION_NIL;
    	}
    }
    
}
