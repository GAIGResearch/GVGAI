package agents.SJA862;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import java.util.ArrayList;

import java.util.Random;

public class MinMaxNode 
{ 
	public MinMaxNode parent;//
	public int depth;
	public StateObservation state;//
	public double maxValue;
	public double myValue;//
	public Vector2d myPos;//
	public double distance;//
	public double maxDistance;//
	public int rollout_depth = 2;//
	public int simulation_depth = 10;
	//testing
	public int levelOfCaution = 5;
	public Random r;//
	public MinMaxNode[] children;//
	public boolean alternate_on = false;
	public boolean been_simulated = false;
	//
	public double greatestDistance = 0;
	
	
	public static int WIN_BONUS = 1000000;
	public static int LOSE_PENALTY = -1000000;
	
	
	public MinMaxNode (StateObservation state, MinMaxNode parent, Random r, Vector2d originalPos) {
		this.state = state;
		this.r = r;
		children = new MinMaxNode[Agent.NUM_ACTIONS];
		myPos = this.state.getAvatarPosition();
		myValue = this.state.getGameScore();
		
		if (parent == null) {depth = 0;}
		else{
			this.parent = parent; 
			distance = myPos.dist(originalPos);
			if (this.state.isGameOver()){
				Types.WINNER win = this.state.getGameWinner();
				if (win == Types.WINNER.PLAYER_LOSES){
					distance += LOSE_PENALTY;
					myValue += LOSE_PENALTY;
				}	
				if (win == Types.WINNER.PLAYER_WINS) {
					distance += WIN_BONUS;
					myValue += WIN_BONUS;
				}
			}
			maxValue = myValue;
			maxDistance = distance;
			if ((this.myValue > parent.maxValue) || (this.myValue == parent.maxValue && r.nextInt(2) == 1)) {parent.maxValue = this.myValue;}
			if ((this.distance > parent.maxDistance)|| (this.distance == parent.maxDistance && r.nextInt(2) == 1)) {parent.maxDistance = this.distance;}
			depth = parent.depth +1;
		}
	}
	
	public int search (ElapsedCpuTimer elapsedTimer, Vector2d originalPos) {
		if (originalPos == null){originalPos = myPos;}
		double avgTimeTaken = 0;
        double acumTimeTaken = 0;
		int numIters = 0; 
		long remaining = elapsedTimer.remainingTimeMillis();
		int remainingLimit = 5;
		while ( remaining > 2*avgTimeTaken && remaining > remainingLimit && this.notFullyExpanded()){
			ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
			int action = getRandomChild();
			StateObservation newState = state.copy();
			newState.advance(Agent.actions[action]);
			MinMaxNode newNode = new MinMaxNode (newState, this, r, originalPos);
			children[action] = newNode;
			newNode.expand(originalPos);
			numIters++;
			acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
			avgTimeTaken  = acumTimeTaken/numIters;
			remaining = elapsedTimer.remainingTimeMillis();
		}
		int action = selectBestOption(elapsedTimer);
		myPos = originalPos;
		mutate();
		return action;
	}
				
	public void mutate () {
		int addorsubtract = r.nextInt(2);
		int multiplier = r.nextInt(2);
		if (addorsubtract == 1) {myPos.x += multiplier * 30.0;}
		if (addorsubtract == 0) {myPos.x -= multiplier * 30.0;}
		addorsubtract = r.nextInt(2);
		multiplier = r.nextInt(2);
		if (addorsubtract == 1) {myPos.y += multiplier * 30.0;}
		if (addorsubtract == 0) {myPos.y -= multiplier * 30.0;}
	}
	public void caution (MinMaxNode node) {
		StateObservation simulState = node.state.copy();
		int i = 0;
		while (i < levelOfCaution && !simulState.isGameOver()){
			simulState.advance(Agent.actions[r.nextInt(Agent.NUM_ACTIONS)]);
			i++;
		}
		if (simulState.isGameOver() && simulState.getGameWinner() == Types.WINNER.PLAYER_LOSES){
			node.maxValue += LOSE_PENALTY;
			node.maxDistance += LOSE_PENALTY;
		}
	}
	public boolean isScoreCandidate(MinMaxNode node, double scoreMax){
		boolean scoreCandidate = (node.maxValue > myValue && (node.maxValue > scoreMax||(node.maxValue == scoreMax && r.nextInt(2) == 1)));
		return scoreCandidate;
	}
	
	public boolean isDistanceCandidate (MinMaxNode node, double distanceMax) {
		boolean distanceCandidate =(node.maxDistance > 0 && (node.maxDistance > distanceMax||(node.maxDistance == distanceMax && r.nextInt(2) == 1)));
		return distanceCandidate;
	}
	public int selectBestOption(ElapsedCpuTimer elapsedTimer){
		double scoreMax = -Double.MAX_VALUE;
		double distanceMax = -Double.MAX_VALUE;
		int bestScoringAction = -1;
		int bestDistanceAction = -1;
		long remaining = elapsedTimer.remainingTimeMillis();
		int remainingLimit = 5;
		
		/* Version 3 - caution included
		for (int i = 0; i < children.length; i++){
			if (children[i] != null){	
				if (isScoreCandidate (children[i], scoreMax)|| isDistanceCandidate(children[i], distanceMax)) {
					if (remaining > remainingLimit){caution(children[i]);}
					if (isScoreCandidate(children[i], scoreMax)){
						scoreMax = children[i].maxValue;
						bestScoringAction = i;
					}
					if (isDistanceCandidate(children[i], distanceMax)) {
						distanceMax = children[i].maxDistance;
						bestDistanceAction = i;
					}
				}
			}	
		}*/
		for (int i = 0; i < children.length; i++){
			if (children[i] != null){	
				if (isScoreCandidate(children[i], scoreMax)){
					scoreMax = children[i].maxValue;
					bestScoringAction = i;
				}
				if (isDistanceCandidate(children[i], distanceMax)) {
					distanceMax = children[i].maxDistance;
					bestDistanceAction = i;
				}
			}
		}
		
		if (bestScoringAction == -1) {
			double avgTimeTaken = 0;
			double acumTimeTaken = 0;
			int numIters = 0; 
			remaining = elapsedTimer.remainingTimeMillis();
			double simulscore = - Double.MAX_VALUE;
			double bestScore = myValue + .000001;
			while (remaining > 2* avgTimeTaken && remaining > remainingLimit && numIters < children.length) {
				ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
				int chosen = getRandomSimulation();
				if (children[chosen] != null){
					children[chosen].been_simulated = true;
					StateObservation so = children[chosen].state.copy();
					int i = 0;
					while (i < simulation_depth && !so.isGameOver()){
						so.advance(Agent.actions[r.nextInt(Agent.NUM_ACTIONS)]);
						i++;
					}	
					simulscore = value (so);
				}
				
				//should make this better...better? Seems to perform better without this??? Version 2?
				if (simulscore > bestScore || (simulscore == bestScore && r.nextInt(2) == 1)) {
					bestScoringAction = chosen;
					bestScore = simulscore;
				}
				//Version 1
				//if (simulscore > myValue){bestScoringAction = chosen;}
				numIters++;
				acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
				avgTimeTaken  = acumTimeTaken/numIters;
				remaining = elapsedTimer.remainingTimeMillis();
			}
		}
			
		
		if (bestScoringAction >= 0){
			alternate_on = false;
			return bestScoringAction;
		}
		if (bestDistanceAction >= 0) {
			if (distanceMax > greatestDistance) {
				alternate_on = true;
				greatestDistance = distanceMax;
			}
			else {
				greatestDistance = 0;
				alternate_on = false;
			}
			return bestDistanceAction;
		}
		return r.nextInt(children.length);
	}
	
	public Vector2d returnPos () {
		if (!alternate_on) {return null;}
		else {return myPos;}
	}
		
	
	void expand (Vector2d originalPos){
		while (this.notFullyExpanded()){
			int action = getRandomChild();
			int test = 0;
			StateObservation newState = state.copy();
			newState.advance(Agent.actions[action]);
			MinMaxNode newNode = new MinMaxNode (newState, this, r, originalPos);
			children[action] = newNode;
			if (newNode.depth < rollout_depth){newNode.expand(originalPos);}
		}
	}	
		
		
	// Some functions snatched from the SampleMCTS agent classes	
	public int getRandomChild () {
		int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = r.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }
		return bestAction;
	}
	
	public int getRandomSimulation () {
		int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = r.nextDouble();
			if (children[i] != null){
				if (x > bestValue && children[i].been_simulated == false) {
					bestAction = i;
					bestValue = x;
				}
			}
		}
		return bestAction;
	}
	
	public boolean notFullyExpanded() {
        for (MinMaxNode node : children) {
            if (node == null) {
                return true;
            }
        }

        return false;
    }
   public double value(StateObservation a_gameState) {
	
        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore;
		rawScore = a_gameState.getGameScore();
        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            rawScore += LOSE_PENALTY;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            rawScore += WIN_BONUS;

        return rawScore;
    }	
}