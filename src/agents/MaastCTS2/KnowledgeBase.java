package agents.MaastCTS2;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;
import tools.Vector2d;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.gnu.trove.impl.Constants;
import agents.MaastCTS2.gnu.trove.list.array.TByteArrayList;
import agents.MaastCTS2.gnu.trove.list.array.TDoubleArrayList;
import agents.MaastCTS2.gnu.trove.list.array.TIntArrayList;
import agents.MaastCTS2.gnu.trove.map.hash.TIntIntHashMap;
import agents.MaastCTS2.heuristics.events.EventKnowledge;
import agents.MaastCTS2.utils.OrderedIntPair;
import agents.MaastCTS2.utils.Pathfinder;

/**
 * A knowledge base that can be queried for information that has been gathered so far
 * on the game and/or level currently being played.
 *
 * @author Dennis Soemers
 */
public class KnowledgeBase {
	
	public enum GameClassifications{
		DETERMINISTIC,
		NONDETERMINISTIC
	}
	
	private int[][] pheromones;
	
	/** The height of the map in blocks */
	private int mapHeightBlocks;
	/** The height of the map in pixels */
	private int mapHeightPixels;
	/** The width of the map in blocks */
	private int mapWidthBlocks;
	/** The width of the map in pixels */
	private int mapWidthPixels;
	/** The number of pixels that form a single block in the game */
	private int pixelsPerBlock;
	
	/** The maximum distance possible between any pair of cells in the current level */
	private int maxDistance;
	
	/** 
	 * A mapping from types observed in the games to indices.
	 * 
	 * <p> Given a mapping from type i to index j in this map, all other collections in
	 * the knowledge base that keep data related to types will be ArrayLists where index 
	 * j contains data for type i
	 * 
	 * <p> Even though the framework typically seems to use consecutive integers for types
	 * anyway (0, 1, 2, ..), it does not seem to guarantee this, so to be safe we use this map
	 */
	private TIntIntHashMap typesToIndexMap;
	
	/**
	 * List to figure out which index corresponds to which type.
	 */
	private TIntArrayList indicesToTypesList;
	
	/** The index that ''0'' maps to in typesToIndexMap (0 is the wall type) */
	private int wallIdx = -1;
	
	/**
	 * This list contains, at index i, the current weight of the feature for the distance
	 * to the closest sprite of the type indexed by i.
	 */
	private TDoubleArrayList distFeatureWeights;
	
	/**
	 * This list contains, at index i, the average manhattan distance between avatar
	 * and objects of type indexed by i when interacting
	 */
	private TDoubleArrayList averageInteractionDistances;
	
	/**
	 * This list contains, at index i, the current learning rate with which we update the
	 * weight of the feature for the distance to the closest sprite of the type indexed 
	 * by i.
	 */
	private TDoubleArrayList distFeatureLearningRates;
	
	/**
	 * This list contains, at index i, the maximum number of objects of the type indexed
	 * by i that we have ever observed in a single state.
	 */
	private TIntArrayList maxNumObservationsPerType;
	
	/**
	 * This list contains, at index i, the category of sprites with types indexed by i.
	 */
	private TIntArrayList typeCategories;
	
	/** A map of the minimum resource values we've ever observed in evaluated states for all resources */
	private TIntIntHashMap minResourceValues;
	/** A map of the maximum resource values we've ever observed in evaluated states for all resources */
	private TIntIntHashMap maxResourceValues;
	
	private int minHealthValue;
	private int maxHealthValue;
	
	private static final double DEFAULT_DIST_WEIGHT_MOVABLE = 0.25;
	private static final double DEFAULT_DIST_WEIGHT_NPC = 0.1;
	private static final double DEFAULT_DIST_WEIGHT_PORTAL = 1.0;
	private static final double DEFAULT_DIST_WEIGHT_RESOURCE = 1.0;
	private static final double DEFAULT_DIST_WEIGHT_STATIC = 0.1;
	
	private double maxSingleStepScoreChange = 1.0;
	
	/**
	 * This list contains, at index i, knowledge gathered from observing collision events
	 * involving the avatar and a sprite of the type indexed by i
	 */
	private ArrayList<EventKnowledge> eventKnowledge;
	
	/** The minimum knowledge-based evaluation found so far in the entire game */
	private double MIN_KB_EVAL = -2.0;
	/** The maximum knowledge-based evaluation found so far in the entire game */
	private double MAX_KB_EVAL = 2.0;
	
	private double MIN_DANGEROUS_TYPES_EVAL = 0.0;
	private double MAX_DANGEROUS_TYPES_EVAL = 0.0;
	
	private double MIN_BLOCKING_OBSERVATIONS_EVAL = 0.0;
	private double MAX_BLOCKING_OBSERVATIONS_EVAL = 0.0;
	
	/** The pathfinder we'll use to compute distances */
	private Pathfinder pathfinder;
	
	private TIntArrayList rootShortestDistances;
	private TIntArrayList rootNumObservationsPerType;
	private HashMap<Integer, Integer> rootResources;
	
	private GameClassifications gameClassification = GameClassifications.NONDETERMINISTIC;
	
	public void init(StateObservation stateObs, boolean detectDeterministicGames){
		//System.out.println("INIT KB");
		Dimension pixels = stateObs.getWorldDimension();
		pixelsPerBlock = stateObs.getBlockSize();
		mapHeightPixels = pixels.height;
		mapWidthPixels = pixels.width;
		mapHeightBlocks = (mapHeightPixels / pixelsPerBlock);
		mapWidthBlocks = (mapWidthPixels / pixelsPerBlock);
		
		maxDistance = mapHeightBlocks * mapWidthBlocks;
		
		typesToIndexMap = new TIntIntHashMap(8, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
		indicesToTypesList = new TIntArrayList();
		
		distFeatureWeights = new TDoubleArrayList();
		averageInteractionDistances = new TDoubleArrayList(Constants.DEFAULT_CAPACITY, -1.0);
		distFeatureLearningRates = new TDoubleArrayList();
		maxNumObservationsPerType = new TIntArrayList(Constants.DEFAULT_CAPACITY, -1);
		typeCategories = new TIntArrayList();
		eventKnowledge = new ArrayList<EventKnowledge>();
		rootShortestDistances = new TIntArrayList();
		rootNumObservationsPerType = new TIntArrayList();
		rootResources = new HashMap<Integer, Integer>();
		
		minResourceValues = new TIntIntHashMap(8, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
		maxResourceValues = new TIntIntHashMap(8, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
		
		pathfinder = new Pathfinder();
		pathfinder.init(stateObs);
		
		gameClassification = GameClassifications.NONDETERMINISTIC;
		if(detectDeterministicGames){
			classifyGame(stateObs);
			//System.out.println("Game classified as: " + gameClassification);
		}
		
		if(gameClassification == GameClassifications.DETERMINISTIC){
			MctsController.MAX_NUM_SAFETY_CHECKS = 1;	// dont need more than 1 safety check in deterministic games
			//System.out.println("=============");
			//System.out.println("DETERMINISTIC");
			//System.out.println("=============");
		}
		else{
			//System.out.println("==========");
			//System.out.println("NONDETERMINISTIC");
			//System.out.println("==========");
		}
		
		pheromones = new int[mapWidthBlocks][mapHeightBlocks];
	}
	
	/**
	 * Given the score in the previous state, and the number of events that existed in the previous state,
	 * adds knowledge of all events that are new in the given new state.
	 * 
	 * @param previousScore
	 * @param previousNumEvents
	 * @param newState
	 */
	public void addEventKnowledge(double previousScore, int previousNumEvents, Vector2d previousAvatarPos, 
									Vector2d previousAvatarOrientation, ACTIONS action, StateObservation newState,
									HashMap<Integer, Integer> previousResources, HashMap<Integer, Integer> newResources,
									boolean playout){
		if(!((MctsController)(Agent.controller)).allowsKnowledgeBasedEvaluation()){
			return;
		}
		
		/*for(int i = 0; i < distFeatureWeights.size(); ++i){
			if(Math.abs(distFeatureWeights.getQuick(i)) > 0.01){
				System.out.println("weight of feature " + indicesToTypesList.get(i) + " = " + distFeatureWeights.get(i));
			}
		}*/
		
		TreeSet<Event> newEventSet = newState.getHistoricEventsHistory();
		Iterator<Event> it = newEventSet.descendingSet().iterator();
		int numNewEvents = newEventSet.size() - previousNumEvents;
		double scoreChange = newState.getGameScore() - previousScore;
		boolean loss = (newState.isGameOver() && newState.getGameWinner() == WINNER.PLAYER_LOSES);
		boolean win = (newState.isGameOver() && newState.getGameWinner() == WINNER.PLAYER_WINS);
		
		// using small values here to avoid potential overflows. 
		if(loss){
			scoreChange -= 1.0;
		}
		else if(win){
			scoreChange += 1.0;
		}
		
		boolean gainedResources = false;
		for(Integer resourceID : newResources.keySet()){
			Integer previousValue = previousResources.get(resourceID);
			
			if(previousValue == null){
				gainedResources = true;
				break;
			}
			else{
				Integer newValue = newResources.get(resourceID);
				if(newValue > previousValue){
					gainedResources = true;
					break;
				}
			}
		}
		
		if(gainedResources){
			// looks like collision events may be giving us resources, we want to reward that
			scoreChange += 0.1;
		}
		
		boolean movementBlocked = false;
		Vector2d newAvatarPos = newState.getAvatarPosition();
		OrderedIntPair avatarCell = positionToCell(newAvatarPos);
		if(newState.getAvatarOrientation().equals(previousAvatarOrientation) && !win && !loss){	// wins/losses make our orientation/position invalid
			// did not change orientation, so if we played a movement action we expect to have a different position
			if(Globals.isMovementAction(action)){
				// played a movement action
				movementBlocked = (newAvatarPos.equals(previousAvatarPos));
			}
		}
		
		// we ignore losses from playout step
		if(!(loss && playout)){
			TByteArrayList collidedWithList = new TByteArrayList(eventKnowledge.size(), (byte)-1);
			for(int i = 0; i < eventKnowledge.size(); ++i){
				collidedWithList.add((byte)0);
			}
			
			ArrayList<Observation>[][] obsGrid = newState.getObservationGrid();
			
			// loop through the events that are unique to this new state (so, events that occured on the state transition)
			while(numNewEvents > 0 && it.hasNext()){
				Event event = it.next();
				
				// update event knowledge
				int spriteType = event.passiveTypeId;
				
				/*if(spriteType == 5 || spriteType == 2 || spriteType == 6){
					System.out.println("collision with type " + spriteType);
				}*/
				
				boolean disappearedObject = true;
				OrderedIntPair eventCell = positionToCell(event.position);
				
				if(movementBlocked){
					// if our movement was blocked, we won't find the object we collided with in eventCell
					// we could maybe instead find it in an adjacent cell, but it takes effort to look through those,
					// so instead we'll pretend the object didn't disappear TODO maybe fix this?
					disappearedObject = false;
				}
				else{
					if(isValidCell(eventCell)){
						ArrayList<Observation> observations = obsGrid[eventCell.first][eventCell.second];
						
						for(Observation obs : observations){
							if(spriteType == obs.itype){
								disappearedObject = false;
								break;
							}
						}
					}
					else{
						disappearedObject = false;
					}
				}
				
				/*if(movementBlocked){
					System.out.println("movement blocked upon collision with type " + spriteType);
				}
				else{
					System.out.println("movement not blocked upon collision with type " + spriteType);
				}*/
				
				int spriteTypeIdx = getIndexForType(spriteType, -1);
				
				if(spriteTypeIdx == collidedWithList.size()){
					collidedWithList.add((byte)1);
				}
				else{
					collidedWithList.setQuick(spriteTypeIdx, (byte)1);
				}
				
				EventKnowledge ek = eventKnowledge.get(spriteTypeIdx);
				ek.updateObservation(scoreChange, movementBlocked, loss, disappearedObject);
				
				// incremental update of average interaction distance
				// modified the update a little bit so that we always assume an initial observation with distance 0.0
				if(!win && !loss){	// wins or losses make avatarCell invalid, so dont update average distance if win or loss
					//System.out.println("avatar cell = " + avatarCell);
					//System.out.println("event cell = " + eventCell);
					int manhattanDistance = Globals.manhattanDistance(avatarCell, eventCell);
					int interactionCounter = ek.getObservationCount();
					averageInteractionDistances.setQuick(spriteTypeIdx, 
							((double)(manhattanDistance + averageInteractionDistances.getQuick(spriteTypeIdx) * (interactionCounter)) / 
									(interactionCounter + 1)));
				}				
					
				// update weight corresponding to the updated event
				double currentWeight = distFeatureWeights.get(spriteTypeIdx);
				double learningRate = distFeatureLearningRates.get(spriteTypeIdx);
				
				//System.out.println("weight for type " + spriteType + " before update = " + currentWeight);
				
				double error = ek.getAverageDeltaScore() - currentWeight;
				double nextWeight = (currentWeight + learningRate * error);
				/*if(isResourceCategory(typeCategories.getQuick(spriteTypeIdx))){
					nextWeight = Math.max(nextWeight, 0.1);	// always want to stay a bit interested in resources
				}*/
				distFeatureWeights.set(spriteTypeIdx, nextWeight);
				
				/*if(spriteType == 5 || spriteType == 2 || spriteType == 6){
					System.out.println("weight for type " + spriteType + " after update = " + distFeatureWeights.get(spriteTypeIdx));
				}*/
				
				// also update the learning rate
				distFeatureLearningRates.set(spriteTypeIdx, Math.max(0.1, 0.75 * learningRate));
				
				--numNewEvents;
			}
			
			if(Math.abs(scoreChange) > maxSingleStepScoreChange){
				// we've observed a larger single-step score change than before
				maxSingleStepScoreChange = Math.abs(scoreChange);
				
				// we'll update the weights of all types that we've still never collided with to be on the same scale
				for(int i = 0; i < eventKnowledge.size(); ++i){
					if(eventKnowledge.get(i).getObservationCount() == 0){
						// we've never observed collision events of this type yet, so reset the default with the new scale
						distFeatureWeights.set(i, getInitialDistWeight(typeCategories.getQuick(i)));
						//System.out.println("Set weight of feature " + indicesToTypesList.get(i) + " to " + distFeatureWeights.get(i));
					}
				}
			}
			
			if(isValidCell(avatarCell)){
				ArrayList<Observation> observationsAtAvatar = obsGrid[avatarCell.first][avatarCell.second];
				
				for(Observation obs : observationsAtAvatar){
					int obsType = obs.itype;
					int obsTypeIdx = getIndexForType(obsType, -1);
					
					if(obsTypeIdx == collidedWithList.size()){
						collidedWithList.add((byte)0);
					}
					
					if(collidedWithList.getQuick(obsTypeIdx) == (byte)0){
						// didn't observe a collision event with objects of this type, even though we're right on top of it
						// probably means it's not an interesting object
						EventKnowledge ek = eventKnowledge.get(obsTypeIdx);
						ek.updateObservation(0.0, movementBlocked, loss, false);
						
						// update weight corresponding to the updated event
						double currentWeight = distFeatureWeights.get(obsTypeIdx);
						double learningRate = distFeatureLearningRates.get(obsTypeIdx);
						
						double error = ek.getAverageDeltaScore() - currentWeight;
						double nextWeight = (currentWeight + learningRate * error);
						distFeatureWeights.set(obsTypeIdx, nextWeight);
						
						// also update the learning rate
						distFeatureLearningRates.set(obsTypeIdx, Math.max(0.1, 0.75 * learningRate));
					}
				}
			}
		}		
	}
	
	public Vector2d cellToPosition(int x, int y){
		return new Vector2d(x * pixelsPerBlock, y * pixelsPerBlock);
	}
	
	/**
	 * Returns the weight of distance features for the type indexed by the given index
	 * 
	 * @param typeIdx
	 * @return
	 */
	public double getDistFeatureWeight(int typeIdx){
		if(typeIdx == wallIdx){
			return 0.0;	// always weight of 0.0 for walls
		}
		
		double weight = distFeatureWeights.getQuick(typeIdx);
		
		if(!isNpcCategory(typeCategories.getQuick(typeIdx))){
			// no negative weights for non-NPCs
			weight = Math.max(weight, 0.0);	
		}
		
		return weight;
	}
	
	/**
	 * Returns the index for the given sprite type. Creates one if sprites of this type
	 * haven't been seen before yet.
	 * 
	 * <p> Returns -1 for avatar or from-avatar types, because we don't want to include those
	 * in any features.
	 * 
	 * @param type 
	 * 				The type of the observed sprite
	 * @param category 
	 * 				The category of the observed sprite. Can be set to a negative number if not known,
	 * 				but then the caller should make sure that it is not an avatar or from-avatar category
	 * @return
	 */
	public int getIndexForType(int type, int category){
		int idx = typesToIndexMap.get(type);
		
		if(idx >= 0){	// valid idx
			if(category > -1){
				// we might have previously set an invalid category, so fix that here
				typeCategories.setQuick(idx, category);
			}
			
			return idx;
		}
		else if(isAvatarCategory(category) || isFromAvatarCategory(category)){
			return -1;
		}
		else{
			idx = typesToIndexMap.size();
			typesToIndexMap.put(type, idx);
			
			if(type == 0){	// wall
				wallIdx = idx;	// we want to remember this index since it's important
			}
			
			// make sure our lists have the correct size
			distFeatureWeights.add(getInitialDistWeight(category));
			averageInteractionDistances.add(0.0);
			distFeatureLearningRates.add(0.8);	// start with a learning rate of 0.8 for every feature
			maxNumObservationsPerType.add(0);
			typeCategories.add(category);
			eventKnowledge.add(new EventKnowledge());
			rootShortestDistances.add(3 * maxDistance);	// 3 * maxDistance because our pathfinder sometimes returns twice the maxDistance
			rootNumObservationsPerType.add(0);
			
			indicesToTypesList.add(type);
			
			return idx;
		}
	}
	
	/**
	 * Returns an initial weight for distance features to objects of the given category
	 * 
	 * @param category
	 * @return
	 */
	private double getInitialDistWeight(int category){
		if(isMovableCategory(category)){
			return maxSingleStepScoreChange * DEFAULT_DIST_WEIGHT_MOVABLE + Globals.smallNoise();
		}
		else if(isNpcCategory(category)){
			return maxSingleStepScoreChange * DEFAULT_DIST_WEIGHT_NPC + Globals.smallNoise();
		}
		else if(isPortalCategory(category)){
			return maxSingleStepScoreChange * DEFAULT_DIST_WEIGHT_PORTAL + Globals.smallNoise();
		}
		else if(isResourceCategory(category)){
			return maxSingleStepScoreChange * DEFAULT_DIST_WEIGHT_RESOURCE + Globals.smallNoise();
		}
		else if(isStaticCategory(category)){
			return maxSingleStepScoreChange * DEFAULT_DIST_WEIGHT_STATIC + Globals.smallNoise();
		}
		
		return 0.0;
	}
	
	/**
	 * Returns the maximum possible distance between any pair of cells on our map, assuming
	 * that they are connected (= map width times map height). 
	 * 
	 * @return
	 */
	public int getMaxDistance(){
		return maxDistance;
	}
	
	/**
	 * Returns a TIntArrayList of non-wall objects that are considered to be movement-blockers
	 * 
	 * @return
	 */
	public TIntArrayList getMovementBlockers(){
		TIntArrayList movementBlockers = new TIntArrayList(Constants.DEFAULT_CAPACITY, -1);
		
		for(int i = 0; i < eventKnowledge.size(); ++i){
			int type = indicesToTypesList.getQuick(i);
			EventKnowledge knowledge = eventKnowledge.get(i);
			
			int observationCount = knowledge.getObservationCount();
			if(observationCount >= 10){		// will only start trusting this once we've observed at least 10 events
				if(0.1 * observationCount >= knowledge.getDisappearCount()){
					// less than 10% of the observed events result in objects of this type disappearing, so we may
					// take it into account as an obstacle
					int blockCount = knowledge.getBlockCount();
					
					if(0.25 * observationCount < blockCount){
						// more than 25% of observed events resulted in movement block, so we'll trust it
						movementBlockers.add(type);
						
						// make sure we'll actually be using the pathfinder, since we detected obstacles
						if(i != wallIdx){
							pathfinder.setPathfinderNecessary();
						}
					}
					else if(isStaticCategory(typeCategories.getQuick(i))){
						// for static objects, we'll assume they also block movement if they frequently kill us
						int lossCount = knowledge.getLossCount();
						
						if(0.95 * observationCount < lossCount){
							movementBlockers.add(type);
							pathfinder.setPathfinderNecessary();
						}
					}
				}
			}
		}
		
		return movementBlockers;
	}
	
	public int getPixelsPerBlock(){
		return pixelsPerBlock;
	}
	
	public boolean isAvatarCategory(int category){
		return (category == Types.TYPE_AVATAR);
	}
	
	public boolean isFromAvatarCategory(int category){
		return (category == Types.TYPE_FROMAVATAR);
	}
	
	public boolean isGameDeterministic(){
		return (gameClassification == GameClassifications.DETERMINISTIC);
	}
	
	public boolean isGameStochastic(){
		return (gameClassification == GameClassifications.NONDETERMINISTIC);
	}
	
	public boolean isMovableCategory(int category){
		return (category == Types.TYPE_MOVABLE);
	}
	
	public boolean isNpcCategory(int category){
		return (category == Types.TYPE_NPC);
	}
	
	public boolean isPortalCategory(int category){
		return (category == Types.TYPE_PORTAL);
	}
	
	public boolean isResourceCategory(int category){
		return (category == Types.TYPE_RESOURCE);
	}
	
	public boolean isStaticCategory(int category){
		return (category == Types.TYPE_STATIC);
	}
	
	public boolean isValidCell(OrderedIntPair cell){
		return (cell.first >= 0 			&& 
				cell.second >= 0	 		&&
				cell.first < mapWidthBlocks && 
				cell.second < mapHeightBlocks	);
	}
	
	/**
	 * Returns true if and only if the given weight is considered to be relevant,
	 * where epsilon is the minimum absolute value that the weight should have to
	 * be considered relevant
	 * 
	 * @param typeIdx
	 * @param epsilon
	 * @return
	 */
	public boolean isWeightRelevant(int typeIdx, double epsilon){
		double weight = getDistFeatureWeight(typeIdx);
		return (Math.abs(weight) >= epsilon);
	}
	
	public int getPheromoneStrength(OrderedIntPair cell){
		if(isValidCell(cell)){
			return pheromones[cell.first][cell.second];
		}
		else{
			return 0;
		}
	}
	
	/**
	 * Returns a knowledge-based evaluation in [-0.80, 0.80] of the given state
	 * 
	 * This used to be only on distances and scores, as described in CIG paper and in master thesis.
	 * Since writing those, a number of other features have been implemented and taken into account
	 * in the heuristic evaluation function
	 * 
	 * @param state
	 * @return
	 */
	public double knowledgeBasedEval(StateObservation state){
		// array to store best distance for every type TODO dont think we actually still need this array
		TIntArrayList minDistances = new TIntArrayList(typesToIndexMap.size());
		// will store here, at index i, an ArrayList of all cells in which we observed objects of type i
		ArrayList<ArrayList<OrderedIntPair>> observationCells = new ArrayList<ArrayList<OrderedIntPair>>(typesToIndexMap.size());
		
		for(int i = 0; i < typesToIndexMap.size(); ++i){
			minDistances.add(maxDistance);		// max possible distance as initial value for every type
			observationCells.add(new ArrayList<OrderedIntPair>());
		}
		
		ArrayList<Observation>[][] observationGrid = state.getObservationGrid();
		OrderedIntPair avatarCell = positionToCell(state.getAvatarPosition());
		
		int width = observationGrid.length;
		int height = observationGrid[0].length;
		
		boolean[][] blockedGrid = new boolean[width][height];
		TIntArrayList movementBlockers = getMovementBlockers();
		
		TIntArrayList numObservationsPerType = new TIntArrayList(maxNumObservationsPerType.size(), -1);
		numObservationsPerType.fill(0, maxNumObservationsPerType.size(), 0);
		
		// will collect cells in which Movables are located in this arraylist
		ArrayList<OrderedIntPair> movablesCells = new ArrayList<OrderedIntPair>();
		
		// will collect cells that contain ''important'' objects in this arraylist
		// the avatar will always be considered important, and objects that often
		// give a score increase upon collision as well
		ArrayList<OrderedIntPair> importantCells = new ArrayList<OrderedIntPair>();
		
		for (int x = 0; x < width; ++x){
			for (int y = 0; y < height; ++y){
				ArrayList<Observation> observations = observationGrid[x][y];
				int numObservations = observations.size();
				for (int i = 0; i < numObservations; ++i){
					Observation observation = observations.get(i);
					int category = observation.category;
					int type = observation.itype;
					
					if(movementBlockers.contains(type)){
						blockedGrid[x][y] = true;
					}
					
					int typeIdx = getIndexForType(type, category);
					
					if(typeIdx == minDistances.size()){
						// didn't encounter sprites of this type yet, so add new entries
						minDistances.add(maxDistance);
						observationCells.add(new ArrayList<OrderedIntPair>());
						numObservationsPerType.add(0);
					}
					
					OrderedIntPair cell = positionToCell(observation.position);
					
					if(isAvatarCategory(category)){
						importantCells.add(cell);	// avatar is important
						continue;					// dont care about including him for distance features though
					}
					if(isFromAvatarCategory(category)){
						continue;		// dont care about including these in features
					}
					else if(type == 0){		// a wall, dont care about walls either
						continue;
					}	
					
					// TODO we might be adding the same cell multiple times here if an observation occupies multiple cells
					observationCells.get(typeIdx).add(cell);
					
					numObservationsPerType.setQuick(typeIdx, numObservationsPerType.getQuick(typeIdx) + 1);
					
					if(isMovableCategory(category)){
						movablesCells.add(cell);
					}
					
					if(getDistFeatureWeight(typeIdx) > 0.5){
						// seems like a good object to collide with, so it's important
						importantCells.add(cell);
					}
				}
			}
		}
		
		// compute evaluation
		double eval = 0.0;
		
		//System.out.println();
		for(int i = 0; i < minDistances.size(); ++i){
			if(isWeightRelevant(i, 0.0001)){
				// this type has a relevant weight, so need to compute minimum distance for it
				ArrayList<OrderedIntPair> obsList = observationCells.get(i);
				
				if(averageInteractionDistances.getQuick(i) < 3.0){
					// cannot interact from distance, so need exact distances
					
					// some code to print what our lists that we want to sort look like
					/*String list = "[";
					for(int idx = 0; idx < obsList.size(); ++idx){
						if(idx < obsList.size() - 1){
							list += Globals.manhattanDistance(obsList.get(idx), avatarCell) + ", ";
						}
						else{
							list += Globals.manhattanDistance(obsList.get(idx), avatarCell) + "]";
						}
					}
					System.out.println(list);*/
					
					// sort list of observations of this type according to manhattan distance
					obsList.sort(new Comparator<OrderedIntPair>(){

						@Override
						public int compare(OrderedIntPair cell1, OrderedIntPair cell2) {
							return (Globals.manhattanDistance(cell1, avatarCell) - Globals.manhattanDistance(cell2, avatarCell));
						}
						
					});
					
					// find closest observation of this type
					for(int obsIdx = 0; obsIdx < obsList.size(); ++obsIdx){
						OrderedIntPair cell = obsList.get(obsIdx);
						//System.out.println("computing distance to object of type " + indicesToTypesList.getQuick(i));
						int dist = pathfinder.computeDistance(avatarCell, cell, minDistances.getQuick(i), blockedGrid);
						//System.out.println("dist to observation of type " + indicesToTypesList.getQuick(i) + " >= " + dist);
						
						if(dist < minDistances.getQuick(i)){
							minDistances.setQuick(i, dist);
						}
					}
				}
				else{
					// can interact from a longer distance, so only want to minimize horizontal OR vertical distance, don't need
					// to minimize both
					
					// find closest observation of this type
					for(int obsIdx = 0; obsIdx < obsList.size(); ++obsIdx){
						OrderedIntPair cell = obsList.get(obsIdx);
						int dist = Globals.minHorizontalOrVerticalDist(avatarCell, cell);
						
						// in this case we want to slightly motivate not to move TOO close in manhattan distance,
						// only want to be in the same row OR same column, not both
						//dist -= 0.1 * Globals.manhattanDistance(avatarCell, cell);
						
						if(dist < minDistances.getQuick(i)){
							minDistances.setQuick(i, dist);
						}
					}
				}
				
				double weight = getDistFeatureWeight(i);
				//System.out.println("Weight for type " + indicesToTypesList.get(i) + " = " + distFeatureWeights.get(i));
				double deltaDistance = rootShortestDistances.getQuick(i) - minDistances.getQuick(i);
				//System.out.println("modifying eval by (" + weight + " * " + deltaDistance + ") for type " + indicesToTypesList.getQuick(i));
				eval += (weight * deltaDistance);
			}
		}
		//System.out.println();
		
		MIN_KB_EVAL = Math.min(MIN_KB_EVAL, eval);
		MAX_KB_EVAL = Math.max(MAX_KB_EVAL, eval);
		
		// normalise event-based eval to [0.0, 0.5]
		eval = 0.5 * Globals.normalise(eval, MIN_KB_EVAL, MAX_KB_EVAL);
		
		// compute eval for num observations of blocking types
		double blockingObservationsEval = 0.0;
		boolean fewerBlockingsThanRoot = false;
				
		for(int i = 0; i < movementBlockers.size(); ++i){
			int blockingType = movementBlockers.getQuick(i);
					
			if(blockingType != 0){	// dont want to take walls into account here
				int blockingTypeIdx = typesToIndexMap.get(blockingType);
				int numObservations = numObservationsPerType.getQuick(blockingTypeIdx);
				int maxNumObservations = maxNumObservationsPerType.getQuick(blockingTypeIdx);
						
				if(numObservations > maxNumObservations){
					maxNumObservations = numObservations;
					maxNumObservationsPerType.setQuick(i, numObservations);
				}
				
				if(numObservations < rootNumObservationsPerType.getQuick(blockingTypeIdx)){
					fewerBlockingsThanRoot = true;
				}
						
				blockingObservationsEval += (double)numObservations / maxNumObservations;
			}
		}
						
		// normalise to [0.0, 0.05]
		MIN_BLOCKING_OBSERVATIONS_EVAL = Math.min(MIN_BLOCKING_OBSERVATIONS_EVAL, blockingObservationsEval);
		MAX_BLOCKING_OBSERVATIONS_EVAL = Math.max(MAX_BLOCKING_OBSERVATIONS_EVAL, blockingObservationsEval);
		blockingObservationsEval = Globals.normalise(blockingObservationsEval, MIN_BLOCKING_OBSERVATIONS_EVAL, MAX_BLOCKING_OBSERVATIONS_EVAL) * 0.05;
		
		// also include resources in evaluation
		double resourceEval = 0.0;
		int numResources = 0;
		
		HashMap<Integer, Integer> resources = state.getAvatarResources();
		for(java.util.Map.Entry<Integer, Integer> entry : resources.entrySet()){
			int key = entry.getKey();
			int value = entry.getValue();
			
			if(fewerBlockingsThanRoot){
				// we reduced the number of blocking objects on the map in comparison to the root state
				// this means that we don't want to punish any resource going 1 lower than in root state,
				// because that is likely some kind of key we have used
				Integer rootValue = rootResources.get(key);
				if(rootValue != null){
					if(value == rootValue - 1){
						value = rootValue;
					}
				}
			}
			
			int minValue = minResourceValues.containsKey(key) ? Math.min(value, minResourceValues.get(key)) : value;
			int maxValue = maxResourceValues.containsKey(key) ? Math.max(value, maxResourceValues.get(key)) : value;
			
			minResourceValues.put(key, minValue);
			maxResourceValues.put(key, maxValue);
			
			resourceEval += Globals.normalise(value, minValue, maxValue);
			++numResources;
		}
		
		int hp = state.getAvatarHealthPoints();
		minHealthValue = Math.min(hp, minHealthValue);
		maxHealthValue = Math.max(hp, maxHealthValue);
		
		if(minHealthValue < maxHealthValue){
			resourceEval += Globals.normalise(hp, minHealthValue, maxHealthValue);
			++numResources;
		}
		
		// compute average resource value
		if(numResources > 0){
			resourceEval /= numResources;
		}

		// normalise resource-based eval to [0.0, 0.25]
		resourceEval *= 0.25;
		
		// compute eval for num observations of dangerous types
		double dangerousObservationsEval = 0.0;
		
		for(int i = 0; i < numObservationsPerType.size(); ++i){
			double weight = distFeatureWeights.getQuick(i);
			int numObservations = numObservationsPerType.getQuick(i);
			int maxNumObservations = maxNumObservationsPerType.getQuick(i);
			
			if(numObservations > maxNumObservations){
				maxNumObservations = numObservations;
				maxNumObservationsPerType.setQuick(i, numObservations);
			}
			
			if(weight < 0.0){	// dangerous type
				// the difference will be negative if we reduce the number of dangerous types there are
				// multiplied by a negative weight, this will give us a positive reward
				dangerousObservationsEval += (numObservations - maxNumObservations) * weight;
			}
		}
		
		// normalise to [0.0, 0.05]
		MIN_DANGEROUS_TYPES_EVAL = Math.min(MIN_DANGEROUS_TYPES_EVAL, dangerousObservationsEval);
		MAX_DANGEROUS_TYPES_EVAL = Math.max(MAX_DANGEROUS_TYPES_EVAL, dangerousObservationsEval);
		dangerousObservationsEval = Globals.normalise(dangerousObservationsEval, MIN_DANGEROUS_TYPES_EVAL, MAX_DANGEROUS_TYPES_EVAL) * 0.05;
		
		// with this, we'll punish movables that are touching obstacles, because them touching obstacles makes
		// it more difficult for us to push them around (which may be necessary for victory)
		double obstacleTouchingMovablesPenalty = 0.0;
		int numMovables = movablesCells.size();
		
		for(OrderedIntPair cell : movablesCells){
			int x = cell.first;
			int y = cell.second;
			
			if(x > 0){
				if(blockedGrid[x - 1][y]){
					obstacleTouchingMovablesPenalty += 0.1;
				}
			}
			
			if(x < mapWidthBlocks - 1){
				if(blockedGrid[x + 1][y]){
					obstacleTouchingMovablesPenalty += 0.1;
				}
			}
			
			if(y > 0){
				if(blockedGrid[x][y - 1]){
					obstacleTouchingMovablesPenalty += 0.1;
				}
			}
			
			if(y < mapHeightBlocks - 1){
				if(blockedGrid[x][y + 1]){
					obstacleTouchingMovablesPenalty += 0.1;
				}
			}
		}
		
		if(numMovables > 0){
			obstacleTouchingMovablesPenalty /= numMovables;
		}
		
		// with this, we'll punish important objects that are surrounded by obstacles
		// if the avatar is completely surrounded, he's stuck, which is obviously bad
		// other important objects are objects that give a score increase upon collision,
		// we don't want them to become unreachable either. Them being completely surrounded
		// is the easiest case of unreachability to compute
		int numImportantCells = importantCells.size();
		int numStuckObjects = 0;

		for(OrderedIntPair cell : importantCells){
			int x = cell.first;
			int y = cell.second;
			
			if(x > 0 && x < mapWidthBlocks - 1 && y > 0 && y < mapHeightBlocks - 1){
				if(blockedGrid[x - 1][y] && blockedGrid[x + 1][y] && blockedGrid[x][y - 1] && blockedGrid[x][y + 1]){
					numStuckObjects += 1;
				}
			}
		}
		
		double stuckObjectsPenalty = 0.0;
		if(numImportantCells > 0){
			double ratioStuck = (double)numStuckObjects / numImportantCells;
			
			// this formula rapidly increases as the ratio of stuck objects increases 
			// (so even a single stuck object gets punished relatively heavily), and increases
			// more slowly as the ratio increases, for a final punishment of 0.4 at a ratio of 1.0
			stuckObjectsPenalty = Math.sqrt(0.16 * Math.sqrt(ratioStuck));
		}

		// return total evaluation (which should be in [-0.85, 0.80])
		return eval + resourceEval + dangerousObservationsEval - obstacleTouchingMovablesPenalty - stuckObjectsPenalty - blockingObservationsEval;
	}
	
	public OrderedIntPair positionToCell(Vector2d position){
		int x = ((int) position.x) / pixelsPerBlock;
		int y = ((int) position.y) / pixelsPerBlock;
		
		return new OrderedIntPair(x, y);
	}
	
	/**
	 * Converts a vector describing a position by it's pixel-coordinates into
	 * an integer, where every block (which may consist of multiple pixels) is converted
	 * into a unique integer.
	 * 
	 * @param position
	 * @return
	 */
	public int positionToInt(Vector2d position){
		int x = ((int) position.x) / pixelsPerBlock;
		int y = ((int) position.y) / pixelsPerBlock;
		
		return (y * mapWidthBlocks) + x;
	}
	
	public String toString(Observation observation){
		return "[Observation: ID=" + observation.obsID +
				", type=" + observation.itype +
				", location=(" + observation.position.x +
				", " + observation.position.y +
				")]";
	}
	
	public void update(StateObservation stateObs){
		// slightly increase all the distance feature weights over time to reward exploration
		for(int i = 0; i < distFeatureWeights.size(); ++i){
			distFeatureWeights.set(i, distFeatureWeights.get(i) + 0.0001);
		}
		
		OrderedIntPair avatarCell = positionToCell(stateObs.getAvatarPosition());
		if(isValidCell(avatarCell)){
			pheromones[avatarCell.first][avatarCell.second] += 1;
		}
	}
	
	public void updateRoot(StateObservation rootState){
		rootResources = rootState.getAvatarResources();
		
		// we'll compute the current shortest distances to objects of every type so we can compute change in distances
		// for evaluation of states
		
		// will store here, at index i, an ArrayList of all cells in which we observed objects of type i
		ArrayList<ArrayList<OrderedIntPair>> observationCells = new ArrayList<ArrayList<OrderedIntPair>>(rootShortestDistances.size());
		
		// start by resetting any existing distances and reserving space for observationCells
		for(int i = 0; i < rootShortestDistances.size(); ++i){
			rootShortestDistances.setQuick(i, 3 * maxDistance);	// 3 * maxDistance because our pathfinder sometimes returns twice the maxDistance
			observationCells.add(new ArrayList<OrderedIntPair>());
			
			// also reset observation counts
			rootNumObservationsPerType.setQuick(i, 0);
		}
		
		ArrayList<Observation>[][] observationGrid = rootState.getObservationGrid();
		OrderedIntPair avatarCell = positionToCell(rootState.getAvatarPosition());
		
		int width = observationGrid.length;
		int height = observationGrid[0].length;
		
		boolean[][] blockedGrid = new boolean[width][height];
		TIntArrayList movementBlockers = getMovementBlockers();
		
		for (int x = 0; x < width; ++x){
			for (int y = 0; y < height; ++y){
				ArrayList<Observation> observations = observationGrid[x][y];
				int numObservations = observations.size();
				for (int i = 0; i < numObservations; ++i){
					Observation observation = observations.get(i);
					int category = observation.category;
					int type = observation.itype;
					
					if(movementBlockers.contains(type)){
						blockedGrid[x][y] = true;
					}
					
					int typeIdx = getIndexForType(type, category);
					
					if(typeIdx == observationCells.size()){
						// didn't encounter sprites of this type yet, so add new entry
						observationCells.add(new ArrayList<OrderedIntPair>());
					}
					
					if(isAvatarCategory(category) || isFromAvatarCategory(category)){
						continue;		// dont care about including these in features
					}
					else if(type == 0){		// a wall, dont care about walls either
						continue;
					}
					
					rootNumObservationsPerType.setQuick(typeIdx, rootNumObservationsPerType.getQuick(typeIdx) + 1);
					
					OrderedIntPair cell = positionToCell(observation.position);
					
					// TODO we might be adding the same cell multiple times here if an observation occupies multiple cells
					observationCells.get(typeIdx).add(cell);	
				}
			}
		}

		for(int i = 0; i < rootShortestDistances.size(); ++i){
			// sort list of observations of this type according to manhattan distance
			ArrayList<OrderedIntPair> obsList = observationCells.get(i);
			
			if(averageInteractionDistances.getQuick(i) < 3.0){
				// cannot interact from distance, so need exact distances
				Collections.sort(obsList, new Comparator<OrderedIntPair>(){

					@Override
					public int compare(OrderedIntPair cell1, OrderedIntPair cell2) {
						return (Globals.manhattanDistance(cell1, avatarCell) - Globals.manhattanDistance(cell2, avatarCell));
					}
					
				});
				
				// find closest observation of this type
				for(int obsIdx = 0; obsIdx < obsList.size(); ++obsIdx){
					OrderedIntPair cell = obsList.get(obsIdx);
					//System.out.println("computing distance to object of type " + indicesToTypesList.getQuick(i));
					int dist = pathfinder.computeDistance(avatarCell, cell, rootShortestDistances.getQuick(i), blockedGrid);
					
					if(dist < rootShortestDistances.getQuick(i)){
						rootShortestDistances.setQuick(i, dist);
					}
				}
			}
			else{
				// can interact from a longer distance, so only want to minimize horizontal OR vertical distance, don't need
				// to minimize both
				
				// find closest observation of this type
				for(int obsIdx = 0; obsIdx < obsList.size(); ++obsIdx){
					OrderedIntPair cell = obsList.get(obsIdx);
					int dist = Globals.minHorizontalOrVerticalDist(avatarCell, cell);
					
					// in this case we want to slightly motivate not to move TOO close in manhattan distance,
					// only want to be in the same row OR same column, not both
					//dist -= 0.1 * Globals.manhattanDistance(avatarCell, cell);
					
					if(dist < rootShortestDistances.getQuick(i)){
						rootShortestDistances.setQuick(i, dist);
					}
				}
			}
		}
	}
	
	private void classifyGame(StateObservation initialGameState){
		if(initialGameState.getNPCPositions() != null){
			// we'll classify any game that has NPCs has stochastic
			gameClassification = GameClassifications.NONDETERMINISTIC;
			return;
		}
		
		// we'll generate a few random sequences of actions
		// for every sequence, we'll repeat a number of runs playing that action sequence
		//
		// if every repetition of the same sequence of actions results in the same final state, 
		// we'll assume it is deterministic
		
		final int numRandomActionSequences = 5;
		final int randomActionSequenceLength = 5;
		final int numRepetitionsPerActionSequence = 3;
		
		// we'll assume the same set of actions is valid in every state 
		final ArrayList<ACTIONS> actions = initialGameState.getAvailableActions();
		final int numActions = actions.size();
		ACTIONS[][] randomActionSequences = new ACTIONS[numRandomActionSequences][randomActionSequenceLength];
		
		// generate the action sequences
		for(int i = 0; i < numRandomActionSequences; ++i){
			for(int j = 0; j < randomActionSequenceLength; ++j){
				randomActionSequences[i][j] = actions.get(Globals.RNG.nextInt(numActions));
			}
		}
		
		// play out the random sequences and see if resulting state observations are equivalent
		// (using hash values for ''equality'')
		final int prime = 31;
		
		for(int seq = 0; seq < numRandomActionSequences; ++seq){
			int[] finalStateHashValues = new int[numRepetitionsPerActionSequence];
			
			for(int rep = 0; rep < numRepetitionsPerActionSequence; ++rep){
				StateObservation state = initialGameState.copy();
				int actionIdx = 0;
				
				// play actions
				while(!state.isGameOver() && actionIdx < randomActionSequenceLength){
					// TODO use states generated here learning KB eval weights?
					state.advance(randomActionSequences[seq][actionIdx]);
					++actionIdx;
				}
				
				// compute hash value for this state
				int hash = 1;
				
				ArrayList<Observation>[][] observationGrid = state.getObservationGrid();
				int width = observationGrid.length;
				int height = observationGrid[0].length;
				
				for (int x = 0; x < width; ++x){
					for (int y = 0; y < height; ++y){
						ArrayList<Observation> observations = observationGrid[x][y];
						int numObservations = observations.size();
						for (int i = 0; i < numObservations; ++i){
							Observation observation = observations.get(i);
							
							if(isNpcCategory(observation.category)){
								// looks like some NPC spawned, so we'll classify as nondeterministic game
								gameClassification = GameClassifications.NONDETERMINISTIC;
								return;
							}
							
							// incorporate this observation's location and type in our hash value
							hash = prime * hash + (positionToInt(observation.position) * observation.itype);
						}
					}
				}
				
				finalStateHashValues[rep] = hash;
				
				if(rep > 0){
					// compare hash value with previous hash value; if not equal, we have a nondeterministic game
					if(finalStateHashValues[rep] != finalStateHashValues[rep - 1]){
						gameClassification = GameClassifications.NONDETERMINISTIC;
						return;
					}
				}
			}
		}
		
		gameClassification = GameClassifications.DETERMINISTIC;
	}
	
	/**
	 * Useful for debugging
	 * 
	 * @param g
	 */
	public void draw(Graphics2D g){
		//pathfinder.drawConnectivity(g);
		
		//final java.awt.Color yellow = java.awt.Color.YELLOW;
		//final java.awt.Color red = java.awt.Color.RED;
		//final java.awt.Color green = java.awt.Color.GREEN;
		
		// draw pheromones
		/*for(int x = 0; x < mapWidthBlocks; ++x){
			for(int y = 0; y < mapHeightBlocks; ++y){
				double pheromone = pheromones[x][y] / maxPheromoneValue;
				Vector2d pos = Globals.knowledgeBase.cellToPosition(x,  y);	
				
				int colorR = 0;
				int colorG = 0;
				int colorB = 0;
				
				if(pheromone == 0.5){
					colorB = yellow.getBlue();
					colorG = yellow.getGreen();
					colorR = yellow.getRed();
				}
				else if(pheromone > 0.5){
					double redRatio = pheromone;
					double yellowRatio = 1.0 - redRatio;
					
					colorB = (int)(redRatio * red.getBlue() + yellowRatio * yellow.getBlue());
					colorG = (int)(redRatio * red.getGreen() + yellowRatio * yellow.getGreen());
					colorR = (int)(redRatio * red.getRed() + yellowRatio * yellow.getRed());
				}
				else{
					double greenRatio = 1.0 - pheromone;
					double yellowRatio = 1.0 - greenRatio;
					
					colorB = (int)(greenRatio * green.getBlue() + yellowRatio * yellow.getBlue());
					colorG = (int)(greenRatio * green.getGreen() + yellowRatio * yellow.getGreen());
					colorR = (int)(greenRatio * green.getRed() + yellowRatio * yellow.getRed());
				}
				
				//System.out.println(colorR + " " + colorG + " " + colorB);
				Color color = new Color(colorR, colorG, colorB, 100);
				g.setColor(color);
				g.fillRect((int) (pos.x), (int) (pos.y), 
						Globals.knowledgeBase.getPixelsPerBlock(), Globals.knowledgeBase.getPixelsPerBlock());
			}
		}*/
		
		// draw death influence
		/*for(int x = 0; x < mapWidthBlocks; ++x){
			for(int y = 0; y < mapHeightBlocks; ++y){
				double deathInfluenceValue = deathInfluence[x][y] / maxDeathInfluenceValue;
				Vector2d pos = Globals.knowledgeBase.cellToPosition(x,  y);	
				
				int colorR = 0;
				int colorG = 0;
				int colorB = 0;
				
				if(deathInfluenceValue == 0.5){
					colorB = yellow.getBlue();
					colorG = yellow.getGreen();
					colorR = yellow.getRed();
				}
				else if(deathInfluenceValue > 0.5){
					double redRatio = deathInfluenceValue;
					double yellowRatio = 1.0 - redRatio;
					
					colorB = (int)(redRatio * red.getBlue() + yellowRatio * yellow.getBlue());
					colorG = (int)(redRatio * red.getGreen() + yellowRatio * yellow.getGreen());
					colorR = (int)(redRatio * red.getRed() + yellowRatio * yellow.getRed());
				}
				else{
					double greenRatio = 1.0 - deathInfluenceValue;
					double yellowRatio = 1.0 - greenRatio;
					
					colorB = (int)(greenRatio * green.getBlue() + yellowRatio * yellow.getBlue());
					colorG = (int)(greenRatio * green.getGreen() + yellowRatio * yellow.getGreen());
					colorR = (int)(greenRatio * green.getRed() + yellowRatio * yellow.getRed());
				}
				
				//System.out.println(colorR + " " + colorG + " " + colorB);
				Color color = new Color(colorR, colorG, colorB, 100);
				g.setColor(color);
				g.fillRect((int) (pos.x), (int) (pos.y), 
						Globals.knowledgeBase.getPixelsPerBlock(), Globals.knowledgeBase.getPixelsPerBlock());
			}
		}*/
	}

}
