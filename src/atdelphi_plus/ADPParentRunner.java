package atdelphi_plus;

//for use on the NYU HPC server


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import atdelphi_plus.evaluator.ParentEvaluator;

public class ADPParentRunner {

	//clear everything from a specified directory
	private static void deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		directoryToBeDeleted.delete();
	}
	
	//parse the parameters from the external file
	private static HashMap<String, String> readParameters(String filename) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get("", filename));
		HashMap<String, String> parameters = new HashMap<String, String>();
		for(int i=0; i<lines.size(); i++) {
			if(lines.get(i).trim().length() == 0) {
				continue;
			}
			String[] parts = lines.get(i).split("=");
			parameters.put(parts[0].trim(), parts[1].trim());
		}
		return parameters;
	}
	
	//parse csv file
	public static HashMap<Integer, String[]> readGamesCSV(String csv) throws IOException{
		HashMap<Integer, String[]> gameSet = new HashMap<Integer, String[]>();
		List<String> lines = Files.readAllLines(Paths.get("", csv));
		for(int i=0;i<lines.size();i++) {
			//skip empty lines
			if(lines.get(i).trim().length() == 0)
				continue;
			
			//grab the game name from the end of the directory string
			String[] partStr = lines.get(i).split(",");
			String[] dir = partStr[1].split("/");
			String game_name = dir[dir.length-1].replace(".txt", "");
			
			//add the index of the csv and the string array of the game path and the game name
			gameSet.put(Integer.parseInt(partStr[0]), new String[]{game_name, partStr[1]});
		}
		
		return gameSet;
		
	}
	
	public static void main(String[] args)  throws IOException{
		////////////////		IMPORT PARAMETERS         //////////////
		
		//import the parameter values
		HashMap<String, String> parameters = null;
		try {
			parameters = readParameters("AtDelphiPlusParameters.txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//parse the parameters
		Random seed = new Random(Integer.parseInt(parameters.get("seed")));
		int gameIndex = Integer.parseInt(parameters.get("gameIndex"));
		int popSize = Integer.parseInt(parameters.get("populationSize"));
		double coinFlip = Double.parseDouble("coinFlip");
		
		//import the game list
		HashMap<Integer, String[]> gameList = readGamesCSV(parameters.get("gameListCSV"));
		
		/////////////		START OF TUTORIAL LEVEL GENERATION   	/////////////////
		
		
		//TODO: Repeat for all games
		
		
		//get the game name and game location
		String gameName = gameList.get(gameIndex)[0];
		String gameLoc = gameList.get(gameIndex)[1];
		
		//setup map elites and the first chromosomes
		CMEMapElites map = new CMEMapElites(gameName, gameLoc, seed, coinFlip);
		ParentEvaluator parent = new ParentEvaluator(parameters.get("inputFolder"), parameters.get("outputFolder"));
		System.out.println("First Batch of Chromosomes");
		Chromosome[] chromosomes = map.randomChromosomes(popSize);
		int iteration = 0;
		int maxIterations = -1;
		if(args.length > 0) {
			maxIterations = Integer.parseInt(args[0]);
		}
		
		while(true) {
			break;
		}
		
	}
}
