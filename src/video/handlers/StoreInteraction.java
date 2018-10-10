package video.handlers;

import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import video.basics.Interaction;

public class StoreInteraction {
	
	JSONArray interactionArray;
	
	public StoreInteraction()
	{
		interactionArray = new JSONArray();
	}
	
	public JSONObject storeInteraction(Interaction interaction)
	{
		JSONObject interactionObject = new JSONObject();
		interactionObject.put("tick", interaction.tick);
		interactionObject.put("interaction", interaction.rule);
		interactionObject.put("sprite1", interaction.sprite1);
		interactionObject.put("sprite2", interaction.sprite2);
		interactionObject.put("pairInteractionTick", interaction.pairInteractionTick);
		return interactionObject;
	}
	
	public void storeAllInteraction(Interaction interaction)
	{
		JSONObject interactionObj = storeInteraction(interaction);
		interactionArray.add(interactionObj);
	}
	
	public void writeInteractionJSONFile()
	{
		try (FileWriter file = new FileWriter("interaction/interaction.json")) {

			file.write(interactionArray.toJSONString());
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeInteractionJSONFile(String interactionFile)
	{
		try (FileWriter file = new FileWriter(interactionFile)) {

			file.write(interactionArray.toJSONString());
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}