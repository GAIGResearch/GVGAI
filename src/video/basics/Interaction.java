package video.basics;

import core.game.Event;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 12/02/2018
 * @author Tiago Machado
 */
public class Interaction extends GameEvent{
	
	public String rule;
	public String sprite1;
	public String sprite2;
	public String pairInteractionTick;
	
	public Interaction(String tick, String interaction, String sprite1, String sprite2)
	{
		this.rule = interactionName(interaction);
		this.sprite1 = sprite1;
		this.sprite2 = sprite2;
		this.gameTick = tick;
		this.pairInteractionTick = tick + " - " + this.rule;
	}
	
	public Interaction(String interaction, String sprite1, String sprite2)
	{
		this.rule = interaction;
		this.sprite1 = sprite1;
		this.sprite2 = sprite2;
	}
	
	public static String interactionName(String interaction)
	{
		String reverse = new StringBuilder(interaction).reverse().toString();
		int index = reverse.indexOf(".");
		String interactionNameReturn = reverse.substring(0, index);
		interactionNameReturn = new StringBuilder(interactionNameReturn).reverse().toString();
		
		return interactionNameReturn;
	}
	
	public static String spriteName(String sprite)
	{
		String reverse = new StringBuilder(sprite).reverse().toString();
		int index = reverse.indexOf(".");
		String spriteNameReturn = reverse.substring(0, index);
		spriteNameReturn = new StringBuilder(spriteNameReturn).reverse().toString();
		
		return spriteNameReturn;
	}
	
	public Interaction()
	{
		
	}
	
	public static void main(String [] args)
	{
		System.out.println(interactionName("class.xyz.killSprite"));
	}
	
	
	public String toString() {
		return sprite1 + " " + sprite2 + " " + rule; 
	}



}
