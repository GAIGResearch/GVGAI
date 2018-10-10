package video.handlers;

import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import video.basics.PlayerAction;

public class StorePlayerAction {

	public JSONArray playerActionArray;
	
	public StorePlayerAction()
	{
		playerActionArray = new JSONArray();
	}
	
	public JSONObject storePlayerAction(PlayerAction playerAction)
	{
		JSONObject playerActionObejct = new JSONObject();
		playerActionObejct.put("tick", playerAction.gameTick);
		playerActionObejct.put("action", playerAction.action);
		return playerActionObejct;
	}
	
	public void storeAllPlayerActions(PlayerAction playerAction)
	{
		JSONObject playerActionObject = storePlayerAction(playerAction);
		playerActionArray.add(playerActionObject);
	}
	
	public void writePlayerActionJSONFile(String playerActionFile)
	{
		try (FileWriter file = new FileWriter(playerActionFile)) {

			file.write(playerActionArray.toJSONString());
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}