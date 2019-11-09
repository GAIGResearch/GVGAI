package agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import agents.ICELab.GameInfo;
import agents.ICELab.OpenLoopRLBiasMCTS.Node;

public class RLBiasedActionSelector {

	double E = 0.1; 						// (1 - E) chance to select action greedily, E chance randomly

	int rollout_count = 0;
    boolean printQValuesToCSV = false;
    final int qLearningPrintFrequency = 2000;
    private FileWriter csvWriter;
	String reportDate;
	String directory;
    String fitnessRecord;

	int selectedAction;
	int numFeatures = 0;
	boolean printNewState = false;
	Random rand = new Random();

	public final int EXPERIENCE_REPLAY_MEMORY_SIZE = 500;
	public final int MINI_BATCH_SIZE = 10;
	public final int C = 500;
	public final double LEARNING_RATE = 0.1;

	HashMap <Position, HashMap<Types.ACTIONS, Double>> Q_values = new HashMap <Position, HashMap<Types.ACTIONS, Double>>();
	HashMap <Position, Integer> Q_values_update_count = new HashMap <Position, Integer>();

	HashMap <Position, HashMap<Types.ACTIONS, Double>> Q_head = new HashMap <Position, HashMap<Types.ACTIONS, Double>>();

    ArrayList<Position> currentFeatures;

    class Transition{			// four tuples {s, r, a, s'}
    		ArrayList<Position> features; 	// s
    		double reward;			// r
    		Types.ACTIONS aciton;				// a
    		ArrayList<Position> newFeatures;	// s'
    		Transition(ArrayList<Position> currentFeatures, Types.ACTIONS selectedAction, double value, ArrayList<Position> newStateFeatures){
    			this.features = new ArrayList<Position>();
    			for (Position p : currentFeatures)
    				this.features.add(new Position(p));
    			this.aciton = selectedAction;
    			this.reward = value;
    			this.newFeatures = new ArrayList<Position>();
    			for (Position p : newStateFeatures)
    				this.newFeatures.add(new Position(p));
    		}
    }

    int updateStepCount = 0;

    ArrayList<Transition> replayMemory = new ArrayList<Transition>();

    public RLBiasedActionSelector(){
    		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
	    java.util.Date today = Calendar.getInstance().getTime();
	    reportDate = df.format(today);
		directory = "RLBiasedMCTS/";
		File resultsDir = new File(directory);
        	if (!resultsDir.exists())
        		resultsDir.mkdirs();
    }

	public Node selectAction(Node node) {
		ArrayList<Position> currentFeatures = extractFeatures(node.state);

		if ((Q_head == null || updateStepCount % C == 0)){
			Q_head = copyValues(Q_values);
		}
		updateStepCount++;

		double[] a = getBias(currentFeatures);

		double acum = 0;
		for (int i = 0; i<a.length; i++){
			//System.out.println("a[" + i + "] = " + a[i]);
			a[i] = 1.0 / (1.0 + Math.pow(Math.E, -a[i]));
			acum += a[i];
		}

		int i = 0;
		double temp = a[i] / acum;
		double target = rand.nextDouble();
		while (target > temp)
			temp += a[++i] / acum;
		return node.children.get(GameInfo.actions[i]);
	}

	public void updateQValues(Node selected) {
		ArrayList<Position> prevStateFeatures = extractFeatures(selected.prev.state);
		ArrayList<Position> newStateFeatures = extractFeatures(selected.state);
		storeTransition(prevStateFeatures, selected.action, selected.reward, newStateFeatures);
		Collections.shuffle(replayMemory);
		for (int i =0; i < ((MINI_BATCH_SIZE < replayMemory.size())? MINI_BATCH_SIZE:replayMemory.size()); i++)
			learnFromTransition(replayMemory.get(i));

	}

	private void storeTransition(ArrayList<Position> currentFeatures,
			Types.ACTIONS selectedAction, double value, ArrayList<Position> newStateFeatures) {
		if (replayMemory.size() >= EXPERIENCE_REPLAY_MEMORY_SIZE)
			replayMemory.remove(0);
		if (Math.abs(value) > 1e3)
			value /= 1e3;
		else
			value /= 10;
		Transition t = new Transition(currentFeatures, selectedAction, value, newStateFeatures);
		replayMemory.add(t);
		//System.out.println("replayMemory.size: " + replayMemory.size());
	}

	private void learnFromTransition(Transition t){
		Position new_p;
		Types.ACTIONS new_action = GameInfo.actions[getActionWithHighestQ(t.newFeatures)];
		for (Position p : t.features){
			new_p = null;
			for (Position np: t.newFeatures)
				if (np.type == p.type)
					new_p = np;
				double deltaQ = t.reward
								+ ((new_p == null)? 0:Q_head.get(new_p).get(new_action))
								- Q_values.get(p).get(t.aciton);
				Q_values.get(p).put(t.aciton,
					 Q_values.get(p).get(t.aciton) + LEARNING_RATE * deltaQ);
				Integer updateCount = Q_values_update_count.get(p) + 1;
				Q_values_update_count.put(p, updateCount);
		}
	}

	int getActionWithHighestQ (ArrayList<Position> features){
		double[] a = getBias(features);
		double max = -99999; int max_index = 0;
		for (int i = 0; i< a.length; i++){
			//if (rollout_count % 100 == 0)
			//System.out.print( ICELab.Agent.actions[i].name() + ":" + a[i] + " ");
			if (a[i] > max){
				max = a[i]; max_index = i;
			}
		}
		//if (rollout_count % 100 == 0) System.out.println();
		return max_index;
	}

	private double[] getBias (ArrayList<Position> currentFeatures){
		double[] a = new double [GameInfo.NUM_ACTIONS];

		for (int k = 0; k < currentFeatures.size(); k++){
			HashMap<Types.ACTIONS, Double> weights = Q_values.get(currentFeatures.get(k));
			for (int l = 0; l < GameInfo.actions.length; l++)
				a[l] += weights.get(GameInfo.actions[l]);
		}

		return a;
	}

	ArrayList<Position> extractFeatures(StateObservation so){
		/*ArrayList<Position> features = new ArrayList<Position>();

		ArrayList<ArrayList<Observation>[]> sprites = new ArrayList<ArrayList<Observation>[]>();
		Vector2d avatarPosition = so.getAvatarPosition();
		int avatar_x = (int) avatarPosition.x/GameInfo.blocksize;
		int avatar_y = (int) avatarPosition.y/GameInfo.blocksize;

		sprites.add(so.getResourcesPositions(avatarPosition));
		sprites.add(so.getNPCPositions(avatarPosition));
		sprites.add(so.getImmovablePositions(avatarPosition));
		sprites.add(so.getMovablePositions(avatarPosition));
		sprites.add(so.getPortalsPositions(avatarPosition));


		int x, y = 0;
		for (ArrayList<Observation>[] type : sprites){
			if (type != null)
				for (int i = 0; i < type.length; i++){
					if (type[i].size() > 0){

						int itemType = type[i].get(0).itype;

						x = (int) type[i].get(0).position.x/so.getBlockSize() - avatar_x;
						y = (int) type[i].get(0).position.y/so.getBlockSize() - avatar_y;

						if (x > 1) x = 2; else if (x < -1) x = -2;
							else if (x == 1) x = 1; else if (x == -1) x = -1;
						if (y > 1) y = 2; else if (y < -1) y = -2;
							else if (y == 1) y = 1; else if (y == -1) y = -1;

						features.add(new Position(x, y, itemType));
					}
				}
		}
		*/
		ArrayList<Position> features = new ArrayList<Position>();
        ArrayList<Observation>[][] observationGrid = so.getObservationGrid();

		ArrayList<Observation> sprites = new ArrayList<Observation>();
		Vector2d avatarPosition = so.getAvatarPosition();
		int avatar_x = (int) avatarPosition.x/GameInfo.blocksize;
		int avatar_y = (int) avatarPosition.y/GameInfo.blocksize;

		for (int y = avatar_y - 1; y <= avatar_y + 1; y++)
			for (int x = avatar_x - 1; x <= avatar_x + 1; x++){
				if (x < 0 || x >= GameInfo.width || y < 0 || y >= GameInfo.height)
					continue;
				for (Observation o : observationGrid[x][y])
					sprites.add(o);
			}
		int x, y = 0;
		for (Observation o: sprites){
			int itemType = o.itype;

			x = (int) o.position.x/so.getBlockSize() - avatar_x;
			y = (int) o.position.y/so.getBlockSize() - avatar_y;

			features.add(new Position(x, y, itemType));
		}

		boolean newFeatureFound = false;
		for (Position p : features){
			if(!Q_values.containsKey(p)){
				addNewFeature(p);
				newFeatureFound = true;
			}
		}


		if (printQValuesToCSV && newFeatureFound)
			printQValuesToCSV();
		return features;
	}

	private void addNewFeature(Position position) {
		HashMap<Types.ACTIONS, Double> weights = new HashMap<Types.ACTIONS, Double> ();
		HashMap<Types.ACTIONS, Double> weightsHead = new HashMap<Types.ACTIONS, Double> ();
		for (int i = 0; i < GameInfo.NUM_ACTIONS; i++){
			double v = rand.nextDouble() * 0.1;
			weights.put(GameInfo.actions[i], v);
			weightsHead.put(GameInfo.actions[i], v);
		}
		Q_values.put(position, weights);
		Q_head.put(position, weightsHead);
		Q_values_update_count.put(position, 0);
		numFeatures++;
		if (printNewState)
			System.out.println("Feature#" + numFeatures + " " + position.type + ":(" + position.x + "," + position.y + ")");
	}

	private HashMap<Position, HashMap<Types.ACTIONS, Double>> copyValues(HashMap<Position, HashMap<Types.ACTIONS, Double>> a){
		HashMap<Position, HashMap<Types.ACTIONS, Double>> b = new HashMap<Position, HashMap<Types.ACTIONS, Double>>();
		Set<Position> keys = a.keySet();
		for(Position p : keys){
			HashMap<Types.ACTIONS, Double> Q_a = a.get(p);
			HashMap<Types.ACTIONS, Double> weights = new HashMap<Types.ACTIONS, Double>();
			for (Types.ACTIONS action : Q_a.keySet())
				weights.put(action, new Double(Q_a.get(action).doubleValue()));
			b.put(new Position(p), weights);
		}
		return b;
	}

	public void printQValuesToCSV(){
		if (!printQValuesToCSV)
			return;
        	String csvFileName = directory + reportDate + ".csv";
		try {
			File csvFile = new File(csvFileName);
			if(!csvFile.exists()){
				csvWriter = new FileWriter(csvFileName, true);
				csvWriter.append("type"); csvWriter.append(',');
				csvWriter.append("x"); csvWriter.append(',');
				csvWriter.append("y"); csvWriter.append(',');
				csvWriter.append("#iterations"); csvWriter.append(',');
				for(int i = 0; i < GameInfo.NUM_ACTIONS; i++){
					csvWriter.append(GameInfo.actions[i].toString());
					csvWriter.append(',');
				}
				csvWriter.append("updateCount");
				csvWriter.append("\n");
				csvWriter.flush(); csvWriter.close();
			}
			for(Position p : Q_values.keySet()){
				csvWriter = new FileWriter(csvFileName, true);
				csvWriter.append(String.valueOf(p.type)); csvWriter.append(',');
				csvWriter.append(String.valueOf(p.x)); csvWriter.append(',');
				csvWriter.append(String.valueOf(p.y)); csvWriter.append(',');
				csvWriter.append(String.valueOf(rollout_count)); csvWriter.append(',');
				for(Types.ACTIONS a : GameInfo.actions){
					csvWriter.append(String.valueOf(Q_values.get(p).get(a)));
					csvWriter.append(',');
				}
				csvWriter.append(String.valueOf(Q_values_update_count.get(p)));
				csvWriter.append("\n");

				csvWriter.flush(); csvWriter.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




}
