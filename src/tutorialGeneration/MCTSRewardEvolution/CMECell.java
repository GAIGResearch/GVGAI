package tutorialGeneration.MCTSRewardEvolution;

//not in use (for now)

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class CMECell {
    private int[] _dimensions;
    private ArrayList<Chromosome> _pop;
    private Chromosome _elite;
    private int _size;
    private Random _rnd;
    
    public CMECell(int[] dimensions, int size, Random rnd) {
	this._dimensions = dimensions;
	this._size = size;
	this._pop = new ArrayList<Chromosome>();
	this._elite = null;
	this._rnd = rnd;
    }
    
    public int[] getDimensions(){
	return this._dimensions;
    }
    
    public Chromosome getElite() {
	return this._elite;
    }
    
    public Chromosome[] getInfeasible(boolean descending) {
	Collections.sort(this._pop);
	if(descending) {
	    Collections.reverse(this._pop);
	}
	return this._pop.toArray(new Chromosome[0]);
    }
    
    private Chromosome rankSelection(Chromosome[] pop) {
	double[] ranks = new double[pop.length];
	ranks[0] = 1;
	for(int i=1; i<pop.length; i++) {
	    ranks[i] = ranks[i-1] + i + 1;
	}
	for(int i=0; i<pop.length; i++) {
	    ranks[i] /= ranks[ranks.length - 1];
	}
	double randValue = this._rnd.nextDouble();
	for(int i=0; i<ranks.length; i++){
	    if(randValue <= ranks[i]) {
		return pop[i];
	    }
	}
	return pop[pop.length - 1];
    }
    
    public Chromosome getChromosome(double eliteProb) {
	Chromosome elite = this.getElite();
	Chromosome[] infeasible = this.getInfeasible(false);
	if(infeasible.length == 0 || (elite != null && this._rnd.nextDouble() < eliteProb)) {
	    return elite;
	}
	return this.rankSelection(infeasible);
    }
    
    public void setChromosome(Chromosome c) {
	if(c.getConstraints() == 1) {
		//if no elite set, or the chromosome c beats the current elite
	    if(this._elite == null || c.getFitness() > this._elite.getFitness()) {
		this._elite = c;
	    }
	    return;
	}
	//remove the first bad chromosome
	if(this._pop.size() >= this._size) {
	    Chromosome[] chromosomes = this.getInfeasible(false);
	    this._pop.remove(chromosomes[0]);
	}

	//add the new chromosome
	this._pop.add(c);
    }
}
