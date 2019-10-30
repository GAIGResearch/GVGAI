/**
 * Objective Controller:
 * - Creates a value map that guides the avatar to valuable objects
 * - The value of an object consists of its frequency, influence on the score and a bonus if it is unexplored
 * - Objects are evaluated when they are part of an event or the avatar is standing on top of them
 * - Remaining in one spot is penalized by the stay duration penalty. 
 * 	 Each frame the penalty increases on the player tile and the adjacent ones and decreases everywhere by a small amount
 * - Notable events prompt the player to revisit each object and are caused by reaching the resource limit or a change of the avatar type
 * 
 * @author Florian Kirchgessner (Alias: Number27)
 * @version customGA3
 */

package agents.Number27;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;
import tools.Vector2d;

public class ObjectiveController {
	
	private double[][] valueMap;
	private HashMap<Integer, double[]> objectValues;	// 0: Frequency value, 1: Score, 2: Relative score, 3: Times checked, 4: Exploration bonus
	private HashMap<Integer, Integer> resourceLimit;	// How much the avatar is able to carry
	private final double EXPLORATION_BONUS = 4;			// Bonus for unexplored objects
	private static double NEGATIVE_FACTOR = 1;			// Will be multiplied to negative values
	private final double NEGATIVE_FACTOR_AXIS_RESTRICTED = 0.1;
	private double[][] valueDistributionTemplate;
	private double[][] mapOverlay;						// Map overlay, only used for games with axis restricted movement
	private boolean considerXAxis;
	private boolean considerYAxis;
	
	private double[][] stayDurationPenaltyMap;
	private static double SDP_DECAY = 0.001;
	private static double SDP_INCREASE = 0.04;
	private static double SDP_INCREASE_ADDITION = 0.02;
	
	private final int VALUE_MAP_UPDATE_FREQU = 5;
	private int framesSinceLastUpdate;
	
	private int levelSizeX;
	private int levelSizeY;
	private int levelSizeXDouble;
	private int levelSizeYDouble;
	private double width;
	private double height;
	private double blockSizeFactor;
	private int immovableCategory;
	private ArrayList<Integer> immovableBlockTypes;
	
	
	public ObjectiveController(StateObservation stateObs) {
		width = stateObs.getWorldDimension().getWidth();
		height = stateObs.getWorldDimension().getHeight();
		blockSizeFactor = 1 / (double)stateObs.getBlockSize();
		levelSizeX = (int) (width * blockSizeFactor);
		levelSizeY = (int) (height * blockSizeFactor);
		levelSizeXDouble = levelSizeX * 2;
		levelSizeYDouble = levelSizeY * 2;
		framesSinceLastUpdate = 0;
		
		objectValues = new HashMap<Integer, double[]>();
		resourceLimit = new HashMap<Integer, Integer>();
		valueMap = new double[levelSizeX][levelSizeY];
		stayDurationPenaltyMap = new double[levelSizeX][levelSizeY];
		valueDistributionTemplate = new double[levelSizeXDouble][levelSizeYDouble];
		
		for (int x = 0; x < levelSizeX; x++) {
			for (int y = 0; y < levelSizeY; y++) {
				stayDurationPenaltyMap[x][y] = 0;
			}
		}
		
		checkImmovableBlocks(stateObs);
		
		checkAxisRestriction(stateObs);
		
		// Calculate templates
		calculateValueDistributionTemplate();
		calculateMapOverlay(stateObs);
		
		analyseLevel(stateObs);
	}
	
	
	public void update(StateObservation stateObs, StateObservation previousState) {
		updateStayDurationPenalty(stateObs);
		
		checkObjectives(stateObs, previousState.getGameScore(), previousState.getAvatarPosition(), true);
		
		checkForNotableEvent(stateObs, previousState);
		
		// Update value map every n frames
		if(framesSinceLastUpdate >= VALUE_MAP_UPDATE_FREQU) {
			analyseLevel(stateObs);
			framesSinceLastUpdate = 0;
		}
			
		framesSinceLastUpdate++;
	}
	
	
	public double checkObjectives(StateObservation stateObs, double prevScore, Vector2d prevPos, boolean updateExplorationBonus) {
		// Check for events
		if (stateObs.getHistoricEventsHistory().size() > 0 && stateObs.getHistoricEventsHistory().last().gameStep == stateObs.getGameTick() - 1) {
			double eventCount = 0;
			Iterator<Event> iter = stateObs.getHistoricEventsHistory().descendingIterator();
			
			// Check all events that occurred last frame
			while (iter.hasNext()) {
				Event event = iter.next();
				
				// In previous frame
				if (event.gameStep == stateObs.getGameTick() - 1) {
					double[] values = objectValues.get(event.passiveTypeId);
					if(values != null)
						eventCount += values[0];
					
					updateObjectiveScore(stateObs, event.passiveTypeId, prevScore, updateExplorationBonus);
				}
				// No more events, return event count
				else {
					Vector2d avatarPos = stateObs.getAvatarPosition();
					Types.ACTIONS lastAction = stateObs.getAvatarLastAction();
					// No event bonus if avatar is standing still or score got decreased
					if(prevScore > stateObs.getGameScore() || (avatarPos.x == prevPos.x && avatarPos.y == prevPos.y && lastAction != ACTIONS.ACTION_USE && lastAction != ACTIONS.ACTION_NIL))
						return 0;
					else
						return eventCount;
				}
			}
		}
		
		// Is the avatar standing on top the objective
		else if(!stateObs.isGameOver()) {
			// Get avatar position
			Vector2d avatarPos = stateObs.getAvatarPosition();
			int avatarPosX = (int)(avatarPos.x * blockSizeFactor);
			int avatarPosY = (int)(avatarPos.y * blockSizeFactor);
			if(avatarPosY < 0 || avatarPosY >= levelSizeY || avatarPosX < 0 || avatarPosX >= levelSizeX)
				return 0;
			
			ArrayList<Observation> observations = stateObs.getObservationGrid()[avatarPosX][avatarPosY];
			
			// Check all objects on the same tile
			for(Observation obs : observations) {
				if(obs.category != 0) {	// Not avatar
					updateObjectiveScore(stateObs, obs.itype, prevScore, updateExplorationBonus);
				}
			}
		}
		
		return 0;
	}
	
	
	private void updateObjectiveScore(StateObservation stateObs, int objectiveId, double prevScore, boolean updateExplorationBonus) {
		double score = stateObs.getGameScore();
		double[] objScore = objectValues.get(objectiveId);
		
		// Unknown object
		if(objScore == null) {
			objScore = new double[]{0, 0, 0, 0, 0};
			objectValues.put(objectiveId, objScore);
		}
		
		if(stateObs.isGameOver()) {
			WINNER win = stateObs.getGameWinner();
			if(win == WINNER.PLAYER_LOSES)
				prevScore += 1;
			else if(win == WINNER.PLAYER_WINS)
				prevScore -= 5;
		}
		
		// Update objective score
		if (objScore[1] != score - prevScore) {
			objScore[1] = (objScore[1] * objScore[3] + score - prevScore) / (objScore[3] + 1);
		}
		
		// Update exploration bonus when the score decreases or the objective was completed in a non simulated frame
		if(stateObs.getGameScore() - prevScore <= 0 || updateExplorationBonus) {
			objScore[4] = 0;
		}
		
		// Increase how many times the same objective was checked
		objScore[3]++;
	}
	
	
	public void updateStayDurationPenalty(StateObservation stateObs) {
		// Increase penalty in a 3 by 3 area around the avatar
		int posX = (int)(stateObs.getAvatarPosition().x * blockSizeFactor);
		int posY = (int)(stateObs.getAvatarPosition().y * blockSizeFactor);
		if(!(posY < 0 || posY >= levelSizeY || posX < 0 || posX >= levelSizeX)) {
			stayDurationPenaltyMap[posX][posY] += SDP_INCREASE_ADDITION;
			for (int x = posX - 1; x <= posX + 1; x++) {
				for (int y = posY - 1; y <= posY + 1; y++) {
					if (x >= 0 && x < levelSizeX && y >= 0 && y < levelSizeY) {
						stayDurationPenaltyMap[x][y] += SDP_INCREASE;
					}
				}
			}
		}
		
		// Decrease penalty everywhere
		for (int x = 0; x < levelSizeX; x++) {
			for (int y = 0; y < levelSizeY; y++) {
				if (stayDurationPenaltyMap[x][y] > 0) {
					stayDurationPenaltyMap[x][y] -= SDP_DECAY;
					if (stayDurationPenaltyMap[x][y] < 0) {
						stayDurationPenaltyMap[x][y] = 0;
					}
				}
			}
		}
	}
	
	
	private void analyseLevel(StateObservation stateObs) {
		resetValueMap();
		
		updateRelativeScore();
		
		analyseObjects(stateObs);
		
		ArrayList<Observation>[][] observGrid = stateObs.getObservationGrid();
		
		// Add object influences to the value map
		for (int y = 0; y < levelSizeY; y++) {
			for (int x = 0; x < levelSizeX; x++) {
				for (int i = 0; i < observGrid[x][y].size(); i++) {
					Observation observ = observGrid[x][y].get(i);
					
					insertObjectValue(observ.itype, x, y);
				}
			}
		}
		
		// Use the map overlay for axis restricted games
		if(!considerYAxis || !considerXAxis) {
			for (int y = 0; y < levelSizeY; y++) {
				for (int x = 0; x < levelSizeX; x++) {
					valueMap[x][y] *= mapOverlay[x][y];
				}
			}
		}
		
		normalizeValueMap();
		
		printObjectives();
		printValueMap();
	}
	
	
	private void analyseObjects(StateObservation stateObs) {
		HashMap<Integer, Integer> objectDict = new HashMap<Integer, Integer>();
		ArrayList<Observation>[][] observGrid = stateObs.getObservationGrid();
		
		// Add known objects
		for (int objectId : objectValues.keySet()) {
			objectDict.put(objectId, 0);
		}
		
		// Count objects
		for (int y = 0; y < levelSizeY; y++) {
			for (int x = 0; x < levelSizeX; x++) {
				for (int i = 0; i < observGrid[x][y].size(); i++) {
					Observation observ = observGrid[x][y].get(i);
					
					if (!objectDict.containsKey(observ.itype)) {
						objectDict.put(observ.itype, 1);
					} else {
						objectDict.put(observ.itype, objectDict.get(observ.itype) + 1);
					}
				}
			}
		}
		
		// Remove the avatar sprite
		if(!stateObs.isGameOver()) {
			objectDict.remove(stateObs.getAvatarType());
		}
		
		// Calculate object values
		for (int objectId : objectDict.keySet()) {
			if (!objectValues.containsKey(objectId)) {
				objectValues.put(objectId, new double[]{0, 0, 0, 0, 0});
			}
			
			double[] object = objectValues.get(objectId);
			// For known objects that are not present on the map set frequency to 0
			if(objectDict.get(objectId) == 0) {
				object[0] = 0;
			}
			// Give exploration bonus to unexplored objects
			else if (object[3] == 0) {
				object[0] = 1 / ((double) objectDict.get(objectId));
				
				// No exploration bonus in axis restricted games
				if(considerXAxis && considerYAxis)
					object[4] = EXPLORATION_BONUS;
			}
			// Calculate frequency
			else {
				object[0] = 1 / ((double) objectDict.get(objectId));
			}
		}
		
		// Ignore walls in axis restricted games
		if(!considerYAxis || !considerYAxis) {
			for(int type : immovableBlockTypes) {
				double[] values = objectValues.get(type);
				if(values != null && values[0] <= 1 / (double)levelSizeX && values[1] > -0.05 && values[1] < 0.05 && values[4] <= 0) {
					values[4] = - (values[0] + values[2]);
				}
				else if(values[4] < 0) {
					values[4] = 0;
				}
			}
		}
	}
	
	
	private void insertObjectValue(int objectId, int posX, int posY) {
		double[] objectValue = objectValues.get(objectId);
		
		if(objectValue == null)
			return;
		
		double totalValue = (objectValue[0] * objectValue[0] + objectValue[2] + objectValue[4]) * mapOverlay[posX][posY];
		int diffX = levelSizeX - posX;
		int diffY = levelSizeY - posY;
		
		// Overlay the value distribution template over the value map
		for (int x = 0; x < levelSizeX; x++) {
			for (int y = 0; y < levelSizeY; y++) {
				valueMap[x][y] += totalValue * valueDistributionTemplate[x + diffX][y + diffY];
			}
		}
	}
	
	
	private void checkForNotableEvent(StateObservation stateObs, StateObservation previousState) {
		if(stateObs.isGameOver()) {
			return;
		}
		
		// Avatar type changed
		if(stateObs.getAvatarType() != previousState.getAvatarType()) {
			foundNotableEvent();
		}
		
		// Has the resource limit been reached
		HashMap<Integer, Integer> resources = stateObs.getAvatarResources();
		if(resources != null) {
			// Check for events
			if (stateObs.getHistoricEventsHistory().size() > 0 && stateObs.getHistoricEventsHistory().last().gameStep == stateObs.getGameTick() - 1) {
				Iterator<Event> iter = stateObs.getHistoricEventsHistory().descendingIterator();
				HashMap<Integer, Integer> prevResources = previousState.getAvatarResources();
				
				while (iter.hasNext()) {
					Event event = iter.next();
					
					// In previous frame
					if (event.gameStep == stateObs.getGameTick() - 1) {
						int type = event.passiveTypeId;
						Integer resCount = resources.get(type);
						
						// Is a resource and the limit is unknown
						if(resCount != null && !resourceLimit.containsKey(type)) {
							Integer prevCount = prevResources.get(type);
							
							// No change or removed the only object, therefore reached limit
							double[] value = objectValues.get(type);
							if(prevCount == resCount || (value != null && value[0] == 1)) {
								resourceLimit.put(type, resCount);
								foundNotableEvent();
							}
						}
					} else {
						break;
					}
				}
			}
			
			// Is the avatar standing on top the objective
			else if(!stateObs.isGameOver()) {
				Vector2d avatarPos = stateObs.getAvatarPosition();
				int avatarPosX = (int)(avatarPos.x * blockSizeFactor);
				int avatarPosY = (int)(avatarPos.y * blockSizeFactor);
				if(avatarPosY < 0 || avatarPosY >= levelSizeY || avatarPosX < 0 || avatarPosX >= levelSizeX)
					return;
				
				ArrayList<Observation> observations = stateObs.getObservationGrid()[avatarPosX][avatarPosY];
				
				for(Observation obs : observations) {
					if(obs.category != 0) {	// Not avatar
						int type = obs.itype;
						HashMap<Integer, Integer> prevResources = previousState.getAvatarResources();
						Integer resCount = resources.get(type);
						
						// Is a resource and the limit is unknown
						if(resCount != null && !resourceLimit.containsKey(type)) {
							Integer prevCount = prevResources.get(type);
							
							// No change or removed the only object, therefore reached limit
							double[] value = objectValues.get(type);
							if(prevCount == resCount || (value != null && value[0] == 1)) {
								resourceLimit.put(type, resCount);
								foundNotableEvent();
							}
						}
					}
				}
			}
		}
	}
	
	
	private void foundNotableEvent() {
		// Reset exploration bonus
		if(considerXAxis && considerYAxis) {
			for (double[] value : objectValues.values()) {
				value[4] = EXPLORATION_BONUS;
			}
		}
		
		// Reset SDP
		for (int x = 0; x < levelSizeX; x++) {
			for (int y = 0; y < levelSizeY; y++) {
				stayDurationPenaltyMap[x][y] = 0;
			}
		}
	}
	
	
	private void resetValueMap() {
		for (int x = 0; x < levelSizeX; x++) {
			for (int y = 0; y < levelSizeY; y++) {
				valueMap[x][y] = 0;
			}
		}
	}
	
	
	private void updateRelativeScore() {
		double maxScore = 0.0001;
		double minScore = -0.0001;
		Collection<double[]> valuesList = objectValues.values();
		
		// Get max score
		for(double[] values : valuesList) {
			if (values[1] > maxScore) {
				maxScore = values[1];
			}
			
			if (values[1] < minScore) {
				minScore = values[1];
			}
		}
		
		if(minScore > -maxScore)
			minScore = -maxScore;
		
		maxScore = 1 / maxScore;
		minScore = -NEGATIVE_FACTOR / minScore;

		// Calculate relative score
		for(double[] values : valuesList) {
			if(values[1] >= 0) {
				values[2] = values[1] * maxScore;
			}
			else {
				values[2] = values[1] * minScore;
			}
		}
	}
	
	
	private void normalizeValueMap() {
		double maxValue = 0.0001;
		double minValue = -0.0001;
		for (int x = 0; x < levelSizeX; x++) {
			for (int y = 0; y < levelSizeY; y++) {
				if(valueMap[x][y] > maxValue) {
					maxValue = valueMap[x][y];
				}
				
				if(valueMap[x][y] < minValue) {
					minValue = valueMap[x][y];
				}
			}
		}
		
		maxValue = 1 / maxValue;
		minValue = -NEGATIVE_FACTOR / minValue;
		
		for (int x = 0; x < levelSizeX; x++) {
			for (int y = 0; y < levelSizeY; y++) {
				if(valueMap[x][y] >= 0) {
					valueMap[x][y] *= maxValue;
				}
				else {
					valueMap[x][y] *= minValue;
				}
			}
		}
	}
	
	
	private void checkImmovableBlocks(StateObservation stateObs) {
		immovableCategory = -1;
		immovableBlockTypes = new ArrayList<Integer>();
		
		ArrayList<Observation>[] immovablePositions = stateObs.getImmovablePositions();
		if(immovablePositions != null) {
			ArrayList<Observation> list = immovablePositions[0];
			if(list != null && list.size() > 0) {
				immovableCategory = list.get(0).category;
			}
			
			for(int i = 0; i < immovablePositions.length; i++) {
				for (int j = 0; j < immovablePositions[i].size(); j++) {
					Observation observ = immovablePositions[i].get(j);
					
					if(observ.category == immovableCategory && !immovableBlockTypes.contains(observ.itype)) {
						immovableBlockTypes.add(observ.itype);
					}
				}
			}
		}
	}
	
	
	private void checkAxisRestriction(StateObservation stateObs) {
		ArrayList<ACTIONS> availableActions = stateObs.getAvailableActions();
		if(!availableActions.contains(ACTIONS.ACTION_UP) && !availableActions.contains(ACTIONS.ACTION_DOWN) && 
			availableActions.contains(ACTIONS.ACTION_LEFT) && availableActions.contains(ACTIONS.ACTION_RIGHT)) {
			considerXAxis = true;
			considerYAxis = false;
		}
		else if(!availableActions.contains(ACTIONS.ACTION_LEFT) && !availableActions.contains(ACTIONS.ACTION_RIGHT) &&
			availableActions.contains(ACTIONS.ACTION_UP) && availableActions.contains(ACTIONS.ACTION_DOWN)) {
			considerXAxis = false;
			considerYAxis = true;
		}
		else {
			considerXAxis = true;
			considerYAxis = true;
		}
		
		// Disable stay duration penalty
		if(!considerXAxis || !considerYAxis) {
			NEGATIVE_FACTOR = NEGATIVE_FACTOR_AXIS_RESTRICTED;
			SDP_DECAY = 0;
			SDP_INCREASE = 0;
			SDP_INCREASE_ADDITION = 0;
		}
	}
	
	
	private void calculateMapOverlay(StateObservation stateObs) {
		mapOverlay = new double[levelSizeX][levelSizeY];
		
		// Restricted to x-axis
		if(!considerYAxis) {
			double avatarPosY = stateObs.getAvatarPosition().y * blockSizeFactor;
			for (int y = 0; y < levelSizeY; y++) {
				double factor = 2 - Math.abs(avatarPosY - y) / levelSizeY;
				for (int x = 0; x < levelSizeX; x++) {
					mapOverlay[x][y] = factor;
				}
			}
		}
		// Restricted to y-axis
		else if(!considerXAxis) {
			double avatarPosX = stateObs.getAvatarPosition().x * blockSizeFactor;
			for (int x = 0; x < levelSizeX; x++) {
				double factor = 2 - Math.abs(avatarPosX - x) / levelSizeX;
				for (int y = 0; y < levelSizeY; y++) {
					mapOverlay[x][y] = factor;
				}
			}
		}
		// default
		else {
			double factor = 1;
			for (int x = 0; x < levelSizeX; x++) {
				for (int y = 0; y < levelSizeY; y++) {
					mapOverlay[x][y] = factor;
				}
			}
		}
	}
	
	
	private void calculateValueDistributionTemplate() {
		if(!considerXAxis || !considerYAxis) {
			for (int x = 0; x < levelSizeXDouble; x++) {
				for (int y = 0; y < levelSizeYDouble; y++) {
					double dist = calcDistance(x, y, levelSizeX, levelSizeY);
					if(dist == 0)
						valueDistributionTemplate[x][y] = 1;
					else
						valueDistributionTemplate[x][y] = 1 / dist;
				}
			}
		}
		else {
			for (int x = 0; x < levelSizeXDouble; x++) {
				for (int y = 0; y < levelSizeYDouble; y++) {
					double dist = calcDistance(x*2, y, levelSizeX*2, levelSizeY);
					
					if(dist == 0)
						valueDistributionTemplate[x][y] = 1;
					else
						valueDistributionTemplate[x][y] = Math.pow(2, - dist * dist * 0.2);
				}
			}
		}
	}
	
	
	public double getTileValue(Vector2d position) {
		int posX = (int)(position.x * blockSizeFactor);
		int posY = (int)(position.y * blockSizeFactor);
		
		// Outside of the tracked level
		if(posY < 0 || posY >= levelSizeY || posX < 0 || posX >= levelSizeX) {
			if(posY < -1 || posY >= levelSizeY + 1 || posX < -1 || posX >= levelSizeX + 1)
				return -30;
			else
				return -10;
		}
		else {
			return valueMap[posX][posY] - stayDurationPenaltyMap[posX][posY];
		}
	}
	
	
	private double calcDistance(double pos1X, double pos1Y, double pos2X, double pos2Y) {
		return (Math.abs(pos1X - pos2X) + Math.abs(pos1Y - pos2Y));
	}
	
	
	public boolean isConsiderXAxis() {
		return considerXAxis;
	}


	public boolean isConsiderYAxis() {
		return considerYAxis;
	}


	public void printValueMap() {
//		String str;
//		System.out.println(" \n");
//		for(int y = 0; y < levelSizeY; y++) {
//			str = "";
//			for(int x = 0; x < levelSizeX; x++) {
//				int value = (int)(valueMap[x][y] * 1000);
//				if(value <= 0)
//					str +=  "-    ";
//				else if(value < 10)
//					str += value + "    ";
//				else if(value < 100)
//					str += value + "   ";
//				else if(value < 1000)
//					str += value + "  ";
//				else
//					str += value + " ";
//			}
//			System.out.println(str + "\n");
//		}
//		System.out.println(" \n");
		
		// Stay duration penalty map
//		for(int y = 0; y < levelSizeY; y++) {
//			str = "";
//			for(int x = 0; x < levelSizeX; x++) {
//				int value = (int)(stayDurationPenaltyMap[x][y] * 1000);
//				if(value <= 0)
//					str +=  "-    ";
//				else if(value < 10)
//					str += value + "    ";
//				else if(value < 100)
//					str += value + "   ";
//				else if(value < 1000)
//					str += value + "  ";
//				else
//					str += value + " ";
//			}
//			System.out.println(str + "\n");
//		}
//		System.out.println(" \n");
	}
	
	
	private void printObjectives() {
//		System.out.println("----- Objectives -----");
//		Iterator<Entry<Integer, double[]>> iter = objectValues.entrySet().iterator();
//		while (iter.hasNext()) {
//			Entry<Integer, double[]> object = iter.next();
//			String str = "";
//			str += object.getKey() + " Freq: " + object.getValue()[0] + " Score: " + object.getValue()[1] + " RelScore: " + object.getValue()[2] + " TimesChecked: " + object.getValue()[3] + " ExplBonus: " + object.getValue()[4];
//			System.out.println(str);
//		}
//		System.out.println("----------------------");
	}
}
