package tutorialGeneration.MCTSRewardEvolution;

//Program by Megan "Milk" Charity
//frankensteined from CMEMapElites MarioICDL by amidos


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tutorialGeneration.MechanicParser;
import video.basics.GameEvent;

public class CMEMapElites {
	private String _gameName;
	private double _coinFlip;
	
	private HashMap<String, Chromosome> _map = new HashMap<String, Chromosome>();		//as a test use just the chromosome as the value
	//private HashMap<String, CMECell> _map = new HashMap<String, CMECell>();
	
	private HashSet<String> varNames;
	private List<GameEvent> rules;
	
	
	public CMEMapElites(String gn, String gl, Random seed, double coinFlip, String mechanicsFile, String maxTreeDepth) {
		this._gameName = gn;
		this._coinFlip = coinFlip;
		
		rules = parseTutorialRules(mechanicsFile);
		varNames = this.convertToRuleNames(rules);
		Chromosome.SetStaticVar(seed, gn, gl, rules, varNames, Double.parseDouble(maxTreeDepth));
	}
	
	//returns a batch of randomly created chromosomes
	public Chromosome[] randomChromosomes(int batchSize, String placeholder) {
		Chromosome[] randos = new Chromosome[batchSize];
		for(int i=0;i<batchSize;i++) {
			randos[i] = new Chromosome();
		}
		return randos;
	}
	
	//assigns the new set of chromosomes to the map elites hash if their fitness scores are better than the saved chromosomes 
	public void assignChromosomes(Chromosome[] csomes) {
		for(Chromosome c : csomes) {
			int[] raw_dimen = c._dimensions;
			String dimen = dimensionsString(raw_dimen);
			
			//this dimensionality hasn't been saved to the map yet - so add it automatically
			if(!_map.containsKey(dimen)) {
				 _map.put(dimen, c);
			}else {
				Chromosome set_c = _map.get(dimen);
				//replace the current chromosome if the new one is better
				if(set_c.compareTo(c) < 0) {
					_map.replace(dimen, c);
				}
			}
		}
	}
	
	//retuns the current mapping of chromosomes
	public Chromosome[] getCells() {
		Chromosome[] cells = new Chromosome[_map.size()];
		int index = 0;
		for(Entry<String,Chromosome> pair : this._map.entrySet()) {
			cells[index] = pair.getValue();
			index += 1;
		}
		return cells;
	}
	
	public Chromosome[] makeNextGeneration(int batchSize) {
		Chromosome[] nextGen = new Chromosome[batchSize];
		Chromosome[] eliteCells = getCells();
		
		for(int b=0;b<batchSize;b++) {
			//pick a random elite chromosome
			Chromosome randElite = eliteCells[new Random().nextInt(eliteCells.length)];
			Chromosome mutChromo = randElite.clone();
			
			//mutate it
			mutChromo.mutate(this._coinFlip);
			
			//add it to the next generation
			nextGen[b] = mutChromo;
		}
		
		return nextGen;
	}
	
	//returns the dimensions binary vector as a string (i.e. [0,1,0,1] => 0101)
	private String dimensionsString(int[] d) {
		String s = "";
		for(int i=0;i<d.length;i++) {
			s += d[i];
		}
		return s;
	}
	
	//sets the game interaction set (rules) for the dimensionality
	private List<GameEvent> parseTutorialRules(String mechanicFile) {
		List<GameEvent> mechanics = MechanicParser.readMechFile(mechanicFile);

		return mechanics;
	}
	
	// Converts the raw mechanic info into strings for variables in the equation trees
	private HashSet<String> convertToRuleNames(List<GameEvent> mechanics) {
		List<String> mechNames = new ArrayList<String>();
		for (GameEvent event : mechanics) {
			mechNames.add(event.toString());
		}
		HashSet<String> varSet = new HashSet<String>(mechNames);
		return varSet;
	}
	
	
	//debug to print the imported rules
	public void printRules() {
		for(String s : this.varNames) 
			System.out.println(s);
	}
	
	//writes the map to a string (overrides toString() function)
	public String toString() {
		String str = "";
		//write the game name and # of elite cells first
		str += "GAME: " + this._gameName + "\n";
		str += "TOTAL CELLS: " + this._map.size() + "\n";
		str += "\n\n";
		
		//print the map to the file
		Set<String> keys = this._map.keySet();
		for(String k : keys) {
			Chromosome l = this._map.get(k);
			
			str += ("Dimensions: [" + k + "]\n");
			str += ("Age: " + l.get_age());
			str += ("\nConstraints: " + l.getConstraints());
			str += ("\nFitness: " + l.getFitness());
			str += "\nLevel: \n";
			str += (l.toString());
			
			str += "\n\n";
		} 
		
		return str;
	}
	
	//prints each dimension chromosome to their own file
	public void deepExport(String exportPath) throws Exception {
		
		Set<String> keys = this._map.keySet();
		
		//individually writes every level dimension, age, constraints, fitness, and text level
		for(String k : keys) {
			String wholePath = exportPath + "/" + this._gameName + "_" + k + ".txt";
			BufferedWriter bw = new BufferedWriter(new FileWriter(wholePath));
			
			Chromosome l = this._map.get(k);
			String str = "";
			
			str += ("Dimensions: [" + k + "]\n");
			str += ("Age: " + l.get_age());
			str += ("\nConstraints: " + l.getConstraints());
			str += ("\nFitness: " + l.getFitness());
			str += "\nLevel: \n";
			str += (l.toString());
			
			str += "\n\n";
			
			
			bw.write(str);
			bw.close();
		}
	}
	
}
