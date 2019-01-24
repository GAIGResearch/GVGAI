package agents.MaastCTS2;

import ontology.Types.ACTIONS;
import agents.MaastCTS2.libs.it.unimi.dsi.util.XorShift64StarRandom;
import agents.MaastCTS2.utils.OrderedIntPair;

/**
 * Some globals used throughout the entire agent's code
 *
 * @author Dennis Soemers
 */
public class Globals {
	
	/** A large constant score added to winning game states and subtracted from losing game states */
	public static final double HUGE_ENDGAME_SCORE = 10000000.0;
	
	/** Random Number Generator */
	public static final XorShift64StarRandom RNG = new XorShift64StarRandom();
	
	/** Knowledge Base for the current game/level */
	public static KnowledgeBase knowledgeBase;
	
	/** If true, we'll draw some stuff for debugging */
	public static final boolean DEBUG_DRAW = false;
	
	public static boolean isMovementAction(ACTIONS action){
		return (action != ACTIONS.ACTION_ESCAPE &&
				action != ACTIONS.ACTION_NIL &&
				action != ACTIONS.ACTION_USE);
	}
	
	public static boolean isOppositeMovement(ACTIONS a1, ACTIONS a2){
		if(a1 == ACTIONS.ACTION_DOWN){
			return (a2 == ACTIONS.ACTION_UP);
		}
		else if(a1 == ACTIONS.ACTION_LEFT){
			return (a2 == ACTIONS.ACTION_RIGHT);
		}
		else if(a1 == ACTIONS.ACTION_RIGHT){
			return (a2 == ACTIONS.ACTION_LEFT);
		}
		else if(a1 == ACTIONS.ACTION_UP){
			return (a2 == ACTIONS.ACTION_DOWN);
		}
		
		return false;
	}
	
	/**
	 * Computes the Manhattan distance between two given cells
	 * 
	 * @param cell1
	 * @param cell2
	 * @return
	 */
	public static int manhattanDistance(OrderedIntPair cell1, OrderedIntPair cell2){
		return (Math.abs(cell1.first - cell2.first) + Math.abs(cell1.second - cell2.second));
	}
	
	public static int minHorizontalOrVerticalDist(OrderedIntPair cell1, OrderedIntPair cell2){
		return Math.min(Math.abs(cell1.first - cell2.first), Math.abs(cell1.second - cell2.second));
	}
	
	/**
	 * Normalises the given value to lie in [0.0, 1.0] given the provided
	 * 'min' and 'max' bounds.
	 * 
	 * <p> Different from Utils::normalise() implementation of the GVG framework
	 * in that this method returns a value of 0.5 if min >= max, instead of returning
	 * the given value.
	 * 
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static double normalise(double value, double min, double max){
		if(min < max){
			return (value - min) / (max - min);
		}
		else{
			return 0.5;
		}
	}
	
	/**
	 * Returns a small number in [-0.0000005, 0.0000005] for noise
	 * 
	 * @return
	 */
	public static double smallNoise(){
		return (RNG.nextDouble() - 0.5) * 0.000001;
	}

}
