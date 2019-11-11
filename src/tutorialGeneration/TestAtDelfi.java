package tutorialGeneration;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import core.game.Game;
import core.game.GameDescription;
import core.game.SLDescription;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;
import tools.IO;
import tools.Utils;
import tutorialGeneration.criticalPathing.CriticalPather;
import tutorialGeneration.criticalPathing.BFSPather;

public class TestAtDelfi {
	boolean verbose = true;
	
	
    String gamesPath = "examples/gridphysics/";
    String physicsGamesPath = "examples/contphysics/";
    String generateTutorialPath = gamesPath;

    // All public games (gridphysics)
    String[] gamesNames = new String[]{
    		"aliens", "bait", "boloadventures", "boulderchase", "boulderdash", // 0-4
    		"brainman", "butterflies", "camelRace", "catapults", "chase",	   // 5-9
    		"chipschallenge", "crossfire", "defem", "digdug", "eggomania",	   // 10-14
    		"escape", "factorymanager", "firecaster", "firestorms", "frogs",   // 15-19
    		"iceandfire", "infection", "jaws", "labyrinth", "lemmings",        // 20-24
    		"missilecommand", "modality", "overload", "pacman", "painter",     // 25-29
    		"plants", "plaqueattack", "portals", "racebet2", "realportals",    // 30-34
    		"realsokoban", "roguelike", "seaquest", "sokoban", "solarfox",     // 35-39
    		"superman", "surround", "survivezombies", "tercio", "thecitadel",  // 40-44
    		"waitforbreakfast", "whackamole", "zelda", "zenpuzzle"};			// 45-48
	String atDelfiGamesCollection =  "examples/atDelfi_games.csv";
	String[][] games = Utils.readAtDelfiGames(atDelfiGamesCollection);
	
    String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
    
    String gameFile, levelFile, recordTutorialFile;

    int levelIdx = 2; // level names from 0 to 4 (game_lvlN.txt).
    int gameIdx = 34;

    public TestAtDelfi() {
        // settings        
        this.gameFile = this.generateTutorialPath + getGame(gameIdx)[1] + ".txt";
        this.levelFile = this.gamesPath + getGame(gameIdx)[1] + "_lvl" + this.levelIdx + ".txt";
        this.recordTutorialFile = this.generateTutorialPath + getGame(gameIdx)[1] + "_tutorial.txt";
    }
    
	public static void main(String[] args) {
		
        int seed = new Random().nextInt();
		TestAtDelfi tester = new TestAtDelfi();		
		
//		tester.testAllGames(seed);
		tester.testOneGame(seed, tester.gameIdx);
		
//		tester.testOneGame_HPC(seed, Integer.parseInt(args[0]));
//		tester.testFirstGames(seed);
//		tester.testSecondGames(seed);
//		tester.testThirdGames(seed);
//		tester.testFourthGames(seed);
	}
	
	public void testAllGames(int seed) {
		// Set up AtDelfi
		for (String[] gameInfo : this.games) {
			this.gameFile = this.generateTutorialPath + gameInfo[1] + ".txt";
			this.levelFile = this.gamesPath + gameInfo[1] + "_lvl" + this.levelIdx + ".txt";
	        this.recordTutorialFile = this.generateTutorialPath + gameInfo[1] + "_tutorial.txt";


			AtDelfi atdelfi = new AtDelfi(this.gameFile, this.levelFile, gameInfo[1], seed, this.verbose);
//			atdelfi.playGames();
			atdelfi.buildGraph("adrienctx.Agent", 0);

		}
	}
	
	public void testFirstGames(int seed) {
		// Set up AtDelfi
		for (String[] gameInfo : Arrays.copyOfRange(this.games, 0, 12)) {
			this.gameFile = this.generateTutorialPath + gameInfo[1] + ".txt";
			this.levelFile = this.gamesPath + gameInfo[1] + "_lvl" + this.levelIdx + ".txt";
	        this.recordTutorialFile = this.generateTutorialPath + gameInfo[1] + "_tutorial.txt";


			AtDelfi atdelfi = new AtDelfi(this.gameFile, this.levelFile, gameInfo[1], seed, this.verbose);
			atdelfi.playGames();
//			atdelfi.buildGraph();
			
		}
	}
	
	public void testSecondGames(int seed) {
		// Set up AtDelfi
		for (String[] gameInfo : Arrays.copyOfRange(this.games, 12, 24)) {
			this.gameFile = this.generateTutorialPath + gameInfo[1] + ".txt";
			this.levelFile = this.gamesPath + gameInfo[1] + "_lvl" + this.levelIdx + ".txt";
	        this.recordTutorialFile = this.generateTutorialPath + gameInfo[1] + "_tutorial.txt";


			AtDelfi atdelfi = new AtDelfi(this.gameFile, this.levelFile, gameInfo[1], seed, this.verbose);
			atdelfi.playGames();
//			atdelfi.buildGraph();
			
		}
	}
	
	public void testThirdGames(int seed) {
		// Set up AtDelfi
		for (String[] gameInfo : Arrays.copyOfRange(this.games, 24, 36)) {
			this.gameFile = this.generateTutorialPath + gameInfo[1] + ".txt";
			this.levelFile = this.gamesPath + gameInfo[1] + "_lvl" + this.levelIdx + ".txt";
	        this.recordTutorialFile = this.generateTutorialPath + gameInfo[1] + "_tutorial.txt";


			AtDelfi atdelfi = new AtDelfi(this.gameFile, this.levelFile, gameInfo[1], seed, this.verbose);
			atdelfi.playGames();
//			atdelfi.buildGraph();
			
		}
	}
	
	public void testFourthGames(int seed) {
		// Set up AtDelfi
		for (String[] gameInfo : Arrays.copyOfRange(this.games, 36, 49)) {
			this.gameFile = this.generateTutorialPath + gameInfo[1] + ".txt";
			this.levelFile = this.gamesPath + gameInfo[1] + "_lvl" + this.levelIdx + ".txt";
	        this.recordTutorialFile = this.generateTutorialPath + gameInfo[1] + "_tutorial.txt";


			AtDelfi atdelfi = new AtDelfi(this.gameFile, this.levelFile, gameInfo[1], seed, this.verbose);
			atdelfi.playGames();
//			atdelfi.buildGraph();
			
		}
	}
	
	public void testOneGame(int seed, int gameIdx) {
		this.gameFile = this.generateTutorialPath + games[gameIdx][1] + ".txt";
		this.levelFile = this.gamesPath + games[gameIdx][1] + "_lvl" + this.levelIdx + ".txt";
        this.recordTutorialFile = this.generateTutorialPath + games[gameIdx][1] + "_tutorial.txt";

		AtDelfi atdelfi = new AtDelfi(this.gameFile, this.levelFile, this.getGame(this.gameIdx)[1], seed, this.verbose);
//		atdelfi.testPlayGames();
		atdelfi.buildGraph("human", levelIdx);	
		
		CriticalPather criticalPather = new BFSPather(atdelfi.getGameGraph());
//		
		List<Mechanic> critPath = atdelfi.criticalPath(criticalPather, "adrienctx.Agent", true, 0);

		atdelfi.saveGameMechanics("mechanics.json", atdelfi.getGameGraph().getMechanics());
		atdelfi.saveGameMechanics("critical_mechanics.json", critPath);
		System.out.println("boop");
//		for (Mechanic m : critPath) {
//			System.out.println(m.());
//		}
	}
	
	public void testOneGame_HPC(int seed, int id) {
		
		int gameIdx;
		
		
		if(id > 57) {
			gameIdx = 47;
		} else if(id > 28) {
			gameIdx = 34;
		} else {
			gameIdx = 39;
		}
		this.gameFile = this.generateTutorialPath + games[gameIdx][1] + ".txt";
		this.levelFile = this.gamesPath + games[gameIdx][1] + "_lvl" + this.levelIdx + ".txt";
        this.recordTutorialFile = this.generateTutorialPath + games[gameIdx][1] + "_tutorial.txt";

        
		AtDelfi atdelfi = new AtDelfi(this.gameFile, this.levelFile, this.getGame(gameIdx)[1], seed, this.verbose);
		atdelfi.path = this.gamesPath + games[gameIdx][1] + "_lvl";
		atdelfi.playGames(id);
//		atdelfi.buildGraph();	
		
//		CriticalPather criticalPather = new GreedyPather(atdelfi.getGameGraph());
//		
//		List<Mechanic> critPath = atdelfi.criticalPath(criticalPather, "adrienctx.Agent", true);
//		
//		for (Mechanic m : critPath) {
//			System.out.println(m.getSprites().get(0).getName() + " " + m.getReadibleAction() + " " + m.getActions().get(0).getName());
//		}
	}
	
	/**
	 * Testing to make sure automatic node ID assignment is working correctly
	 */
	public void testNodeCounting() {
		Node a = new Node("Test 1", "test", "test");
		Node b = new Node("Test 2", "test", "test");
		
		System.out.println("Expected Ouput: 0 1");
		System.out.println("Actual Output: " + a.getId() + " " + b.getId());
		
	}
	

	public String[] findGame(String name) {
		for (String[] gameInfo : games) {
			if (gameInfo[1].equals(name)) {
				return gameInfo;
			}
		}
		return null;
	}
	
	public String[] getGame(int id) {
		return games[id];
	}
}
