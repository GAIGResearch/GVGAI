package agents.MaastCTS2.iw;

import java.util.ArrayList;
import java.util.HashSet;

import agents.MaastCTS2.utils.OrderedIntPair;


/**
 * An object of this class memorizes boolean features that have been true in game states
 * previously passed into the object, and can be used for novelty tests as seen in the Iterated Width 
 * (IW) algorithm.
 *
 * <p>( Nir Lipovetzky and Hector Geffner. Width and Serialization of Classical Planning Problems. 
 * link: https://repositori.upf.edu/bitstream/handle/10230/20578/FAIA242-0540.pdf?sequence=1 )
 *
 * @author Dennis Soemers
 */
public class NoveltyTester {
	/**
	 * The novelty of a state is defined as the size of the smallest unordered tuple of atoms that
	 * is true in that state and was false in all previously generated states.
	 * 
	 * <p>For example, if a state makes some atom true that was not true in previously generated states,
	 * it has a novelty of 1. If it does not have novelty = 1, but it makes some pair of atoms true
	 * that was not true in previously generated states, the novelty of the state is 2, etc.
	 * 
	 * <p>Being able to precisely compute the novelty value of states that have a high novelty value
	 * (for instance, novelty = 5) requires a huge amount of memory and likely also processing time
	 * to generate and memorize all the observed tuples up to such a large size.
	 * 
	 * <p>Therefore, we only support the exact computation of novelty values of states with a low novelty,
	 * and for other states we are only able to say that they have a higher novelty value than supported
	 * (without being able to return the EXACT value).
	 * 
	 * <p>A NoveltyTester object needs to be set to one of the modes in this enum first to determine which
	 * novelty values are supported.
	 *
	 * @author Dennis Soemers
	 */
	public static enum NOVELTY_TEST_MODES{
		/** The mode for novelty tests has not been chosen yet, and therefore novelty tests cannot be performed */
		NOT_SET,
		
		/** Novelty tests will only distinguish between states with a novelty of 1, and >1 */
		One,
		/** Novelty tests will only distinguish between states with a novelty of 1, 2, and >2 */
		Two,
		/** 
		 * Novelty tests will only distinguish between states with a novelty of 1, 3/2, and >(3/2).
		 * 
		 * A novelty of 3/2 is assigned to states that do not have a novelty of 1, but make a pair
		 * of atoms true where at least one of the atoms in the pair is a feature of the avatar/player.
		 * 
		 * This idea is from: Tomas Geffner and Hector Geffner. Width-based Planning for General
		 * Video-Game Playing. ( link: http://giga15.ru.is/giga15-paper2.pdf )
		 */
		ThreeOverTwo
	}
	
	/**
	 * The different novelty levels that can be returned by novelty tests
	 *
	 * @author Dennis Soemers
	 */
	public static enum NOVELTY_LEVELS{
		/** Returned when a state is not novel or its novelty value is higher than we care to compute */
		NotNovel,
		/** Returned for states with a novelty of 1 */
		One,
		/** Returned for states with a novelty of 3/2 */
		ThreeOverTwo,
		/** Returned for states with a novelty of 2 */
		Two
	}
	
	/** The mode that this Novelty Tester will use for novelty tests */
	private NOVELTY_TEST_MODES noveltyTestMode = NOVELTY_TEST_MODES.NOT_SET;
	
	/** A Hash Set of all Avatar States that have been previously observed */
	private HashSet<AvatarState> oldAvatarStates;
	
	/** A Hash Set of all previous observations of sprites */
	private HashSet<ObservedSprite> oldSpriteObservations;
	
	/** A Hash Set of all pairs (object type, object count) previously observed */
	private HashSet<OrderedIntPair> oldTypeObjectCountPairs;
	
	/** A Hash Set of all previous pairs of Avatar States and Observed Sprites */
	private HashSet<PairAvatarStateObservedSprite> oldAvatarSpritePairs;
	
	/** A Hash Set of all (unordered) pairs of two Observed Sprites */
	private HashSet<PairObservedSprites> oldObservedSpritePairs;
	
	/**
	 * Performs a novelty test for the state of which the state memory is given
	 * 
	 * @param stateMemory The memory of the state to test for
	 * @param memorize If true, will memorize any features novel in the given state so they will no longer be considered novel in future tests
	 * @return
	 */
	public NOVELTY_LEVELS noveltyTest(StateMemory stateMemory, boolean memorize){
		if(noveltyTestMode == NOVELTY_TEST_MODES.NOT_SET){
			System.err.println("NoveltyTester::noveltyTestAndMemorize(): Cannot perform novelty test when test mode has not been set!");
			return NOVELTY_LEVELS.NotNovel;
		}
		
		ArrayList<ObservedSprite> observedSprites = null;
		if(noveltyTestMode != NOVELTY_TEST_MODES.One){
			// we'll want to store the observed sprites in a list so that we can iterate through them
			// in a proper way to create unordered pairs
			observedSprites = new ArrayList<ObservedSprite>(stateMemory.observedSprites.size());
		}
		
		if(!memorize){
			// if we don't care about memorizing novel (tuples of) atoms, we can perform novelty tests more efficiently,
			// because we can terminate as soon as we find some novel atom or novel tuple of atoms, and we don't need to
			// continue looping through other atoms to add them in case they are novel too
			
			// check if the state is terminal or if it improves upon previous state's score, then we'll always consider it novel
			if(stateMemory.importantGameStateChange){
				return NOVELTY_LEVELS.One;
			}
			
			// single atoms
			if(!oldAvatarStates.contains(stateMemory.avatarState)){
				return NOVELTY_LEVELS.One;		// found a novel atom
			}
			
			for(int type : stateMemory.numObjectsPerType.keys()){
				int numObjects = stateMemory.numObjectsPerType.get(type);
				if(!oldTypeObjectCountPairs.contains(new OrderedIntPair(type, numObjects))){
					// without setting the importantGameStateChange flag here, it seems like the agent will
					// NEVER play moves that, for example, transform the key into a missile in a game like BrainMan
					// TODO find a cleaner solution for this
					stateMemory.importantGameStateChange = true;
					
					return NOVELTY_LEVELS.One;	// found a novel atom
				}
			}
			
			for(ObservedSprite observedSprite : stateMemory.observedSprites){
				if(!oldSpriteObservations.contains(observedSprite)){
					return NOVELTY_LEVELS.One;		// found a novel atom
				}
				
				if(noveltyTestMode != NOVELTY_TEST_MODES.One){
					observedSprites.add(observedSprite);	// store the observation in list so we can create pairs later if necessary
				}
			}
			
			// we don't have a novelty of 1 if we reach this line
			if(noveltyTestMode != NOVELTY_TEST_MODES.One){
				// TODO pairs with type counts?
				
				// we want to test for novelty of 3/2 (and maybe also 2)
				if(stateMemory.cachedAvatarSpritePairs != null){
					// we've already generated these pairs and cached them
					for(PairAvatarStateObservedSprite pair : stateMemory.cachedAvatarSpritePairs){
						if(!oldAvatarSpritePairs.contains(pair)){
							return NOVELTY_LEVELS.ThreeOverTwo;		// found a novel pair of atoms of which one is the avatar state
						}
					}
				}
				else{
					// didn't generate and cache these pairs yet
					ArrayList<PairAvatarStateObservedSprite> pairs = new ArrayList<PairAvatarStateObservedSprite>(observedSprites.size());
					for(ObservedSprite observedSprite : observedSprites){
						// test if pair of our current avatar state with this observed sprite is novel
						PairAvatarStateObservedSprite pair = new PairAvatarStateObservedSprite(stateMemory.avatarState, observedSprite);
						if(!oldAvatarSpritePairs.contains(pair)){
							return NOVELTY_LEVELS.ThreeOverTwo;		// found a novel pair of atoms of which one is the avatar state
						}
						pairs.add(pair);
					}
					
					// cache the list of pairs in case it's useful for future novelty tests
					stateMemory.cachedAvatarSpritePairs = pairs;
				}
				
				// we don't have a novelty of 3/2 if we reach this line
				if(noveltyTestMode != NOVELTY_TEST_MODES.ThreeOverTwo){					
					// we also want to test for novelty of 2
					if(stateMemory.cachedSpritePairs != null){
						// we've already generated these pairs and cached them
						for(PairObservedSprites pair : stateMemory.cachedSpritePairs){
							if(!oldObservedSpritePairs.contains(pair)){
								return NOVELTY_LEVELS.Two;		// found a novel pair of atoms
							}
						}
					}
					else{
						// didn't generate and cache these pairs yet
						ArrayList<PairObservedSprites> pairs = new ArrayList<PairObservedSprites>((observedSprites.size() * (observedSprites.size() - 1)) / 2);
						for(int i = 0; i < observedSprites.size(); ++i){
							// for every observed sprite after the one indexed by i, create a pair
							ObservedSprite first = observedSprites.get(i);
							for(int j = i + 1; j < observedSprites.size(); ++ j){
								ObservedSprite second = observedSprites.get(j);
								PairObservedSprites pair = new PairObservedSprites(first, second);
								if(!oldObservedSpritePairs.contains(pair)){
									return NOVELTY_LEVELS.Two;		// found a novel pair of atoms
								}
								pairs.add(pair);
							}
						}
						
						// cache the list of pairs in case it's useful for future novelty tests
						stateMemory.cachedSpritePairs = pairs;
					}
				}
			}
		}
		else{
			// we don't only want to return the result of the novelty test, but also memorize any novel
			// (tuples of) atoms encountered. This means that we cannot terminate as soon as we find
			// something novel, but need to examine every atom and/or tuple of atoms
			boolean novelty1 = false;
			boolean novelty3_2 = false;
			boolean novelty2 = false;
			
			// check if the state is terminal or if it improves upon previous state's score, then we'll always consider it novel
			novelty1 = (novelty1 || stateMemory.importantGameStateChange);
			
			// single atoms
			boolean avatarStateAdded = oldAvatarStates.add(stateMemory.avatarState);
			novelty1 = (novelty1 || avatarStateAdded);
			
			for(int type : stateMemory.numObjectsPerType.keys()){
				int numObjects = stateMemory.numObjectsPerType.get(type);
				boolean pairAdded = oldTypeObjectCountPairs.add(new OrderedIntPair(type, numObjects));
				novelty1 = (novelty1 || pairAdded);
				
				// TODO pairs with type counts?
			}
			
			for(ObservedSprite observedSprite : stateMemory.observedSprites){
				boolean spriteAdded = oldSpriteObservations.add(observedSprite);
				novelty1 = (novelty1 || spriteAdded);
				
				if(noveltyTestMode != NOVELTY_TEST_MODES.One){
					observedSprites.add(observedSprite);	// store the observation in list so we can create pairs later if necessary
				}
			}
			
			if(noveltyTestMode != NOVELTY_TEST_MODES.One){
				// we want to test for novelty of 3/2 (and maybe also 2)
				if(stateMemory.cachedAvatarSpritePairs != null){
					// we've already generated these pairs and cached them
					for(PairAvatarStateObservedSprite pair : stateMemory.cachedAvatarSpritePairs){
						boolean pairAdded = oldAvatarSpritePairs.add(pair);
						novelty3_2 = (novelty3_2 || pairAdded);
					}
				}
				else{
					// didn't generate and cache these pairs yet
					ArrayList<PairAvatarStateObservedSprite> pairs = new ArrayList<PairAvatarStateObservedSprite>(observedSprites.size());
					for(ObservedSprite observedSprite : observedSprites){
						// test if pair of our current avatar state with this observed sprite is novel
						PairAvatarStateObservedSprite pair = new PairAvatarStateObservedSprite(stateMemory.avatarState, observedSprite);
						boolean pairAdded = oldAvatarSpritePairs.add(pair);
						novelty3_2 = (novelty3_2 || pairAdded);
						pairs.add(pair);
					}
					
					// cache the list of pairs in case it's useful for future novelty tests
					stateMemory.cachedAvatarSpritePairs = pairs;
				}
				
				if(noveltyTestMode != NOVELTY_TEST_MODES.ThreeOverTwo){
					// we also want to test for novelty of 2
					if(stateMemory.cachedSpritePairs != null){
						// we've already generated these pairs and cached them
						for(PairObservedSprites pair : stateMemory.cachedSpritePairs){
							boolean pairAdded = oldObservedSpritePairs.add(pair);
							novelty2 = (novelty2 || pairAdded);
						}
					}
					else{
						// didn't generate and cache these pairs yet
						ArrayList<PairObservedSprites> pairs = new ArrayList<PairObservedSprites>((observedSprites.size() * (observedSprites.size() - 1)) / 2);
						for(int i = 0; i < observedSprites.size(); ++i){
							// for every observed sprite after the one indexed by i, create a pair
							ObservedSprite first = observedSprites.get(i);
							for(int j = i + 1; j < observedSprites.size(); ++ j){
								ObservedSprite second = observedSprites.get(j);
								PairObservedSprites pair = new PairObservedSprites(first, second);
								boolean pairAdded = oldObservedSpritePairs.add(pair);
								novelty2 = (novelty2 || pairAdded);
								pairs.add(pair);
							}
						}
						
						// cache the list of pairs in case it's useful for future novelty tests
						stateMemory.cachedSpritePairs = pairs;
					}
				}
			}
			
			if(novelty1){
				return NOVELTY_LEVELS.One;
			}
			else if(novelty3_2){
				return NOVELTY_LEVELS.ThreeOverTwo;
			}
			else if(novelty2){
				return NOVELTY_LEVELS.Two;
			}
		}
		
		return NOVELTY_LEVELS.NotNovel;
	}
	
	/**
	 * Sets the mode for novelty tests to the given mode.
	 * 
	 * <p>Cannot be used to change the mode anymore after it has been set to something other
	 * than NOT_SET.
	 * 
	 * @param testMode
	 */
	public void setNoveltyTestMode(NOVELTY_TEST_MODES testMode){
		if(noveltyTestMode != NOVELTY_TEST_MODES.NOT_SET){
			System.err.println("NoveltyTester::setNoveltyTestMode(): Cannot change novelty test mode once it has been set!");
			return;
		}
		
		noveltyTestMode = testMode;
		oldAvatarStates = new HashSet<AvatarState>(8);	// the NovTea agent uses 600 as initial capacity. TODO check if this is reasonable
		oldSpriteObservations = new HashSet<ObservedSprite>(256);		// TODO also check if this is a reasonable initial capacity
		oldTypeObjectCountPairs = new HashSet<OrderedIntPair>(16);
		
		if(noveltyTestMode != NOVELTY_TEST_MODES.One){
			// we'll need space for pairs of Avatar States and Observed Sprites
			oldAvatarSpritePairs = new HashSet<PairAvatarStateObservedSprite>(8192);	// TODO check if 8192 is reasonable initial capacity
			
			if(noveltyTestMode != NOVELTY_TEST_MODES.ThreeOverTwo){
				// we'll need space for pairs of two Observed Sprites
				oldObservedSpritePairs = new HashSet<PairObservedSprites>(8192);	// TODO check if 8192 is reasonable initial capacity
			}
		}
	}
	
	public void printSizes(){
		System.out.println();
		System.out.println("oldAvatarStates.size() = " + oldAvatarStates.size());
		System.out.println("oldSpriteObservations.size() = " + oldSpriteObservations.size());
		System.out.println("oldTypeObjectCountPairs.size() = " + oldTypeObjectCountPairs.size());
		//System.out.println("oldAvatarSpritePairs.size() = " + oldAvatarSpritePairs.size());
		//System.out.println("oldObservedSpritePairs.size() = " + oldObservedSpritePairs.size());
	}
}
