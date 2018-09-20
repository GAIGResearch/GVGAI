package tutorialGeneration;

import java.util.Random;

import core.game.Game;
import core.game.GameDescription;
import core.game.SLDescription;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;
import tools.IO;

public class TestAtDelfi {
	boolean verbose = true;
	
	
    String gamesPath = "examples/gridphysics/";
    String physicsGamesPath = "examples/contphysics/";
    String generateTutorialPath = gamesPath;

    // All public games (gridphysics)
    String[] games = new String[]{"aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", // 0-4
            "beltmanager", "blacksmoke", "boloadventures", "bomber", "bomberman", // 5-9
            "boulderchase", "boulderdash", "brainman", "butterflies", "cakybaky", // 10-14
            "camelRace", "catapults", "chainreaction", "chase", "chipschallenge", // 15-19
            "clusters", "colourescape", "chopper", "cookmepasta", "cops", // 20-24
            "crossfire", "defem", "defender", "digdug", "dungeon", // 25-29
            "eighthpassenger", "eggomania", "enemycitadel", "escape", "factorymanager", // 30-34
            "firecaster", "fireman", "firestorms", "freeway", "frogs", // 35-39
            "garbagecollector", "gymkhana", "hungrybirds", "iceandfire", "ikaruga", // 40-44
            "infection", "intersection", "islands", "jaws", "killBillVol1", // 45-49
            "labyrinth", "labyrinthdual", "lasers", "lasers2", "lemmings", // 50-54
            "missilecommand", "modality", "overload", "pacman", "painter", // 55-59
            "pokemon", "plants", "plaqueattack", "portals", "raceBet", // 60-64
            "raceBet2", "realportals", "realsokoban", "rivers", "roadfighter", // 65-69
            "roguelike", "run", "seaquest", "sheriff", "shipwreck", // 70-74
            "sokoban", "solarfox", "superman", "surround", "survivezombies", // 75-79
            "tercio", "thecitadel", "thesnowman", "waitforbreakfast", "watergame", // 80-84
            "waves", "whackamole", "wildgunman", "witnessprotection", "wrapsokoban", // 85-89
            "zelda", "zenpuzzle"}; // 90, 91
    
    String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
    
    String gameFile, levelFile, recordTutorialFile;

    int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
    int gameIdx = 90;
    // + levelIdx + "_" + seed + ".txt";
    // where to record the actions
    // executed. null if not to save.


    public TestAtDelfi() {
        // Other settings
        boolean visuals = true;
        
        this.gameFile = this.generateTutorialPath + this.games[this.gameIdx] + ".txt";
        this.levelFile = this.gamesPath + this.games[this.gameIdx] + "_lvl" + this.levelIdx + ".txt";
        this.recordTutorialFile = this.generateTutorialPath + this.games[this.gameIdx] + "_tutorial.txt";
    }
    
	public static void main(String[] args) {
		
        int seed = new Random().nextInt();
		TestAtDelfi tester = new TestAtDelfi();		
		
		// Set up AtDelfi
        AtDelfi atdelfi = new AtDelfi(tester.gameFile, tester.levelFile, tester.games[tester.gameIdx], seed, tester.verbose);

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
	

}
