/**
 * Genetic algorithm:
 * - One population of action sequences
 * - Each parent selected from two randomly chosen individuals
 * - One point crossover and mutation
 * - Children replace the parents when they have a higher fitness
 * - Fitness score consists of the game score, the tile value calculated by the objective controller and a bonus to certain events, resources and wins/losses
 * - Choosing optimal action:
 * 	* Deterministic: Action sequence with the highest score
 * 	* Not deterministic: Combination of average scores for each action and their highest score
 * 	* First action of the max action sequence will be removed and a random action will be added at the end
 * 
 * @author Florian Kirchgessner (Alias: Number27)
 * @version customGA4
 */

package agents.Number27;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class GA extends CustomAbstractPlayer{
	private static int SIMULATION_DEPTH = 7;
	private final int POPULATION_SIZE = 100;
	private final double RECPROB_PROB = 0.95;
	private final double MUTATION_PROB = (1.0 / SIMULATION_DEPTH);
	private final long BREAK_MS = 2;
	private final double GAMMA = 0.90;
	private final int N_ACTIONS;
    private final int ACTION_SEQU_ID_MIN = (int)Math.pow(10, SIMULATION_DEPTH - 1);
	private final double GAME_OVER_PENALTY = 100000;
	private final double EVENT_BONUS = 0.1;
	private final double RESOURCE_BONUS = 0.1;
	private final double[] MAX_DECREASE = new double[]{0.5, 0.75, 0.875, 1, 1, 1, 1, 1, 1};
	
	private static HashMap<Integer, Types.ACTIONS> LOOKUP_INT_ACTION;	// Mapped from 1 to N_ACTIONS for unique ActionSequenceIds
	private static HashMap<Types.ACTIONS, Integer> LOOKUP_ACTION_INT;
	private static double SCORE_DEGREDATION[];							// Amount of score degradation with simulation depth
	private static int POWERS_OF_TEN[];
	
	private int population[][];											// 0..SIMULATION_DEPTH-1:Actions, SIMULATION_DEPTH:ActionSequenceId
	private HashMap<Integer, double[]> populationScores;				// 0:Average score, 1:Times visited
	
	private ObjectiveController objectiveController;
	private double maxFactor;											// Max score percentage vs average score percentage when choosing the optimal action
	private Random random;
	private StateObservation previousState;
	private ElapsedCpuTimer timer;
	
	private int numFrames;
	private int numGenerations;
	
	
	public GA(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		random = new Random();
		
		objectiveController = new ObjectiveController(stateObs);
		
		// One axis
		if(!objectiveController.isConsiderXAxis() || !objectiveController.isConsiderYAxis()) {
			SIMULATION_DEPTH = 9;
		}

		// Generate Lookup tables
        LOOKUP_INT_ACTION = new HashMap<Integer, Types.ACTIONS>();
        LOOKUP_ACTION_INT = new HashMap<Types.ACTIONS, Integer>();
        int i = 1;
        for(Types.ACTIONS action : stateObs.getAvailableActions()) {
            LOOKUP_INT_ACTION.put(i, action);
            LOOKUP_ACTION_INT.put(action, i);
            i++;
        }

        int mult = 1;
        SCORE_DEGREDATION = new double[SIMULATION_DEPTH];
        POWERS_OF_TEN = new int[SIMULATION_DEPTH];
        for(i = 0; i< SIMULATION_DEPTH; i++) {
        	SCORE_DEGREDATION[i] = Math.pow(GAMMA, i);
        	POWERS_OF_TEN[i] = mult;
        	mult *= 10;
        }
        
        N_ACTIONS = stateObs.getAvailableActions().size();
        maxFactor = 1;
        
        numFrames = 0;
        numGenerations = 0;
        
        previousState = stateObs.copy();

        initPopulation(stateObs);
    }
	
	
	private void initPopulation(StateObservation stateObs) {
		population = new int[POPULATION_SIZE][SIMULATION_DEPTH + 1];
    	
    	// Randomize initial genome
    	for (int i = 0; i < POPULATION_SIZE; i++) {
    		for (int j = 0; j < SIMULATION_DEPTH; j++) {
    			population[i][j] = getRandomActionId();
    		}
    		population[i][SIMULATION_DEPTH] = generateActionSequenceId(population[i]);
    	}
    }
	
	
	public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.timer = elapsedTimer;
        
        // Reset action sequence scores
        populationScores = new HashMap<Integer, double[]>();
        
        // Update objectives
        objectiveController.update(stateObs, previousState);

        // Main loop
        analysePossibilities(stateObs);
        
        previousState = stateObs.copy();
        numFrames++;

        return getOptimalAction();
    }
	
	
	private void analysePossibilities(StateObservation stateObs) {
		long remaining = timer.remainingTimeMillis();
        while(remaining >= BREAK_MS) {
			try {
				nextGeneration(stateObs);
			} catch (TimeoutException e) {
				return;
			}
            
            remaining = timer.remainingTimeMillis();
        }
    }
	
	
	private void nextGeneration(StateObservation stateObs) throws TimeoutException {
        int a, b;
        int actSequIdA, actSequIdB;
        int i;
        int[][] parents = new int[2][SIMULATION_DEPTH + 1];
        int children[][];

        // Selection
        a = selectParent(stateObs);
        do {
            b = selectParent(stateObs);
        } while (a == b);
        actSequIdA = population[a][SIMULATION_DEPTH];
        actSequIdB = population[b][SIMULATION_DEPTH];
        
        for(i = 0; i < SIMULATION_DEPTH; i++) {
        	parents[0][i] = population[a][i];
        	parents[1][i] = population[b][i];
        }

        // Recombination
        if (random.nextDouble() < RECPROB_PROB) {
        	children = onePointCrossover(parents[0], parents[1]);
        }
        else {
        	children = new int[2][];
        	children[0] = parents[0];
        	children[1] = parents[1];
        }
        
        // Mutation
        children[0] = mutate(children[0]);
        children[1] = mutate(children[1]);
        
        // Incorporate children
        double childScores[] = new double[2];
        childScores[0] = simulate(stateObs, children[0]);
        childScores[1] = simulate(stateObs, children[1]);
        
        if(childScores[0] >= populationScores.get(actSequIdA)[0]) {
        	population[a] = children[0];
        	children[0][SIMULATION_DEPTH] = generateActionSequenceId(children[0]);
        }
        if(childScores[1] >= populationScores.get(actSequIdB)[0]) {
        	population[b] = children[1];
        	children[1][SIMULATION_DEPTH] = generateActionSequenceId(children[1]);
        }
        
        numGenerations++;
    }
    
    
    int selectParent(StateObservation stateObs) throws TimeoutException {
    	// First parent candidate
		int index1 = random.nextInt(POPULATION_SIZE);
		int actionSequ1 = population[index1][SIMULATION_DEPTH];
		
		// Simulate unknown action sequence
		if(!populationScores.containsKey(actionSequ1)) {
			simulate(stateObs, population[index1]);
		}
		
		// Second parent candidate
		int index2 = random.nextInt(POPULATION_SIZE);
		int actionSequ2 = population[index2][SIMULATION_DEPTH];
		
		// Simulate unknown action sequence
		if(!populationScores.containsKey(actionSequ2)) {
			simulate(stateObs, population[index2]);
		}
		
		// Use best action sequence
		if(populationScores.get(actionSequ1)[0] < populationScores.get(actionSequ2)[0]) {
			index1 = index2;
		}
		
		return index1;
	}
    
    
    int[][] onePointCrossover(int[] parent1, int[] parent2) {
    	int crossoverPoint = random.nextInt(SIMULATION_DEPTH);
		int[][] children = new int[2][];

		for(int i=0; i<SIMULATION_DEPTH; i++) {
			if(i > crossoverPoint) {
				int tmp = parent1[i];
				parent1[i] = parent2[i];
				parent2[i] = tmp;
			}
		}
		
		children[0] = parent1;
		children[1] = parent2;
		return children;
	}
    
    
    int[] mutate(int[] gen) {
    	for (int i = 0; i < SIMULATION_DEPTH; i++) {
            if (random.nextDouble() < MUTATION_PROB) gen[i] = getRandomActionId();
        }
    	return gen;
    }


    private double simulate(StateObservation stateObs, int[] policy) throws TimeoutException {
        if (timer.remainingTimeMillis() < BREAK_MS) {
            throw new TimeoutException("Timeout");
        }
        
        StateObservation stCopy = stateObs.copy();

        int actionSequence = 0;
        Vector2d avatarPos;
        Vector2d prevPos = stCopy.getAvatarPosition().copy();
        double prevResCount = getResourceCount(stCopy.getAvatarResources());
        double currResCount = 0;
        double resCount = 0;
        double prevStateScore = stCopy.getGameScore();
        double score = 0;
        
        // Simulate
        for (int depth = 0; depth < SIMULATION_DEPTH; depth++) {
        	// Get action
            Types.ACTIONS action = LOOKUP_INT_ACTION.get(policy[depth]);
            actionSequence += policy[depth] * POWERS_OF_TEN[depth];
            
            // Advance state
            stCopy.advance(action);
            avatarPos = stCopy.getAvatarPosition();
            currResCount = getResourceCount(stCopy.getAvatarResources());
            
            // Resource difference
            resCount = currResCount - prevResCount;
            // No penalty for resource loss
            if(resCount < 0)
            	resCount = 0;
            
            // Update objective
            double eventCount = objectiveController.checkObjectives(stCopy, prevStateScore, prevPos, false);
            
            // Game over
            if (stCopy.isGameOver()) {
            	score += evaluateState(stCopy, depth, eventCount, resCount);
            	
            	if(stCopy.getGameWinner() == Types.WINNER.PLAYER_WINS) {
            		double rawScore = stCopy.getGameScore();
            		for (int d = depth + 1; d < SIMULATION_DEPTH; d++) {
            			score += SCORE_DEGREDATION[d] * (rawScore + EVENT_BONUS);
            		}
            	}
            	
            	checkActionSequence(actionSequence, score, depth);
            	checkActionSequence(generateActionSequenceId(policy), score, depth);
            	
            	break;
            }
            
            // Calculate score
            score += evaluateState(stCopy, depth, eventCount, resCount) + objectiveController.getTileValue(avatarPos);
            
            // Update score
            checkActionSequence(actionSequence, score, depth);
            
            prevStateScore = stCopy.getGameScore();
            prevResCount = currResCount;
            prevPos.x = avatarPos.x;
            prevPos.y = avatarPos.y;
        }
        
        return score;
    }
    
    
    private void checkActionSequence(int actionSequence, double score, int depth) {
    	double[] scoreEntry = populationScores.get(actionSequence);
    	
    	// Unknown action sequence
    	if(scoreEntry == null) {
    		scoreEntry = new double[]{score, 1};
        	populationScores.put(actionSequence, scoreEntry);
        }
    	// Unexpected state score
    	else {
    		if(scoreEntry[0] != score) {
    			scoreEntry[0] = (scoreEntry[0] * scoreEntry[1] + score) / (scoreEntry[1] + 1);
    			maxFactor *= MAX_DECREASE[depth];
    		}
    		
    		scoreEntry[1]++;
    	}
    }
    
    
    public double evaluateState(StateObservation stateObs, int depth, double eventCount, double resourceCount) {
        boolean gameOver = stateObs.isGameOver();
        double rawScore = stateObs.getGameScore();
        
        if(gameOver) {
        	Types.WINNER win = stateObs.getGameWinner();
        	
        	if(win == Types.WINNER.PLAYER_LOSES)
        		return SCORE_DEGREDATION[depth] * (rawScore + eventCount * EVENT_BONUS + resourceCount * RESOURCE_BONUS - GAME_OVER_PENALTY);
        	
        	else if(win == Types.WINNER.PLAYER_WINS)
        		return SCORE_DEGREDATION[depth] * (rawScore + eventCount * EVENT_BONUS + resourceCount * RESOURCE_BONUS) + GAME_OVER_PENALTY;
        }

        return SCORE_DEGREDATION[depth] * (rawScore + eventCount * EVENT_BONUS + resourceCount * RESOURCE_BONUS);
    }
    
    
    private int getResourceCount(HashMap<Integer, Integer> resources) {
    	if(resources.size() == 0)
    		return 0;
    	
    	int count = 0;
    	for(int value : resources.values()) {
    		count += value;
    	}
    	return count;
    }
	
    
    private int generateActionSequenceId(int[] actionSequ) {
		int seq = 0;
    	for(int i = 0; i < SIMULATION_DEPTH; i++) {
    		seq += actionSequ[i] * POWERS_OF_TEN[i];
    	}
    	return seq;
    }
    
    
    private int getRandomActionId() {
    	return random.nextInt(N_ACTIONS) + 1;
    }
    
	
	private Types.ACTIONS getOptimalAction() {
		int optAction = 1;
		
		// Deterministic state simulations
		if(maxFactor == 1) {
			Iterator<Entry<Integer, double[]>> iter = populationScores.entrySet().iterator();
			double maxScore = Double.NEGATIVE_INFINITY;
			int actionSequence = 1;
			
			// Get action sequence with max score
			while(iter.hasNext()) {
				Entry<Integer, double[]> indiv = iter.next();
				if(indiv.getKey() >= ACTION_SEQU_ID_MIN && indiv.getValue()[0] > maxScore) {
					maxScore = indiv.getValue()[0];
					actionSequence = indiv.getKey();
				}
			}
			
			optAction = actionSequence % 10;
		}
		// Random object movements
		else {
			Iterator<Entry<Integer, double[]>> iter = populationScores.entrySet().iterator();
			int[] maxActionSeq = new int[N_ACTIONS];
			double[] maxScores = new double[N_ACTIONS];
			double[] avgScores = new double[N_ACTIONS];
			int[] actionCount = new int[N_ACTIONS];
			
			for(int i = 0; i < N_ACTIONS; i++) {
				maxScores[i] = Double.NEGATIVE_INFINITY;
				maxActionSeq[i] = 0;
				avgScores[i] = 0;
				actionCount[i] = 0;
			}
			
			// Get data
			while(iter.hasNext()) {
				Entry<Integer, double[]> indiv = iter.next();
				int actionSequence = indiv.getKey();
				int action = (actionSequence % 10) - 1;
				double[] score = indiv.getValue();
				
				avgScores[action] += score[0] * score[1];
				actionCount[action] += score[1];
				
				if(actionSequence >= ACTION_SEQU_ID_MIN && score[0] > maxScores[action]) {
					maxScores[action] = score[0];
					maxActionSeq[action] = actionSequence;
				}
			}
			
			// Combine the average and max scores for each action and select the action with the max total score
			double maxTotalScore = Double.NEGATIVE_INFINITY;
			for(int i = 0; i < N_ACTIONS; i++) {
				avgScores[i] = (avgScores[i] / actionCount[i]) * (1 - maxFactor) + maxScores[i] * maxFactor;
				
				if(avgScores[i] > maxTotalScore) {
					maxTotalScore = avgScores[i];
					optAction = i + 1;
				}
			}
			
			// Reset max factor
			maxFactor = 1;
		}
		
		cullActionType(optAction);
		
		return LOOKUP_INT_ACTION.get(optAction);
	}
	
	
	private void cullActionType(int action) {
		for(int i = 0; i < POPULATION_SIZE; i++) {
			// Find the action sequence
			if(population[i][0] == action) {
				// Remove the first action and add a random one at the end
				for(int j = 0; j < SIMULATION_DEPTH - 1; j++) {
					population[i][j] = population[i][j+1];
				}
				
				population[i][SIMULATION_DEPTH - 1] = random.nextInt(N_ACTIONS) + 1;
				population[i][SIMULATION_DEPTH] = generateActionSequenceId(population[i]);
			}
		}
	}
    
	
	public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer)
	{
	}
    
	
    public boolean switchController() {
		return false;
	}
}