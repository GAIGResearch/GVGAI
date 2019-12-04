package tutorialGeneration.MCTSRewardEvolution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class TestGeneration{
	
	//linear program to test the generation, mutation, and creation of MAPElites

	//location of games
	static String gamesPath = "examples/gridphysics/";
	static String physicsGamesPath = "examples/contphysics/";
	static String generateLevelPath = "src/atdelphi_plus/";
	

	//all public games (from LevelGenerator.java)
	static String games[] = new String[] { "aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", // 0-4
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
			"zelda", "zenpuzzle"}; //90, 91
	
	
	//the tutorial interactions to look for (this will be set in CMEMapElites when it is created)
	//in the form: key = [interaction, sprite2, sprite1]; value = index
	static ArrayList<String[]> tutInteractionDict = new ArrayList<String[]>();
	
	
	// Other settings - these will become parameters in a seperate file
	static Random seed = new Random();			//randomization seed to start from
	static int gameIdx = 90;							//index of the game to use	[ZELDA]
	static String gameName = games[gameIdx];
	//String recordLevelFile = generateLevelPath + games[gameIdx] + "_glvl.txt";
	static String gameLoc = gamesPath + games[gameIdx] + ".txt";
	
	static String aiRunner = "tracks.singlePlayer.advanced.boostedMCTS.Agent";
	
	//static int eliteNum = 2;			//for mapelites can only have 1 (which is the best out of the group)
	static int popNum = 10;
	static int iterations = 5;
	
	
	public static void main(String[] args) throws IOException{
		long startTime = System.nanoTime();
		
		CMEMapElites map = new CMEMapElites(gameName, gameLoc, seed, 0.5, "rules/mechanics_zelda.json", "8");
		
		
		//initialize the 10 random chromosomes
		Chromosome[] myChromos = map.randomChromosomes(popNum, null);
		
		//TODO: for parallelization, write to the files and read them back in
		
		for(int g=0;g<iterations;g++) {
			System.out.println("");
			System.out.println("----- MUTATION SET #" + g + " -----");
			
			//run simulations on all the chromosomes and calculate the fitness + dimension vector for them
			for(Chromosome c : myChromos){
				//System.out.println(c);
				System.out.println("");
				System.out.println(c.rewardEquation);
				//System.out.println(String.join(" ", c._allChar));
				c.calculateResults(aiRunner, 0);
				System.out.println("Constraints score: " + c._constraints);
				System.out.println("Fitness score: " + c._fitness);
				System.out.println("Dimension vector: " + Arrays.toString(c._dimensions));
				System.out.println("");
			}
			
			//assign the chromosomes to the map elite hash table if their fitness scores are better
			map.assignChromosomes(myChromos);
			
			//set the new generation
			myChromos = map.makeNextGeneration(popNum);
		}

//		//export the MAPElites set
//		System.out.println("Exporting map!");
//		BufferedWriter bw = new BufferedWriter(new FileWriter("mapelites.txt"));
//		bw.write(map.toString());
//		bw.close();
		
		
		//time debug ending
		long endTime = System.nanoTime();
		System.out.println("Took "+(endTime - startTime) + " ns"); 
		
	}
	
}