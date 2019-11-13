package tutorialGeneration.biasedOnetreeMCTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
import video.basics.GameEvent;
import video.basics.Interaction;
import video.basics.PlayerAction;

public class SingleTreeNode
{
	public static int deepest = 0;
	public static boolean randomWon = false;
    public static SingleTreeNode deepestNode;

	private boolean mixmax = false;
	
	private boolean verbose = true;
	
    private final double HUGE_NEGATIVE = -100000.0;
    private final double HUGE_POSITIVE =  100000.0;
    
    private final double BONUS = 1;
    private final double Q = 0.125;
    
    private final double K_DECAY = 0.00;
    private final double BONUS_DECAY = 0.10;
    // number of MCTS iterations
    public int numIterations;
    
    public float bonus = 0;
    public double epsilon = 1e-6;
    public double egreedyEpsilon = 0.05;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    public double totValue;
    public int nVisits;
    public Random m_rnd;
    public int m_depth;
    protected double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    public int childIdx;

    public int num_actions;
    Types.ACTIONS[] actions;
    public int ROLLOUT_DEPTH = 50;
    public int bonus_count = 0;
    
    public double K = Math.sqrt(2);
//    public double K = 0;
    public SingleTreeNode bestNode;
    public SingleTreeNode rootNode;
    public StateObservation rootState;
    
    public ArrayList<GameEvent> interactions;
    
    public List<GameEvent> critPath;

    public SingleTreeNode(Random rnd, int num_actions, int numIterations, Types.ACTIONS[] actions) {
        this(null,null, -1, rnd, num_actions, actions, new ArrayList<GameEvent>());
        this.numIterations = numIterations;
        rootNode = this;
        // read in critical_mechanics file
    }

    public SingleTreeNode(SingleTreeNode root, SingleTreeNode parent, int childIdx, Random rnd, int num_actions, Types.ACTIONS[] actions, ArrayList<GameEvent> interactions) {
        this.parent = parent;
        this.rootNode = root;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
        this.interactions = interactions;
        this.K = Math.sqrt(2);
        children = new SingleTreeNode[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
        
        if (m_depth > SingleTreeNode.deepest) {
        	SingleTreeNode.deepest = m_depth;
        	SingleTreeNode.deepestNode = this;
        }
    }
 
    /***
     * Build a whole MCTS tree
     * @param elapsedTimer
     * @param improved
     */
    public void mctsSearch(boolean improved, List<GameEvent> critPath2) {
        int numIters = 0;
        bestNode = null;
        SingleTreeNode.deepest = 0;
        while(numIters < numIterations){

//        	if(numIters % 1000 == 0 && verbose) {
//        		System.out.println("*********************\n");
//        		System.out.println("Iteration: " + numIters);
//        		System.out.println("Deepest Node: " + SingleTreeNode.deepest);
//        	}
            StateObservation state = rootState.copy();

            SingleTreeNode selected = treePolicy(state);
            selected.critPath = critPath2;
            double delta = selected.rollOut(state, improved);
            backUp(selected, delta);

            if(bestNode != null) {
            	break;
            }
            numIters++;
        }
        System.out.println("Deepest Node: " + SingleTreeNode.deepest);
//        int won = 0;
//        if(bestNode != null) {
//        	won = 1;
//        }
//        System.out.println("Game Over\nResult: " + won);
    }
    public SingleTreeNode getBestNode() {
    	return bestNode;
    }
    public SingleTreeNode treePolicy(StateObservation state) {

        SingleTreeNode cur = this;

        while (!state.isGameOver() && cur.m_depth < ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand(state);

            } else {
            	cur.K = cur.K * (1 - K_DECAY);
                SingleTreeNode next = cur.uct(state);
                cur = next;
            }
        }

        return cur;
    }


     public SingleTreeNode expand(StateObservation state) {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        //Roll the state
        state.advance(actions[bestAction]);
        ArrayList<GameEvent> interactions = state.getFirstTimeEventsHistory();

        // add any interactions that occured during this event
        
        SingleTreeNode tn = new SingleTreeNode(this.rootNode, this, bestAction, this.m_rnd, num_actions, actions, interactions);
        tn.bonus = this.bonus;
        children[bestAction] = tn;
        
        if(state.isGameOver() && state.getGameWinner() == Types.WINNER.PLAYER_WINS) {
        	this.rootNode.bestNode = this;
        }
        return tn;
    }

    public SingleTreeNode uct(StateObservation state) {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode child : this.children)
        {
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + this.epsilon);

            //childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
            //System.out.println("norm child value: " + childValue);

            double uctValue = childValue +
                    K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon));
//            		(K/SingleTreeNode.deepest) * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon));
            uctValue = Utils.noise(uctValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
            + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        state.advance(actions[selected.childIdx]);

        return selected;
    }


    public double rollOut(StateObservation state, boolean improved)
    {
    	int ogGameTick = state.getGameTick();
        int thisDepth = 0;

        while (!finishRollout(state,thisDepth)) {

            int action = m_rnd.nextInt(num_actions);
            state.advance(actions[action]);
            thisDepth++;
        }


        double delta = value(state);
        
        if(improved) {
        	delta += getCritPathBonus(ogGameTick, state.getFirstTimeEventsHistory());
        	// WARNING: For testing with just the bonus as the reward!!!
//        	delta = getCritPathBonus(ogGameTick, state.getFirstTimeEventsHistory());
        }
        if(delta < bounds[0])
            bounds[0] = delta;
        if(delta > bounds[1])
            bounds[1] = delta;
        
//        this.totValue = delta;

        return delta;
    }

    public double value(StateObservation a_gameState) {

        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore = a_gameState.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            rawScore += HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
        {    
        	rawScore += HUGE_POSITIVE;
        	
    	}

        return rawScore;
    }
    
    public double getCritPathBonus(int ogGameTick, ArrayList<GameEvent> interactions) {
    	
//    	ArrayList<GameEvent> critPath = new ArrayList<GameEvent>();
    	// Aliens
//    	critPath.add(new PlayerAction("ACTION_USE"));
//    	critPath.add(new Interaction("KillBoth", "base", "sam"));
//    	critPath.add(new Interaction("KillSprite", "alienBlue", "sam"));
    	
    	// Zelda
//    	critPath.add(new PlayerAction("ACTION_USE"));
//    	critPath.add(new Interaction("KillSprite", "monsterQuick", "sword"));
//    	critPath.add(new Interaction("KillSprite", "monsterNormal", "sword"));
//    	critPath.add(new Interaction("KillSprite", "monsterSlow", "sword"));
//    	critPath.add(new Interaction("TransformTo", "nokey",  "key"));
//    	critPath.add(new Interaction("KillSprite", "goal", "withkey"));
//    	int indexFloor = 0;
    	
    	// Solarfox
//    	critPath.add(new Interaction("KillSprite","blib","avatar"));
    	
    	// SurviveZombies
//    	critPath.add(new Interaction("SubtractHealthPoints", "avatar", "zombie"));
//    	critPath.add(new Interaction("KillSprite", "avatar", "zombie"));
//    	critPath.add(new Interaction("StepBack", "avatar", "wall"));
//    	critPath.add(new Interaction("AddHealthPoints", "avatar", "honey"));
    	
    	// RealPortals
//    	critPath.add(new PlayerAction("ACTION_USE"));
//    	critPath.add(new Interaction("TransformTo", "avatarIn", "weaponToggle1"));
//    	critPath.add(new Interaction("TransformTo", "avatarOut", "weaponToggle2"));
//    	critPath.add(new Interaction("TransformTo", "wall", "missileOut"));
//    	critPath.add(new Interaction("TransformTo", "wall", "missileIn"));
//    	critPath.add(new Interaction("TeleportToExit","avatarIn","portalentry"));
//    	critPath.add(new Interaction("TeleportToExit","avatarOut","portalentry"));
//    	critPath.add(new Interaction("StepBack","avatarOut","portalExit"));
//    	critPath.add(new Interaction("StepBack","avatarIn","portalExit"));
//    	critPath.add(new Interaction("KillSprite", "key", "avatarIn"));
//    	critPath.add(new Interaction("KillSprite", "key", "avatarOut"));
//    	critPath.add(new Interaction("KillIfOtherHasMore", "lock", "avatarOut"));
//    	critPath.add(new Interaction("KillIfOtherHasMore", "lock", "avatarIn"));
//    	critPath.add(new Interaction("KillSprite", "goal", "avatarOut"));
//    	critPath.add(new Interaction("KillSprite", "goal", "avatarIn"));

    	
    	// Sokoban
//    	critPath.add(new Interaction("BounceForward", "box", "avatar"));
//    	critPath.add(new Interaction("KillSprite", "box", "hole"));
    	
    	// Plants
//    	critPath.add(new PlayerAction("ACTION_USE"));
//    	critPath.add(new Interaction("TransformTo", "shovel", "marsh"));
//    	critPath.add(new Interaction("TransformTo", "plant", "axe"));
    	
    	// one to one mapping to critPath
    	int[] mechCounter = new int[critPath.size()];
    	
    	Object[] interactionArray = interactions.toArray();
    	for(int i = 0; i < critPath.size(); i++) {
    		for(int j = 0; j < interactionArray.length; j++) {
    			GameEvent interaction = (GameEvent) interactionArray[j];
    			
    			if(critPath.get(i).equals(interaction)) {
    				mechCounter[i]++;
    				if(Integer.parseInt(interaction.gameTick) >= ogGameTick-1) {
//    				indexFloor = i;
//    					bonus += BONUS * (1.0 / (float)(Math.pow(1.1, Integer.parseInt(interaction.gameTick) - (ogGameTick))));
//    					if(Integer.parseInt(interaction.gameTick) == rootNode.rootState.getGameTick() + 1) {
//    						bonus+= 10000;
//    					}
    					if(mechCounter[i] < 100)	
    						bonus += BONUS  * ((1 - BONUS_DECAY) / mechCounter[i]) * (1.0 / (float)(Math.pow(1.1, Integer.parseInt(interaction.gameTick) - (ogGameTick))));
//    					System.out.println(interaction.toString() + " : " + interaction.gameTick);
    					this.bonus_count += 1;
    				}
    			}
    		}
    	}
    	bonus += this.parent.bonus;
    	return bonus;
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        if(rollerState.isGameOver())  {             //end of game
            if(rollerState.getGameWinner() == Types.WINNER.PLAYER_WINS) {
            	SingleTreeNode.randomWon = true;
            }
        	return true;
        }

        return false;
    }

    public void backUp(SingleTreeNode node, double result)
    {
        SingleTreeNode n = node;
        while(n != null)
        {
        	if(!mixmax){
        		n.nVisits++;
//            n.totValue += result;
            	n.totValue = Math.max(n.totValue, result);
//            n.totValue = n.totValue / n.nVisits;
        	}
        	else {
        		double all = 0;
        		double max = 0;
        		boolean allNull = true;
        		for(SingleTreeNode child : n.children) {
        			if(child != null) {
        				all += child.totValue;	
        				max = Math.max(max, child.totValue);
        				allNull = false;
        			}
        		}

        		double avg = 0;
        		if(n.children.length > 0)
        			avg = all / n.children.length;
        		
        		n.totValue = Q * max + (1-Q) * avg;
        		
        		if(allNull) {
        			n.totValue = result;
        		}
        	}
        	
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            if(n != rootNode)
            	n.parent.bonus_count += this.bonus_count;
            n = n.parent;
        }
    }


    public int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
            	
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = Utils.noise(childValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                childValue = children[i].totValue;
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
                
                System.out.println(actions[i] + " - value: " + childValue);
//                		+ " - critPath hits: " + children[i].bonus_count);
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }
        return selected;
    }

    public int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                //double tieBreaker = m_rnd.nextDouble() * epsilon;
                double childValue = children[i].totValue / (children[i].nVisits + this.epsilon);
                childValue = Utils.noise(childValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    public boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
}
