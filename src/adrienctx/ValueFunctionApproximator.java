package adrienctx;

import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author acouetoux
 */
class ValueFunctionApproximator {

//    private int[] nbTypesPerCategory;

    private final int nbWeights;

    private final int nbBasisPerType;

    private final int numberOfLayers;

    private final int numberOfTransitionsStoredPerLayer;

    private final IntArrayOfDoubleHashMap[] weights;

    private final Transition[][] observedTransitions;

    private final int[] currentTransitionIndex;

    private final int[] currentTajectoryIndex;

    private final int miniBatchSize;

    private final double discountFactor;

    private final double learningRate;

    private final double regularizationTerm;

    private final Trajectory[][] observedTrajectories;

    private final int numberOfTrajectoriesPerLayer;

    private double initialWeightValue;

    private final IntArrayOfDoubleHashMap[] weightsTraj;

    //normalizing variables

    private final int nbNeighbors;

    private final List<Integer> listOfVisitedITypes;

    private boolean recordLog;

    public ValueFunctionApproximator(int _nbWeights) {
        recordLog = false;
        nbWeights = _nbWeights;
        nbBasisPerType = 1;
        //Initializing the hashmap array of weights
        weights = new IntArrayOfDoubleHashMap[nbWeights];
        for (int i = 0; i < nbWeights; i++) {
            weights[i] = new IntArrayOfDoubleHashMap();
        }

        weightsTraj = new IntArrayOfDoubleHashMap[nbWeights];
        for (int i = 0; i < nbWeights; i++) {
            weightsTraj[i] = new IntArrayOfDoubleHashMap();
        }

        //initialize transition storage
        numberOfTransitionsStoredPerLayer = 5000;
        numberOfLayers = 3;
        observedTransitions = new Transition[numberOfLayers][numberOfTransitionsStoredPerLayer];
        currentTransitionIndex = new int[numberOfLayers];

        //trajectory storage
        numberOfTrajectoriesPerLayer = 5000;
        observedTrajectories = new Trajectory[numberOfLayers][numberOfTrajectoriesPerLayer];
        currentTajectoryIndex = new int[numberOfLayers];

        //feature engineering - location bias

        initialWeightValue = 0.5;


        //gradient learning parameters
        miniBatchSize = 200;
        discountFactor = 0.80;
        learningRate = 0.15;
        regularizationTerm = 0.01;

        nbNeighbors = 10;

        listOfVisitedITypes = new ArrayList<>();
    }

    public void init(StateObservation a_gameState) {

//        double h = a_gameState.getWorldDimension().getHeight();
//        double w = a_gameState.getWorldDimension().getWidth();
//        double debug = 0.0;
    }

    public IntArrayOfDoubleHashMap[] getWeights() {
        return weights;
    }

    private IntArrayOfDoubleHashMap[] analyticGradForOneObs(IntArrayOfDoubleHashMap[] theta, Transition _transition) {
        IntArrayOfDoubleHashMap[] DeltaTheta = new IntArrayOfDoubleHashMap[nbWeights];
        for (int i = 0; i < nbWeights; i++) {
            DeltaTheta[i] = new IntArrayOfDoubleHashMap();
        }

        double prediction = getBasisFunctionLinearApproximation(_transition.basisFunctionValues1, theta);
        double label = _transition.reward;
        if (label < 0.0) {
            label = -1.0;
        } else if (label > 0.0) {
            label = 1.0;
        } else {
            label = 0.0;
        }

        double error = prediction - label;

        for (int i = 0; i < nbWeights; i++) {
            for (Integer type : _transition.basisFunctionValues1[i].keySet()) {
                double[] gradientVector = new double[_transition.basisFunctionValues1[i].get(type).length];
                for (int j = 0; j < gradientVector.length; j++) {
                    gradientVector[j] = error * (_transition.basisFunctionValues1[i].get(type)[j]);
                }
                DeltaTheta[i].put(type, gradientVector);
            }
        }
        return DeltaTheta;
    }

    private IntArrayOfDoubleHashMap[] analyticGradient(IntArrayOfDoubleHashMap[] theta) {
        IntArrayOfDoubleHashMap[] DeltaTheta = new IntArrayOfDoubleHashMap[nbWeights];
        for (int i = 0; i < nbWeights; i++) {
            DeltaTheta[i] = new IntArrayOfDoubleHashMap();
        }
        Transition currentTransition;

        IntArrayOfDoubleHashMap[] basisFunctions1;
//        IntArrayOfDoubleHashMap[] basisFunctions2;

        IntArrayOfDoubleHashMap[][] gradientW = new IntArrayOfDoubleHashMap[nbWeights][numberOfLayers];
        for (int i = 0; i < nbWeights; i++) {
            for (int j = 0; j < numberOfLayers; j++) {
                gradientW[i][j] = new IntArrayOfDoubleHashMap();
            }
        }

        double observedValue;
        double belief;
        double error;

        int[][] miniBatchIndexes = new int[numberOfLayers][];
        int[] actualNumberOfTransitionsPerLayer = new int[numberOfLayers];
        int a;
        int m;
        Random randomGenerator = new Random();

        int nbTransitions = 0;

        double[] rewards = new double[numberOfLayers];
        rewards[0] = -1.0;
        rewards[1] = 0.0;
        rewards[2] = 1.0;

        int[] countTransitions = new int[numberOfLayers];

        double[] cumulatedError = new double[numberOfLayers];
        for (int l = 0; l<numberOfLayers; l++){
            cumulatedError[l] = 0;
        }

        for (int l = 0; l < numberOfLayers; l++) {
            m = 0;
            while ((m < numberOfTransitionsStoredPerLayer) && (observedTransitions[l][m] != null)) {
                m++;
            }
            actualNumberOfTransitionsPerLayer[l] = m;
            miniBatchIndexes[l] = new int[Math.min(miniBatchSize, actualNumberOfTransitionsPerLayer[l])];
            for (int b = miniBatchIndexes[l].length - 1; b > -1; b--) {
                miniBatchIndexes[l][b] = b;
            }
            for (int b = miniBatchIndexes[l].length - 1; b > -1; b--) {
                a = randomGenerator.nextInt(b + 1);
                miniBatchIndexes[l][b] = miniBatchIndexes[l][a];
                miniBatchIndexes[l][a] = b;
            }

            for (int k = 0; k < miniBatchIndexes[l].length; k++) {

                m = miniBatchIndexes[l][k];
                nbTransitions += 1;
                countTransitions[l] += 1;
                currentTransition = observedTransitions[l][m];
                basisFunctions1 = currentTransition.basisFunctionValues1;
//                basisFunctions2 = currentTransition.basisFunctionValues2;



                belief = getBasisFunctionLinearApproximation(basisFunctions1, theta);
//                observedValue = rewards[l] + discountFactor * getBasisFunctionLinearApproximation(basisFunctions2, theta);
//                if (currentTransition.isFinal) {
                if (true) {
                    observedValue = rewards[l];
                }
                error = belief - observedValue;
                cumulatedError[l] += Math.sqrt(Math.pow(error, 2.));
                nbTransitions += 1;

                for (int i = 0; i < nbWeights; i++) {
                    for (Integer type : basisFunctions1[i].keySet()) {
//                        if(basisFunctions2[i].containsKey(type)){
//                        if (true) {
                        double[] newGradient = new double[nbBasisPerType];
                        for (int j = 0; j < nbBasisPerType; j++) {
//                                newGradient[j] = learningRate * error * (basisFunctions1[i].get(type)[j]);
                            newGradient[j] = learningRate * (error * (basisFunctions1[i].get(type)[j]) + regularizationTerm * Math.signum(weights[i].get(type)[j]));
                        }
                        if (!gradientW[i][l].containsKey(type)) {
                            gradientW[i][l].put(type, newGradient);
                        } else {
                            for (int j = 0; j < nbBasisPerType; j++) {
                                gradientW[i][l].get(type)[j] += newGradient[j];
                            }
                        }
//                        }
                    }
                }
                if (k == 0) {
                    IntArrayOfDoubleHashMap[] trueDelta = analyticGradForOneObs(weights, currentTransition);
                    IntArrayOfDoubleHashMap[] approxDelta = approxGradForOneObs(weights, currentTransition, 0.1);
                    int debug = 1;
                }
            }
        }

        double[] gradientWeights = new double[numberOfLayers];

        for (int l = 0; l < numberOfLayers; l++) {

            if (countTransitions[l] > 0) {
                if(false){
//                if(l==1){
                    gradientWeights[l] = 0.;
                }
                else{
                    gradientWeights[l] = 1.0 / (2.0 * countTransitions[l]);
                }
//                gradientWeights[l] = (double) nbTransitions / ((double) numberOfLayers * (double) countTransitions[l]);
//                gradientWeights[l] = 1.0 / (3.0 * countTransitions[l]);
            }
        }

        for (int i = 0; i < nbWeights; i++) {
            for (Integer type : theta[i].keySet()) {
                double[] gradientVector = new double[nbBasisPerType];
                for (int j = 0; j < nbBasisPerType; j++) {
//                    newWeights[j] = (1.0 - regularizationTerm) * weights[i].get(type)[j];
                    for (int l = 0; l < numberOfLayers; l++) {
                        if (gradientW[i][l].containsKey(type)) {
                            gradientVector[j] -= gradientWeights[l] * gradientW[i][l].get(type)[j];
                        }
                    }
                }
                DeltaTheta[i].put(type, gradientVector);
            }
        }
        double[] meanSquareError = new double[numberOfLayers];
        for (int l = 0; l < numberOfLayers; l++) {
            meanSquareError[l] = cumulatedError[l] * gradientWeights[l];
        }

        return DeltaTheta;
    }

    private IntArrayOfDoubleHashMap[] approxGradForOneObs(IntArrayOfDoubleHashMap[] theta, Transition _transition, double _epsilon) {
        IntArrayOfDoubleHashMap[] gradient = new IntArrayOfDoubleHashMap[theta.length];
        IntArrayOfDoubleHashMap[] thetaPlus = new IntArrayOfDoubleHashMap[theta.length];
        IntArrayOfDoubleHashMap[] thetaMinus = new IntArrayOfDoubleHashMap[theta.length];

        for (int i = 0; i < theta.length; i++) {
            gradient[i] = new IntArrayOfDoubleHashMap(theta[i]);
            thetaPlus[i] = new IntArrayOfDoubleHashMap(theta[i]);
            thetaMinus[i] = new IntArrayOfDoubleHashMap(theta[i]);
        }

        for (int i = 0; i < theta.length; i++) {
            for (Integer key : theta[i].keySet()) {
                double[] gradVector = new double[theta[i].get(key).length];
                double[] plusVector = new double[theta[i].get(key).length];
                double[] minusVector = new double[theta[i].get(key).length];
                double[] plusVector_temp = new double[theta[i].get(key).length];
                double[] minusVector_temp = new double[theta[i].get(key).length];

                for (int j = 0; j < theta[i].get(key).length; j++) {
                    plusVector[j] = thetaPlus[i].get(key)[j] + _epsilon;
                    plusVector_temp[j] = thetaPlus[i].get(key)[j];
                    minusVector[j] = thetaMinus[i].get(key)[j] - _epsilon;
                    minusVector_temp[j] = thetaMinus[i].get(key)[j];
                }
                thetaPlus[i].put(key, plusVector);
                thetaMinus[i].put(key, minusVector);

                for (int j = 0; j < theta[i].get(key).length; j++) {
                    double costPlus = modelCostForOneObservation(thetaPlus, _transition);
                    double costMinus = modelCostForOneObservation(thetaMinus, _transition);
                    gradVector[j] = (costPlus - costMinus) / (2.0 * _epsilon);
                }

                thetaPlus[i].put(key, plusVector_temp);
                thetaMinus[i].put(key, minusVector_temp);
                gradient[i].put(key, gradVector);
            }
        }
        return gradient;
    }

    public void addTrajectory(IntDoubleHashMap[] _feature1, IntDoubleHashMap[] _feature2, double _reward, boolean _isFinal) {
        IntArrayOfDoubleHashMap[] basisFunctions1 = getBasisFunctionsFromFeatures(_feature1);
        IntArrayOfDoubleHashMap[] basisFunctions2 = getBasisFunctionsFromFeatures(_feature2);

        Trajectory _trajectory = new Trajectory(_isFinal, basisFunctions1, basisFunctions2);

        if (_reward < 0.0) {
            observedTrajectories[0][currentTajectoryIndex[0]] = _trajectory;
            if (currentTajectoryIndex[0] < numberOfTrajectoriesPerLayer - 1) {
                currentTajectoryIndex[0] += 1;
            } else {
                currentTajectoryIndex[0] = 0;
            }
        } else if (_reward == 0.0) {
            observedTrajectories[1][currentTajectoryIndex[1]] = _trajectory;
            if (currentTajectoryIndex[1] < numberOfTrajectoriesPerLayer - 1) {
                currentTajectoryIndex[1] += 1;
            } else {
                currentTajectoryIndex[1] = 0;
            }
        } else {
            observedTrajectories[2][currentTajectoryIndex[2]] = _trajectory;
            if (currentTajectoryIndex[2] < numberOfTrajectoriesPerLayer - 1) {
                currentTajectoryIndex[2] += 1;
            } else {
                currentTajectoryIndex[2] = 0;
            }
        }
    }

    public void addTransition(IntDoubleHashMap[] _feature1, int _action, IntDoubleHashMap[] _feature2, double _reward, boolean _isFinal) {
        IntArrayOfDoubleHashMap[] basisFunctions1 = getBasisFunctionsFromFeatures(_feature1);
        IntArrayOfDoubleHashMap[] basisFunctions2 = new IntArrayOfDoubleHashMap[nbBasisPerType];
//                getBasisFunctionsFromFeatures(_feature2);

        Transition _transition = new Transition(_feature1, _action, _feature2, _reward, _isFinal, basisFunctions1, basisFunctions2);

        if(recordLog){
            addTransitionToString(_transition);
        }

        if (_reward < 0.0) {
            observedTransitions[0][currentTransitionIndex[0]] = _transition;
            if (currentTransitionIndex[0] < numberOfTransitionsStoredPerLayer - 1) {
                currentTransitionIndex[0] += 1;
            } else {
                currentTransitionIndex[0] = 0;
            }
        } else if (_reward == 0.0) {
            observedTransitions[1][currentTransitionIndex[1]] = _transition;
            if (currentTransitionIndex[1] < numberOfTransitionsStoredPerLayer - 1) {
                currentTransitionIndex[1] += 1;
            } else {
                currentTransitionIndex[1] = 0;
            }
        } else {
            observedTransitions[2][currentTransitionIndex[2]] = _transition;
            if (currentTransitionIndex[2] < numberOfTransitionsStoredPerLayer - 1) {
                currentTransitionIndex[2] += 1;
            } else {
                currentTransitionIndex[2] = 0;
            }
        }
    }

    public IntArrayOfDoubleHashMap[] approximateGradient(IntArrayOfDoubleHashMap[] theta, double epsilon) {
        IntArrayOfDoubleHashMap[] gradient = new IntArrayOfDoubleHashMap[theta.length];
        IntArrayOfDoubleHashMap[] thetaPlus = new IntArrayOfDoubleHashMap[theta.length];
        IntArrayOfDoubleHashMap[] thetaMinus = new IntArrayOfDoubleHashMap[theta.length];

        for (int i = 0; i < theta.length; i++) {
            gradient[i] = new IntArrayOfDoubleHashMap(theta[i]);
            thetaPlus[i] = new IntArrayOfDoubleHashMap(theta[i]);
            thetaMinus[i] = new IntArrayOfDoubleHashMap(theta[i]);
        }

        for (int i = 0; i < theta.length; i++) {
            for (Integer key : theta[i].keySet()) {
                double[] vector = new double[theta[i].get(key).length];
                for (int j = 0; j < theta[i].get(key).length; j++) {
                    thetaPlus[i].get(key)[j] = thetaPlus[i].get(key)[j] + epsilon;
                    thetaMinus[i].get(key)[j] = thetaMinus[i].get(key)[j] - epsilon;

                    vector[j] = (batchSquaredError(thetaPlus) - batchSquaredError(thetaMinus)) / (2.0 * epsilon);

                    thetaPlus[i].get(key)[j] = thetaPlus[i].get(key)[j] - epsilon;
                    thetaMinus[i].get(key)[j] = thetaMinus[i].get(key)[j] + epsilon;
                }
                gradient[i].put(key, vector);
            }
        }
        return gradient;
    }

    private double batchSquaredError(IntArrayOfDoubleHashMap[] theta) {
//        initializing variables
        double cost = 0.;
        Transition currentTransition;
        IntArrayOfDoubleHashMap[] basisFunctions1;
        IntArrayOfDoubleHashMap[] basisFunctions2;

        double observedValue;
        double belief;
        double error;
        double[] cumulatedErrorsPerLayer = new double[numberOfLayers];

        int[][] miniBatchIndexes = new int[numberOfLayers][];
        int[] actualNumberOfTransitionsPerLayer = new int[numberOfLayers];
        int a;
        int m;
        Random randomGenerator = new Random();

        double[] rewards = new double[numberOfLayers];
        rewards[0] = -1.0;
        rewards[1] = 0.0;
        rewards[2] = 1.0;

        int[] countTransitions = new int[numberOfLayers];

        for (int l = 0; l < numberOfLayers; l++) {
            m = 0;
            while ((m < numberOfTransitionsStoredPerLayer) && (observedTransitions[l][m] != null)) {
                m++;
            }
            actualNumberOfTransitionsPerLayer[l] = m;
            miniBatchIndexes[l] = new int[Math.min(miniBatchSize, actualNumberOfTransitionsPerLayer[l])];
            for (int b = miniBatchIndexes[l].length - 1; b > -1; b--) {
                miniBatchIndexes[l][b] = b;
            }
            for (int b = miniBatchIndexes[l].length - 1; b > -1; b--) {
                a = randomGenerator.nextInt(b + 1);
                miniBatchIndexes[l][b] = miniBatchIndexes[l][a];
                miniBatchIndexes[l][a] = b;
            }

            for (int k = 0; k < miniBatchIndexes[l].length; k++) {
                m = miniBatchIndexes[l][k];
                countTransitions[l] += 1;
                currentTransition = observedTransitions[l][m];
                basisFunctions1 = currentTransition.basisFunctionValues1;
                basisFunctions2 = currentTransition.basisFunctionValues2;

                belief = getBasisFunctionLinearApproximation(basisFunctions1, theta);
                observedValue = rewards[l] + discountFactor * getBasisFunctionLinearApproximation(basisFunctions2, theta);
                if (currentTransition.isFinal) {
                    observedValue = rewards[l];
                }
                error = belief - observedValue;
                cumulatedErrorsPerLayer[l] += Math.pow(error, 2.0);
            }
        }

        double[] layerWeights = new double[numberOfLayers];

        for (int l = 0; l < numberOfLayers; l++) {
            if (countTransitions[l] > 0) {
                layerWeights[l] = 1.0 / ((double) numberOfLayers * countTransitions[l]);
                cost += layerWeights[l] * cumulatedErrorsPerLayer[l];
            }
        }

        return cost;
    }

    public IntArrayOfDoubleHashMap[] getBasisFunctionsFromFeatures(IntDoubleHashMap[] _features) {
        IntArrayOfDoubleHashMap[] result = new IntArrayOfDoubleHashMap[nbWeights];
        for (int i = 0; i < nbWeights; i++) {
            result[i] = new IntArrayOfDoubleHashMap();
            for (Integer type : _features[i].keySet()) {
                double[] basisFunctionsForThisType = new double[nbBasisPerType];
                for (int j = 0; j < nbBasisPerType; j++) {
//                    basisFunctionsForThisType[j] = logisticFunction(_features[i].get(type), offsets[j], logisticSlope);
//                    basisFunctionsForThisType[j] = alpha[j] + beta[j]* stumpFunction(_features[i].get(type) - thresholds[j]);
//                      basisFunctionsForThisType[j] = impulseFunction(_features[i].get(type), thresholds[j], thresholds[j+1]);
                    basisFunctionsForThisType[j] = _features[i].get(type);
//                    basisFunctionsForThisType[j] = Math.pow(_features[i].get(type), 6.0);
//                    basisFunctionsForThisType[j] = pyramidFunction(_features[i].get(type) - thresholds[j]);
                }
                result[i].put(type, basisFunctionsForThisType);
            }
        }
        return result;
    }


    public double getBasisFunctionLinearApproximation(IntArrayOfDoubleHashMap[] _basisFunctions, IntArrayOfDoubleHashMap[] theta) {
        double result = 0.0;
        for (int i = 0; i < _basisFunctions.length; i++) {
            for (Integer type : _basisFunctions[i].keySet()) {
                for (int j = 0; j < theta[i].get(type).length; j++) {
                    result += theta[i].get(type)[j] * _basisFunctions[i].get(type)[j];
                }
            }
        }
        return (result);
    }

    double getKNNValue(IntDoubleHashMap[] _features) {
        double result = 0.0;
        double[] neighborsValue = new double[nbNeighbors];
        double[] neighborsDistances = new double[nbNeighbors];
        int nextBossIndex;
        int actualNbNeighbors = 0;

        boolean challenge;
        IntDoubleHashMap[] tempFeatures;
        double tempValue;
        int layerIndex = 0;
        int intraLayerIndex = 0;

        while (layerIndex < observedTransitions.length) { //for each layer of observations
            while ((intraLayerIndex < 20) && (observedTransitions[layerIndex][intraLayerIndex] != null)) {
//            while((intraLayerIndex<observedTransitions[layerIndex].length)&&(observedTransitions[layerIndex][intraLayerIndex]!=null)){  //for each observation in that layer
                tempFeatures = observedTransitions[layerIndex][intraLayerIndex].features1;  //compare this observation's features with the current top K closest
                tempValue = observedTransitions[layerIndex][intraLayerIndex].reward;

                challenge = true;
                nextBossIndex = actualNbNeighbors - 1;
                double tempDistance = getDistance(_features, tempFeatures);
                while (challenge) {  //start with the Kth closest, then K-1, etc
                    if ((nextBossIndex < 0) || (tempDistance > neighborsDistances[nextBossIndex])) { //if we reach the closest neighbor or if we don't beat the current one, insert here
                        challenge = false;
                        for (int i = nbNeighbors - 1; i > nextBossIndex + 1; i--) {
                            neighborsDistances[i] = neighborsDistances[i - 1];
                            neighborsValue[i] = neighborsValue[i - 1];
                        }

                        if (nextBossIndex < nbNeighbors - 1) {
                            neighborsDistances[nextBossIndex + 1] = tempDistance;
                            neighborsValue[nextBossIndex + 1] = tempValue;
                        }

                        if (actualNbNeighbors < nbNeighbors) {
                            actualNbNeighbors += 1;
                        }
                    } else {  //if closer than current challenger, then move one rank up
                        nextBossIndex -= 1;
                    }
                }
                intraLayerIndex++;
            }
            layerIndex++;
            intraLayerIndex = 0;
        }

        int i = 0;
        while (i < actualNbNeighbors) {
            result += neighborsValue[i];
            i++;
        }
        return result / (double) i;
    }

    public void updateBasisFunctionRegressionUsingDatabase() {
        IntArrayOfDoubleHashMap[] deltaTheta = analyticGradient(weights);
//        IntArrayOfDoubleHashMap[] approxGradient = approximateGradient(weights, 0.00001);
        int justdebug = 1;

        for (int i = 0; i < nbWeights; i++) {
            for (Integer type : weights[i].keySet()) {
                for (int j = 0; j < nbBasisPerType; j++) {
                    weights[i].get(type)[j] = weights[i].get(type)[j] + deltaTheta[i].get(type)[j];
                }
            }
        }

        for (int k = 0; k < nbBasisPerType; k++) {
//            weights[2].get(0)[k] = locationBiasWeight;
            weights[7].get(0)[k] = 0.0;
        }
    }


    private double modelCostForOneObservation(IntArrayOfDoubleHashMap[] theta, Transition _transition) {
        double cost;

        double prediction = getBasisFunctionLinearApproximation(_transition.basisFunctionValues1, theta);
        double label = _transition.reward;
        if (label < 0.0) {
            label = -1.0;
        } else if (label > 0.0) {
            label = 1.0;
        } else {
            label = 0.0;
        }
        cost = 0.5 * Math.pow(prediction - label, 2.0);

        return cost;
    }

    public void updateBasisFunctionRegressionUsingTrajectoryDatabase() {
        Trajectory currentTrajectory;

        IntArrayOfDoubleHashMap[] basisFunctions1;
        IntArrayOfDoubleHashMap[] basisFunctions2;

        IntArrayOfDoubleHashMap[][] gradientW = new IntArrayOfDoubleHashMap[nbWeights][numberOfLayers];
        for (int i = 0; i < nbWeights; i++) {
            for (int j = 0; j < numberOfLayers; j++) {
                gradientW[i][j] = new IntArrayOfDoubleHashMap();
            }
        }

        double observedValue;
        double belief;
        double error;

        int[][] miniBatchIndexes = new int[numberOfLayers][];
        int[] actualNumberOfTransitionsPerLayer = new int[numberOfLayers];
        int a;
        int m;
        Random randomGenerator = new Random();

        int nbTransitions = 0;

        double[] rewards = new double[numberOfLayers];
        rewards[0] = -1.0;
        rewards[1] = 0.0;
        rewards[2] = 1.0;

        int[] countTransitions = new int[numberOfLayers];

        for (int l = 0; l < numberOfLayers; l++) {
            m = 0;
            while ((m < numberOfTrajectoriesPerLayer) && (observedTrajectories[l][m] != null)) {
                m++;
            }
            actualNumberOfTransitionsPerLayer[l] = m;
            miniBatchIndexes[l] = new int[Math.min(miniBatchSize, actualNumberOfTransitionsPerLayer[l])];
            for (int b = miniBatchIndexes[l].length - 1; b > -1; b--) {
                miniBatchIndexes[l][b] = b;
            }
            for (int b = miniBatchIndexes[l].length - 1; b > -1; b--) {
                a = randomGenerator.nextInt(b + 1);
                miniBatchIndexes[l][b] = miniBatchIndexes[l][a];
                miniBatchIndexes[l][a] = b;
            }

            for (int k = 0; k < miniBatchIndexes[l].length; k++) {
                m = miniBatchIndexes[l][k];
                nbTransitions += 1;
                countTransitions[l] += 1;
                currentTrajectory = observedTrajectories[l][m];
                basisFunctions1 = currentTrajectory.basisFunctionValues1;
                basisFunctions2 = currentTrajectory.basisFunctionValues2;

                belief = getBasisFunctionLinearApproximation(basisFunctions1, weights);
                observedValue = rewards[l] + 0.7 * getBasisFunctionLinearApproximation(basisFunctions2, weights);
                if (currentTrajectory.isFinal) {
//                if (true) {
                    observedValue = rewards[l];
                }
                error = belief - observedValue;

                for (int i = 0; i < nbWeights; i++) {
                    for (Integer type : basisFunctions1[i].keySet()) {
//                        if(basisFunctions2[i].containsKey(type)){
                        if (true) {
                            double[] newGradient = new double[nbBasisPerType];
                            for (int j = 0; j < nbBasisPerType; j++) {
                                newGradient[j] = learningRate * (error * (basisFunctions1[i].get(type)[j]));
                            }
                            if (!gradientW[i][l].containsKey(type)) {
                                gradientW[i][l].put(type, newGradient);
                            } else {
                                for (int j = 0; j < nbBasisPerType; j++) {
                                    gradientW[i][l].get(type)[j] += newGradient[j];
                                }
                            }
                        }
                    }
                }
            }
        }

        double[] gradientWeights = new double[numberOfLayers];

        for (int l = 0; l < numberOfLayers; l++) {
            if (countTransitions[l] > 0) {
//                gradientWeights[l] = (double) nbTransitions / ((double) numberOfLayers * (double) countTransitions[l]);
                gradientWeights[l] = 1.0 / ((double) countTransitions[l]);
            }
        }

        for (int i = 0; i < nbWeights; i++) {
            for (Integer type : weightsTraj[i].keySet()) {
                double[] newWeights = new double[nbBasisPerType];
                for (int j = 0; j < nbBasisPerType; j++) {
                    newWeights[j] = (1.0 - regularizationTerm) * weightsTraj[i].get(type)[j];
                    for (int l = 0; l < numberOfLayers; l++) {
                        if (gradientW[i][l].containsKey(type)) {
                            newWeights[j] -= gradientWeights[l] * gradientW[i][l].get(type)[j];
                        }
                    }
                }
                for (int j = 0; j < nbBasisPerType; j++) {
                    weightsTraj[i].get(type)[j] = newWeights[j];
                }

            }
        }

        for (int k = 0; k < nbBasisPerType; k++) {
            weightsTraj[0].get(0)[k] = 0.0;
        }
    }

    public void updateTreeAttributes(IntDoubleHashMap[] _features) {

        for (int i = 0; i < _features.length; i++) {
            for (Integer type : _features[i].keySet()) {
                if (!weights[i].containsKey(type)) {
                    double[] newWeightVector = new double[nbBasisPerType];
                    for (int j = 0; j < newWeightVector.length; j++) {
                        newWeightVector[j] = initialWeightValue;
                    }
//                    newWeightVector[0]=0.1;
                    weights[i].put(type, newWeightVector);
                }

                if (!weightsTraj[i].containsKey(type)) {
                    double[] newWeightVector = new double[nbBasisPerType];
                    for (int j = 0; j < newWeightVector.length; j++) {
                        newWeightVector[j] = 0.1;
                    }
//                    newWeightVector[0]=0.1;
                    weightsTraj[i].put(type, newWeightVector);
                }
            }
        }
    }

    private void addTransitionToString(Transition _transition) {
        IntDoubleHashMap[] f1 = _transition.features1;
        Double r = _transition.reward;
        int a = _transition.action;

        StringBuilder builder = new StringBuilder();

        builder.append(r.toString()).append(',');
        builder.append(String.valueOf(a)).append(',');

        int i;
        boolean foundType;
        boolean firstWriteOnFile = true;

        for (Integer itype : listOfVisitedITypes) {
            i = 0;
            foundType = false;
            while ((i < f1.length) && (!foundType)) {
                if (f1[i].containsKey(itype)) {
                    if (firstWriteOnFile) {
                        builder.append(f1[i].get(itype).toString());
                        firstWriteOnFile = false;
                    } else {
                        builder.append(',').append(f1[i].get(itype).toString());
                    }
                    foundType = true;
                }
                i++;
            }
            if (!foundType) {
                if (firstWriteOnFile) {
                    builder.append(",");
                    firstWriteOnFile = false;
                } else {
                    builder.append(",");
                }

            }
        }

        i = 0;
        while (i < f1.length) {
            for (Integer key : f1[i].keySet()) {
                if (!(listOfVisitedITypes.contains(key))) {
//                    System.out.println("adding a column to list of visited types \n");
                    listOfVisitedITypes.add(key);
                    if (firstWriteOnFile) {
                        builder.append(f1[i].get(key).toString());
                        firstWriteOnFile = false;
                    } else {
                        builder.append(',').append(f1[i].get(key).toString());
                    }
                }
            }
            i++;
        }
        builder.append('\n');

        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter("./transitions_log.csv", true));
            bw.write(builder.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            bw = new BufferedWriter(new FileWriter("./header_log.csv", true));
            bw.write("reward,action");
            for (Integer itype : listOfVisitedITypes) {
                bw.write(',' + itype.toString());
            }
            bw.write("\n");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getDistance(IntDoubleHashMap[] _f1, IntDoubleHashMap[] _f2) {
        double result = 0.0;
        double nbElements = 0.0;

        for (int i = 0; i < _f1.length; i++) {
            for (Integer type : _f1[i].keySet()) {
                if (_f2[i].containsKey(type)) {
                    result += 0.5 * Math.pow(_f1[i].get(type) - _f2[i].get(type), 2);
                    nbElements += 0.5;
                } else {
                    result += 1.0;
                    nbElements += 1.0;
                }
            }
            for (Integer type : _f2[i].keySet()) {
                if (_f1[i].containsKey(type)) {
                    result += 0.5 * Math.pow(_f1[i].get(type) - _f2[i].get(type), 2);
                    nbElements += 0.5;
                } else {
                    result += 1.0;
                    nbElements += 1.0;
                }
            }
        }
        return Math.sqrt(result) / nbElements;
    }
}
