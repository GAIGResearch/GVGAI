package agents.MaastCTS2.model;


public class ActionNGram {
	private final ActionLocation[] actions;
	
	/** 
	 * It seems like a good idea to cache the hash code, because: 
	 * 1) we ALWAYS need it anyway once we construct an ActionNGram object, 
	 * 2) it is fairly expensive to compute, with a loop through an array, and
	 * 3) we sometimes need the same hash code twice in a row
	 */
	private final int cachedHashCode;

	public ActionNGram(ActionLocation[] actions) {
		this.actions = actions;
		
		final int prime = 31;
		int result = 1;
		
		for(ActionLocation action : actions){
			result = prime * result + action.hashCode();
		}
		
		cachedHashCode = result;
	}
	
	private ActionLocation[] getActions(){
		return actions;
	}

	@Override
	public int hashCode() {
		return cachedHashCode;
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
		
		if(!(obj instanceof ActionNGram)){
			return false;
		}
		
		ActionNGram other = (ActionNGram) obj;
		ActionLocation[] otherActions = other.getActions();
		
		if(actions.length != otherActions.length){
			return false;
		}
		
		for(int i = 0; i < actions.length; ++i){
			if(!(actions[i].equals(otherActions[i]))){
				return false;
			}
		}

		return true;
	}
	
	@Override
	public String toString(){
		String result = "[";
		for(int i = 0; i < actions.length; ++i){
			result += actions[i].getAction();
			
			if(i < actions.length - 1){
				result += ", ";
			}
		}
		return result + "]";
	}
	
}
