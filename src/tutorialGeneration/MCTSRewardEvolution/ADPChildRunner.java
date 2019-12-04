package tutorialGeneration.MCTSRewardEvolution;

//for use on the NYU HPC server


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import tutorialGeneration.MCTSRewardEvolution.evaluator.ChildEvaluator;


public class ADPChildRunner {

	public static int id;
	public static int size;


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

	public static void main(String[] args) throws IOException{
		////////////////		IMPORT PARAMETERS         //////////////
		
		//read in the program arguments
		id = Integer.parseInt(args[0]);
		size = Integer.parseInt(args[1]);
		
		//read the simulation parameters
		HashMap<String, String> parameters = null;
		try {
		    parameters = readParameters("MCTSRewardEvolutionParameters.txt");
		} catch (IOException e1) {
		    e1.printStackTrace();
		}
		
		//add extra parameters?
		/*
		parameters.put("agentType", "AStarAgent");
		if(args.length > 2) {
		    parameters.put("agentType", args[2]);
		}
		parameters.put("agentSTD", "1");
		if(args.length > 3) {
		    parameters.put("agentSTD", args[3]);
		}
		parameters.put("numTrials", "1");
		if(args.length > 4) {
		    parameters.put("numTrials", args[4]);
		}
		*/
		//initialize the child reader
		ChildEvaluator child = new ChildEvaluator(id, size, parameters.get("inputFolder"), parameters.get("outputFolder"));
		Random seed = new Random(Integer.parseInt(parameters.get("seed")));
		String runner = parameters.get("runner");
		double coinFlip = Double.parseDouble(parameters.get("coinFlip"));
		int gameIndex = Integer.parseInt(parameters.get("gameIndex"));

		//import the game list
		HashMap<Integer, String[]> gameList = readGamesCSV(parameters.get("gameListCSV"));
		
		//get the game name and game location
		String gameName = gameList.get(gameIndex)[0];
		String gameLoc = gameList.get(gameIndex)[1];
		
		
		CMEMapElites map = new CMEMapElites(gameName, gameLoc, seed, coinFlip, parameters.get("tutorialFile"), parameters.get("maxTreeDepth"));
		
		/////////////		START OF TUTORIAL LEVEL GENERATION   	/////////////////
		Chromosome[] chromosomes = null;
		
		
		//run forever, or until all the iterations have been completed
		while(true) {
			try {
				// 1c) Wait for files to be written by the parent
				System.out.println("C" + id + ": Waiting for parent to finish writing input files...");
				while(!child.checkChromosomes()) {
				    Thread.sleep(500);
				}
				Thread.sleep(1000);
				
				// 2c) Read in chromosomes levels from files in the input folder
				System.out.println("C" + id + ": Reading in levels...");
				String[] levels = child.readChromosomes();
				
				// 3c) Initialize new chromosomes
				chromosomes = new Chromosome[levels.length];
				for(int i=0; i<chromosomes.length; i++) {
				    chromosomes[i] = new Chromosome();
				    //System.out.println(levels[i]);
				    chromosomes[i].fileInit(levels[i]);
				}
				
				// 4c) Run simulation and calculate results
				int index = 0;
				for(Chromosome c:chromosomes) {
				    System.out.println("\t C" + id + ": Running Chromosome number: \t" + ++index + "/" + size + " (#" + (index+(id*size)) + ")");
				    c.calculateResults(runner, id);
				}
				
				// 4.5c) delete old input files
				child.clearInputFiles();
				
				// 5c) Write results to the output folder
				System.out.println("C" + id + ": Writing chromosome results...");
				String[] values = new String[chromosomes.length];
				for(int i=0;i<chromosomes.length;i++) {
					values[i] = chromosomes[i].toOutputFile();
				}
				child.writeResults(values);
				
				// 6c) Repeat back to (1c)
				
				
			}catch(Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
