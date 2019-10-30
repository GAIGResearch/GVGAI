package video.constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import core.game.Game;
import core.game.GameDescription;
import core.game.GameDescription.SpriteData;
import core.vgdl.SpriteGroup;
import core.vgdl.VGDLRegistry;
import video.handlers.StoreInteraction;
/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 24/02/2018
 * @author Tiago Machado
 */
public class InteractionStaticData {

	public static int counter = 0;
	public static int resultsCounter = 0;
	public static int spriteCaptureCounter = 0;
	
	public static boolean saveSpriteGroup = false;
	public static String gameName = "simulation";
	public static String agentName = "agent";
	public static String levelCount = "0";
	public static String playthroughCount = "0";
	
	public static ArrayList<String> itypesJson;
	
	
    public static void createJSONInfo(Game toPlay) {
    	GameDescription gd = new GameDescription(toPlay);
		SpriteGroup[] spriteGroups = toPlay.getSpriteGroups();
		ArrayList<String> json = new ArrayList<String>();
		json.add("{");
		for(int i = 0; i < spriteGroups.length; i++) {
			String entry = "";
			SpriteGroup group = spriteGroups[i];
			String name = VGDLRegistry.GetInstance().getRegisteredSpriteKey(group.getItype());
			if(gd.findSprite(name) != null) {
			entry = "\"" + name + "\" : {" + "\"itype\" : \"" + group.getItype() + "\"";
				if (gd.findSprite(name).getParameters().get("img") != null) {
					entry += ", \"img\" : \"" + gd.findSprite(name).getParameters().get("img") + "\"";
				}
				if(gd.findSprite(name).getParameters().get("color") != null) {
					entry += ", \"color\" :\"" + gd.findSprite(name).getParameters().get("color") + "\"";
				}
				entry += "}";
				entry += ",";
				
			}
			json.add(entry);
			
		}
		
		// level mapping
		json.add("\"level_mapping\" : {");
		Iterator it = gd.getLevelMapping().entrySet().iterator();
		while(it.hasNext()) {
			String entry = "";
			Map.Entry pairs = (Map.Entry) it.next();
			entry = "\"" + pairs.getKey() + "\" : \"" + pairs.getValue() + "\"";
			if(it.hasNext()) {
				entry += ",";
			}
			json.add(entry);
		}
		json.add("}");
		json.add("}");
		InteractionStaticData.itypesJson = json;
    }
}


