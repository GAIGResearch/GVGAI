package agents.AtheneAI.mcts;

import core.game.StateObservation;

public class ActionChain {

	private double propabilities[];
	private int actionChain[];
	private double temp;
	private double score;
	
	public ActionChain(int numActions, int[] actionChain, double score, double temp){
		propabilities = new double[numActions];
		this.actionChain = actionChain;
		this.score = score;
		this.temp = temp;
		setNewPropabilities(actionChain, score);
	}
	
	public void setNewPropabilities(int newBestChain[], double score){
		actionChain = newBestChain;
		this.score = score;
		// calculate softMax probabilities
		double gradient = 0;
		int[] absoluteOccurence = new int[propabilities.length];
		for (int i = 0; i < actionChain.length; ++i){
			++absoluteOccurence[actionChain[i]];
		}
		for (int i = 0; i < absoluteOccurence.length; ++i){
			gradient += Math.exp(absoluteOccurence[i] / temp);
		}
		for (int i = 0; i < absoluteOccurence.length; ++i){
			propabilities[i] = Math.exp(absoluteOccurence[i] / temp) / gradient;
		}
	}
	
	public double[] getPropabilities(){
		return propabilities;
	}
	
	public int[] getActionChain(){
		return actionChain;
	}
	
	public double getTemperature(){
		return temp;
	}
	
	public void setTemperature(double t){
		temp = t;
		return;
	}
	
	public double getScore(){
		return this.score;
	}
	
}
