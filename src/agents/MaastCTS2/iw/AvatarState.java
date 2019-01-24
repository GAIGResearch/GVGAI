package agents.MaastCTS2.iw;

import agents.MaastCTS2.Globals;
import core.game.StateObservation;
import tools.Vector2d;

/**
 * Contains data describing the state of the Avatar at a certain point in the game.
 * Used for novelty tests.
 * 
 * <p>Contains the same data as described in:
 *  Tomas Geffner and Hector Geffner. Width-based Planning for General 
 *  Video-Game Playing. ( link: http://giga15.ru.is/giga15-paper2.pdf )
 *
 * @author Dennis Soemers
 */
public class AvatarState {
	
	public final int position;
	public final int orientationX;
	public final int orientationY;
	public final int avatarType;
	
	public AvatarState(StateObservation stateObs){
		Vector2d position = stateObs.getAvatarPosition();
		Vector2d orientation = stateObs.getAvatarOrientation();
				
		this.position = Globals.knowledgeBase.positionToInt(position);
		orientationX = (int) orientation.x;
		orientationY = (int) orientation.y;
		
		if(stateObs.isGameOver()){
			avatarType = -1;	// need to do this because in some games the avatar becomes null when the game ends
		}
		else{
			avatarType = stateObs.getAvatarType();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + avatarType;
		result = prime * result + orientationX;
		result = prime * result + orientationY;
		result = prime * result + position;
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
		
		if (!(otherObject instanceof AvatarState)){
			return false;
		}
		
		AvatarState other = (AvatarState) otherObject;
		return (position == other.position 			&&
				orientationX == other.orientationX 	&&
				orientationY == other.orientationY 	&&
				avatarType == other.avatarType			);
	}

}
