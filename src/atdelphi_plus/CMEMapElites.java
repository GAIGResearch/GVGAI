package atdelphi_plus;

//Program by Megan "Milk" Charity
//frankensteined from CMEMapElites MarioICDL by amidos


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CMEMapElites {
	private String _gameName;
	private String _gameLoc;
	
	private Random _rnd;
	private double _coinFlip;
	
	private HashMap<String, Chromosome> _map = new HashMap<String, Chromosome>();		//as a test use just the chromosome as the value
	//private HashMap<String, CMECell> _map = new HashMap<String, CMECell>();
	
	private ArrayList<String[]> tutInteractionDict = new ArrayList<String[]>();
	
	
	
	public CMEMapElites(String gn, String gl, Random seed, double coinFlip) {
		this._gameName = gn;
		this._gameLoc = gl;
		this._rnd = seed;
		this._coinFlip = coinFlip;
		
		ParseTutorialRules();
	}
	
	//returns a batch of randomly created chromosomes
	public Chromosome[] randomChromosomes(int batchSize) {
		Chromosome[] randos = new Chromosome[batchSize];
		for(int i=0;i<batchSize;i++) {
			randos[i] = new Chromosome(_rnd, _gameName, _gameLoc, tutInteractionDict);
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
	private void ParseTutorialRules() {
		//assume that the rules will come from the game's specific json file
		//as a test we will use one custom made for zelda
		//however, for the real simulation - assume we can call a function from AtDelphi (original) 
		//	that will provide these rules
		//the format of the JSON file is:
		//		{inputs (sprite2) : [], outputs (sprite1) : [], action : ""}
		
		String gameRuleJSON = "src/atdelphi_plus/" + _gameName + "_tut.json";		//in this scenario it is in the same folder
		try {
	        //read the file
			BufferedReader jsonRead = new BufferedReader(new FileReader(gameRuleJSON));
			System.out.println("game rules file: " + gameRuleJSON);

			//parse each line (assuming 1 object per line)
			String line = jsonRead.readLine();
	        while(line != null) {
	        	//get the input sprite list, output sprite list, and action value
	            JSONObject obj = (JSONObject) new JSONParser().parse(line);
	            JSONArray inputs = (JSONArray)obj.get("input");
	            JSONArray outputs = (JSONArray)obj.get("output");
	            String action = obj.get("action").toString();
	            
	            //add the set to the dictionary
	            for(int a=0;a<inputs.size();a++) {
	            	for(int b=0;b<outputs.size();b++) {
	            		String[] key = {action, inputs.get(a).toString(), outputs.get(b).toString()};
	            		tutInteractionDict.add(key);
	            	}
	            }
	            
	            line = jsonRead.readLine();
	        }
	        //close the file
	        jsonRead.close();         
	    }
	    catch(FileNotFoundException e) {
	        System.out.println("Unable to open file '" + gameRuleJSON + "'");                
	    }
	    catch(IOException e) {
	    	System.out.println("IO EXCEPTION");
	        e.printStackTrace();
	    }catch (ParseException e) {
	    	System.out.println("PARSE EXCEPTION");
			e.printStackTrace();
		}
		
		
	}
	
	//debug to print the imported rules
	public void printRules() {
		for(String[] s : tutInteractionDict) 
			System.out.println("[" + s[0] + ", " + s[1] + ", " + s[2] + "]");
	}
	
	//writes the map to a string (overrides toString() function)
	public String toString() {
		String str = "";
		//write the game name and # of elite cells first
		str += "GAME: " + this._gameName + "\n";
		str += "TOTAL CELLS: " + this._map.size() + "\n";
		
		//print the map to the file
		Set<String> keys = this._map.keySet();
		for(String k : keys) {
			str += ("[" + k + "]\n");
			str += (this._map.get(k).toString());
			str += "\n\n";
		} 
		
		return str;
	}
	
}
