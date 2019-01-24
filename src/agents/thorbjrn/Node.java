package agents.thorbjrn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import core.game.StateObservation;

public class Node {
	public LinkedList<Integer> path;
	public StateObservation so;
	
	public int initAct;
	
	public double value = 0;
	public double deadliness = 0;
	public double boringness = 0;
	
	public int earliestDeath = 1000000000;
	
	public int advancements = 0;
	
	public int directDeaths = 0;
	
	public int initAdvancements = 0;
	
	public boolean allPathsEndsInDeath = false;
	
	public HashMap<LinkedList<Integer>, Integer> pathsTried;
	
	public HashSet<LinkedList<Integer>> randomDeathEventsPaths;
	
	public Node(StateObservation so, int act){
		this.so = so;
		path = new LinkedList<Integer>();
		path.add(act);
		initAct = act;
		pathsTried = new HashMap<LinkedList<Integer>, Integer>();
		randomDeathEventsPaths = new HashSet<LinkedList<Integer>>();
	}

	public void addGoodActionToPath() {
		
		//it is good if: action leads to new path, action leads to new location
		//, actions leads far from initial position
		
		//it is bad if: action leads to a already visited path, 
		//action leads to a RANDOMDANGER position
		
		double bestActVal = Double.NEGATIVE_INFINITY; int bestAct = -1;
		for (int i = 0; i < Tools.actions.length; i++) {
			double actVal = Tools.r.nextDouble() * 0.01;
			
			if (Tools.isOppositeAction(path.getFirst(), i)) actVal -= 0.0025;
			
			
			if (path.getFirst() == i) actVal += 0.005;
			
			boolean hasTriedActBefore = false;
			for (Entry<LinkedList<Integer>, Integer> entr : pathsTried.entrySet()) {
				LinkedList<Integer> otherPath = entr.getKey();
				if (otherPath.size() <= path.size()) continue;
			
				
				for (int j = 0; j < path.size() + 1; j++) {
					if (j == path.size()){
						if (otherPath.get(j) == i) hasTriedActBefore = true;
					}else{
						if (otherPath.get(j) != path.get(j)){
							break;
						}
					}
				}
			}
			
			if (hasTriedActBefore) actVal -= 0.4;
			
			if (actVal > bestActVal){
				bestActVal = actVal;
				bestAct = i;
			}

			
			
		}
		
		
		path.add(bestAct);
		
	}
	
	public void reactToAdvancment(StateObservation thisState, StateObservation initState, int win) {
		
		if (path.size() == 1) initAdvancements++;
		
		int initResources = 0;
		for (Integer count : initState.getAvatarResources().values()) {
			initResources += count;
		}
		
		int thisStateResources = 0;
		for (Integer count : thisState.getAvatarResources().values()) {
			thisStateResources += count;
		}
		
		int resDiff = thisStateResources - initResources;

		
		//QUICK FIX
		int initBoringness = 0; 
		int thisBoringness = 0;
		if (Tools.getPositionKey(initState.getAvatarPosition()) < Tools.boringPlaces.length &&
				Tools.getPositionKey(thisState.getAvatarPosition()) < Tools.boringPlaces.length){
			initBoringness = Tools.boringPlaces[Tools.getPositionKey(initState.getAvatarPosition())];
			thisBoringness = Tools.boringPlaces[Tools.getPositionKey(thisState.getAvatarPosition())];
		}
		
//		int initBoringness = Tools.boringPlaces[Tools.getPositionKey(initState.getAvatarPosition())];
//		int thisBoringness = Tools.boringPlaces[Tools.getPositionKey(thisState.getAvatarPosition())];
		

		
		double brngDiff = initBoringness - thisBoringness;
		
		//Boringness does not matter for 3-actions games
		if (Tools.actions.length == 3) brngDiff = 0;
		
		//Firecaster haxx test
//		if (Tools.isPuzzleGame && Tools.actions[initAct] == ACTIONS.ACTION_USE){
//			brngDiff = 0;
//			resDiff *= 95000;
//		}
		
//		double distBetweenPos = initState.getAvatarPosition().dist(thisState.getAvatarPosition()) / (double)Tools.blockSize;
//		brngDiff *= distBetweenPos;
		
//		System.out.println("distBetweenPos: " + distBetweenPos);
		double valDiff = thisState.getGameScore() - initState.getGameScore() + win*500000000;

		//survivezombies haxx test
//		if (valDiff > 0 && resDiff < 0) valDiff = 0;
		
		
		if (win != -1){
			value += (valDiff + resDiff*0.00001 + brngDiff*0.00001) / Math.pow(Tools.actions.length, path.size()-1);
		}
	}
	
	private void endedPath(int win){
		
		if (win != 1){
			for (Entry<LinkedList<Integer>, Integer> iterable_element : pathsTried.entrySet()) {
				LinkedList<Integer> otherPath = iterable_element.getKey();
				int otherVal = iterable_element.getValue();
				if (otherVal == 1) continue;
				for (int i = 0; i < path.size(); i++) {
					
					if (otherPath.size() == i){
						//other path ended, while this path continue. there must randomness here!
	//					System.out.println("other path ended, while this path continue. there must randomness here!");
						randomDeathEventsPaths.add(Tools.getCloneOfPath(otherPath));
						break;
					}
					
					if (i == path.size()-1){
						if (otherPath.size() > path.size() && otherPath.get(i) == path.get(i)){
							//this path ended, while other continue.  there must randomness here!
	//						System.out.println("this path ended, while other continue.  there must randomness here!");
							randomDeathEventsPaths.add(Tools.getCloneOfPath(path));
							break;
						}else if (otherPath.get(i) == path.get(i)){
							//both paths ended same place. there must be randomness if win-value is not same
	//						System.out.println("both paths ended same place. there must be randomness if win-value is not same");
	//						System.out.println(pathsTried + ", "  +otherPath + " " + pathsTried.containsKey(otherPath));
							if (otherVal != win){
								randomDeathEventsPaths.add(Tools.getCloneOfPath(path));
								break;
							}
						}
					}
							
						
					
					if (i < otherPath.size() && otherPath.get(i) == path.get(i)){
						//keep checking
					}else{
						break;
					}

				}
			}
		}
		
		
		if (pathsTried.size() == 0 && win == -1) allPathsEndsInDeath = true;
		else if (win != -1) allPathsEndsInDeath = false;

		pathsTried.put(Tools.getCloneOfPath(path), win);		
	}
	
	
	public void dead(StateObservation initSo, boolean allNodesLeadToDirectDeath){
		endedPath(-1);
		earliestDeath = Math.min(earliestDeath, path.size());
		
		if (path.size() == 1) directDeaths++;
		
		if (path.size() != 1 || pathsTried.get(path) > -1 || allNodesLeadToDirectDeath){
			restartNode(initSo);
		}
		
	}
	
	public void win(StateObservation initSo){
		endedPath(1);
		restartNode(initSo);
		
	}

	
	private void restartNode(StateObservation initSo) {
		so = initSo.copy();
		int initAct = path.getFirst();
		path.clear();
		path.add(initAct);
	}

	

	
	public String toString() {
		return "Node{initAct: " + Tools.actions[initAct] + ", value: " + value + ", earliestDeath: " + earliestDeath + ", advancements: " + advancements + ", directDeaths: "+ directDeaths  + ", initAdvancements: " + initAdvancements + ", allPathsEndsInDeath: " + allPathsEndsInDeath + " }";
	}

	
	public void reachMaxDepth(StateObservation initSo) {
		endedPath(0);
		restartNode(initSo);
	}


	
	
}
