package agents.NovelTS;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.Random;
import java.util.ArrayList;
import tools.Vector2d;


public class IWPlayer {

    // Root of the tree.
    private TreeNode rootNode;

    private Position positionMatrix;

    public static Random randomGenerator;

    // State observation at the root of the tree.
    private StateObservation rootObservation;

    public static NoveltyTable noveltyTable;
    public static int HUGE_NUMBER = 3000;
    public static RandomAction randomAction;

    public int totalDepth;
    public int numRun;
    public int numIters;
    public int totalNumIters;
    public static int blocksize;

    public IWPlayer(Random random) {
        randomGenerator = random;
        randomAction = new RandomAction(Agent.NUM_ACTIONS, 100, random);
        numRun = 0;
        totalDepth = 0;
   }


    /**
     * Init is invoked every action, init will update the rootObservation,
     * init will also reuse previous expanded tree
     */
    public void init(StateObservation gameState) {
        if (positionMatrix == null) {
            positionMatrix = new Position();
        }

        blocksize = gameState.getBlockSize();

        // Initialize novelty table
        noveltyTable = new NoveltyTable();

        rootNode = new TreeNode();

        // Add current position to position Matrix
        rootObservation = gameState;

        Feature feature = new Feature(gameState);
        positionMatrix.setPosition(feature);


        noveltyTable.getNovelty(feature);

        //Set the game observation to a newly root node.

        rootNode.updateScore(value(gameState), disHeuristic(feature));
    }


    public int run(ElapsedCpuTimer elapsedTimer) {
        // Build the serach Tree
        iwSearch(elapsedTimer, this.rootObservation);

        // Find the best action
        int action = rootNode.bestAction();
        this.numRun +=1;

        return action;
    }


    /**
     * Builds the search tree, given a time budget and a state observation
     *
     * @param elapsedTimer  Timer when the action returned is due
     * @param rootObservation  Initial state observation
     */
    private void iwSearch(ElapsedCpuTimer elapsedTimer, StateObservation rootObservation) {
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        numIters = 0;
        StateObservation tempState;
        int remainingLimit = 10;
        while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
            tempState = rootObservation.copy();
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // SelecteNode will mutate tempState until it reaches the leaf
            TreeNode selected = selectNode(tempState, rootNode);

            // Traverse the reward back to root node
            backPropagate(selected);

            // Update the status
            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
            avgTimeTaken = acumTimeTaken / numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }
        this.totalDepth += rootNode.childDepth;
        totalNumIters += numIters;
        // printInfo();
    }


    /**
    * Return a leaf in the tree for expansion
    */
    private TreeNode selectNode(StateObservation state, TreeNode parentNode) {
        // Failed to expand a new leaf
        if (state.isGameOver() == true || parentNode.isPruned()) {
            return parentNode;
        }
        else if (parentNode.isExpanded()) {
            // Select a children and advance the state observation
            int nextAction = parentNode.unprunedAction();
            Types.ACTIONS action = Agent.actions[nextAction];
            state.advance(action);
            parentNode = parentNode.getchild(nextAction);

            // Update the score of the revisted node
            parentNode.updateScore(value(state));
            return selectNode(state, parentNode);
        } else {
            return expand(parentNode, state);
        }
    }


    /**
     * Expand a leaf node, creates a new child and append
     * it to the tree. Returns the newly creaated child
     */
    private TreeNode expand(TreeNode parentNode, StateObservation currentObservation) {
        int actIndex = parentNode.nextChild();
        currentObservation.advance(Agent.actions[actIndex]);

        Feature feature = new Feature(currentObservation);

        double score = value(currentObservation);
        double novelty = noveltyTable.getNovelty(feature);

        // Consider all rewarding action as novel
        if (score > 0) {
            novelty = 1;
        }
        TreeNode newNode = new TreeNode(parentNode, actIndex, novelty);
        parentNode.addchild(newNode, actIndex);

        newNode.updateScore(score, disHeuristic(feature));
        return newNode;
    }


    /**
     * This method will recursively propagate back the reward
     * from to the parent state until the root node
     */
    private void backPropagate(TreeNode node) {
        if (node == null) {
            return;
        }
        node.update();
        backPropagate(node.parent);
    }


    /**
     * Computes the value associated with a state observation and a tree depth
     * @param a_gameState   the state observation that is evaluated
     * @param treeDepth    the depth in the tree where this evaluation is made
     * @return  the value of the state
     */
    private double value(StateObservation state) {
        if(state.isGameOver()) {
            if(state.getGameWinner() == Types.WINNER.PLAYER_WINS) {
                return HUGE_NUMBER;
            }
            else if(state.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
                return -HUGE_NUMBER;
            } else {
                return state.getGameScore() / 2;
            }
        }
        return state.getGameScore();
    }


    /**
     * Compute the distance heuristic
     */
    private double disHeuristic(Feature feature) {
        return positionMatrix.getScore(feature);
    }


    /**
     * Print debug stat, only use for debug purpose
     */
    public void printInfo() {
        // System.out.println( " " + rootNode.childDepth );
        System.out.println("Tree Size: " + rootNode.countChild() +" numIters: " + numIters + " Avgnum "+ totalNumIters);
        System.out.printf("Best Route: %.05f bestChild: %d\n" ,rootNode.bestRoute,rootNode.bestChild);
        //System.out.println("bestChild: "+ rootNode.bestChild+" Score: " + salvagedTree.score  + " NumChild: "+salvagedTree.numchild+ " Game Over: "+ salvagedTree.gameOver);
        for (int i = 0; i < Agent.NUM_ACTIONS; i++) {
            TreeNode node = rootNode.getchild(i);
            if (node != null) {
                System.out.printf("Child score %.05f best Route %.05f num Visit %d\n",node.getScore(), node.bestRoute, node.numVisit);
                System.out.printf("Pruned: " + node.isPruned() + "Death Rate " + (float)node.numGameOver);
                System.out.println(" numchild " + node.numchild);
            }
        }

        System.out.println();
    }
}
