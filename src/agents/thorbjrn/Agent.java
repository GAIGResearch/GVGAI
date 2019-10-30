package agents.thorbjrn;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import ontology.Types;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;
import tools.ElapsedCpuTimer;
import core.game.StateObservation;
import core.player.AbstractPlayer;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {


    //Constants
	static boolean MINOR_VERBOSE = true;
    static boolean VERBOSE = false;
    static boolean LOOP_VERBOSE = false;
    
    int lastIters = -1;
    int lastPathDepth = 0;
    
    HashSet<MoveableSet> visitedNodes = new HashSet<MoveableSet>();
    ArrayDeque<LinkedList<Integer>> q = new ArrayDeque<LinkedList<Integer>>();
    LinkedList<Integer> solution = null; int solIncr = 0;
    
    
    public Agent(StateObservation so, ElapsedCpuTimer ect){     
        Tools.initialize(so);
        if (VERBOSE || MINOR_VERBOSE) System.out.println("isPuzzleGame: " + Tools.isPuzzleGame + ", hasNPCs: " + Tools.hasNPCs);
    }


	public ACTIONS act(StateObservation so, ElapsedCpuTimer ect) {
        
    	 if (Tools.isPuzzleGame && Tools.actions.length < 5){
    		 return breadthFirstLoop(so, ect);
    	 }

        
        int posKey = Tools.getPositionKey(so.getAvatarPosition());
        Tools.boringPlaces[posKey] += 1;
        
        int currentBoringness = Tools.boringPlaces[Tools.getPositionKey(so.getAvatarPosition())];

        if (lastIters > -1){
        	if (Tools.actions.length == 3){
        		Tools.maxSearchDepth = 25;
        	}else{
            	Tools.maxSearchDepth = (int)Math.sqrt(lastIters)/Tools.actions.length + 1;
            	if (Tools.maxSearchDepth < 3)  Tools.maxSearchDepth = 3;
        	}
        	if (Tools.isPuzzleGame) Tools.maxSearchDepth = 6;
        }
        
        Node[] actNodes = new Node[Tools.actions.length];
        
        
        int seed = Math.abs(Tools.r.nextInt()) % Tools.faculty[Tools.actions.length];
        for (int i = 0; i < Tools.actions.length; i++) {
			int act = Tools.randomActMap[seed][i];
			StateObservation soCopy = so.copy();
			Node firstMoveNode = new Node(soCopy, act);
			actNodes[act] = firstMoveNode;
		}
        

    	defaultLoop(actNodes, so, ect);
        	
        	
        //find least spooky action
        boolean isSpooked = false;
        double leastSpookiness = Double.POSITIVE_INFINITY; int leastSpookyAct = -1;
        for (int i = 0; i < Tools.actions.length; i++) {    		 
//    		 if (Tools.actions[i] == ACTIONS.ACTION_USE) continue; //
    		 Node n = actNodes[i];
    		 
    		 double spookinessOfAct = Tools.r.nextDouble()*0.0000001;
    		 
    		 for (Entry<LinkedList<Integer>, Integer> entry : n.pathsTried.entrySet()) {
    			 LinkedList<Integer> path = entry.getKey();
    			 int val = entry.getValue();
    			 
    			 if (val == -1){
    				 if (path.size() == 1){
    					 spookinessOfAct += n.directDeaths/Math.pow(Tools.actions.length, path.size()-1);
    				 }else{
    					 spookinessOfAct += 1/Math.pow(Tools.actions.length, path.size()-1);
    				 }
    			 }
			}
    		 
    		 
    		 spookinessOfAct /= (double)n.advancements;
    		 
    		 if (LOOP_VERBOSE) System.out.println(Tools.actions[i] + " spookiness: " + spookinessOfAct);
    		 
    		 if (spookinessOfAct < leastSpookiness){
    			 leastSpookiness = spookinessOfAct;
    			 leastSpookyAct = i;
    		 }
    		 
    		 //should least spooky action be used?
    		 
    		 
    		 //TODO make proper
    		 if (n.randomDeathEventsPaths.size() > 0) isSpooked = true;
    	 }
        
    	 //find best score node
    	 double bestVal = Double.NEGATIVE_INFINITY; int bestValAct = -1; 
    	 if (!isSpooked){
    		 for (int i = 0; i < Tools.actions.length; i++) {
    			 if (actNodes[i].directDeaths > 0) continue;
    			 double val = Tools.r.nextDouble()*0.0000000001;
    			 val += actNodes[i].value;
    			 val /= (double)actNodes[i].advancements;    			 
    			 if (val > bestVal){
    				 bestVal = val;
    				 bestValAct = i;
    			 }
    		 }
    	 }
    	 
    	 if (Tools.hasNPCs && currentBoringness < 10){
//    		 if (bestValAct > -1 && actNodes[bestValAct].earliestDeath <= 3) isSpooked = true;
    		 for (int i = 0; i < Tools.actions.length; i++) {
				if (actNodes[i].earliestDeath <= 3){
					isSpooked = true;
					break;
				}
				
			}
    	 }
    	 
        
        if (VERBOSE){
        	System.out.println("-------Ended act------- returning: " + (isSpooked ? (leastSpookyAct==-1?"ERR":Tools.actions[leastSpookyAct]) : (bestValAct==-1?"ERR":Tools.actions[bestValAct])) + ", isSpooked: " + isSpooked + ", iters: " + lastIters);
            for (int i = 0; i < Tools.actions.length; i++) {
    			System.out.println(actNodes[i]);
    			System.out.println("randomEventsPaths: " + actNodes[i].randomDeathEventsPaths + ", pathsTried: " + actNodes[i].pathsTried);
    		}
            System.out.println("Current boringness:  " + currentBoringness);
            System.out.println();
        }

        if (!isSpooked){
        	if (bestValAct == -1) return ACTIONS.ACTION_NIL;
        	return Tools.actions[bestValAct];
        }
        return Tools.actions[leastSpookyAct];
    }
    



	private void defaultLoop(Node[] actNodes, StateObservation so, ElapsedCpuTimer ect) {		
        double avgTimeTaken = 0, acumTimeTaken = 0;
        long remaining = ect.remainingTimeMillis();
        int numIters = 0, remainingLimit = 5, actIncr = 0;
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
        int seed = Math.abs(Tools.r.nextInt()) % Tools.faculty[Tools.actions.length];			
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
        	actIncr++;
        	if (actIncr == Tools.actions.length) actIncr = 0;
        	int act = Tools.randomActMap[seed][actIncr];
        	
    		boolean allNodesLeadToDirectDeath = true;
    		for (int i = 0; i < Tools.actions.length; i++) {
				if (actNodes[i].directDeaths == 0){
					allNodesLeadToDirectDeath = false;
					break;
				}
			}
        	
        	Node n = actNodes[act];
        	
        	if (!allNodesLeadToDirectDeath && n.directDeaths > 0){
        		continue;
        	}
        	
        	n.so.advance(Tools.actions[n.path.getLast()]);
        	n.advancements++;
        	int d = n.path.size();
        	
        	//get info from new node
        	int win = n.so.isGameOver() ? (n.so.getGameWinner() == WINNER.PLAYER_WINS ? 1 : -1) : 0;
        	
        	//react to info
        	n.reactToAdvancment(n.so, so, win);
        	
        	//print out new node
        	if (LOOP_VERBOSE){
        		System.out.println("Init node: " + Tools.actions[act] + ", path: " + Tools.getActionList(n.path));
	        	System.out.println("outcome - iters: " + numIters + ", score: " + n.so.getGameScore()  + ", win: " + win) ;
	            System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
	            System.out.println("allNodesLeadToDirectDeath: " + allNodesLeadToDirectDeath);
        	}
        	
        	//expansion
        	if (win == 0){
        		//expand state with interesting action
           		if (d == Tools.maxSearchDepth){
        			n.reachMaxDepth(so);
        		}else{
            		n.addGoodActionToPath();
        		}
        	}else if (win == 1){
        		//restart simulation from initial state
        		n.win(so);
        	}else{
        		//restart simulation from initial state, unless !allNodesLeadToDirectDeath and n.directDeath   		
                int posKey = Tools.getPositionKey(n.so.getAvatarPosition());
                Tools.boringPlaces[posKey] += 1;
                
           		n.dead(so, allNodesLeadToDirectDeath);
        	}
        	
            numIters++; acumTimeTaken = elapsedTimerIteration.elapsedMillis();
            avgTimeTaken  = acumTimeTaken/numIters; remaining = ect.remainingTimeMillis();                
        }
        lastIters = numIters;
	}
	
	
	private ACTIONS breadthFirstLoop(StateObservation so, ElapsedCpuTimer ect) {
		double avgTimeTaken = 0, acumTimeTaken = 0;
        long remaining = ect.remainingTimeMillis();
        int numIters = 0, remainingLimit = 5;
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
        
        if (solution == null){
        	if (q.size() == 0){
		    	for (int i = 0; i < Tools.actions.length; i++) {
		    		LinkedList<Integer> path = new LinkedList<Integer>();
		    		path.add(i);
		    		q.add(path);
		    	}
        	}
	        while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
	        	LinkedList<Integer> path = q.poll();
	        	if (path == null) return ACTIONS.ACTION_NIL;
	        	boolean expandFromPath = true;
	        	StateObservation newState = Tools.playbackActions(so, path);
	        	
	        	//check if found solution
	        	int win = newState.isGameOver() ? (newState.getGameWinner() == WINNER.PLAYER_WINS ? 1 : -1) : 0;	        	
	        	if (win == -1) expandFromPath = false;
	        	else if (win == 1){
	        		solution = Tools.getCloneOfPath(path);
	        		if (VERBOSE) System.out.println("FOUND SOLUTION: "+ Tools.getActionList(solution));
	        		break;
	        	}
	        	
	        	//check if situation/state/signature happened before
	        	MoveableSet ms = new MoveableSet(Tools.getMoveables(newState), newState.getAvatarPosition(), newState.getAvatarOrientation());

	        	if (visitedNodes.contains(ms)) expandFromPath = false;
	        	else visitedNodes.add(ms);
	        	
	        	//expansion
	        	if (expandFromPath){
		        	for (int i = 0; i < Tools.actions.length; i++) {
		        		LinkedList<Integer> newPath = Tools.getCloneOfPath(path);
		        		newPath.add(i);
		        		q.add(newPath);
		        	}
	        	}
	            
	            numIters++; acumTimeTaken = elapsedTimerIteration.elapsedMillis();
	            avgTimeTaken  = acumTimeTaken/numIters; remaining = ect.remainingTimeMillis();  
	            lastPathDepth = path.size();
	        }
        }
		
        if (solution != null){
        	if (solution.size() > solIncr){
        		return Tools.actions[solution.get(solIncr++)];
        	}
        }else{
        	if (VERBOSE) System.out.println("Has not found solution yet -- lastPathDepth: " + lastPathDepth);
        }
        return ACTIONS.ACTION_NIL;
	}
	
}
