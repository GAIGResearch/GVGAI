package agents.MaastCTS2.model;

import ontology.Types.ACTIONS;

/**
 * Encapsulates an action with a location in which the avatar executes that action
 *
 * @author Dennis Soemers
 */
public class ActionLocation {
	
	private final ACTIONS action;
	private final int avatarCell;
	
	public ActionLocation(ACTIONS action, int avatarCell){
		this.action = action;
		this.avatarCell = avatarCell;
	}
	
	public ACTIONS getAction(){
		return action;
	}
	
	public int getAvatarCell(){
		return avatarCell;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		// ordinal() seems safe here instead of hashCode() since we're mixing it with another value, 
		// and we're not planning to also access the same HashMap with different enums than ACTIONS
		// (which would result in many collisions if multiple different enums were returning the same
		// values with ordinal())
		result = prime * result + action.ordinal();
		result = prime * result + avatarCell;
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		
		// don't need explicit null check if we're doing instanceof right afterwards
		//if (obj == null){
		//	return false;
		//}
		
		if(!(obj instanceof ActionLocation)){
			return false;
		}
		
		ActionLocation other = (ActionLocation) obj;
		return (action == other.getAction() && avatarCell == other.getAvatarCell());
	}

}
