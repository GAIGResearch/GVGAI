package agents.thorbjrn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import ontology.Types.ACTIONS;
import tools.Vector2d;
import core.game.Observation;
import core.game.StateObservation;

public class Tools {

	protected static Random r;
	
	protected static ACTIONS[] actions;

	protected static int[] boringPlaces;

	protected static boolean isPuzzleGame;

	protected static int maxSearchDepth = 8;
    
    static int blockSize = -1;
    static int heightOfLevel = -1;
    static int widthOfLevel = -1;
	
    
    protected static int[][] randomActMap;
    protected static int[] faculty = new int[6];

    protected static boolean hasNPCs;
	static{
		for (int i = 0; i < faculty.length; i++) {
			int val = 1;
			for (int j = i; j > 1; j--) {
				val *= j; 
			}
			faculty[i] = val;
		}
	}
    
	protected static boolean isOppositeAction(int act, int lastAct) {
		ACTIONS action = actions[act];
		ACTIONS lastAction = actions[lastAct];
		switch (action) {
		case ACTION_UP:
			if (lastAction == ACTIONS.ACTION_DOWN) return true;
			break;
		case ACTION_DOWN:
			if (lastAction == ACTIONS.ACTION_UP) return true;
			break;
		case ACTION_LEFT:
			if (lastAction == ACTIONS.ACTION_RIGHT) return true;
			break;
		case ACTION_RIGHT:
			if (lastAction == ACTIONS.ACTION_LEFT) return true;
			break;
		default:
			break;
		}
		
		return false;
	}

	protected static void initialize(StateObservation so) {
		
        r = new Random();
		
        blockSize = so.getBlockSize();
    	heightOfLevel = (int) (so.getWorldDimension().height / so.getBlockSize());
    	widthOfLevel = (int) (so.getWorldDimension().width / so.getBlockSize());
		
    	boringPlaces = new int[((heightOfLevel) * (widthOfLevel)) + 1];
    	for (int i = 0; i < boringPlaces.length; i++) {
    		boringPlaces[i] = 0;
		} 
    	
    	actions = new ACTIONS[so.getAvailableActions().size()];
        for (int i = 0; i < so.getAvailableActions().size(); i++) {
        	actions[i] = so.getAvailableActions().get(i);
		}
 
        
        randomActMap = new int[faculty[actions.length]][];
    	int n = actions.length;
		int counter = 0;
		for (int i = 0; i < n; i++) {
			int n2 = n > 1 ? n : 1;
			for (int j = 0; j < n2; j++) {
				if (n2 > 1 && j == i) continue;
				int n3 = n > 2 ? n : 1;
				for (int j2 = 0; j2 < n3; j2++) {
					if (n3 > 1 && (j2 == i || j2 == j)) continue;
					int n4 = n > 3 ? n : 1;
					for (int k = 0; k < n4; k++) {
						if (n4 > 1 && (k == i || k == j || k == j2)) continue;
						int n5 = n > 4 ? n : 1;
						for (int k2 = 0; k2 < n5; k2++) {
							if (n5 > 1 && (k2 == i || k2 == j || k2 == j2 || k2 == k)) continue;
							int mapping = i + j*n + j2*n*n + k*n*n*n + k2*n*n*n*n;
							randomActMap[counter] = new int[n];
							for (int l = 0; l < n; l++) {
								int map = (mapping / ((int)Math.pow(n, l))) % n;
								randomActMap[counter][l] = map;
								mapping -= map;
							}
							
							counter++;
						}
					}
				}
			}
		}
		
		simulationCheck(so, 800);
		
	}
	
	private static void simulationCheck(StateObservation so, int depth) {
		StateObservation so1 = so.copy();
//		StateObservation so2 = so.copy();

		MoveableSet initMs = new MoveableSet(Tools.getMoveables(so), so.getAvatarPosition(), so.getAvatarOrientation());
		
		isPuzzleGame = true;
		hasNPCs = false;

		for (int i = 0; i < depth; i++) {
			so1.advance(ACTIONS.ACTION_NIL);
			MoveableSet so1Ms = new MoveableSet(Tools.getMoveables(so1), so1.getAvatarPosition(), so1.getAvatarOrientation());

			if (!initMs.equals(so1Ms)){
				isPuzzleGame = false;
			}
			
//			so2.advance(ACTIONS.ACTION_NIL);
//			if (!so1.equiv(so)){
//				isPuzzleGame = true;
//			}
						
			if (checkIfHasNPCs(so1)) hasNPCs = true;
			
		}
		if (hasNPCs) isPuzzleGame = false; 
		if (hasNPCs) checkSpawnPoints(so);
		
	}
	
	private static void checkSpawnPoints(StateObservation so) {
		ArrayList<Observation>[] moveables = so.getPortalsPositions();
		
		if (moveables != null){
			for (int i = 0; i < moveables.length; i++) {
				ArrayList<Observation> moveableList = moveables[i];
				for (Observation observation : moveableList) {
	                int posKey = Tools.getPositionKey(observation.position);
	                Tools.boringPlaces[posKey] += 10;
//					System.out.println("observation - itype: " + observation.itype + ", pos: " + observation.position);
				}
			}
		}
		
	}

	protected static int getPositionKey(Vector2d vec){
    	if (vec == null) return 0;
    	if (vec.x < 0 || vec.y < 0 || vec.x > blockSize*widthOfLevel || vec.y > blockSize*heightOfLevel) return widthOfLevel *heightOfLevel;
		return (int)((vec.x/blockSize) + (vec.y/blockSize) * widthOfLevel);
    	
    }
	
	protected static ArrayList<ACTIONS> getActionList(LinkedList<Integer> list){
    	ArrayList<ACTIONS> result = new ArrayList<ACTIONS>();
    	
    	for (Integer integer : list){
    		result.add(Tools.actions[integer]);
		}
    	return result;
    }
	
	
	
	protected static boolean checkIfHasNPCs(StateObservation so) {
	
    	ArrayList<Observation>[] npcPositions = so.getNPCPositions();
    	
    	if (npcPositions != null){
    		return true;
    	}
		return false;
	}
    
    
	protected static LinkedList<Integer> getCloneOfPath(LinkedList<Integer> list) {
		LinkedList<Integer> listClone = new LinkedList<Integer>();
		
		if (list.clone() instanceof LinkedList<?>){
			for(int i = 0; i < ((LinkedList<?>)list).size(); i++){
				Object item = ((LinkedList<?>)list).get(i);
	            if(item instanceof Integer){
	            	listClone.add((Integer) item);
	            }
			}
		}
		return listClone;
	}
	
	protected static StateObservation playbackActions(StateObservation currentState,	LinkedList<Integer> list) {
    	StateObservation result = currentState.copy();

        for (Integer act : list) {
            result.advance(Tools.actions[act]);
        }
        return result;
    }
    
	protected static HashSet<Moveable> getMoveables(StateObservation so){
		
		HashSet<Moveable> result = new HashSet<Moveable>();
		
		
		if (so.getMovablePositions() != null){
			for (ArrayList<Observation> arrayList : so.getMovablePositions()) {
				for (Observation observation : arrayList) {
					result.add(new Moveable(observation.position, observation.itype));
				}
			}
		}
			
				
		if (so.getImmovablePositions() != null){
			for (ArrayList<Observation> arrayList : so.getImmovablePositions()) {
				for (Observation observation : arrayList) {
					if (observation.itype == 0) continue; //<- wall
					result.add(new Moveable(observation.position, observation.itype));
				}
			}
		}
		
		if (so.getResourcesPositions() != null){
			for (ArrayList<Observation> arrayList : so.getResourcesPositions()) {
				for (Observation observation : arrayList) {
					result.add(new Moveable(observation.position, observation.itype));
				}
			}
		}

		return result;
	}
}
