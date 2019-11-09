package video.query;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import core.content.SpriteContent;
import core.vgdl.Node;
import core.vgdl.VGDLParser;
import tools.IO;
import video.basics.Interaction;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 03/06/2018
 * @author Tiago Machado
 */

/*Number of unique interactions, Number of interacted sprites, 
 * Average number of frames before interaction, Average number
 *  of frames towards win, Average number of frames towards lose*/
public class Query {
	
	public HashSet<String> avatarNames;
	
	public Query()
	{
		avatarNames = new HashSet<>();
	}
	
	public int numberOfUniqueInteractions(String interactionFile) throws FileNotFoundException, IOException, ParseException
	{
		HashSet<String> uniqueInteractions = new HashSet<String>();
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(interactionFile));
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject obj = (JSONObject)interactionArray.get(i);
			uniqueInteractions.add((String) obj.get("interaction"));
		}
		return uniqueInteractions.size();
	}
	
	public int numberofInteractedSprites(String interactionFile, String game) throws FileNotFoundException, IOException, ParseException
	{
		avatarNames = avatarNamesCollection(game);
		HashSet<String> interactedSprites = new HashSet<String>();
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(interactionFile));
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject obj = (JSONObject)interactionArray.get(i);
			String sprite1 = (String)obj.get("sprite1");
			String sprite2 = (String)obj.get("sprite2");
			if(avatarNames.contains(sprite1))
			{
				interactedSprites.add(sprite2);
			}
			else if(avatarNames.contains(sprite2))
			{
				interactedSprites.add(sprite1);
			}
		}
		return interactedSprites.size();
	}
	
	public int numberofInteractedSpritesWithThisParticularSprite(String sprite, String interactionFile, String game) throws FileNotFoundException, IOException, ParseException
	{
		HashSet<String> interactedSprites = new HashSet<String>();
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(interactionFile));
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject obj = (JSONObject)interactionArray.get(i);
			String sprite1 = (String)obj.get("sprite1");
			String sprite2 = (String)obj.get("sprite2");
			if(sprite.equals(sprite1))
			{
				interactedSprites.add(sprite2);
			}
			else if(sprite.equals(sprite2))
			{
				interactedSprites.add(sprite1);
			}
		}
		return interactedSprites.size();
	}
	
	public int firstFrameOfInteraction(String interactionFile) throws FileNotFoundException, IOException, ParseException
	{
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(interactionFile));
		JSONObject obj = (JSONObject)interactionArray.get(0);
		String tick = (String)obj.get("tick");
		return Integer.parseInt(tick);
	}
	
	public int firstFrameOfSpecificInteraction(String interaction, String interactionFile) throws FileNotFoundException, IOException, ParseException
	{
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(interactionFile));
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject obj = (JSONObject)interactionArray.get(i);
			String rule = (String)obj.get("interaction");
			if(rule.equals(interaction))
			{
				String tick = (String)obj.get("tick");
				return Integer.parseInt(tick);
			}
		}
		return -1;
	}
	
	public int firstFrameOfThisInteraction(Interaction interaction, String interactionFile) throws FileNotFoundException, IOException, ParseException
	{
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(interactionFile));
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject obj = (JSONObject)interactionArray.get(i);
			String rule = (String)obj.get("interaction");
			String sprite1 = (String)obj.get("sprite1");
			String sprite2 = (String)obj.get("sprite2");
			if(rule.equals(interaction.rule)
					&& sprite1.equals(interaction.sprite1)
					&& sprite2.equals(interaction.sprite2))
			{
				String tick = (String)obj.get("tick");
				return Integer.parseInt(tick);
			}
		}
		return -1;
	}
	
	public int retrieveTerminalInteraction(Interaction terminalCondition, String interactionFile) throws FileNotFoundException, IOException, ParseException
	{
		int frameNumber = -1;
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(interactionFile));
		JSONObject obj = (JSONObject)interactionArray.get(interactionArray.size()-1);
		String tick, rule, sprite1, sprite2;
		rule = (String) obj.get("interaction");
		sprite1 = (String) obj.get("sprite1");
		sprite2 = (String) obj.get("sprite2");
		tick = (String) obj.get("tick");
		
		if(terminalCondition.rule.equals(rule)
				&& terminalCondition.sprite1.equals(sprite1)
				&& terminalCondition.sprite2.equals(sprite2))
		{
			frameNumber = Integer.parseInt(tick);
		}
		return frameNumber;
	}
	
	public HashSet<String> avatarNamesCollection(String game)
	{
		ArrayList<String> avatars = new ArrayList<>();
		VGDLParser v = new VGDLParser();
		String[] desc_lines = new IO().readFile(game);
		Node root = v.indentTreeParser(desc_lines);
		root.correctSetValues();
		Node spriteNode = root.children.get(root.sets[0]);
		dfsNavigation(spriteNode.children);
		return avatarNames;
	}
	
	public void dfsNavigation(ArrayList<Node> spriteSet)
	{
		ArrayList<Node> stack = new ArrayList<>();
		stack.add(spriteSet.get(spriteSet.size()-1));
		
		while(stack.size() > 0)
		{
			Node evaluationNode = stack.remove(stack.size()-1);
			getAvatarNames(evaluationNode);
			if(evaluationNode.children.size() > 0)
			{
				for (Node node : evaluationNode.children) 
				{
					stack.add(node);
				}
			}
		}
	}

	/**
	 * @param evaluationNode
	 */
	public void getAvatarNames(Node evaluationNode) {

		SpriteContent spriteNodeContent = (SpriteContent)evaluationNode.content;
		if(spriteNodeContent != null)
		{
			if(spriteNodeContent.referenceClass != null && spriteNodeContent.referenceClass.contains("Avatar"))
			{
				avatarNames.add(spriteNodeContent.identifier);
				for (int i = 0; i < evaluationNode.children.size(); i++) 
				{
					avatarNames.add(evaluationNode.children.get(i).content.identifier);
					getAvatarNames(evaluationNode.children.get(i));
				}
			}
		}
	}
	
	public int countSpacesUntilFirstWord(String [] line)
	{
		int count = 0;
		for (int i = 0; i < line.length; i++) {
			if(line[i].equals(" "))
				count++;
			else
				return count;
		}
		return count;
	}
	
	public void lookingForParentClass(ArrayList<String> avatarNames, Node evaluationNode, String parentClass)
	{
		Node copy = evaluationNode;
		while(copy.parent != null)
		{
			SpriteContent evaluationNodeContent = (SpriteContent)copy.parent.content;
			if(evaluationNodeContent.referenceClass != null
					&& evaluationNodeContent.referenceClass.equals("Avatar"))
			{
				avatarNames.add(evaluationNode.content.identifier);
				break;
			}
		}
	}
	
	public static void main(String [] args) throws FileNotFoundException, IOException, ParseException
	{
		Query query = new Query();
		
		//Ex1 - Number of Unique Interactions
		int numberOfUniqueInteractions = query.numberOfUniqueInteractions("simulation/game0/interactions/interaction.json");
		System.out.println("number of interactions = " + numberOfUniqueInteractions);
		
		//Ex2 - Number of interacted sprites 
		int numberofInteractedSprites = query.numberofInteractedSprites("simulation/game0/interactions/interaction.json", "examples/gridphysics/zelda.txt");
		System.out.println("number of interacted sprites = " + numberofInteractedSprites);
		
		//Ex3 - Number of interacted sprites with a particular sprite
		int numberOfInteractedSpritesWithThisParticularSprite = 
				query.numberofInteractedSpritesWithThisParticularSprite("sword", "simulation/game0/interactions/interaction.json", "examples/gridphysics/zelda.txt");
		System.out.println("number of interacted sprites with this particular sprite = " + numberOfInteractedSpritesWithThisParticularSprite);
		
		//Ex4 - First frame of interaction
		int firstFrameOfInteraction = query.firstFrameOfInteraction("simulation/game0/interactions/interaction.json");
		System.out.println("First frame of interaction = " + firstFrameOfInteraction);
		
		//Ex5 - first frame of specified interaction
		int firstFrameOfSpecifiedInteraction = 
				query.firstFrameOfSpecificInteraction("StepBack", "simulation/game0/interactions/interaction.json");
		System.out.println("First frame of specified interaction = " + firstFrameOfSpecifiedInteraction);
		
		//Ex6 - frames until win
		int framesUntilWin = query.retrieveTerminalInteraction(new Interaction("KillSprite", "goal", "withkey"),
				"simulation/game0/interactions/interaction.json");
		System.out.println("frames until win = " + framesUntilWin);
		
		//Ex7 - frames until lose
		int framesUntilLose = query.retrieveTerminalInteraction(new Interaction("KillSprite", "nokey", "monsterNormal"),
				"simulation/game1/interactions/interaction.json");
		System.out.println("frames until win = " + framesUntilLose);
		
	}

}
