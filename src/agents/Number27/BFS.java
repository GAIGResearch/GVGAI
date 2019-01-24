/**
 * Basic breadth first search:
 * - Each object configuration is given an unique ID
 * - Therefore same configurations can be avoided
 * - When the used memory reaches its limits, certain configurations are culled
 * 
 * @author Florian Kirchgessner (Alias: Number27)
 * @version customGA3
 */

package agents.Number27;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.WINNER;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class BFS extends CustomAbstractPlayer {
	private static int MIN_TIME = 2;
	private final long BREAK_FREE_MEMORY = 256 * 1024 * 1024L;	// Execute actions when this amount of memory left
	private final long REDUCTION_MEMORY = 1500 * 1024 * 1024L;	// Reduce node count when this amount of memory left
	private final int ACCEPTABLE_NODE_COUNT = 2500;
	private final int MAX_NEW_NODE_COUNT = 50;
	
	private static int NUM_ACTIONS;
	private static HashMap<Integer, Types.ACTIONS> LOOKUP_INT_ACTION;
	private static HashMap<Types.ACTIONS, Integer> LOOKUP_ACTION_INT;
	
	private LinkedList<ActionNode> actionQueue;		// Node list
	private HashSet<Long> exploredStates;
	
	private LinkedList<Integer> calculatedActions;	// Actions that will be executed
	private double prevScore;
	private double currentScore;					// Score of the live frame
	private double targetScore;						// Score that will be reached when the calculated actions are executed
	private int blockSize;
	private boolean switchController;
	private long reductionMemory;
	
	private StateObservation prevStateObs;
	private boolean moved;
	private boolean reduceNodeCount;
	private int newNodeCount;
	
	
	public BFS(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		LOOKUP_INT_ACTION = new HashMap<Integer, Types.ACTIONS>();
		LOOKUP_ACTION_INT = new HashMap<Types.ACTIONS, Integer>();
		int i = 0;
		for (Types.ACTIONS action : stateObs.getAvailableActions()) {
			LOOKUP_INT_ACTION.put(i, action);
			LOOKUP_ACTION_INT.put(action, i);
			i++;
		}

		NUM_ACTIONS = stateObs.getAvailableActions().size();
		
		actionQueue = new LinkedList<ActionNode>();
		exploredStates = new HashSet<Long>();
		
		calculatedActions = new LinkedList<Integer>();
		prevScore = stateObs.getGameScore();
		currentScore = stateObs.getGameScore();
		blockSize = stateObs.getBlockSize();
		switchController = false;
		reductionMemory = REDUCTION_MEMORY;
		
		prevStateObs = stateObs;
		moved = false;
		reduceNodeCount = false;
		newNodeCount = 0;
		
		ActionNode initialNode = new ActionNode(stateObs.copy(), new LinkedList<Integer>(), NUM_ACTIONS);
		actionQueue.add(initialNode);
		
	}
	
	
	public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		long remaining;
		currentScore = stateObs.getGameScore();
		
		checkMemoryUsage();
		checkForEnd(stateObs);
		
		if(!moved && !compareStates(stateObs, prevStateObs)) {
			switchController = true;
			return getAction(stateObs);
		}
		
		if(reduceNodeCount) {
			reduceNodeCount(false);
		}
		
		do {
			expand();
			
			if(actionQueue.size() == 0) {
				break;
			}
			
			remaining = elapsedTimer.remainingTimeMillis();
		} while(remaining >= MIN_TIME);
		
		return getAction(stateObs);
	}
	
	
	private void expand() {
		if(actionQueue.size() == 0) {
			return;
		}
		
		ActionNode node = actionQueue.getFirst();
		
		StateObservation state = node.getCurrentState();
		int actionId = node.getUnexploredAction();
		
		state = advanceState(state, actionId);
		
		// Explored state
		if(state == null) {
			checkNode(node);
		}
		// Unexplored state
		else {
			// Create new node
			LinkedList<Integer> actionHistory = cloneLinkedList(node.getActionHistory());
			actionHistory.add(actionId);
			
			ActionNode followingNode = new ActionNode(state, actionHistory, NUM_ACTIONS);
			
			actionQueue.add(followingNode);
			
			checkNode(node);
			
			evaluate(state, followingNode);
		}
	}
	
	
	private StateObservation advanceState(StateObservation stateObs, int actionId) {
		stateObs = stateObs.copy();
		prevScore = stateObs.getGameScore();
		
		stateObs.advance(LOOKUP_INT_ACTION.get(actionId));
		
		if(prevScore < stateObs.getGameScore()) {
			reduceNodeCount = true;
			
		}
		
		// Dead end
		if(stateObs.isGameOver() && stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
			return null;
		}
		
		long stateId = calculateStateId(stateObs);
		
		// State already explored
		if(exploredStates.contains(stateId)) {// || stateObs.getGameScore() < prevScore) {
			return null;
		}
		
		exploredStates.add(stateId);
		
		return stateObs;
	}
	
	
	private void evaluate(StateObservation stateObs, ActionNode node) {
		if (stateObs.isGameOver() && stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS) {
			transferLinkedList(calculatedActions, node.getActionHistory());
			targetScore = Integer.MAX_VALUE;
			
			clear();
		}
	}
	
	
	private void executeActionNode(ActionNode node) {
		if(node == null) {
			clear();
			return;
		}
		
		StateObservation state = node.getCurrentState().copy();
		transferLinkedList(calculatedActions, node.getActionHistory());
		targetScore = state.getGameScore();
		
		clear();
		
		// This state becomes the new default state
		prevScore = state.getGameScore();
		
		ActionNode initialNode = new ActionNode(state, new LinkedList<Integer>(), NUM_ACTIONS);
		actionQueue.add(initialNode);
	}
	
	
	private ActionNode getOptimalActionNode() {
		if(actionQueue.size() == 0) {
			return null;
		}
		
		ActionNode optNode = actionQueue.getFirst();
		double maxScore = prevScore;
		int minEventCount = Integer.MAX_VALUE;
		Iterator<ActionNode> iter = actionQueue.iterator();
		
		StateObservation state;
		double score;
		int eventCount;
		while(iter.hasNext()) {
			ActionNode node = iter.next();
			state = node.getCurrentState();
			score = state.getGameScore();
			eventCount = state.getHistoricEventsHistory().size();
			
			// Choose the highest score with minimal events
			if(score >= maxScore) {
				if(score > maxScore || eventCount < minEventCount) {
					maxScore = score;
					minEventCount = eventCount;
					optNode = node;
				}
			}
		}
		
		// No progress was made
		if(maxScore <= currentScore && newNodeCount <= MAX_NEW_NODE_COUNT) {
			return null;
		}
		
		return optNode;
	}
	
	
	private void checkNode(ActionNode node) {
		if(node.check()) {
			if(actionQueue.size() == 1) {
				if(node.getCurrentState().getGameScore() > currentScore)
					executeActionNode(actionQueue.getFirst());
				else
					clear();
			}
			else {
				node.clear();
				actionQueue.removeFirst();
			}
		}
	}
	
	
	private void reduceNodeCount(boolean override) {
		int nodeCount = actionQueue.size();
	
		double eventsMin = Integer.MAX_VALUE;
		double eventsMax = 0;
		double scoreMin = Double.POSITIVE_INFINITY;
		double scoreMax = Double.NEGATIVE_INFINITY;
		
		Iterator<ActionNode> iter = actionQueue.iterator();
		
		while(iter.hasNext()) {
			ActionNode node = iter.next();
			StateObservation state = node.getCurrentState();
			int events = state.getHistoricEventsHistory().size();
			double score = state.getGameScore();
			
			if(events < eventsMin) {
				eventsMin = events;
			}
			else if(events > eventsMax) {
				eventsMax = events;
			}
			
			if(score < scoreMin) {
				scoreMin = score;
			}
			else if(score > scoreMax) {
				scoreMax = score;
			}
		}
		
		if(nodeCount > ACCEPTABLE_NODE_COUNT || override) {
			if(scoreMax != scoreMin || eventsMax != eventsMin) {
				// Reduce Nodes
				iter = actionQueue.iterator();
				while(iter.hasNext()) {
					ActionNode node = iter.next();
					StateObservation state = node.getCurrentState();
					int events = state.getHistoricEventsHistory().size();
					double score = state.getGameScore();
					
					if(events <= eventsMax - ((eventsMax - eventsMin) / 3) && score < scoreMax) {
						node.clear();
						iter.remove();
					}
				}
			}
			
			// No reduction, ignore events
			if(nodeCount == actionQueue.size()) {
				iter = actionQueue.iterator();
				while(iter.hasNext()) {
					ActionNode node = iter.next();
					StateObservation state = node.getCurrentState();
					double score = state.getGameScore();
					
					if(score < scoreMax) {
						node.clear();
						iter.remove();
					}
				}
			}
			
			reduceNodeCount = false;
		}
	}
	
	
	private long calculateStateId(StateObservation stateObs) {
		long h = 1125899906842597L;
		ArrayList<Observation>[][] observGrid = stateObs.getObservationGrid();

		for (int y = 0; y < observGrid[0].length; y++) {
			for (int x = 0; x < observGrid.length; x++) {
				for (int i = 0; i < observGrid[x][y].size(); i++) {
					Observation observ = observGrid[x][y].get(i);
					
					h = 31 * h + x;
					h = 31 * h + y;
					h = 31 * h + observ.category;
					h = 31 * h + observ.itype;
				}
			}
		}
		
		h = 31 * h + (int)(stateObs.getAvatarPosition().x/blockSize);
		h = 31 * h + (int)(stateObs.getAvatarPosition().y/blockSize);
		h = 31 * h + stateObs.getAvatarType();
		h = 31 * h + stateObs.getAvatarResources().size();
		h = 31 * h + (int)(stateObs.getGameScore() * 100);
		
		return h;
	}
	
	
	private Types.ACTIONS getAction(StateObservation stateObs) {
		if(calculatedActions.size() != 0) {
			// Reached target score, no more progress from here on out
			if(stateObs.getGameScore() == targetScore && (newNodeCount <= MAX_NEW_NODE_COUNT || calculatedActions.size() > 20)) {
				calculatedActions.clear();
				return Types.ACTIONS.ACTION_NIL;
			}
			
			Types.ACTIONS action = LOOKUP_INT_ACTION.get(calculatedActions.removeFirst());
			moved = true;
			
			stateObs = stateObs.copy();
			stateObs.advance(action);
			if(calculatedActions.size() != 0) {
				stateObs.advance(LOOKUP_INT_ACTION.get(calculatedActions.getFirst()));
			}
			
			if(stateObs.getGameWinner() == WINNER.PLAYER_LOSES) {
				switchController = true;
				return Types.ACTIONS.ACTION_NIL; 
			}
			
			return action;
		}
		
		return Types.ACTIONS.ACTION_NIL;
	}
	
	
	private LinkedList<Integer> cloneLinkedList(LinkedList<Integer> list) {
		LinkedList<Integer> newList = new LinkedList<Integer>();
		
		Iterator<Integer> iter = list.iterator();
		while(iter.hasNext()) {
			newList.add(iter.next());
		}
		
		return newList;
	}
	
	
	private void transferLinkedList(LinkedList<Integer> destList, LinkedList<Integer> sourceList) {
		Iterator<Integer> iter = sourceList.iterator();
		while(iter.hasNext()) {
			destList.addLast(iter.next());
		}
	}
	
	
	private void checkMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long freeMemory = runtime.maxMemory() - runtime.totalMemory();
		
		if(freeMemory < BREAK_FREE_MEMORY) {
			executeActionNode(getOptimalActionNode());
		}
		else if(freeMemory < reductionMemory) {
			reduceNodeCount(true);
			reductionMemory = freeMemory - 1048576;
		}
		else if(freeMemory > REDUCTION_MEMORY) {
			reductionMemory = REDUCTION_MEMORY;
		}
	}
	
	
	private void checkForEnd(StateObservation stateObs) {
		// Is the game nearly over
		if(actionQueue.size() > 0 && stateObs.getGameTick() + actionQueue.getFirst().getCurrentState().getGameTick() + 10 >= 2000) {
			executeActionNode(getOptimalActionNode());
		}
		// No nodes in the queue
		else if(actionQueue.size() == 0 && calculatedActions.size() == 0) {
			newNodeCount++;
			clear();
			
			if(stateObs.getGameTick() < 5) {
				switchController = true;
			}
			else {
				ActionNode newNode = new ActionNode(stateObs.copy(), new LinkedList<Integer>(), NUM_ACTIONS);
				actionQueue.add(newNode);
			}
		}
	}
	
	
	private boolean compareStates(StateObservation stateObs1, StateObservation stateObs2) {
		if(!compareObservationLists(stateObs1.getImmovablePositions(), stateObs2.getImmovablePositions()))
			return false;
		
		if(!compareObservationLists(stateObs1.getMovablePositions(), stateObs2.getMovablePositions()))
			return false;
		
		return true;
	}
	
	
	private boolean compareObservationLists(ArrayList<Observation>[] obsList1, ArrayList<Observation>[] obsList2) {
		if(obsList1 == null || obsList2 == null) {
			if(obsList1 == null && obsList2 == null)
				return true;
			return false;
		}
		
		if(obsList1.length != obsList2.length)
			return false;
		
		for(int type = 0; type < obsList1.length; type++) {
			if(obsList1[type].size() != obsList2[type].size())
				return false;
			
			for (int i = 0; i < obsList1[type].size(); i++) {
				Vector2d pos1 = obsList1[type].get(i).position;
				Vector2d pos2 = obsList2[type].get(i).position;
				
				if(pos1.x != pos2.x || pos1.y != pos2.y)
					return false;
			}
		}
					
		return true;
	}
	
	
	private void clear() {
		Iterator<ActionNode> iter = actionQueue.iterator();
		while(iter.hasNext()) {
			ActionNode n = iter.next();
			n.clear();
		}
		actionQueue.clear();
		exploredStates.clear();
		
		// This state becomes the new default state
		actionQueue = new LinkedList<ActionNode>();
		exploredStates = new HashSet<Long>();
	}
	
	
	public boolean switchController() {
		if(switchController) {
			return true;
		}
		
		return false;
	}
	
	
	public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
	}
}

























