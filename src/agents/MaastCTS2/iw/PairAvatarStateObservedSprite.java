package agents.MaastCTS2.iw;

/**
 * A pair consisting of an AvatarState and an ObservedSprite object.
 * Used for testing if states have a novelty value of 3/2
 * 
 * <p>NOTE: Checks for null have been omitted in hashCode() and equals() for optimization,
 * so there should never be pairs created where either of the objects is null.
 *
 * @author Dennis Soemers
 */
public class PairAvatarStateObservedSprite {
	
	public final AvatarState avatarState;
	public final ObservedSprite observedSprite;
	
	public PairAvatarStateObservedSprite(AvatarState avatarState, ObservedSprite observedSprite){
		this.avatarState = avatarState;
		this.observedSprite = observedSprite;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + avatarState.hashCode();
		result = prime * result + observedSprite.hashCode();
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
		
		if (!(otherObject instanceof PairAvatarStateObservedSprite)){
			return false;
		}
		
		PairAvatarStateObservedSprite other = (PairAvatarStateObservedSprite) otherObject;
		return (avatarState.equals(other.avatarState)		&&
				observedSprite.equals(other.observedSprite)		);
	}

}
