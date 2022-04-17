package homeTest;


import java.util.Random;

import tools.Utils;
import tracks.ArcadeMachine;
import homeTest.Feature;

public class Learning {

    public static void main(String[] args) {

		// Available tracks:
    	/*
		String sampleRandomController = "tracks.singlePlayer.simple.sampleRandom.Agent";
		String doNothingController = "tracks.singlePlayer.simple.doNothing.Agent";
		String sampleOneStepController = "tracks.singlePlayer.simple.sampleonesteplookahead.Agent";
		String sampleFlatMCTSController = "tracks.singlePlayer.simple.greedyTreeSearch.Agent";

		String sampleMCTSController = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
        String sampleRSController = "tracks.singlePlayer.advanced.sampleRS.Agent";
        String sampleRHEAController = "tracks.singlePlayer.advanced.sampleRHEA.Agent";
		String sampleOLETSController = "tracks.singlePlayer.advanced.olets.Agent";
		*/
    	
		String testController = "homeTest.Agent";
		
		//Load available games
		String spGamesCollection =  "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);

		//Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
		int gameIdx = 0;
		int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		String recordActionsFile =  "actions_" + games[gameIdx] + "_lvl"
						 + levelIdx + "_" + seed + ".txt";
						 //where to record the actions
						// executed. null if not to save.
		
		int iter = 0;
		String dir = "homeTest/";
		String homeTest = dir + "actions_" + games[gameIdx] + "_" + iter + "_" + seed +".txt"; 
		System.out.println(game);
		
		Feature test = new Feature(0.5f, false, "Avatar", "Wall", 6.0f, "Nil", false);
		
		System.out.println(test.toString());
		//ArcadeMachine.playOneGame(game, level1, recordActionsFile, seed);

		// 2. This plays a game in a level by the controller.
		//ArcadeMachine.runOneGame(game, level1, visuals, sampleRHEAController, recordActionsFile, seed, 0);
		
		ArcadeMachine.runOneGame(game, level1, visuals, testController, homeTest, seed, 0);
		
		//ArcadeMachine.replayGame(game, level1, visuals, recordActionsFile);
		//System.out.println(recordActionsFile);


    }
}
