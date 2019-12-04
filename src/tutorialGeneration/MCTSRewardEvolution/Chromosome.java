/*
 * Program by Megan "Milk" Charity
 * GVGAI-compatible version of Chromosome.java from MarioAIExperiment 
 * Creates a chromosome for use with MapElites
 */

package tutorialGeneration.MCTSRewardEvolution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tools.IO;
import tracks.ArcadeMachine;
import tracks.levelGeneration.LevelGenMachine;

import tracks.singlePlayer.advanced.boostedMCTS.Agent;
import video.basics.GameEvent;
import eveqt.EquationNode;
import eveqt.EquationParser;
import eveqt.EvEqT;

public class Chromosome implements Comparable<Chromosome>{
	
	/********************
	 * STATIC VARIABLES *
	 ********************/
		
	//taken directly from Chromosome.java [MarioAI]
		static protected Random _rnd;
		//protected int _appendingSize;		//size is dependent on the game itself
	
	//extended variables
		static protected String _gameName;
		static protected String _gamePath;
		static protected String[] _allChar;
		static protected EquationParser _eParser;
		static protected List<GameEvent> _rules;
		static protected double _maxDepth; 
	
	/********************
	 * OBJECT VARIABLES *
	 ********************/
		
	
	//taken directly from Chromosome.java [MarioAI]		
		protected double _constraints;	
		protected double _fitness;
		protected int[] _dimensions;		//binary vector for the interactions that occured for this chromosome
		private int _age;					//age of the current chromosome

	//extended variables
		protected EquationNode rewardEquation;

	
	//sets the static variables for the Chromsome class - shared between all chromosomes
	public static void SetStaticVar(Random seed, String gn, String gp, List<GameEvent> rules, HashSet<String> varNames, double maxDepth) {
		Chromosome._rnd = seed;
		Chromosome._gameName = gn;
		Chromosome._gamePath = gp;
		Chromosome._eParser = new EquationParser(new Random(), varNames, EvEqT.generateConstants(20, 1000));
		Chromosome._maxDepth = maxDepth;
		Chromosome._rules = rules;
	}
	
	
	//constructor for random initialization
	public Chromosome() {
		this._constraints = 0;
		this._fitness = 0;
		this._dimensions = null;
		this._age = 0;
		
		this.randomInit();
	}

	//constructor for cloning and mutation
	public Chromosome(EquationNode rewardEquation) {
		this._constraints = 0;
		this._fitness = 0;
		this._dimensions = null;
		this._age = 0;

		this.rewardEquation = rewardEquation;
	}

	/**
	 * Randomly initialize a new equation tree

	 */
	public void randomInit() {
		try {
			this.rewardEquation = EvEqT.generateRandomTreeEquation(Chromosome._eParser, 10);
		} catch (Exception e) {
			e.printStackTrace();
		};
		

	}
	
	//overwrites the results from an already calculated chromosome of a child process
	public void saveResults(String fileContents) {
		String[] fileStuff = fileContents.split("\n");
		
		this._age = Integer.parseInt(fileStuff[0]);
		this._constraints = Double.parseDouble(fileStuff[2]);
		this._fitness = Double.parseDouble(fileStuff[3]);
			String[] d = fileStuff[4].split("");
			this._dimensions = new int[d.length];
			for(int i=0;i<d.length;i++) {
				this._dimensions[i] = Integer.parseInt(d[i]);
			}
	}
	

	// run a chromosome with an MCTS agent
	public void calculateResults(String aiAgent, int id) throws IOException {

		// run on all levels multiple times
		double average = 0.0;
		int levelCount = 1;
		int playthroughCount = 1;
		for (int i = 0; i < levelCount; i++) {
			for(int j = 0; j < playthroughCount; j++) {
				String levelName = Chromosome._gamePath.replace(".txt", "") + "_lvl" + i + ".txt";
				Agent._rewardEquation = rewardEquation;
				Agent._critPath = Chromosome._rules;
				System.out.println("Playing! \n * Level: " + i + "\n * Playthrough: " + j);

				double[] results = ArcadeMachine.runOneGame(Chromosome._gamePath, levelName, true, aiAgent, null, Chromosome._rnd.nextInt(), 0);
				double win = results[0];
				double score = results[1];
				double runFitness = win * 0.7 + score * 0.3;
				
				average += runFitness;
			}
		}
		average = average / (levelCount * playthroughCount);

		this._age++;					//increment the age (the chromosome is older now that it has been run)
		setConstraints(0); 	//set the constraints (win or lose)
		//calculateRawFitness(results[2], this._textLevel);
		this._fitness = average;		//set the fitness
		calculateDimensions(id);							//set the dimensions
	}


	/*
	 * sets the constraints of the chromosome from the results of a run
	 */
	private void setConstraints(double value) {
		//just uses the win condition
		this._constraints = value;
	}

	// calculates chromosomes dimensions based on the depth of the equation tree
	private void calculateDimensions(int id) {
		//System.out.println("calculating dimensions...");
		
		//create a new dimension set based on the size of _rules and set all values to 0
		this._dimensions = new int[(int) Chromosome._maxDepth];
		for(int d=0;d<this._dimensions.length;d++) {
			this._dimensions[d] = 0;
		}
		// dimension = tree depth
		this._dimensions[this.rewardEquation.getTreeDepth() - 1] = 1;
	}
	
	
	/**
	 * Mutating
	 * @param coinFlip
	 */
	public void mutate(double coinFlip) {
		double f = 0.0;
		//int ct = 0;

		//if it meets the coin flip, then pick a tile and mutate
		do {
			try {
				f = Math.random();
				if (f < 0.33) {
					// Delete a random node from a clone copy of the input equation
					this.rewardEquation = EvEqT.deleteNode(Chromosome._eParser, this.rewardEquation);
				}
				else if (f < 0.66) {
					// Change a random node from a clone copy of the input equation
					this.rewardEquation = EvEqT.changeNode(Chromosome._eParser, this.rewardEquation);
				}
				else {
					// Insert a new node to a clone copy of the input equation
					this.rewardEquation = EvEqT.insertNode(Chromosome._eParser, this.rewardEquation, 5);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			f = Math.random();
		}while(f < coinFlip);


		
	}
	
	//clone chromosome function
	public Chromosome clone() {
		return new Chromosome(this.rewardEquation);
	}

	//override class toString() function
	public String toString() {
		return this.rewardEquation.toString();
	}

	//creates an input file format for the level (for use with parallelization)
	public String toInputFile() {
		String output = "";
		output += this._age + "\n";
		output += (this.toString());
		return output;
	}
	
	//creates an output file format for the level (for use with parallelization)
	public String toOutputFile() {
		String output = "";
		output += (this._age) + "\n";
		output += (this._constraints) + "\n";
		output += (this._fitness) + "\n";
		for(int i=0;i<this._dimensions.length;i++) {output += ("" + this._dimensions[i]);} output += "\n";
		//output += (this.toString());
		return output;
		
	}

	/**
	 * compares the constraints and fitness of 2 chromosomes
	 * taken directly from Chromosome.java [MarioAI]
	 * 
	 * @param o the compared Chromosome object
	 */
	@Override
	public int compareTo(Chromosome o) {
		double threshold = 1.0/11.0;		//within 10 ticks of ideal time
		
		if (this._constraints >= threshold) {
			return (int) Math.signum(this._fitness - o._fitness);
		}
		return (int) Math.signum(this._constraints - o._constraints);
	}

	//////////  GETTER FUNCTIONS  ///////////
	public int get_age() {
		return _age;
	}

	public double getConstraints() {
		return this._constraints;
	}

	public double getFitness() {
		return this._fitness;
	}

	public int[] getDimensions() {
		return this._dimensions;
	}


	///////////   HELPER FUNCTIONS   ////////////
	//gets the index of a character in an array (helper function)
	public int indexOf(char[] arr, char elem) {
		for(int i=0;i<arr.length;i++) {
			if(arr[i] == elem)
				return i;
		}
		return -1;
	}

	//log base 2 converter (helper function)
	public double log2(double x)
	{
		return (Math.log(x) / Math.log(2.0));
	}


	// TODO implement reading in reward 
	public void fileInit(String string) {
		// read in reward equation into this chromosome's reward equation
		try {
			this.rewardEquation = Chromosome._eParser.parse(string.split("\n")[1]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}