package video.query;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.parser.ParseException;

import tutorialGeneration.VisualDemonstrationInterfacer;
import video.basics.BunchOfGames;
import video.basics.Interaction;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 03/07/2018
 * @author Tiago Machado
 */

public class ScalableQuery extends Query {
	
	public ScalableQuery()
	{
		super();
	}
	
	//
	public int[] numberOfUniqueInteractions(int numberOfSimulations) throws FileNotFoundException, IOException, ParseException
	{
		int [] collection = new int[numberOfSimulations];
		for (int i = 0; i < numberOfSimulations; i++) 
		{
			String interactionFile = "simulation/game" + i + "/interactions/interaction.json";
			collection[i] = super.numberOfUniqueInteractions(interactionFile);
		}
		return collection;
	}
	
	//
	public int[] numberOfInteractedSprites(int numberOfSimulations, String game) throws FileNotFoundException, IOException, ParseException
	{
		int [] collection = new int[numberOfSimulations];
		for (int i = 0; i < numberOfSimulations; i++) 
		{
			String interactionFile = "simulation/game" + i + "/interactions/interaction.json";
			collection[i] = super.numberofInteractedSprites(interactionFile, game);
		}
		return collection;
	}
	
	//
	public int[] numberOfInteractedSpritesWithThisSprite(String sprite, int numberOfSimulations, String game) throws FileNotFoundException, IOException, ParseException
	{
		int [] collection = new int[numberOfSimulations];
		for (int i = 0; i < numberOfSimulations; i++) 
		{
			String interactionFile = "simulation/game" + i + "/interactions/interaction.json";
			collection[i] = super.numberofInteractedSpritesWithThisParticularSprite(sprite, interactionFile, game);
		}
		return collection;
	}
	
	//
	public int[] firstFrameOfInteraction(int numberOfSimulations) throws FileNotFoundException, IOException, ParseException
	{
		int [] collection = new int[numberOfSimulations];
		for (int i = 0; i < numberOfSimulations; i++) 
		{
			String interactionFile = "simulation/game" + i + "/interactions/interaction.json";
			collection[i] = super.firstFrameOfInteraction(interactionFile);
		}
		return collection;
	}
	
	//
	public int[] firstFrameOfSpecifiedInteraction(Interaction interaction, int numberOfSimulations) throws FileNotFoundException, IOException, ParseException
	{
		int [] collection = new int[numberOfSimulations];
		for (int i = 0; i < numberOfSimulations; i++) 
		{
			String interactionFile = "simulation/game" + i + "/interactions/interaction.json";
			collection[i] = super.firstFrameOfThisInteraction(interaction, interactionFile);
		}
		return collection;
	}
	
	//
	public int [] numberOfFramesUntillThisTerminalCondition(Interaction terminalCondition, int numberOfSimulations) throws FileNotFoundException, IOException, ParseException
	{
		int [] collection = new int[numberOfSimulations];
		for (int i = 0; i < numberOfSimulations; i++) 
		{
			String interactionFile = "simulation/game" + i + "/interactions/interaction.json";
			collection[i] = super.retrieveTerminalInteraction(terminalCondition, interactionFile);
		}
		return collection;
	}
	
//	public static void main (String [] args) throws FileNotFoundException, IOException, ParseException
//	{
//		BunchOfGames bog1 = new BunchOfGames("examples/gridphysics/zelda.txt", 
//				"examples/gridphysics/zelda_lvl0.txt", "tracks.singlePlayer.tools.human.Agent");
//		BunchOfGames bog2 = new BunchOfGames("examples/gridphysics/zelda.txt", 
//				"examples/gridphysics/zelda_lvl0.txt", "tracks.singlePlayer.tools.human.Agent");
//		BunchOfGames bog3 = new BunchOfGames("examples/gridphysics/zelda.txt", 
//				"examples/gridphysics/zelda_lvl0.txt", "tracks.singlePlayer.tools.human.Agent");
//		ArrayList<BunchOfGames> bunchOfGames = new ArrayList<>();
//		bunchOfGames.add(bog1); bunchOfGames.add(bog2); bunchOfGames.add(bog3);
//		VisualDemonstrationInterfacer vdi = new VisualDemonstrationInterfacer(true);
//		vdi.runBunchOfGames(bunchOfGames);
//		
//		ScalableQuery scalableQuery = new ScalableQuery();
//		
//		//1 - Gives the number of different interactions in all the games
//		int [] numberOfInteractions = scalableQuery.numberOfUniqueInteractions(3);
//		for (int i = 0; i < numberOfInteractions.length; i++) {
//			System.out.print(numberOfInteractions[i] + " ");
//		}
//		System.out.println();
//		
//		//2 - Gives the number of different sprites which collided with the avatar
//		int [] numberOfInteractedSprites = scalableQuery.numberOfInteractedSprites(3, "examples/gridphysics/zelda.txt");
//		for (int i = 0; i < numberOfInteractedSprites.length; i++) {
//			System.out.print(numberOfInteractedSprites[i] + " ");
//		}
//		System.out.println();
//		
//		//3 - Gives the number of different sprites which collided with the specified sprite - sword in this example
//		int [] numberOfInteractedSpritesWithThisParticularSprite
//			= scalableQuery.numberOfInteractedSpritesWithThisSprite("sword", 3, "examples/gridphysics/zelda.txt");
//		for (int i = 0; i < numberOfInteractedSpritesWithThisParticularSprite.length; i++) {
//			System.out.print(numberOfInteractedSpritesWithThisParticularSprite[i] + " ");
//		}
//		System.out.println();
//		
//		//4 - gives you how many frames it lasts until the first interaction (at all) happens
//		int [] firstFrameOfInteraction = scalableQuery.firstFrameOfInteraction(3);
//		for (int i = 0; i < firstFrameOfInteraction.length; i++) {
//			System.out.print(firstFrameOfInteraction[i] + " ");
//		}
//		System.out.println();
//		
//		//5 - gives you how many frames it lasts until the first interaction specified happens
//		int[] firstFrameOfSpecifiedInteraction = scalableQuery.
//				firstFrameOfSpecifiedInteraction(new Interaction("KillSprite", "monsterNormal", "sword"),
//						3);
//		for (int i = 0; i < firstFrameOfSpecifiedInteraction.length; i++) {
//			System.out.print(firstFrameOfSpecifiedInteraction[i] + " ");
//		}
//		System.out.println();
//		
//		//6 - gives you how many frames until this termination (win) condition 
//		int [] numberOfFramesUntillThisTermination = scalableQuery.
//				numberOfFramesUntillThisTerminalCondition(new Interaction("KillSprite", "goal", "withkey"), 3);
//		for (int i = 0; i < numberOfFramesUntillThisTermination.length; i++) {
//			System.out.print(numberOfFramesUntillThisTermination[i] + " ");
//		}
//		System.out.println();
//	}

}
