package video.query;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tutorialGeneration.VisualDemonstrationInterfacer;
import video.basics.BunchOfGames;
import video.basics.FrameKeeper;
import video.basics.Interaction;
import video.handlers.FrameInteractionAssociation;


public class QueryActionRule extends FrameInteractionAssociation{
	
	public String fileInteraction;//rules
	public String fileAction;
	public String fileResult;
	private QueryGameResult queryGameResult;
	
	public QueryActionRule(String fileInteraction,
	 String fileAction, String fileResult) throws FileNotFoundException, IOException, ParseException
	{
		super(fileInteraction);
		this.fileInteraction = fileInteraction;
		this.fileAction = fileAction;
		this.fileResult = fileResult;
		queryGameResult = new QueryGameResult(this.fileResult);
	}
	
	public QueryActionRule(){};
	
	public QueryActionRule(String fileAction){
		this.fileAction = fileAction ;
	};
	
	public String getFirstRuleActionFrame(String spriteFilter) throws FileNotFoundException, IOException, ParseException
	{
		JSONParser parser = new JSONParser();
		JSONArray actionArray = 
				(JSONArray) parser.parse(new FileReader(this.fileAction));
		for (int i = 0; i < actionArray.size(); i++) 
		{
			JSONObject actionObj = (JSONObject) actionArray.get(i);
			String tick = (String)actionObj.get("tick");
			JSONObject interactionObj = this.getRuleBasedOnTick(tick, spriteFilter);
			if(interactionObj != null)
			{
				return tick;
			}
		}
		
		return "-1";
	}
	
	public int getFirstRuleActionFrameNumber() throws FileNotFoundException, IOException, ParseException
	{
		int frameNumber = -1;
		JSONParser parser = new JSONParser();
		JSONArray actionArray = 
				(JSONArray) parser.parse(new FileReader(this.fileAction));
		
		if(actionArray.size() > 0)
		{
			JSONObject actionObj = (JSONObject) actionArray.get(0);
			String tick = (String)actionObj.get("tick");
			frameNumber = Integer.parseInt(tick);
		}

		return frameNumber;
	}
	
	public String[] getFirstEventActionFrames(String spriteFilter, String simulationNumber) throws FileNotFoundException, IOException, ParseException
	{
		String [] frames = null;
		String frameNumber = getFirstRuleActionFrame(spriteFilter);
		if(!frameNumber.equals("-1"))
		{
			int integerFrame = Integer.parseInt(frameNumber);
			frames = new String[5];
			frames[0] = "simulation/" + "game" + simulationNumber + "/frames/" + "frame" + (integerFrame - 2) + ".png";
			frames[1] = "simulation/" + "game" + simulationNumber + "/frames/" + "frame" + (integerFrame - 1) + ".png";
			frames[2] = "simulation/" + "game" + simulationNumber + "/frames/" + "frame" + integerFrame + ".png";
			frames[3] = "simulation/" + "game" + simulationNumber + "/frames/" + "frame" + (integerFrame + 1) + ".png";
			frames[4] = "simulation/" + "game" + simulationNumber + "/frames/" + "frame" + (integerFrame + 2) + ".png";
		}
		
		if(frames != null)
		{
			super.checkLastTwoFramesAfterInteraction(frames, Integer.parseInt(frameNumber));
			super.checkNegativeFramesBeforeTheInteraction(frames, Integer.parseInt(frameNumber));
		}

		return frames;
	}
	
	public JSONObject getRuleBasedOnTick(String tick, String spriteFilter) throws FileNotFoundException, IOException, ParseException
	{
		JSONParser parser = new JSONParser();
		JSONArray interactionArray = 
				(JSONArray) parser.parse(new FileReader(this.fileInteraction));
		for (int i = 0; i < interactionArray.size(); i++) 
		{
			JSONObject obj = (JSONObject) interactionArray.get(i);
			String objInteractionTick = (String)obj.get("tick");
			String filter = (String)obj.get("sprite2");
			if(objInteractionTick.equals(tick) && filter.equals(spriteFilter))
			{
				return obj;
			}
		}
		return null;
	}
	
	public FrameKeeper[] multipleQueryForFirstAndLastEvents(ArrayList<Interaction> interactions) throws FileNotFoundException, IOException, ParseException
	{
		FrameKeeper [] frameKeeper = new FrameKeeper[interactions.size()];
		for (int i = 0; i < interactions.size(); i++) 
		{
			String fileInteraction = "simulation/game" 
					+ i + "/interactions/interaction.json";
			String fileActions = "simulation/game" 
					+ i + "/actions/actions.json";
			String fileResult = "simulation/game" 
					+ i + "/result/result.json";
			String fileCapture = "simulation/game" 
					 + i + "/capture/capture.json";
			
			QueryCaptureRule rcq = new 
					QueryCaptureRule(fileInteraction, fileCapture, i);
			
			QueryActionRule raq = new 
					QueryActionRule(fileInteraction, fileActions, fileResult);
			
			
			String framesBegin [] = rcq.
					getFrameCollectionOfTheVeryFirstTimeThisEventHappened
					(interactions.get(i).rule, interactions.get(i).sprite1, interactions.get(i).sprite2);
			String framesEnd [] = raq.queryGameResult.
					getLastFrames(i);
			super.applyPrefixToAFrameName(framesEnd, "simulation/" + "game" + i + "/");
			frameKeeper[i] = new FrameKeeper(framesBegin, framesEnd, raq.queryGameResult.getResult());
			}
		return frameKeeper;
		}
	
	
//	public static void main(String[] args) throws FileNotFoundException, IOException, ParseException
//	{
//		//It associates the first time a player hit a (valid) sprite with its object by pressing
//		//the space key. Then it retrieves the frame of the interaction and returns a sequence of frames
//		//wrapping the event
//		QueryActionRule raq = new QueryActionRule();
//		
//	    //1st - configure your games and run the simulations to generate the data
//		BunchOfGames bog1 = new BunchOfGames("examples/gridphysics/aliens.txt", 
//					"examples/gridphysics/aliens_lvl1.txt", 
//					"tracks.singlePlayer.tools.human.Agent");
//			
//		BunchOfGames bog2 = new BunchOfGames("examples/gridphysics/zelda.txt", 
//					"examples/gridphysics/zelda_lvl1.txt", 
//					"tracks.singlePlayer.tools.human.Agent");
//		
//		ArrayList<BunchOfGames> bogs = new ArrayList<>();
//		bogs.add(bog1); /*bogs.add(bog2);*/ 
//		VisualDemonstrationInterfacer vdi = new VisualDemonstrationInterfacer(true);	
//		vdi.runBunchOfGames(bogs);
//		
//		//2 - store your interactions 
//		ArrayList<Interaction> interactions = new ArrayList<>();
//		interactions.add(new Interaction("KillSprite", "alienGreen", "sam"));
//		//interactions.add(new Interaction("KillSprite", "monsterSlow", "sword"));
//		
//		//3 - collect the first (interaction) frames and the last ones (win/lose)
//		//It says with "result:1" if it the final state is a win state 
//		//and with "result:0" if it is a lose one
//		FrameKeeper[] frameKeepers = raq.multipleQueryForFirstAndLastEvents(interactions);
//		for (int i = 0; i < frameKeepers.length; i++) {
//			frameKeepers[i].print();
//		}
//		
//	}

}