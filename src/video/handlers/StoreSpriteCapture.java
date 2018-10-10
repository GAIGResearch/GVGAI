package video.handlers;

import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import video.basics.PlayerAction;

public class StoreSpriteCapture {
	
	public JSONArray spriteCaptureArray;
	
	public StoreSpriteCapture()
	{
		spriteCaptureArray = new JSONArray();
	}
	
	public void storeAllSpritesCaptured(JSONObject spriteCaptureObj)
	{
		
		spriteCaptureArray.add(spriteCaptureObj);
	}
	
	public void writeSpriteCaptureJSONFile(String spriteCaptureFile)
	{
		try (FileWriter file = new FileWriter(spriteCaptureFile)) {

			file.write(spriteCaptureArray.toJSONString());
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
