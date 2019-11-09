package video.basics;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SpriteCapture {

	public String index;
	public ArrayList<String> tickCollection;
	
	public SpriteCapture(){}
	
	public SpriteCapture(String index,
			ArrayList<String> tickCollection)
	{
		this.index = index;
		this.tickCollection = tickCollection;
	}
	
	public JSONObject toJSONObject()
	{
		JSONObject obj = new JSONObject();
		obj.put("identifier", String.valueOf(this.index));
		JSONArray ticks = new JSONArray();
		for (int i = 0; i < tickCollection.size(); i++) 
		{
			ticks.add(String.valueOf(tickCollection.get(i)));
		}
		obj.put("tickCollection", tickCollection);
		return obj;
	}
}
