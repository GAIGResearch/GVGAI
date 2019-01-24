package agents.MaastCTS2.iw;

import java.util.ArrayList;
import java.util.HashSet;

import agents.MaastCTS2.Globals;
import agents.MaastCTS2.gnu.trove.impl.Constants;
import agents.MaastCTS2.gnu.trove.map.hash.TIntIntHashMap;
import core.game.Observation;
import core.game.StateObservation;

/**
 * Objects of this class encapsulate all the features of a game state that need
 * to be memorized for a NoveltyTester to perform novelty tests.
 *
 * @author Dennis Soemers
 */
public class StateMemory {
	
	public final TIntIntHashMap numObjectsPerType;
	public final HashSet<ObservedSprite> observedSprites;
	public final AvatarState avatarState;
	public final double gameScore;
	public final int avatarHP;
	
	/** true if this state is a terminal state, or if it improves upon the previous game state's score */
	public boolean importantGameStateChange;
	
	public ArrayList<PairAvatarStateObservedSprite> cachedAvatarSpritePairs;
	public ArrayList<PairObservedSprites> cachedSpritePairs;
	
	public StateMemory(StateObservation stateObs, StateMemory previousStateMemory){
		// TODO investigate if 8 is a reasonable initial capacity
		numObjectsPerType = new TIntIntHashMap(8, Constants.DEFAULT_LOAD_FACTOR, -1, -1);				
		observedSprites = new HashSet<ObservedSprite>(128);		// TODO investigate if 128 is a reasonable initial capacity
		avatarState = new AvatarState(stateObs);
		gameScore = stateObs.getGameScore();
		avatarHP = stateObs.getAvatarHealthPoints();
		importantGameStateChange = (stateObs.isGameOver() ||
									(previousStateMemory != null && (gameScore > previousStateMemory.gameScore ||
																	avatarHP > previousStateMemory.avatarHP)));
		
		ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
		
		int width = observationGrid.length;
		int height = observationGrid[0].length;
		
		for (int x = 0; x < width; ++x){
			for (int y = 0; y < height; ++y){
				ArrayList<Observation> observations = observationGrid[x][y];
				int numObservations = observations.size();
				for (int i = 0; i < numObservations; ++i){
					Observation observation = observations.get(i);	// TODO ignore walls?
					
					if(!Globals.knowledgeBase.isAvatarCategory(observation.category)){
						int observationType = observation.itype;
						observedSprites.add(new ObservedSprite(observation.position, observationType));
						
						int numTypeObservations = numObjectsPerType.get(observationType);
						if(numTypeObservations == -1){
							numObjectsPerType.put(observationType, 1);
						}
						else{
							numObjectsPerType.increment(observationType);
						}
					}
				}
			}
		}
		
		cachedAvatarSpritePairs = null;
		cachedSpritePairs = null;
	}

}
