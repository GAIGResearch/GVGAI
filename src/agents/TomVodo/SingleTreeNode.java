package agents.TomVodo;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
//import tools.Metrics;    // comment this out when submitting to GVGAI!

import java.util.Random;

public class SingleTreeNode
{
    public static final boolean par_onlyFinalScoring = true;    //perform scoring only at episode end

    public static double par_Qinit = 0.0;
    public static double par_Qasum = par_Qinit;
    public static double par_lambda = 1.00;
    public static double par_gamma = 1.00;
    public static double par_forgettingRate_search = 1.0;   // 1.0 means all knowledge from the previous search will be forgotten
    public static double par_playoutNonUniform = 0.0;   // from 0.0 to 1.0, value of 0.0 equals to completely uniform action selection in playout
    public static final double par_UCBboundMetric = 3;
        // 0 - local children Qvalues
        // 1 - local gamma discounted rewards
        // 2 - local gamma and lambda discounted rewards
        // 3 - global non-discounted rewards (as in original sampleMCTS implementation)

    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    public static double epsilon = 1e-6;
    public static double egreedyEpsilon = 0.05;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    //public double totValue;
    public double nVisits;
    public static Random m_rnd;
    public int m_depth;
    protected static double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    public int childIdx;
    public static int numAdvances;

    public double score;
    public double Qval;
    public double normBoundMin = Double.MAX_VALUE;
    public double normBoundMax = -Double.MAX_VALUE;
    public int lastEvaluationSearch = -1;

    public static double sumRewards = 0.0;
    public static double sumGammaRewards = 0.0;
    public static double sumGammaLambdaRewards = 0.0;

    public static SingleTreeNode nextRootNode = null;
    public static int m_depth_offset = 0;
    public static int currentSearchID = 0;
    public static boolean isLeafTerminal;

    public static StateObservation rootState;

    public SingleTreeNode(Random rnd) {
        this(null, -1, rnd);
    }

    public SingleTreeNode(SingleTreeNode parent, int childIdx, Random rnd) {
        this.parent = parent;
        this.m_rnd = rnd;
        children = new SingleTreeNode[Agent.NUM_ACTIONS];
        //totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;

        this.Qval = par_Qinit;
        this.lastEvaluationSearch = currentSearchID;
    }


    public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        numAdvances = 0;

        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
        //while(numIters < Agent.MCTS_ITERATIONS){

            StateObservation state = rootState.copy();

            SingleTreeNode selected = treePolicy(state);
            double delta = selected.rollOut(state);
            backUp(selected, delta);

            numIters++;
            acumTimeTaken = (elapsedTimerIteration.elapsedMillis()) ;
            //System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }

        currentSearchID++;

//        if(Metrics.isInitalized) {
//            Metrics.lastResults[Metrics.NUM_ITERS] += numIters;
//            Metrics.lastResults[Metrics.NUM_FORWARDS] += numAdvances;
//        }
    }

    public SingleTreeNode treePolicy(StateObservation state) {

        SingleTreeNode cur = this;

        while (!state.isGameOver() && (cur.m_depth - m_depth_offset) < Agent.ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                SingleTreeNode expandedNode = cur.expand(state);
                return expandedNode;

            } else {
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
        state.advance(Agent.actions[bestAction]);
        numAdvances++;

        SingleTreeNode tn = new SingleTreeNode(this,bestAction,this.m_rnd);
        children[bestAction] = tn;

        if(par_onlyFinalScoring)
            tn.score = 0;
        else
            tn.score = value(state);

        return tn;
    }

    public SingleTreeNode uct(StateObservation state) {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode child : this.children)
        {
            //double hvVal = child.totValue;
            //double childValue =  hvVal / (child.nVisits + this.epsilon);
            double childValue = child.Qval;

            //decay the values in the tree
            if(child.lastEvaluationSearch < currentSearchID){
                int power = currentSearchID - child.lastEvaluationSearch;
                child.nVisits = child.nVisits * Math.pow(par_forgettingRate_search, power);
                child.lastEvaluationSearch = currentSearchID;
            }

            //childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
            double boundMin = this.normBoundMin;
            double boundMax = this.normBoundMax;
            if(boundMin >= boundMax){
                boundMin = bounds[0];
                boundMax = bounds[1];
            }
            childValue = Utils.normalise(childValue, boundMin, boundMax);
            //System.out.println("norm child value: " + childValue);

            double uctValue = childValue +
                    Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon));

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
        state.advance(Agent.actions[selected.childIdx]);
        numAdvances++;

        if(par_onlyFinalScoring)
            selected.score = 0;
        else
            selected.score = value(state);

        return selected;
    }


    public double rollOut(StateObservation state)
    {
        // non-uniform random playout policy
        double[] playoutPolicy = new double[Agent.NUM_ACTIONS];
        double sumProb = 0.0;
        for(int a = 0; a < Agent.NUM_ACTIONS; a++) {
            playoutPolicy[a] = (this.m_rnd.nextDouble() * par_playoutNonUniform + (1.0-par_playoutNonUniform));
            sumProb += playoutPolicy[a];
        }
        playoutPolicy[0] = playoutPolicy[0] / sumProb;
        for(int a = 1; a < Agent.NUM_ACTIONS; a++) {
            playoutPolicy[a] = playoutPolicy[a-1] + playoutPolicy[a] / sumProb;
        }

        int thisDepth = this.m_depth - m_depth_offset;

        double eligibility = 1.0;
        double sumTDerrors = 0.0;

        double Qprev = par_Qasum;
        double Qnext, TDerror, reward, scoreNext, scorePrev, prevElig;

        sumRewards = 0.0;
        sumGammaRewards = 0.0;
        sumGammaLambdaRewards = 0.0;
        double gammaDecay = 1.0;

        isLeafTerminal = true;
        prevElig = 1.0;
        scorePrev = this.score;
        while (!finishRollout(state,thisDepth)) {

            isLeafTerminal = false;

            int action = Agent.NUM_ACTIONS - 1;
            double roll = m_rnd.nextDouble();
            for(int a = 0; a < Agent.NUM_ACTIONS - 1; a++) {
                if(roll < playoutPolicy[a]) {
                    action = a;
                    a = Agent.NUM_ACTIONS;
                }
            }

            //int action = m_rnd.nextInt(Agent.NUM_ACTIONS);

            state.advance(Agent.actions[action]);
            numAdvances++;
            thisDepth++;

            if(par_onlyFinalScoring)
                scoreNext = 0;
            else
                scoreNext = value(state);

            reward = scoreNext - scorePrev;

            Qnext = par_Qasum;
            TDerror = reward + par_gamma*Qnext - Qprev;
            Qprev = Qnext;

            sumTDerrors = sumTDerrors + TDerror * eligibility;
            prevElig = eligibility;
            eligibility = eligibility * par_gamma * par_lambda;

            sumRewards = sumRewards + reward;
            sumGammaRewards = sumGammaRewards + reward*gammaDecay;
            sumGammaLambdaRewards = sumGammaLambdaRewards + reward*eligibility;
            gammaDecay = gammaDecay * par_gamma;
        }
        //correct last TDerror (states after the terminal state have a value of 0, and not par_Qasum)
        if(isLeafTerminal == false)
            sumTDerrors = sumTDerrors - par_gamma * Qprev * prevElig;

        sumRewards = sumRewards + this.score;

        double delta;
        if(par_onlyFinalScoring){
            delta = value(state);
            sumRewards = delta;
            sumGammaRewards = delta;
            sumGammaLambdaRewards = delta;
        }else{
            delta = sumTDerrors;
        }

        if(sumRewards < bounds[0])
            bounds[0] = sumRewards;
        if(sumRewards > bounds[1])
            bounds[1] = sumRewards;

        delta = delta + sumTDerrors;
        //double normDelta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);

        return delta;
    }

    public double value(StateObservation a_gameState) {

        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore = a_gameState.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            rawScore += HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            rawScore += HUGE_POSITIVE;

        return rawScore;
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= Agent.ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        if(rollerState.isGameOver())               //end of game
            return true;

        return false;
    }

    public void backUp(SingleTreeNode node, double result)
    {
        double reward, TDerror, alpha;

        double Qdelta = result;
        double Qnext;

        if(isLeafTerminal == false)
            Qnext = par_Qasum;
        else
            Qnext = 0;

        SingleTreeNode n = node;
        while(n != null)
        {

            //// update cumulative TD error

            if(n.parent != null)
                reward = node.score - n.parent.score;
            else
                reward = node.score - node.score;

            TDerror = reward + par_gamma*Qnext - n.Qval;
            Qdelta = par_gamma*par_lambda*Qdelta + TDerror;
            Qnext = n.Qval;

            //// update node value

            n.nVisits += 1.0;
            //n.totValue += result;
            alpha = 1.0 / n.nVisits;
            n.Qval = n.Qval + alpha*Qdelta;

            //// update min/max bounds for UCB

            sumGammaRewards = par_gamma*sumGammaRewards + reward;
            sumGammaLambdaRewards = par_gamma*par_lambda*sumGammaLambdaRewards + reward;
            if(n.parent != null){

                // local bounds
                if(par_UCBboundMetric != 3) {

                    // select metric for bounds
                    double boundMetric = 0;

                    if (par_UCBboundMetric == 0)
                        boundMetric = n.Qval;
                    else if (par_UCBboundMetric == 1)
                        boundMetric = sumGammaRewards;
                    else if (par_UCBboundMetric == 2)
                        boundMetric = sumGammaLambdaRewards;

                    // set new bounds
                    if (boundMetric < n.parent.normBoundMin)
                        n.parent.normBoundMin = boundMetric;
                    if (boundMetric > n.parent.normBoundMax)
                        n.parent.normBoundMax = boundMetric;

                // global bounds
                }else{
                    n.parent.normBoundMin = bounds[0];
                    n.parent.normBoundMax = bounds[1];
                }
            }

            //// move up through the tree
            n = n.parent;
        }
    }


    public int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;
        nextRootNode = null;

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
                if (childValue > bestValue) {
                    nextRootNode = children[i];
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
            nextRootNode = children[0];
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
        nextRootNode = null;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                //double tieBreaker = m_rnd.nextDouble() * epsilon;
                //double childValue = children[i].totValue / (children[i].nVisits + this.epsilon);
                double childValue = children[i].Qval;
                childValue = Utils.noise(childValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    nextRootNode = children[i];
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
            nextRootNode = children[0];
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
