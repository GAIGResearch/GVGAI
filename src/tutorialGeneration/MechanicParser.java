package tutorialGeneration;

import java.util.ArrayList;
import java.util.List;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import video.basics.GameEvent;
import video.basics.Interaction;
import video.basics.PlayerAction;

public class MechanicParser {
	
	public static List<GameEvent> readMechFile(String filename) {
		return MechanicParser.internalReadMechFile(filename);
	}
	
	public static List<GameEvent> readMechFile() {
		String filename = "mechanics.json";
		return internalReadMechFile(filename);
		
	}
	
	private static ArrayList<GameEvent> internalReadMechFile(String filename) {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
        ArrayList<GameEvent> criticalMechanics = new ArrayList<GameEvent>();
        try (FileReader reader = new FileReader(filename))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
 
            JSONArray mechanicsFile = (JSONArray) obj;
            boolean flag = false;
            for(int i = 0; i < mechanicsFile.size(); i++) {
            	// make an interaction and put in a list
            	JSONObject mech = (JSONObject) mechanicsFile.get(i);
            	
            	if (mech.get("condition").equals("Press Space") && !flag) {
            		flag = true;
            		criticalMechanics.add(new PlayerAction("ACTION_USE"));
            	}
            	else {
            		criticalMechanics.add(new Interaction(mech.get("action") + "", mech.get("sprite1") + "", mech.get("sprite2") + ""));
            	}
            }
             
            return criticalMechanics;
 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
		return null;
	}
	
	public static void main(String[] args) {
		MechanicParser.readMechFile("mechanics_plants.json");
	}

}
