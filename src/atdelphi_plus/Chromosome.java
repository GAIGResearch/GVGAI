/*
 * Program by Megan "Milk" Charity
 * GVGAI-compatible version of Chromosome.java from MarioAIExperiment 
 * Creates a chromosome for use with MapElites
 */

package atdelphi_plus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tools.IO;
import tracks.ArcadeMachine;
import tracks.levelGeneration.LevelGenMachine;

public class Chromosome implements Comparable<Chromosome>{
	//custom level generator directly based off randomLevelGenerator.java 
	// - differences:
	//     is the constant size of the level
	static private String chromoLevelGenerator = "atdelphi_plus.ChromosomeLevelGenerator";

	//the default level generator saves the text output to a file - so just write it and read it back in
	public String placeholderLoc = "src/atdelphi_plus/generatedLevels/placeholder.txt";


	//taken directly from Chromosome.java [MarioAI]		
	static protected Random _rnd;
	//protected int _appendingSize;		//size is dependent on the game itself
	protected double _constraints;	
	protected double _fitness;
	protected int[] _dimensions;		//binary vector for the interactions that occured for this chromosome
	private int _age;					//age of the current chromosome

	//extended variables
	static protected String _gameName;
	static protected String _gamePath;
	protected String _textLevel;
	protected String _fullTextLevel;
	static protected String[] _allChar;
	protected boolean _hasBorder;
	static protected ArrayList<String[]> _rules;

	private char borderChar;

	
	
	//constructor for random initialization
	public Chromosome(Random seed, String gn, String gp, ArrayList<String[]> r) {
		_rnd = seed;
		_constraints = 0;
		_fitness = 0;
		_dimensions = null;
		_age = 0;
		_textLevel = "";
		_fullTextLevel = "";
		
		_rules = r;
		randomInit(gn, gp);
	}

	//constructor for cloning and mutation
	public Chromosome(Random seed, String level, String[] mapChar, boolean hasborder, String gn, String gp, ArrayList<String[]> r) {
		_constraints = 0;
		_fitness = 0;
		_dimensions = null;
		_age = 0;
		_fullTextLevel = "";

		this._rnd = seed;
		this._textLevel = level;
		this._allChar = mapChar;
		this._hasBorder = hasborder;
		this._gamePath = gp;
		this._gameName = gn;
		this._rules = r; 
	}


	//random level initialization function using LevelGenMachine.java and ChromosomeLevelGenerator (AtDelphi+ exclusive class)
	public void randomInit(String gamename, String gamePath) {
		this._gameName = gamename;
		this._gamePath = gamePath;
		this._textLevel = "";


		try {
			//run the default level gen and write to the placeholder file
			LevelGenMachine.generateOneLevel(gamePath, chromoLevelGenerator, placeholderLoc);
			this._fullTextLevel = fullLevel();
			this._allChar = getMapChar();
			this._textLevel = parseLevel();
			this._hasBorder = level_has_border();


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//returns the full level
	private String fullLevel() {
		String[] lines = new IO().readFile(placeholderLoc);
		return String.join("\n", lines);
	}

	//stripped from LevelGenMachine's loadGeneratedFile() method
	private String parseLevel() {
		String level = "";

		String[] lines = this._fullTextLevel.split("\n");
		int mode = 0;
		for(String line: lines) {
			if(line.equals("LevelDescription")) {
				mode = 1;
			}else if(mode == 1){
				level += (line + "\n");
			}
		}

		return level;
	}

	//returns a list of characters used for the map (map character key)
	private String[] getMapChar() {
		String level = "";
		String[] lines = this._fullTextLevel.split("\n");

		String charList = "";
		int mode = 0;
		for(String line: lines) {
			if(line.equals("LevelMapping")) {
				mode = 1;
				continue;
			}else if(line.contentEquals("LevelDescription")) {
				mode = 0;
				continue;
			}
			else if(mode == 1 && line.length() > 0) {
				String l = line.trim();
				charList += l.charAt(0);
			}
		}
		charList += " ";
		return charList.split("");
	}

	/* 
	 * check if the level has a border
	 */
	private boolean level_has_border() {
		String[] lines = _textLevel.split("\n");

		//if there is a border, the first character in the level should be the start of the border
		String bordChar = Character.toString(lines[0].charAt(0));		

		//check first and last line for border character (ceiling and floor)
		if(lines[0].replace(bordChar, "").length() > 0)		//if there are any characters leftover, it is not a border
			return false;

		//check the first and last character of each line (walls)
		for(int i=1;i<lines.length-1;i++) {
			String line = lines[i];
			if(line.charAt(0) != bordChar.charAt(0) || line.charAt(line.length()-1) != bordChar.charAt(0))
				return false;
		}

		//passed - has a border
		return true;
	}




	//text level initialization function
	public void stringInit(String gamename, String extLevel) {
		_gameName = gamename;
		_textLevel = extLevel;
	}


	//chromosome level runner
	public void calculateResults(String aiAgent) throws IOException {
		copyLevelToPlaceholder();
		System.out.println("Playing game...");
		double[] results = ArcadeMachine.runOneGame(this._gamePath, placeholderLoc, false, aiAgent, null, this._rnd.nextInt(), 0);
		/*
		for(double d : results) {
			System.out.println(d);
		}
		*/

		this._age++;					//increment the age (the chromosome is older now that it has been run)
		setConstraints(results); 	//set the constraints (win or lose)
		//calculateRawFitness(results[2], this._textLevel);
		this._fitness = calculateFitnessEntropy();		//set the fitness
		calculateDimensions();							//set the dimensions

	}


	/*
	 * sets the constraints of the chromosome from the results of a run
	 * sh-boom
	 */
	private void setConstraints(double[] results) {
		//just uses the win condition
		//this._constraints = results[0];		

		//constraints = (win / timeToWin) + ((1-win) * 0.25 / timeToSurvive)
		//this._constraints = (results[0] / results[2]) + (((1-results[0]) * 0.25) / results[2]);
		
		//constraints = (win / (timeToWin dist from ideal time)) + ((1-win) * 0.25 / (timeToSurvive dist from ideal time))
		int idealTime = 50;
		this._constraints = (results[0] / (Math.abs(idealTime - results[2])+1)) + (((1-results[0]) * 0.25) / (Math.abs(idealTime - results[2])+1));
	}

	//gets the entropy of unique tiles for the level to be used to calculate fitness
	private double calculateFitnessEntropy() {
		char[] charLevel = this._textLevel.toCharArray();
		int[] charCt = new int[this._allChar.length];

		//make a new char set from allChar[] because Java is a problematic whiny little shit
		char[] achar = new char[this._allChar.length];
		for(int c=0;c<this._allChar.length;c++) {
			achar[c] = this._allChar[c].charAt(0);
		}

		//initialize charCt to 0
		for(int i=0;i<charCt.length;i++) {
			charCt[i] = 0;
		}

		//count for each tile
		int allCt = 0;
		for(int a=0;a<charLevel.length;a++) {
			int index = indexOf(achar, charLevel[a]);
			if(index >= 0) {
				charCt[index]++;
				allCt++;
			}

		}	

		//calculate the probabilities for the entropy
		double[] probs = new double[achar.length];
		for(int b=0;b<achar.length;b++) {
			probs[b] = (double)((double)charCt[b] / (double)allCt);
			//System.out.println("CHAR: " + achar[b]);
			//System.out.println("ct: " + charCt[b]);
			//System.out.println("probability: " + probs[b]);
			//System.out.println("");
		}

		//calculate entropy (-sum(plog2(p)))
		double entropy = 0;
		for(int b=0;b<probs.length;b++) {
			entropy = (-1*(probs[b] * log2(probs[b])));
		}

		return entropy;
	}

//	
//	// Fitness calculator - should look at time completed and amount of open space
//	// higher score = worse fitness
//	private void calculateRawFitness(double aiTime, String map) {
//		//setConstraints(results[0]);		//set the constraints (win/lose)
//
//		/*
//		//count the non open spaces (~" ")
//		int openSpace = 0;
//		String[] lines = map.split("\n");
//		for(int i=0;i<lines.length;i++) {
//
//			//skip the border lines
//			if(this._hasBorder && ((i == 0) || (i == lines.length-1)))
//				continue;
//
//			String curline = lines[i];
//			for(int j=0;j<curline.length();j++) {
//				//skip the wall border
//				if(this._hasBorder && (j == 0 || j == curline.length()))
//					continue;
//
//				if(curline.charAt(j) == ' ')
//					openSpace++;
//
//			}
//		}
//		 */
//		double openSpace = 0.0;
//
//		//get the distance for the time completed (difference from best time to actual time)
//		double timeDiff = Math.abs(Chromosome.bestTime - aiTime);
//
//		//add 1000 points if the agent didn't finish the level [1->win; 0->lose]
//		double finishScore = (1-this._constraints) * 1000;
//
//		//add up the scores and set the fitness 
//		double fitnessScore = openSpace + timeDiff + finishScore;
//		this._fitness = fitnessScore;
//	}


	//calculates level dimensions based on the game's interaction ruleset
	//and results in a binary vector for whether the rule was triggered during the agent's run of the level
	
	//DEMO: use Zelda rules (player killed enemy? [0-2] player picked up key? [3])
	private void calculateDimensions() {
		//System.out.println("calculating dimensions...");
		
		//create a new dimension set based on the size of _rules and set all values to 0
		this._dimensions = new int[_rules.size()];
		for(int d=0;d<this._dimensions.length;d++) {
			this._dimensions[d] = 0;
		}
		
		//open the json file that the run just exported the interactions to
		String outputInteractionJSON = "src/atdelphi_plus/generatedLevels/interactions.json";
		
		try {
	        //read the file
			BufferedReader interRead = new BufferedReader(new FileReader(outputInteractionJSON));

			//parse each line (assuming 1 object per line)
			String line = interRead.readLine();
	        while(line != null) {
	        	//System.out.println(line);
	        	JSONObject obj = (JSONObject)new JSONParser().parse(line);
	        	String action = obj.get("interaction").toString();
	        	String sprite2 = obj.get("sprite2").toString();
	        	String sprite1 = obj.get("sprite1").toString();
	        	
	        	String[] tryKey = {action, sprite2, sprite1};
	        	
	        	//System.out.println(Arrays.deepToString(tryKey));
	        	
	        	//confirm the interaction in the dimension space
	        	int ruleIndex = hasRule(tryKey);
	        	if(ruleIndex >= 0) {
	        		_dimensions[ruleIndex] = 1;
	        	}
	        	line = interRead.readLine();
	        }
	        interRead.close();
		}catch(FileNotFoundException e) {
	        System.out.println("Unable to open file '" + outputInteractionJSON + "'");                
	    }
	    catch(IOException e) {
	        e.printStackTrace();
	    } catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	
	private int hasRule(String[] interaction) {
		//iterates through the whole set to see if the arrays match
		for(int r=0;r<_rules.size();r++) {
			String[] rule = _rules.get(r);
			boolean matchRule = true;
			for(int i=0;i<rule.length;i++) {
				if(!rule[i].equals(interaction[i]))	//if a mismatch - then definitely not the same rule
					matchRule = false;
			}
			
			if(matchRule)
				return r;
		}
		return -1;
	}
	
	//mutates a random tile (within the border if applicable) based on a "coin flip" (given probability between 0-1)
	public void mutate(double coinFlip) {
		double f = 0.0;
		int ct = 0;

		//if it meets the coin flip, then pick a tile and mutate
		do {
			String[] rows = this._textLevel.split("\n");
			int r = 0;
			int c = 0;

			//if no border - use the whole game space
			if(!this._hasBorder) {
				r = new Random().nextInt(rows.length);
				c = new Random().nextInt(rows[r].length());
			}
			//if there is a border offset by 1
			else {
				r = new Random().nextInt(rows.length-2)+1;
				c = new Random().nextInt(rows[r].length()-2)+1;
			}

			int n = new Random().nextInt(this._allChar.length);

			//replace the character at the random tile with another character
			String replaceRow = rows[r].substring(0, c) + this._allChar[n] + rows[r].substring(c+1);
			rows[r] = replaceRow;
			this._textLevel = String.join("\n", rows);

			f = Math.random();
			ct++;
		}while(f < coinFlip);

		//System.out.println("Mutated " + ct + " times");

		//remove duplicate avatars
		boolean hasAvatar = false;
		char[] charLevel = this._textLevel.toCharArray();
		for(int a = 0; a < charLevel.length; a++) {
			char levChar = charLevel[a];
			if(levChar == 'A') {
				if(!hasAvatar) {
					hasAvatar = true;
					continue;
				}else {
					//keep replacing the character until it's not an A anymore
					do{
						int n = new Random().nextInt(this._allChar.length);
						charLevel[a] = this._allChar[n].charAt(0);				//TODO: Convert to char array
					}while (charLevel[a] == 'A');
				}
			}
		}

		//if no avatar at all - replace an empty space (or another tile if one doesn't exist) with it
		if(!hasAvatar) {
			int randPt = new Random().nextInt(charLevel.length);
			while(this._textLevel.contains(" ") && charLevel[randPt] != ' ') {
				randPt = new Random().nextInt(charLevel.length);
			}

			charLevel[randPt] = 'A';
		}

		this._textLevel = new String(charLevel);

		//otherwise finish
		return;
	}

	
	
	

	/*
	 * Copies the textLevel to the placeholder file
	 * (for use with LevelGenMachine)
	 */
	public void copyLevelToPlaceholder() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(placeholderLoc));
		writer.write(this._textLevel);
		//writer.write(this._fullTextLevel); 

		writer.close();
	}


	//clone chromosome function
	public Chromosome clone() {
		return new Chromosome(this._rnd, this._textLevel, this._allChar, this._hasBorder, this._gameName, this._gamePath, this._rules);
	}

	//override class toString() function
	public String toString() {
		return _textLevel;
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
}