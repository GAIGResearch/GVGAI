package homeTest;

import ontology.Types.ACTIONS;
import core.game.Observation;


class Feature {
	private float weight; //Reward for this feature
	private boolean type; //0 = Move, 1 = Use
	private String sprite0;
	private String sprite1;
	private float rep; //How many times a reward can be given
	private String avatarState; //An avatar can have different states in it
	// 0 = each
	// 1 all
	private boolean method;
	
	
	public Feature(float weight, boolean type, String sprite0, String sprite1, float rep,
			String avatarState, boolean method) {
		this.weight = weight;
		this.type = type;
		this.sprite0 = sprite0;
		this.sprite1 = sprite1;
		this.rep = rep;
		this.avatarState = avatarState;
		this.method = method;
	}
	
	
	public String toString() {
		String typeString;
		if(!this.type) {
			typeString = "Move";
		}else {
			typeString = "Use";
		}
		
		String methodString;
		if(!method) {
			methodString = "Each";
		}else {
			methodString = "All";
		}
		String result = "<" + this.sprite0 + ',' + this.sprite1 + ',' + this.weight + ',' +
				 methodString + ',' + typeString + ',' + this.rep + "," + this.avatarState + '>';
		//
		
		return result;
	}
	
}
