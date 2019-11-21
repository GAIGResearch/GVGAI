package video.handlers;

import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class StoreGameSimulationResult 
{
	public JSONObject gameResult;
	public JSONArray interactions;
	public JSONArray actions;
	public StoreGameSimulationResult()
	{
		gameResult = new JSONObject();
	}

	public void storeGameSimulationResult(String result, String score, String tick)
	{
		this.gameResult.put("result", result);
		this.gameResult.put("score", score);
		this.gameResult.put("tick", tick);
	}
	
	public void addInteractions(JSONArray interactions)
	{
		
	}

	public void writeResultToAJSONFile(String interactionFile) throws IOException
	{
		//System.out.println("IF:" + interactionFile);
		
		try (FileWriter file = new FileWriter(interactionFile)) {

			file.write(gameResult.toJSONString());
			file.flush();
			file.close();
		}
	}
	
	public void writeAllInfo(String myFile) throws IOException {
		
		try (FileWriter file = new FileWriter(myFile)) {

			file.write("{\"interactions\":[");
			int i = 0;
			for(Object obj : interactions) {
				file.write(((JSONObject)obj).toString());
				if(i < interactions.size()-1) {
					file.write(",\n");
					i++;
				}
			}
			file.write("],\n\"actions\":[");
			
			i = 0;
			for(Object obj : actions) {
				file.write(((JSONObject)obj).toString());
				if(i < actions.size()-1) {
					file.write(",\n");
					i++;
				}
			}
			file.write("],\n\"results\":[");
			file.write(gameResult.toJSONString());
			file.write("]}");
			file.flush();
			file.close();
		}
	}
	
	public void addMechanics(JSONArray interactions, JSONArray actions) {
		this.interactions = interactions;
		this.actions = actions;
	}
}
