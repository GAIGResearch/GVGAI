package atdelphi_plus;

//for use on the NYU HPC server


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import atdelphi_plus.evaluator.ChildEvaluator;

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

	public static void main(String[] args) {
		////////////////		IMPORT PARAMETERS         //////////////
		
		//read in the program arguments
		id = Integer.parseInt(args[0]);
		size = Integer.parseInt(args[1]);
		
		//read the simulation parameters
		HashMap<String, String> parameters = null;
		try {
		    parameters = readParameters("CMEParameters.txt");
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
		Random rnd = new Random(Integer.parseInt(parameters.get("seed")));
		String runner = parameters.get("runner");
		
		
		
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
				    chromosomes[i].fileInit(levels[i]);
				}
				
				// 4c) Run simulation and calculate results
				int index = 0;
				String placeholderFile = parameters.get("generatorLevelFile").replaceAll("$", ""+id);
				for(Chromosome c:chromosomes) {
				    System.out.println("\t C" + id + ": Running Chromosome number: \t" + ++index);
				    c.calculateResults(runner, placeholderFile);
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
