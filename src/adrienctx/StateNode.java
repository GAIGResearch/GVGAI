/**
 * Code written by Adrien Couetoux, acouetoux@ulg.ac.be It is free to use,
 * distribute, and modify. User: adrienctx Date: 12/01/2015
 */
package adrienctx;

import core.game.StateObservation;

import java.util.*;

import ontology.Types;
import tools.Vector2d;

class StateNode {

    private static final double HUGE_NEGATIVE = -10000000.0;

    public final List<StateObservation> encounteredStates;

    public final StateNode[] children;

    public final int[] actionNbSimulations;

    private final double[] actionScores;

    private final Random randomGenerator;

    private int numberOfExits;

    private double cumulatedValueOnExit;

    private double passingValue;

    public int numberOfSimulations;

    private double maxScore;

    private final double rawScore;

    //Parent objects:
    public StateNode parentNode;

    public int parentAction;

    private final TreeSearchPlayer parentTree;

    private IntDoubleHashMap[] features;

    private double locationBias;

    private double barycenterBias;
    
    private double valueWhenLastBacktracked;

    private double featureGridBias;

    public boolean[] actionPruned;

    private int gameTick;

    private Vector2d orientation;

//    private double stateIsAlreadyInTheTree;

    // /**
    //  * Creates a new empty state node
    //  */
//    public StateNode(int _parentAction) {
//        parentAction = _parentAction;
//    }

    /**
     * Creates a new state node with one single state encountered
     *
     * @param _state , the encountered state
     */
    public StateNode(StateObservation _state, Random _random, TreeSearchPlayer _parentTree) {
        encounteredStates = new ArrayList<>();
        encounteredStates.add(_state);
        gameTick = _state.getGameTick();
        orientation = _state.getAvatarOrientation();
        children = new StateNode[Agent.NUM_ACTIONS];
        actionNbSimulations = new int[Agent.NUM_ACTIONS];
        actionScores = new double[Agent.NUM_ACTIONS];
        actionPruned = new boolean[Agent.NUM_ACTIONS];
        randomGenerator = _random;

        parentTree = _parentTree;

        rawScore = _state.getGameScore();

        features = new IntDoubleHashMap[parentTree.nbCategories];
        features = parentTree.getFeaturesFromStateObs(_state);
        locationBias = parentTree.getLocationBias(_state);
        barycenterBias = parentTree.getBarycenterBias(_state);
        featureGridBias = parentTree.getFeatureGridBias(features);
    }

    private StateNode(StateObservation _state, Random _random, StateNode _parentNode) {
        encounteredStates = new ArrayList<>();
        encounteredStates.add(_state);
        gameTick = _state.getGameTick();
        orientation = _state.getAvatarOrientation();
        children = new StateNode[Agent.NUM_ACTIONS];
        actionNbSimulations = new int[Agent.NUM_ACTIONS];
        actionScores = new double[Agent.NUM_ACTIONS];
        actionPruned = new boolean[Agent.NUM_ACTIONS];
        randomGenerator = _random;

        parentTree = _parentNode.parentTree;
        parentNode = _parentNode;

        rawScore = _state.getGameScore();

        features = new IntDoubleHashMap[parentTree.nbCategories];
        features = parentTree.getFeaturesFromStateObs(_state);
        locationBias = parentTree.getLocationBias(_state);
        barycenterBias = parentTree.getBarycenterBias(_state);
        featureGridBias = parentTree.getFeatureGridBias(features);
    }

    public IntDoubleHashMap[] getFeatures(){
        return this.features;
    }

    public IntDoubleHashMap[] getCopyOfFeatures(){
        IntDoubleHashMap[] copy = new IntDoubleHashMap[parentTree.nbCategories];

        for (int i=0; i < parentTree.nbCategories; i++){
            copy[i] = new IntDoubleHashMap(features[i]);
        }

        return(copy);
    }

    private double getNodeValue() {
        //return (rawScore - distanceToClosestResource + (double)totalAvatarResources/4.0 - locationBias);
        //return (rawScore + parentTree.featureWeights[3]*distanceToClosestResource + parentTree.featureWeights[1]*distanceToClosestMovable + parentTree.featureWeights[2]*(double)totalAvatarResources + parentTree.featureWeights[0]*locationBias + parentTree.featureWeights[4]*distanceToClosestPortal);

        //return rawScore + parentTree.getLogisticValueApproximation(parentTree.getFeatureVectorFromStateObs(encounteredStates.get(0)));
//        return rawScore + parentTree.getLinearRegression(parentTree.normalizeFeatures(parentTree.getFeatureVectorFromStateObs(encounteredStates.get(0)) ));
        if(parentTree.useValueApproximation){
            return rawScore + parentTree.V_approximator.getBasisFunctionLinearApproximation(parentTree.V_approximator.getBasisFunctionsFromFeatures(features), parentTree.V_approximator.getWeights()) + locationBias * parentTree.getLocationBiasWeight() + barycenterBias * parentTree.getBarycenterBiasWeight() + featureGridBias * parentTree.getFeatureGridWeight();
        }
        else{
            return rawScore + locationBias * parentTree.getLocationBiasWeight() + barycenterBias * parentTree.getBarycenterBiasWeight();
        }
        //        return rawScore + features[0].get(0);
//        return rawScore + parentTree.getKNNValue(features) + features[0].get(0) + resourceBias;
    }

    /**
     * Adds a new action node to the children if this state node
     *
     * @param _currentObservation , the current state observation to use in
     *                            the simulator (i.e. the advance method from Forward Model)
     */
    public StateNode addStateNode(StateObservation _currentObservation, int actionIndex) {

        StateNode newStateNode = new StateNode(_currentObservation, randomGenerator, this);

//        if (newStateNode.features[2][0] > this.parentTree.maxResources) {
//            this.parentTree.maxResources = newStateNode.features[2][0];
//        }

        //use the new state to create the action node
        newStateNode.parentAction = actionIndex;
        children[actionIndex] = newStateNode;
        actionNbSimulations[actionIndex] += 1;

        return newStateNode;
    }

    /**
     * returns the selected action node
     */
    public int selectRandomAction() {
        //now, we just select a random action - TODO: make this better (UCB, expectimax, etc)
        int bestActionIndex = 0;
        double bestValue = -1;
        double x;
        for (int i = 0; i < children.length; i++) {
            x = randomGenerator.nextDouble();
            if (x > bestValue && children[i] != null) {
                bestActionIndex = i;
                bestValue = x;
            }
        }
        return bestActionIndex;
    }

    /**
     * returns the selected action node
     */
    public int selectAction() {
        //now, we just select a random action - TODO: make this better (UCB, expectimax, etc)
        int bestActionIndex = 0;
        double bestValue = HUGE_NEGATIVE;
        double x;
        double actionValue;

        for (int i = 0; i < children.length; i++) {
            // if(actionNbSimulations[i] < 2){    //if action has 1 or 0 simulations, select it (to avoid having to wait for epsilon greedy to kick in)
            //     return i;
            // }
            //System.out.format("action value, movable bias, movable normalizing %f , %f , %f , %n", actionValue, actionChildren[i].stateNodeChild.distanceToClosestMovable/(2.0*normalizingDistanceToMovables), normalizingDistanceToMovables);
            x = randomGenerator.nextDouble();
            actionValue = this.actionScores[i];
            //System.out.format("i, actionValue, bestValue, bestActionIndex : %d, %f, %f, %d %n", i, actionValue, bestValue, bestActionIndex);
            if ((children[i] != null) && (actionValue + (x / 1000.0) > bestValue)) {
                bestActionIndex = i;
                bestValue = actionValue + (x / 1000.0);
            }
        }
        //System.out.format("returning %d ", bestActionIndex);
//        if (bestValue > HUGE_NEGATIVE) {
        //System.out.format("%n returning %d ", bestActionIndex);
        return bestActionIndex;
//        } else {
//            this.stateIsAlreadyInTheTree = 1.0;
//            //System.out.format("%n branch is a dead end");
//            return (selectRandomAction());
//        }

    }

    // public double getActionValue(int actionIndex)
    // {
    //     return ((1.0 - 0.0/(double)actionNbSimulations[actionIndex])*(actionChildren[actionIndex].expectimax - actionChildren[actionIndex].stateNodeChild.locationBias) + 0.0*biasForActionSelection[actionIndex]/(double)actionNbSimulations[actionIndex]);
    // }

    /**
     * Updates the data stored in this state node - TODO: store more than just
     * state observations; should also store instant values
     */
    public void updateData(StateObservation _state) {
        encounteredStates.add(_state);
    }

    public int getMostVisitedAction() {
        //System.out.format("selecting the best action from TreeSearch : %n");
        int bestActionIndex = 0;
        double bestNumberOfVisits = -1;
        for (int i = 0; i < children.length; i++) {
            //double x = randomGenerator.nextDouble();
            if ((double) actionNbSimulations[i] > bestNumberOfVisits && children[i] != null) {
                bestActionIndex = i;
                bestNumberOfVisits = (double) actionNbSimulations[i];
            }
        }
//        if (encounteredStates.get(0).getGameTick() > 0) {
//            printTree(0);
//        }
        return bestActionIndex;
    }

    public int getHighestScoreAction() {
        int bestActionIndex = 0;
        double bestScore = HUGE_NEGATIVE;
        for (int i = 0; i < children.length; i++) {
            double x = randomGenerator.nextDouble();
            if (((actionScores[i] + (x / 100000.0)) > bestScore) && (children[i] != null)) {
                bestActionIndex = i;
                bestScore = actionScores[i] + x / 100000.0;
            }
        }
        //if (encounteredStates.get(0).getGameTick() > 50) {
//            printTree(0);
        //}
        return bestActionIndex;
    }

    public void backPropagateData(StateObservation _state, ArrayList<IntDoubleHashMap[]> _visitedFeatures, ArrayList<Double> _visitedScores) {
        //Updating gameOver count and score
        boolean gameOver = _state.isGameOver();
        double _rawScore = _state.getGameScore();

        //////
        if (parentTree.useValueApproximation) {
            double cumulatedDiscountedScores = 0.0;
            for (int i = 0; i < _visitedScores.size() - 1; i++) {
                cumulatedDiscountedScores = parentTree.discountFactor * cumulatedDiscountedScores + (_visitedScores.get(i) - _visitedScores.get(i + 1));
                if(false){
                    parentTree.V_approximator.addTrajectory(_visitedFeatures.get(i + 1), _visitedFeatures.get(0), cumulatedDiscountedScores, gameOver);
                }

            }
        }

        //////

        if (gameOver) {
            this.numberOfExits += 1;
            cumulatedValueOnExit += parentTree.getValueOfState(_state);

//            Types.WINNER win = _state.getGameWinner();
//            if (win == Types.WINNER.PLAYER_LOSES) {
//                //cumulatedValueOnExit += rawScore - 2000.0 *( 1.0 + Math.abs(rawScore) );
//                cumulatedValueOnExit += _rawScore - 10000.0;
//            }
//            if (win == Types.WINNER.PLAYER_WINS) {
////                cumulatedValueOnExit += _rawScore + 100.0 * (1.0 + Math.abs(_rawScore));
//                cumulatedValueOnExit += (double) _state.getGameTick() / 500.0;
//            }
        }

        StateNode currentNode = this;

        while (currentNode != null) {
            currentNode.numberOfSimulations += 1;
            if (!currentNode.isLeaf()) {
                if (true) {
                    double bestScore = HUGE_NEGATIVE;
                    for (int i = 0; i < currentNode.children.length; i++) {
                        if (currentNode.children[i] != null) {
                            if (currentNode.actionScores[i] > bestScore) {
                                bestScore = currentNode.actionScores[i];
                            }
                        }
                    }
                    currentNode.maxScore = bestScore;
                } else {
                    double cumulatedScore = 0.0;
                    int totalSims = 0;
                    for (int i = 0; i < currentNode.children.length; i++) {
                        if (currentNode.children[i] != null) {
                            cumulatedScore += currentNode.actionNbSimulations[i] * currentNode.actionScores[i];
                            totalSims += currentNode.actionNbSimulations[i];
                        }
                    }
                    currentNode.maxScore = cumulatedScore / (double) totalSims;
                }

            }

            if (currentNode.parentNode != null) {
                currentNode.valueWhenLastBacktracked = currentNode.getNodeValue();
                currentNode.passingValue = currentNode.getNodeValue() - currentNode.parentNode.getNodeValue() + parentTree.discountFactor * currentNode.maxScore;
                int _actionIndex = currentNode.parentAction;
                currentNode.parentNode.actionScores[_actionIndex] = (currentNode.cumulatedValueOnExit / (double) currentNode.numberOfSimulations) + ((double) (currentNode.numberOfSimulations - currentNode.numberOfExits) / (double) currentNode.numberOfSimulations) * currentNode.passingValue;
            }

            currentNode = currentNode.parentNode;
        }
    }

    public void puzzleBackProp() {
        //Updating gameOver count and score
        StateObservation _state = this.encounteredStates.get(0);
        boolean gameOver = _state.isGameOver();
        double _rawScore = _state.getGameScore();
        StateNode currentNode = this;

        if (gameOver) {
            this.numberOfExits += 1;
            cumulatedValueOnExit += parentTree.getValueOfState(_state);
            currentNode.maxScore = parentTree.getValueOfState(_state);
        }
        else{
            currentNode.maxScore = _rawScore;
        }


        while (currentNode != null) {
            currentNode.numberOfSimulations += 1;
            if (!currentNode.isLeaf()) {
                double bestScore = HUGE_NEGATIVE;
                for (int i = 0; i < currentNode.children.length; i++) {
                    if (currentNode.children[i] != null) {
                        if (currentNode.actionScores[i] > bestScore) {
                            bestScore = currentNode.actionScores[i];
                        }
                    }
                }
                if(bestScore > currentNode.maxScore){
                    currentNode.maxScore = 1.0 * bestScore;
                }
            }

            if (currentNode.parentNode != null) {
                int _actionIndex = currentNode.parentAction;
                currentNode.parentNode.actionScores[_actionIndex] = currentNode.maxScore;
                if(!currentNode.notFullyExpanded()){
                    currentNode.parentNode.actionPruned[_actionIndex] = currentNode.allChildrenPruned();
                }
            }

            currentNode = currentNode.parentNode;
        }
    }
    /**
     * returns true if and only if the
     */
    public boolean notFullyExpanded() {
        for (StateNode children1 : children) {
            if (children1 == null) {
                return true;
            }
        }
        return false;
    }

    public boolean allChildrenPruned(){
        for (boolean b : actionPruned){
            if(b == false){
                return false;
            }
        }
        return true;
    }

    /**
     * returns true if and only if the
     */
    private boolean isLeaf() {
        for (StateNode children1 : children) {
            if (children1 != null) {
                return false;
            }
        }
        return true;
    }

    public void printTree(int depth) {
        System.out.format("%n ##### Printing tree at depth %d and nbsims %d %n", depth, numberOfSimulations);
//        System.out.format("parent rawScore, toRes, toMov, AvatRes, locationBias, nodeV, tick : %f, %f, %f, %d, %f, %f, %d", rawScore, distanceToClosestResource, distanceToClosestMovable, totalAvatarResources, locationBias, this.getNodeValue(), encounteredStates.get(0).getGameTick());
        for (int i = 0; i < children.length; i++) {
            if (children[i] != null) {
                System.out.format("%n ~~~~~~~~~~~~ action number %d :", i);
                System.out.format("%n nbSims, score, avgOnExit, PassingV, maxScore, nbExists, nodeValue, tick: %d, %f, %f, %f, %f, %d, %f, %d", actionNbSimulations[i], actionScores[i], children[i].cumulatedValueOnExit / (double) children[i].numberOfExits, children[i].passingValue, children[i].maxScore, children[i].numberOfExits, children[i].getNodeValue(), children[i].encounteredStates.get(0).getGameTick());
////                System.out.format("%n rawScore, toRes, toMov, AvatRes, locationBias, toPort: %f, %f, %f, %d, %f, %f", children[i].rawScore, children[i].distanceToClosestResource, children[i].distanceToClosestMovable, children[i].totalAvatarResources, children[i].locationBias, children[i].distanceToClosestPortal);
////                //System.out.format("%n nbSims, , nExits, avgValue on exit, locationBias, toRes, toMov, Res : %d, %f, %f, %d, %f, %f, %f, %f, %d ",actionNbSimulations[i],biasForActionSelection[i],actionChildren[i].expectimax,actionChildren[i].stateNodeChild.numberOfExits,actionChildren[i].stateNodeChild.cumulatedValueOnExit/(double)actionChildren[i].stateNodeChild.numberOfExits,actionChildren[i].stateNodeChild.locationBias,actionChildren[i].stateNodeChild.distanceToClosestResource,actionChildren[i].stateNodeChild.distanceToClosestMovable, actionChildren[i].stateNodeChild.totalAvatarResources);
////                if (depth < 1) {
////                    //    actionChildren[i].stateNodeChild.printTree(depth+1);
////                }
//////                System.out.format("%n features ");
//////                System.out.format(Arrays.toString(children[i].features));
            }
        }
    }

    public double getActionScore(int index){
        return actionScores[index];
    }



}
