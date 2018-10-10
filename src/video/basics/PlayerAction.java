package video.basics;

import ontology.Types.ACTIONS;

public class PlayerAction {
	
	public String gameTick;
	public String action;
	
	public PlayerAction(){}
	
	public PlayerAction(String gameTick, String action)
	{
		this.gameTick = gameTick;
		this.action = action;
	}

}
