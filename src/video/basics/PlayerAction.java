package video.basics;

import core.game.Event;
import ontology.Types.ACTIONS;

public class PlayerAction extends GameEvent {
	
	public String action;
	
	public PlayerAction(){}
	
	public PlayerAction(String gameTick, String action)
	{
		this.gameTick = gameTick;
		this.action = action;
	}
	
	public PlayerAction(String action) {
		this.action = action;
	}

//	public int compareTo(PlayerAction o) {
		// TODO Auto-generated method stub
//		if(action.equals(o.action)) {
//			return 1;
//		}
//		return 0;
//	}
	
	
	public String toString() {
		return "Avatar " + action;
	}


}
