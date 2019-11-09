package video.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import video.basics.Interaction;

public class FrameInteractionAssociation 
{
	public JSONArray interactionArray;
	
	public FrameInteractionAssociation(String interactionFileName) throws FileNotFoundException, IOException, ParseException
	{
		interactionArray = retrieveInteractionFile(interactionFileName);
	}
	
	public FrameInteractionAssociation(){}
	
	public JSONObject retrieveInteraction(String interaction)
	{
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject interactionObject = (JSONObject)interactionArray.get(i);
			String interactionName = (String) interactionObject.get("interaction");
			if(interactionName.contains(interaction))
			{
				return interactionObject;
			}
		}
		return null;
	}
	
	/***
	 * Checks to see if the two given interactions are equivalent
	 * @param one the first object
	 * @param two the second object
	 * @return a boolean value
	 */
	public boolean compareInteractions(JSONObject one, JSONObject two) {
		String oneName, oneSprite1, oneSprite2, twoName, twoSprite1, twoSprite2;
		
		if(one != null && two != null) {
			oneName = (String) one.get("interaction");
			twoName = (String) two.get("interaction");
			
			oneSprite1 = (String) one.get("sprite1");
			twoSprite1 = (String) two.get("sprite1");
			
			oneSprite2 = (String) one.get("sprite2");
			twoSprite2 = (String) two.get("sprite2");
			
			if(oneName.equals(twoName) && oneSprite1.equals(twoSprite1) && oneSprite2.equals(twoSprite2)) {
				return true;
			} else {
				return false;
			}
			
		} else {
			return false;
		}
	}
	
	/***
	 * Checks to see if the given list already contains this object or not
	 * @param list the list being looked over
	 * @param interaction the interaction being looked for in the list
	 * @return whether it contains these interactions
	 */
	public boolean containsObject(ArrayList<JSONObject> list, JSONObject interaction) {
		for(JSONObject obj : list) {
			if (compareInteractions(obj, interaction)) {
				return true;
			}
		}
		return false;
	}
	
	/***
	 * Gets a list of unique interactions from this playthrough
	 * @return
	 */
	public ArrayList<JSONObject> getUniqueInteractions() {
		ArrayList<JSONObject> uniques = new ArrayList<JSONObject>();
		
		for(Object interaction : this.interactionArray) {
			if(!containsObject(uniques, (JSONObject) interaction)) {
				uniques.add((JSONObject) interaction);
			}			
		}
		return uniques;
	}
	
	public JSONObject retrieveInteraction(String interaction, String sprite1, String sprite2)
	{
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject interactionObject = (JSONObject)interactionArray.get(i);
			
			String interactionName = (String) interactionObject.get("interaction");
			String sprite1Name = (String) interactionObject.get("sprite1");
			String sprite2Name = (String) interactionObject.get("sprite2");
			
			if(interactionName.contains(interaction)
					&& sprite1Name.contains(sprite1)
					&& sprite2Name.contains(sprite2))
			{
				return interactionObject;
			}
		}
		return null;
	}
	
	public JSONObject retrieveInteraction(String interaction, String tick)
	{	
		int index = interaction.indexOf("-");
		String stringTick = interaction.substring(0, index - 1);
		String stringInteraction = interaction.substring(index + 1, interaction.length()); 
		
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject interactionObject = (JSONObject)interactionArray.get(i);
			String interactionName = (String) interactionObject.get("interaction");
			if(stringInteraction.contains(interactionName) && 
					stringTick.equals(((String)interactionObject.get("tick"))))
			{
				return interactionObject;
			}
		}
		return null;
	}
	
	public String[] retrieveInteractionFrames(JSONObject interactionObject)
	{
		String frames [] = new String[5];
		int tick = Integer.parseInt(interactionObject.get("tick").toString());

		String frame0 = "frames/frame" + (tick - 2) + ".png";
		String frame1 = "frames/frame" + (tick - 1) + ".png";
		String frame2 = "frames/frame" + (tick) + ".png";
		String frame3 = "frames/frame" + (tick + 1) + ".png";
		String frame4 = "frames/frame" + (tick + 2) + ".png";

		frames = new String[]{frame0, frame1, frame2, frame3, frame4};
		
		checkLastTwoFramesAfterInteraction(frames, tick);
		checkNegativeFramesBeforeTheInteraction(frames, tick);

		return frames;
	}
	
	public String[] retrieveInteractionFrames(JSONObject interactionObject, String interactionFilePath)
	{
		String frames [] = new String[5];
		
		int tick = Integer.parseInt(interactionObject.get("tick").toString());
		String frame0 = "frames/frame" + (tick - 2) + ".png";
		String frame1 = "frames/frame" + (tick - 1) + ".png";
		String frame2 = "frames/frame" + (tick) + ".png";
		String frame3 = "frames/frame" + (tick + 1) + ".png";
		String frame4 = "frames/frame" + (tick + 2) + ".png";
		
		frames = new String[]{frame0, frame1, frame2, frame3, frame4};
		
		checkLastTwoFramesAfterInteraction(frames, tick);

		return frames;
	}
	
	public boolean isThereSuchAFrame(String frame)
	{
		File f = new File(frame);
		if(f.exists()) { 
			return true;
		}
		return false;
	}
	
	public void checkLastTwoFramesAfterInteraction(String[] frames, int tick)
	{
		if(!isThereSuchAFrame(frames[3]) || !isThereSuchAFrame(frames[4]))
		{
			frames[0] = "frames/frame" + (tick - 4) + ".png";
			frames[1] = "frames/frame" + (tick - 3) + ".png";
			frames[2] = "frames/frame" + (tick - 2) + ".png";
			frames[3] = "frames/frame" + (tick - 1) + ".png";
			frames[4] = "frames/frame" + (tick) + ".png";
		}
	}
	
	public void applyPrefixToAFrameName(String[] frames, String prefix)
	{
		frames[0] = prefix + frames[0];
		frames[1] = prefix + frames[1];
		frames[2] = prefix + frames[2];
		frames[3] = prefix + frames[3];
		frames[4] = prefix + frames[4];
	}
	
	public void checkNegativeFramesBeforeTheInteraction(String[] frames, int tick)
	{
		if(frames[0].contains("-"))
		{
			frames[0] = frames[1];
		}
		
		if(frames[1].contains("-"))
		{
			frames[1] = frames[2];
			frames[0] = frames[1];
		}
	}
	
	public JSONArray retrieveInteractionFile(String interactionFileName) throws FileNotFoundException, IOException, ParseException
	{
		JSONParser parser = new JSONParser();
		System.out.println(interactionFileName);
		JSONArray interactionArray = (JSONArray) parser.parse(new FileReader(interactionFileName));
		
		return interactionArray;
	}
	
	public String [] retrieveAllInteractionNames()
	{
		String [] interactionNames = null;
		try
		{
			interactionNames = new String[interactionArray.size()];
			for (int i = 0; i < interactionArray.size(); i++) 
			{
				JSONObject interactionObject = (JSONObject)interactionArray.get(i);
				interactionNames[i] = (String) interactionObject.get("pairInteractionTick");
			}
		}catch(NullPointerException e){
			e.getMessage();
		}
		return interactionNames;
	}
	
	public Interaction [] retrieveInteractionsAsArray()
	{
		Interaction [] interactionNames = null;
		try
		{
			interactionNames = new Interaction[interactionArray.size()];
			for (int i = 0; i < interactionArray.size(); i++) 
			{
				JSONObject interactionObject = (JSONObject)interactionArray.get(i);
				Interaction interaction = 
						new Interaction((String) interactionObject.get("tick"),
								(String) interactionObject.get("sprite1"),
								(String) interactionObject.get("sprite2"),
								(String) interactionObject.get("interaction"));
				
				interactionNames[i] = interaction;
			}
		}catch(NullPointerException e){
			e.getMessage();
		}
		return interactionNames;
	}
	
	public static void main(String[] args) {
		try {
			FrameInteractionAssociation fia = new FrameInteractionAssociation("realportals/human/1/0/interactions/interaction.json");
			ArrayList<JSONObject> uniques = fia.getUniqueInteractions();
			
			for(JSONObject unique : uniques) {
				System.out.println(unique);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
