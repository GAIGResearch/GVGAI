package agents.MaastCTS2.iw;

/**
 * A pair consisting of two ObservedSprite objects.
 * Used for testing if states have a novelty value of 2
 * 
 * <p>NOTE: Checks for null have been omitted in hashCode() and equals() for optimization,
 * so there should never be pairs created where either of the objects is null.
 *
 * @author Dennis Soemers
 */
public class PairObservedSprites {
	
	public final ObservedSprite first;
	public final ObservedSprite second;
	
	public PairObservedSprites(ObservedSprite first, ObservedSprite second){
		this.first = first;
		this.second = second;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + first.hashCode();
		result = prime * result + second.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object otherObject) {
		if (this == otherObject){
			return true;
		}
		
		// don't need explicit null check if we're doing instanceof right afterwards
		//if (otherObject == null){
		//	return false;
		//}
		
		if (!(otherObject instanceof PairObservedSprites)){
			return false;
		}
		
		PairObservedSprites other = (PairObservedSprites) otherObject;
		return (first.equals(other.first)	&&
				second.equals(other.second)		);
	}

}
