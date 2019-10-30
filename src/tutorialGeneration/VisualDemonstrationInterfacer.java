package tutorialGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import tracks.ArcadeMachine;
import tutorialGeneration.Node;
import tutorialGeneration.Mechanic;
import video.basics.BunchOfGames;
import video.basics.GameSummary;
import video.basics.Interaction;
import video.basics.InteractionFrame;
import video.basics.InteractionQueryObject;
import video.constants.InteractionStaticData;
import video.gui.main.ShowFrames;
import video.gui.main.VideoPlayer;
import video.handlers.FrameInteractionAssociation;
import video.query.QueryGameResult;
import video.query.QueryActionRule;
import video.query.QueryCaptureRule;

public class VisualDemonstrationInterfacer {

	/*** 
	 * The purpose of this interfacer is to provide a single point of contact between the graph-based representation tutorial generator and the system to query a completed game
	 * for a given interaction/termination rule, and receive in turn a sequence of images/video/gif of that mechanic being activated
	 */
	/***
	 * 
	 */

	private ShowFrames showFrames;

	public ArrayList<GameSummary> summaries;

	private long numberOfSimulations;

	private HashMap<Integer, String> agentMap;
	
	private String gameName;
	public VisualDemonstrationInterfacer(boolean deleteFolders) throws FileNotFoundException, IOException, ParseException {
		numberOfSimulations = 0;
		if(deleteFolders)
			video.utils.Utils.deleteFolder(new File("simulation"));
		summaries = new ArrayList<>();
		agentMap = new HashMap<Integer, String>();
		InteractionStaticData.counter = 0;
		InteractionStaticData.resultsCounter = 0;
		InteractionStaticData.spriteCaptureCounter = 0;
	}

	public VisualDemonstrationInterfacer(String gameName, boolean deleteFolders) throws FileNotFoundException, IOException, ParseException {
		numberOfSimulations = 0;
		this.gameName = gameName;
		InteractionStaticData.gameName = gameName;
		if(deleteFolders)
			video.utils.Utils.deleteFolder(new File(gameName));
		Files.createDirectories(Paths.get(gameName));
		summaries = new ArrayList<>();
		agentMap = new HashMap<Integer, String>();
		InteractionStaticData.counter = 0;
		InteractionStaticData.resultsCounter = 0;
		InteractionStaticData.spriteCaptureCounter = 0;
	}
	
	
	public long numberOfSimulationFoldersAreAvailable()
	{
		long count = 0;
		try {
			return count  = Files.find(
					Paths.get(gameName + "/"), 
					1, 
					(path, attributes) -> attributes.isDirectory()
					).count() - 1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return count;
		} 
	}
	
	
	public void runGame(String game, String level1, String agentName) throws IOException
	{
		InteractionStaticData.saveSpriteGroup = true;
		InteractionStaticData.itypesJson = new ArrayList<String>();
//		ArcadeMachine.runOneGame(game, level1, false, agentName, "", 0, 0);
		ArcadeMachine.playOneGame(game, level1, "", 0);
	}

	public HashMap<Integer, TupleRuleFrames> queryVisualDemonstrations(InteractionQueryObject [] iqos) throws FileNotFoundException, IOException, ParseException
	{
		FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation("interaction/interaction.json");
		JSONObject[] interactionObjects = new JSONObject[iqos.length];
		HashMap<Integer, TupleRuleFrames> frames = new HashMap<Integer, TupleRuleFrames>();
		for (int i = 0; i < iqos.length; i++) 
		{
			JSONObject tempObj = frameInteractionAssociation.retrieveInteraction(iqos[i].rule, iqos[i].sprite1, iqos[i].sprite2);
			interactionObjects[i] = tempObj;
			TupleRuleFrames tuple = new TupleRuleFrames(iqos[i].rule, 
					frameInteractionAssociation.retrieveInteractionFrames(interactionObjects[i]));
			frames.put(i, tuple);
		}

		return frames;
	}

	public JSONArray writeQueriedFramesInJSONArray(HashMap<Integer, TupleRuleFrames> frames)
	{
		JSONArray queriedFrames = new JSONArray();
		for (Integer key : frames.keySet()) 
		{
			TupleRuleFrames sequence = frames.get(key);
			JSONObject obj = new JSONObject();
			obj.put("interactionNumber", String.valueOf(key));
			obj.put("interactionType", sequence.rule);
			obj.put("frame1", sequence.frames[0]);
			obj.put("frame2", sequence.frames[1]);
			obj.put("frame3", sequence.frames[2]);
			queriedFrames.add(obj);
		}

		return queriedFrames;
	}

	public void queryVisualDemonstrator(String sprite1, String sprite2, String rule, String subtitleText) throws FileNotFoundException, IOException, ParseException{
		// TODO use the above information to query the visualization system

		FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation("interaction/interaction.json");
		JSONObject interactionObject = null;
		interactionObject = frameInteractionAssociation.retrieveInteraction(rule, sprite1, sprite2);
		String [] frames = null;
		try{
			frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
			VideoPlayer videoPlayer = new VideoPlayer(frames, 500, subtitleText);
		}
		catch (NullPointerException e)
		{
			System.out.println("The interaction requested does not exist or is not stored with the same argument values");
		}
		// TODO save the video file locally and name it after the rule
	}


	public void writeQueryFramesInJSONFile(JSONArray mediaArray)
	{
		try (FileWriter file = new FileWriter("queriedFrames/qFrames.json")) {

			file.write(mediaArray.toJSONString());
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String[] retrieveFramePaths(String rule, String sprite1, String sprite2) throws FileNotFoundException, IOException, ParseException
	{
		FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation("interaction/interaction.json");
		JSONObject interactionObject = null;
		interactionObject = frameInteractionAssociation.retrieveInteraction(rule, sprite1, sprite2);
		String [] frames = null;
		frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
		return frames;
	}

	public String[] retrieveFramePathsInTheSpecificInteractionFile(String rule, String sprite1, String sprite2,
			String filePath) throws FileNotFoundException, IOException, ParseException
	{
		FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(filePath);
		JSONObject interactionObject = null;
		interactionObject = frameInteractionAssociation.retrieveInteraction(rule, sprite1, sprite2);
		String [] frames = null;
		frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
		return frames;
	}

	/*
	 * It searches for the interaction passed as parameter in all the games whose frames are stored. 
	 * It returns an array if the interaction was ever stored or an empty one otherwise. 
	 */
	public String[] retrieveFramePathsInTheCollection(String rule, String sprite1, String sprite2,
			ArrayList<String> interactionPaths) throws FileNotFoundException, IOException, ParseException
	{
		for(String path : interactionPaths)
		{
			FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
			JSONObject interactionObject = null;
			interactionObject = frameInteractionAssociation.retrieveInteraction(rule, sprite1, sprite2);
			String [] frames = null;
			if(interactionObject != null)
			{
				frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
				path = path.replace("interactions/interaction.json", "");
				for (int i = 0; i < frames.length; i++) 
				{
					frames[i] = path + frames[i];
				}
				return frames;
			}
		}
		return new String[]{""};
	}

	/*
	 * It searches for the interaction passed as parameter in all the games whose frames are stored. 
	 * It returns an array if the interaction was ever stored or an empty one otherwise. 
	 */
	public ArrayList<InteractionFrame> retrieveFramePathsInTheCollection(ArrayList<Interaction> interactions,
			ArrayList<String> interactionPaths) throws FileNotFoundException, IOException, ParseException
	{
		ArrayList<InteractionFrame> frameCollection = new ArrayList<>();

		for(Interaction interaction :  interactions)
		{
			for(String path : interactionPaths)
			{
				FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
				JSONObject interactionObject = null;
				interactionObject = frameInteractionAssociation.
						retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);
				String [] frames = null;
				if(interactionObject != null)
				{
					frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
					path = path.replace("interactions/interaction.json", "");
					for (int i = 0; i < frames.length; i++) 
					{
						frames[i] = path + frames[i];
					}
					frameCollection.add(new InteractionFrame(interaction.rule,
							interaction.sprite1, interaction.sprite2, frames));
				}
			}
		}
		return frameCollection;
	}

	/*
	 * It searches for the interaction passed as parameter in all the games whose frames are stored. 
	 * It returns an array if the interaction was ever stored or an empty one otherwise. 
	 */
	public HashMap<Interaction, String[]> mapFramePathsInTheCollection(ArrayList<Interaction> interactions,
			ArrayList<String> interactionPaths) throws FileNotFoundException, IOException, ParseException
	{
		HashMap<Interaction, String[]> frameCollection = new HashMap<Interaction, String[]>();

		for(Interaction interaction :  interactions)
		{
			for(String path : interactionPaths)
			{
				FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
				JSONObject interactionObject = null;
				interactionObject = frameInteractionAssociation.
						retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);
				String [] frames = null;
				if(interactionObject != null)
				{
					frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
					path = path.replace("interactions/interaction.json", "");
					for (int i = 0; i < frames.length; i++) 
					{
						frames[i] = path + frames[i];
					}
					frameCollection.put(new Interaction(interaction.rule, interaction.sprite1, interaction.sprite2),
							frames);
				}
			}
		}
		return frameCollection;
	}

	/*
	 * It searches for the interaction passed as parameter in all the games whose frames are stored. 
	 * It returns an array if the interaction was ever stored or an empty one otherwise. 
	 */
	public String[] mapFramePathsInTheCollectionByInteraction(Interaction interaction) throws FileNotFoundException, IOException, ParseException
	{
		String [] frames = new String[]{};
		ArrayList<String> interactionPaths = loadInteractionPaths(numberOfSimulationFoldersAreAvailable());
		for(String path : interactionPaths)
		{
			FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
			JSONObject interactionObject = null;
			interactionObject = frameInteractionAssociation.
					retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);

			if(interactionObject != null)
			{
				frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
				path = path.replace("interactions/interaction.json", "");
				for (int i = 0; i < frames.length; i++) 
				{
					frames[i] = path + frames[i];
				}
			}
		}

		return frames;
	}

	public String[] mapFramePathsInTheCollectionByMechanic(Mechanic mechanic) throws FileNotFoundException, IOException, ParseException
	{
		if(!mechanic.getActions().isEmpty() || mechanic.getSprites().size() > 1)
		{
			return new String[]{};
		}
		Interaction interaction = new Interaction(mechanic.getReadibleAction(), mechanic.getSprites().get(0).getName(), mechanic.getSprites().get(1).getName());
		String [] frames = new String[]{};
		ArrayList<String> interactionPaths = loadInteractionPaths(numberOfSimulationFoldersAreAvailable());
		for(String path : interactionPaths)
		{
			FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
			JSONObject interactionObject = null;
			interactionObject = frameInteractionAssociation.
					retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);

			if(interactionObject != null)
			{
				frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
				path = path.replace("interactions/interaction.json", "");
				for (int i = 0; i < frames.length; i++) 
				{
					frames[i] = path + frames[i];
				}
			}
		}

		return frames;
	}

	public int[] mapFrameNumbersInTheSimulationByMechanic(Mechanic mechanic, String playthroughPath) throws FileNotFoundException, IOException, ParseException
	{
		Interaction interaction = new Interaction(mechanic.getReadibleAction(), mechanic.getSprites().get(0).getName(), mechanic.getSprites().get(1).getName());
		int [] frameNumbers = new int[]{};

		String path = playthroughPath + "/interactions/interaction.json";
		FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
		JSONObject interactionObject = null;
		interactionObject = frameInteractionAssociation.
				retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);

		if(interactionObject != null)
		{

			String framePaths [] = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
			path = path.replace("interactions/interaction.json", "");
			frameNumbers = new int[5];
			for (int i = 0; i < framePaths.length; i++) 
			{
				frameNumbers[i] = getTheNumberOfTheFrame(new StringBuilder(framePaths[i]).reverse());
			}
		}

		return frameNumbers;
	}

	public HashMap<String, String[]> mapFramePathsInTheCollectionByMechanics(ArrayList<Mechanic> mechanics) throws FileNotFoundException, IOException, ParseException
	{
		HashMap<String, String[]> mapMechanicsToFrames = new HashMap<>();
		for(int i = 0; i < mechanics.size(); i++)
		{
			Interaction interaction = new Interaction(mechanics.get(i).getReadibleAction(), mechanics.get(i).getSprites().get(0).getName(), mechanics.get(i).getSprites().get(1).getName());
			String [] frames = new String[]{};
			ArrayList<String> interactionPaths = loadInteractionPaths(numberOfSimulationFoldersAreAvailable());
			for(String path : interactionPaths)
			{
				FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
				JSONObject interactionObject = null;
				interactionObject = frameInteractionAssociation.
						retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);

				if(interactionObject != null)
				{
					frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
					path = path.replace("interactions/interaction.json", "");
					for (int j = 0; j < frames.length; j++) 
					{
						frames[j] = path + frames[j];
					}
					mapMechanicsToFrames.put(mechanics.get(i).getReadibleAction(), frames);
				}
			}
		}
		return mapMechanicsToFrames;
	}

	public HashMap<String, Integer> mapFrameNumbersInTheCollectionByMechanics(ArrayList<Mechanic> mechanics) throws FileNotFoundException, IOException, ParseException
	{
		HashMap<String, String[]> mapMechanicsToFrames = new HashMap<>();
		for(int i = 0; i < mechanics.size(); i++)
		{
			Interaction interaction = new Interaction(mechanics.get(i).getReadibleAction(), mechanics.get(i).getSprites().get(0).getName(), mechanics.get(i).getSprites().get(1).getName());
			String [] frames = new String[]{};
			ArrayList<String> interactionPaths = loadInteractionPaths(numberOfSimulationFoldersAreAvailable());
			for(String path : interactionPaths)
			{
				FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
				JSONObject interactionObject = null;
				interactionObject = frameInteractionAssociation.
						retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);

				if(interactionObject != null)
				{
					frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
					path = path.replace("interactions/interaction.json", "");
					for (int j = 0; j < frames.length; j++) 
					{
						frames[j] = path + frames[j];
					}
					mapMechanicsToFrames.put(mechanics.get(i).getReadibleAction(), frames);
				}
			}
		}

		return getMeTheFrameNumbers(mapMechanicsToFrames);
	}

	public HashMap<String, Integer> getMeTheFrameNumbers(HashMap<String, String[]> mapMechanicsToFrames)
	{
		HashMap<String, Integer> mapInteractionToFrameNumber = new HashMap<>();
		for(String key : mapMechanicsToFrames.keySet())
		{
			StringBuilder number = new StringBuilder(mapMechanicsToFrames.get(key)[2]).reverse();
			int frameNumber = getTheNumberOfTheFrame(number);
			mapInteractionToFrameNumber.put(key, frameNumber);
		}
		return mapInteractionToFrameNumber;
	}

	public ArrayList<String> loadInteractionPaths(long numberOfSimulations2)
	{
		ArrayList<String> interactionpaths = new ArrayList<>();
		for(int i = 0; i < numberOfSimulations2; i++)
		{
			interactionpaths.add(gameName + "/game" + i + "/interactions/interaction.json");
		}
		return interactionpaths;
	}

	public void runBunchOfGames(ArrayList<BunchOfGames> bunchOfGames, String[] agents, int levelCount, int playthroughsPerLevelCount) throws IOException
	{
		numberOfSimulations = bunchOfGames.size();
		
		for (BunchOfGames game : bunchOfGames) {
			createDirectories(game.playerPath, game.levelCount, game.playthroughCount);
			InteractionStaticData.agentName = game.playerPath;
			InteractionStaticData.levelCount = game.levelCount;
			InteractionStaticData.playthroughCount = game.playthroughCount;
			this.runGame(game.gamePath, game.gameLevelPath, game.playerPath);
			
//			storeSpriteMapping(game.playerPath, game.levelCount, game.playthroughCount);
		}
	}
	
	
	public void storeSpriteMapping(String agentName, String levelCount, String playthroughCount) {
		try(FileWriter writer = new FileWriter(gameName + "/" + agentName + "/" + levelCount + "/" + playthroughCount + "/mappings/" + "spritemap.json")) {
			for(int i = 0; i < InteractionStaticData.itypesJson.size(); i++){
				String json = InteractionStaticData.itypesJson.get(i);
				writer.write(json + "\n");
			}
			writer.close();

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	
	public void createDirectories(int numberOfSimulations) throws IOException
	{
		for(int i = 0; i < numberOfSimulations; i++)
		{
			Files.createDirectories(Paths.get(gameName + "/game" + i + "/frames/"));
			Files.createDirectories(Paths.get(gameName + "/game" + i + "/interactions/"));
			Files.createDirectories(Paths.get(gameName + "/game" + i + "/actions/"));
			Files.createDirectories(Paths.get(gameName + "/game" + i + "/result/"));
			Files.createDirectories(Paths.get(gameName + "/game" + i + "/capture/"));
		}
	}
	
	public void createDirectories(String agentName, String levelCount, String playthroughCount) throws IOException {
		Files.createDirectories(Paths.get(gameName + "/" + agentName + "/" + levelCount + "/" + playthroughCount + "/frames/"));
		Files.createDirectories(Paths.get(gameName + "/" + agentName + "/" + levelCount + "/" + playthroughCount + "/interactions/"));
		Files.createDirectories(Paths.get(gameName + "/" + agentName + "/" + levelCount + "/" + playthroughCount + "/actions/"));
		Files.createDirectories(Paths.get(gameName + "/" + agentName + "/" + levelCount + "/" + playthroughCount + "/result/"));
		Files.createDirectories(Paths.get(gameName + "/" + agentName + "/" + levelCount + "/" + playthroughCount + "/capture/"));
		Files.createDirectories(Paths.get(gameName + "/" + agentName + "/" + levelCount + "/" + playthroughCount + "/mappings/"));
	}

//	public ArrayList<InteractionFrame> runGameSimulations(ArrayList<BunchOfGames> bunchOfgames,
//			ArrayList<Interaction> interactions) throws IOException
//	{
//		ArrayList<InteractionFrame> frameCollection = new ArrayList<>();
//		try {
//			this.createDirectories(bunchOfgames.size());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		this.runBunchOfGames(bunchOfgames);
//
//		ArrayList<String> interactionFiles = new ArrayList<>();
//		for (int i = 0; i < bunchOfgames.size(); i++) 
//		{
//			interactionFiles.add(gameName + "/game" + i + "/interactions/interaction.json");
//		}
//
//		try {
//			frameCollection = retrieveFramePathsInTheCollection(interactions, interactionFiles);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return frameCollection;
//	}

//	public HashMap<Interaction, String[]> runMultipleGameSimulations(ArrayList<BunchOfGames> bunchOfgames,
//			ArrayList<Interaction> interactions) throws IOException
//	{
//		HashMap<Interaction, String[]> frameCollection = new HashMap<Interaction, String[]>();
//		try {
//			this.createDirectories(bunchOfgames.size());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		this.runBunchOfGames(bunchOfgames);
//
//		ArrayList<String> interactionFiles = new ArrayList<>();
//		for (int i = 0; i < bunchOfgames.size(); i++) 
//		{
//			interactionFiles.add(gameName + "/game" + i + "/interactions/interaction.json");
//		}
//
//		try {
//			frameCollection = mapFramePathsInTheCollection(interactions, interactionFiles);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return frameCollection;
//	}

	public String[] queryFrameCollection(HashMap<Interaction, String[]> frameMap, 
			String rule, String sprite1, String sprite2)
	{
		Interaction interaction = new Interaction(rule,  sprite1, sprite2);
		String [] frames = null;
		try
		{
			frames = frameMap.get(interaction);
		}
		catch(NullPointerException e)
		{
			System.out.println("interaction was not registered during simulations or it does not exist.");
		}
		return frames;
	}

	/**
	 * @param superP
	 * @param bunchOfGames
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	public String[][] retrieveFramesCollisionAndEndState(ArrayList<ArrayList<Mechanic>> superP, ArrayList<BunchOfGames> bunchOfGames)
			throws FileNotFoundException, IOException, ParseException {

		String allFrames[][] = null;
		String [] shootAndCollisionFrames;
		String [] lastFrames;

		int simulationNumber = (int) numberOfSimulationFoldersAreAvailable();
		for (int i = 0; i < simulationNumber; i++) {

			//1 - Load Files
			String fileInteraction = gameName + "/game" 
					+ i + "/interactions/interaction.json";
			String fileCapture = gameName + "/game" 
					+ i + "/capture/capture.json";
			String fileResult = gameName + "/game" 
					+ i + "/result/result.json";

			//2 - Initialize Auxiliary classes
			QueryGameResult qgr = new QueryGameResult(fileResult);
			if(qgr.getResult() == 1)
			{
				QueryCaptureRule rcq = new QueryCaptureRule(fileInteraction, fileCapture, i);
				shootAndCollisionFrames = rcq.getShootFrameAndCollisionFrameActivateFromTheFirstTimeInThisMechanicList(superP.get(2));
				lastFrames = qgr.getLastFrames(i);
				allFrames = new String[][]{
					shootAndCollisionFrames,
					lastFrames
				};
				return allFrames;
			}
		}
		return null;
	}

	public HashMap<String, Integer> ma(ArrayList<Mechanic> mechanics, int simulationNumber) throws FileNotFoundException, IOException, ParseException
	{
		HashMap<String, String[]> mapMechanicsToFrames = new HashMap<>();

		for(int i = 0; i < mechanics.size(); i++)
		{
			Interaction interaction = new Interaction(mechanics.get(i).getReadibleAction(), mechanics.get(i).getSprites().get(0).getName(), mechanics.get(i).getSprites().get(0).getName());
			String [] frames = new String[]{};
			String path = gameName + "/game" 
					+ simulationNumber + "/interactions/interaction.json";
			FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);

			JSONObject interactionObject = null;
			interactionObject = frameInteractionAssociation.
					retrieveInteraction(interaction.rule, interaction.sprite1, interaction.sprite2);

			if(interactionObject != null)
			{
				frames = frameInteractionAssociation.retrieveInteractionFrames(interactionObject);
				path = path.replace("interactions/interaction.json", "");
				for (int j = 0; j < frames.length; j++) 
				{
					frames[j] = path + frames[j];
				}
				mapMechanicsToFrames.put(mechanics.get(i).getReadibleAction(), frames);
			}

		}	
		return getMeTheFrameNumbers(mapMechanicsToFrames);
	}

	/**
	 * @param inputMechanics
	 * @param bunchOfGames
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	public void getAllRelevantFrames(ArrayList<Mechanic> inputMechanics,
			ArrayList<Mechanic> regularMechanics, ArrayList<BunchOfGames> bunchOfGames)
					throws FileNotFoundException, IOException, ParseException {

		HashMap<Integer, int[]> allRelevantFrames = null;
		HashMap<String, Integer> regularInteractionFrames = null;
		String [] shootAndCollisionFrames;
		String [] lastFrames;

		int simulationNumber = (int) numberOfSimulationFoldersAreAvailable();
		for (int i = 0; i < simulationNumber; i++) {

			//1 - Load Files
			String fileInteraction = gameName + "/game" 
					+ i + "/interactions/interaction.json";
			String fileCapture = gameName + "/game" 
					+ i + "/capture/capture.json";
			String fileResult = gameName + "/game" 
					+ i + "/result/result.json";

			//2 - Initialize Auxiliary classes
			QueryGameResult qgr = new QueryGameResult(fileResult);
			int result = qgr.getResult();
			if(result == 1)
			{
				QueryCaptureRule rcq = new QueryCaptureRule(fileInteraction, fileCapture, i);
				shootAndCollisionFrames = rcq.getShootFrameAndCollisionFrameActivateFromTheFirstTimeInThisMechanicList(inputMechanics);
				lastFrames = qgr.getLastFrames(i);
				HashMap<Integer, int[]> shootCollision = retrieveFrameNumbersOfShootEvents(i, shootAndCollisionFrames);
				allRelevantFrames = resultAndAllFrames(i, result, shootCollision, lastFrames[lastFrames.length-1]);
				regularInteractionFrames = ma(regularMechanics, i);
				summaries.add(new GameSummary(i, result, regularInteractionFrames, allRelevantFrames));
			}
			else
			{
				QueryCaptureRule rcq = new QueryCaptureRule(fileInteraction, fileCapture, i);
				shootAndCollisionFrames = rcq.getShootFrameAndCollisionFrameActivateFromTheFirstTimeInThisMechanicList(inputMechanics);
				lastFrames = qgr.getLastFrames(i);
				HashMap<Integer, int[]> shootCollision = retrieveFrameNumbersOfShootEvents(i, shootAndCollisionFrames);
				allRelevantFrames = resultAndAllFrames(i, result, shootCollision, lastFrames[lastFrames.length-1]);
				regularInteractionFrames = ma(regularMechanics, i);
				summaries.add(new GameSummary(i, result, regularInteractionFrames, allRelevantFrames));
			}
		}
	}

	public HashMap<Integer, int[]> retrieveFrameNumbersOfShootEvents(Integer simulation, String [] shootAndCollisionFrames)
	{
		HashMap<Integer, int[]> simulationAndFrameNumbers = new HashMap<>();
		if(shootAndCollisionFrames != null)
		{
			int[] frames = new int[shootAndCollisionFrames.length];
			for (int i = 0; i < shootAndCollisionFrames.length; i++) 
			{
				StringBuilder framePath = new StringBuilder(shootAndCollisionFrames[i]).reverse();
				int frameNumber = getTheNumberOfTheFrame(framePath);
				frames[i] = frameNumber;
			}
			simulationAndFrameNumbers.put(simulation, frames);
		}
		return simulationAndFrameNumbers;
	}

	/**
	 * @param framePath
	 * @return
	 * @throws NumberFormatException
	 */
	public int getTheNumberOfTheFrame(StringBuilder framePath) throws NumberFormatException {
		String frame = framePath.substring(4, framePath.indexOf("e"));
		framePath = new StringBuilder(frame).reverse();
		int frameNumber = Integer.parseInt(framePath.toString());
		return frameNumber;
	}

	public HashMap<Integer, int[]> allFrames (int simulation, HashMap<Integer, int[]> simulationAndFrameNumbers, String lastFrames)
	{
		int [] allFrames = new int[]{simulationAndFrameNumbers.get(simulation)[0],
				simulationAndFrameNumbers.get(simulation)[1],
				getTheNumberOfTheFrame(new StringBuilder(lastFrames).reverse())};
		simulationAndFrameNumbers.put(simulation, allFrames);
		return simulationAndFrameNumbers;
	}

	public HashMap<Integer, int[]> resultAndAllFrames (int simulation, int result, HashMap<Integer, int[]> simulationAndFrameNumbers, String lastFrames)
	{
		int [] allFramesWithResult;
		int simFrameNumbers [] = simulationAndFrameNumbers.get(simulation);

		if(simFrameNumbers != null)
		{
			allFramesWithResult = new int[]{result, simFrameNumbers[0],
					simFrameNumbers[1],
					getTheNumberOfTheFrame(new StringBuilder(lastFrames).reverse())};
			simulationAndFrameNumbers.put(simulation, allFramesWithResult);
		}
		else
		{
			allFramesWithResult = new int[]{result,
					getTheNumberOfTheFrame(new StringBuilder(lastFrames).reverse())};
			simulationAndFrameNumbers.put(simulation, allFramesWithResult);
		}

		return simulationAndFrameNumbers;
	}

//	public HashMap<String, int[]> oneMechanicQuery(Mechanic mech, String[] agents, int levelCount, int playthroughCount) throws FileNotFoundException, IOException, ParseException {
//		HashMap<String, int[]> dict = new HashMap<String, int[]>();
//		
//		int numberOfSimulations = agents.length;
//		for(int i = 0; i < agents.length; i++) {
//			for(int j = 0; j < levelCount; j++) {
//				for(int k = 0; k < playthroughCount; k++){
//					QueryActionRule ruleActionQuery = new QueryActionRule(gameName + "/" + agents[i] + "/" + j + "/" + k + "/actions/actions.json");
//					QueryGameResult queryGameResult = new QueryGameResult(gameName + "/" + agents[i] + "/" + j + "/" + k + "/result/result.json");
//					int frameNumber = -1;
//					System.out.println("Finding frames for: " + mech.getReadibleAction());
//					if(mech.getConditions().get(0).getType().equals("Player Input")) {
//						
//						frameNumber = ruleActionQuery.getFirstRuleActionFrameNumber();
//						
//					}
//					else if(mech.isTerminal()) {
//						if ((mech.getActions().get(0).getName().equals("Win") && queryGameResult.getResult() == 1) 
//								|| mech.getActions().get(0).getName().equals("Lose") && queryGameResult.getResult() == 0) {
//							frameNumber = queryGameResult.getLastFrameNumber();
//						} else {
//							frameNumber = -1;
//						}
//					}
//					else {
//						int[] frames = mapFrameNumbersInTheSimulationByMechanic(mech, k);
//						if(frames.length > 0) {
//							frameNumber = mapFrameNumbersInTheSimulationByMechanic(mech, k)[4];
//						} else {
//							frameNumber = -1;
//						}
//					}
//					dict.put(agents[k], getFrameNumbers(frameNumber));
//				}
//			}
//		}
//		return dict;
//	}
//	
	public int mechAgentLevelQuery(Mechanic mech, String agent, int level, int type) throws FileNotFoundException, IOException, ParseException {
		int avg = 0;
		float count = 0;
		
		File levelFile = new File(gameName + "/" + agent + "/" + level);
		File[] listOfPlaythroughs = levelFile.listFiles();
		
		if(type == 0) {
			for(File playthrough : listOfPlaythroughs) {
				if(playthrough.isDirectory()) {
					String playthroughPath = gameName + "/" + agent + "/" + level + "/" + playthrough.getName();
					int frameNumber = getFrameNumber(mech, playthroughPath);
					if(frameNumber != -1) {
						avg += frameNumber;
						count += 1;
					}
				}
			}
			if(count == 0) {
				count = 1;
				avg = -1;
			}
			
			int myAvg = Math.round(avg / count);
			System.out.println("Avg frame for" + gameName + "/" + agent + "/" + level + "/" + " for "+  mech.getReadibleAction() + " is: " + myAvg);
			return myAvg;
		} else if (type == 1){
			File minPlaythrough = null;
			int minUniques = 100000;
			for(File playthrough : listOfPlaythroughs) {
				if(playthrough.isDirectory()) {
					String playthroughPath = gameName + "/" + agent + "/" + level + "/" + playthrough.getName();		
					String path = playthroughPath + "/interactions/interaction.json";
					FrameInteractionAssociation frameInteractionAssociation = new FrameInteractionAssociation(path);
					QueryGameResult queryGameResult = new QueryGameResult(playthroughPath + "/result/result.json");
					int size = frameInteractionAssociation.getUniqueInteractions().size();
					
					if (minUniques > size && queryGameResult.getResult() == 1) {
						minUniques = size;
						minPlaythrough = playthrough;
					}
					
				}
			}
			if(minPlaythrough == null) {
				System.out.println("Its null, man.");
				return -1;
			} else {
				String playthroughPath = gameName + "/" + agent + "/" + level + "/" + minPlaythrough.getName();
				try {
					int frameNumber = getFrameNumber(mech, playthroughPath);
					return frameNumber;
				} catch(Exception e) {
					e.printStackTrace();
					return -1;
				}
			}
		}
		else {
			return -1;
		}
	}
	
	public int getFrameNumber(Mechanic mech, String playthroughPath) throws FileNotFoundException, IOException, ParseException {
		
		QueryActionRule ruleActionQuery = new QueryActionRule(playthroughPath + "/actions/actions.json");
		QueryGameResult queryGameResult = new QueryGameResult(playthroughPath + "/result/result.json");
		
		int frameNumber = -1;
		
		if(mech.getConditions().get(0).getType().equals("Player Input")) {
			
			frameNumber = ruleActionQuery.getFirstRuleActionFrameNumber();
			
		}
		else if(mech.isTerminal()) {
			if ((mech.getActions().get(0).getName().equals("Win") && queryGameResult.getResult() == 1) 
					|| mech.getActions().get(0).getName().equals("Lose") && queryGameResult.getResult() == 0) {
				frameNumber = queryGameResult.getLastFrameNumber();
			} else {
				frameNumber = -1;
			}
		}
		else {
			try{
				int[] frames = mapFrameNumbersInTheSimulationByMechanic(mech, playthroughPath);
				if(frames.length > 0) {
				frameNumber = mapFrameNumbersInTheSimulationByMechanic(mech, playthroughPath)[4];
				} else {
					frameNumber = -1;
				}
			} catch(Exception e) {
				frameNumber = -1;
			}
		}
		return frameNumber;
	}
	
	/***
	 * Gets a list of agents in a specified game directory
	 * @param gameName the name of the game
	 * @return an arraylist of agent names
	 */
	public ArrayList<String> getAgents(String gameName) {
		ArrayList<String> agents = new ArrayList<String>();
		

		File folder = new File(gameName);
		File[] listOfFiles = folder.listFiles();
		
		for(File file : listOfFiles) {
			if(file.isDirectory()) {
				agents.add(file.getName());
			}
		}
		
		return agents;
		
	}
	
	/***
	 * Gets the number of levels in this specific game directory for any agent
	 * @param gameName
	 * @return
	 */
	public int getLevelCount(String gameName) {
		File folder = new File(gameName);
		File[] listOfFiles = folder.listFiles();
		
		if(listOfFiles[0].isDirectory()) {
			File agentFile = listOfFiles[0];
			return agentFile.listFiles().length;
		}
		return 0;
	}
	
	
	public int getPlaythroughCount(String gameName) {
		File folder = new File(gameName);
		File[] listOfFiles = folder.listFiles();
		
		if(listOfFiles[0].isDirectory()) {
			File agentFile = listOfFiles[0];
			File[] listOfLevels = agentFile.listFiles();
			if(listOfLevels[0].isDirectory()) {
				File levelFile = listOfLevels[0];
				return levelFile.listFiles().length;
			}
		}
		return 0;
	}
	
	public int[] getFrameNumbers(int frame) {
		if(frame == -1) {
			return new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
		}
		int[] frames = new int[10];
		
		int lowerBound = Math.max(0, frame - 5);
		int upperBound = Math.max(10,  frame + 5);
		for (int i = 0; i < 10; i++) {
			frames[i] = lowerBound + i;
		}
		return frames;
	}
	
//	public int[][] mapCriticalPath(ArrayList<ArrayList<Mechanic>> superP) throws FileNotFoundException, IOException, ParseException {
//		
//		int numberOfSimulations = (int)numberOfSimulationFoldersAreAvailable();
//		int[][] myReturnArray = new int[numberOfSimulations][];
//		for(int k = 0; k < numberOfSimulations; k++) {
//			
//			QueryActionRule ruleActionQuery = new QueryActionRule(gameName + "/game" + k + "/actions/actions.json");
//			QueryGameResult queryGameResult = new QueryGameResult(gameName + "/game" + k + "/result/result.json");
//			
//			int[] earliestFrames = new int[superP.size()+1];
//			for(int i = 0; i < superP.size()-1; i++){
//				int smallestFrame = Integer.MAX_VALUE;
//				for(int j = 1; j < superP.get(i).size(); j++) {
//					Mechanic temp = superP.get(i).get(j);
//					if(temp.getConditions().get(0).getType().equals("Player Input")) {
//						
//						int frameNumber = ruleActionQuery.getFirstRuleActionFrameNumber();
//						int frameCount = frameNumber;
//						if (frameCount < smallestFrame) 
//						{
//							smallestFrame = frameCount;
//						}
//					}
//					else {
//						// deal with as a normal mechanic
//						int [] frames = mapFrameNumbersInTheSimulationByMechanic(temp, k);
//						if(frames.length > 0 )
//						{
//							int frameCount = frames[4];
//							if (frameCount < smallestFrame || smallestFrame == -1) 
//							{
//								smallestFrame = frameCount;
//							}
//						}
//					}
//					earliestFrames[i+1] = smallestFrame;
//				}
//				
//			}
//			earliestFrames[0] = queryGameResult.getResult();
//			earliestFrames[earliestFrames.length-1] = queryGameResult.getLastFrameNumber();
//			myReturnArray[k] = earliestFrames;
//		}
//		return myReturnArray;
//	}

	public static void main(String [] args) throws FileNotFoundException, IOException, ParseException
	{
		//0 - Configure the critical path
//		ArrayList<ArrayList<Mechanic>> superP = new ArrayList<ArrayList<Mechanic>>();		
//		// example critical path
//		// first mechanic
//		ArrayList<Mechanic> first = new ArrayList<Mechanic>();
//		Mechanic input = new Mechanic(
//				new Entity("avatar", "Object", "FlakAvatar"), 
//				new Entity("Press Space", "Condition", "Player Input"), 
//				new Entity("Shoot", "Action", "Interaction"));
//
//		input.getAction().getOutputs().add(new Entity("sam","Object","Missile"));
//		first.add(input);
//		first.add(input);
//		superP.add(first);
//
//		ArrayList<Mechanic> next1 = new ArrayList<Mechanic>();
//		next1.add(new Mechanic(
//				new Entity("alien", "Object", "alien"), 
//				new Entity("sam", "Object", "Missile"), 
//				new Entity("Collision", "Condition", "n/a"),
//				new Entity("KillSprite", "Action", "Interaction")));
//		next1.add(new Mechanic(
//				new Entity("alienGreen", "Object", "Bomber"), 
//				new Entity("sam", "Object", "Missile"), 
//				new Entity("Collision", "Condition", "n/a"), 
//				new Entity("KillSprite", "Action", "Interaction")));
//		next1.add(new Mechanic(
//				new Entity("alienBlue", "Object", "Bomber"), 
//				new Entity("sam", "Object", "Missile"),
//				new Entity("Collision", "Condition", "n/a"), 
//				new Entity("KillSprite", "Action", "Interaction")));
//		superP.add(next1);
//
//		ArrayList<Mechanic> last = new ArrayList<Mechanic>();
//		last.add(new Mechanic(
//				new Entity("alien", "Object", "alien"), 
//				new Entity("MultiSpriteCounter", "Condition", "n/a"), 
//				new Entity("Win", "Action","Termination")));
//		superP.add(last);
//
//		//1 - Configure your games
//
//		BunchOfGames bog1 = new BunchOfGames("examples/gridphysics/aliens.txt", 
//				"examples/gridphysics/aliens_lvl0.txt", 
//				"tracks.singlePlayer.tools.human.Agent");
//
//		BunchOfGames bog2 = new BunchOfGames("examples/gridphysics/aliens.txt", 
//				"examples/gridphysics/aliens_lvl0.txt", 
//				"tracks.singlePlayer.tools.human.Agent");
//
//		ArrayList<BunchOfGames> bunchOfGames = new ArrayList<>();
//		bunchOfGames.add(bog1); bunchOfGames.add(bog2);
////		//	
////		//	//2 - Run the games
//		VisualDemonstrationInterfacer vdi = new VisualDemonstrationInterfacer(false);
//		vdi.runBunchOfGames(bunchOfGames);
//
//		//			
//		//3 - Query for everything
//		int [][] array = vdi.mapCriticalPath(superP);
//		
//		for (int i = 0; i < array.length; i++) {
//			System.out.println("game : " + i);
//			for (int j = 0; j < array[i].length; j++) {
//				System.out.println(array[i][j]);
//			}
//			System.out.println();
//		}
//
	}

}

class TupleRuleFrames{

	public String rule;
	public String [] frames;
	public TupleRuleFrames(String rule, String [] frames)
	{
		this.rule = rule;
		this.frames = frames;
	}

}