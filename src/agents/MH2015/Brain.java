package agents.MH2015;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ontology.Types;
import tools.ElapsedCpuTimer;
import core.game.StateObservation;

public class Brain {
	public static int actionNumber;
    public static Types.ACTIONS[] actions;
    
    private static int pSize;
    private static int actionLength = 6;
    
    private Random rnd;
    private Heuristic heuristic;
    
    public class geneType implements Comparable<geneType> {
    	ArrayList<Types.ACTIONS> permutation;
    	double fitness;
    	double oneStepFitness;
    	
    	public geneType() {
			// TODO Auto-generated constructor stub
    		permutation = new ArrayList<Types.ACTIONS>();
    		fitness = 0.0;
    		oneStepFitness = 0.0;
		}

		@Override
		public int compareTo(geneType arg0) {
			// TODO Auto-generated method stub
			if (this.oneStepFitness < arg0.oneStepFitness)
			{
				return 1;
			}
			else if (this.oneStepFitness == arg0.oneStepFitness)
			{
				if (this.fitness < arg0.fitness)
				{
					return 1;
				}
				else 
				{
					return -1;
				}
			}
			else 
			{
				return -1;
			}
		}
    }

	public Brain(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		// TODO Auto-generated constructor stub
		rnd = new Random();
		heuristic = new Heuristic(so);
		//Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for(int i = 0; i < actions.length; ++i)
        {
            actions[i] = act.get(i);
        }
        actionNumber = actions.length;
        pSize = actionNumber*2;
	}
	
	public void update(StateObservation stateObs) {
		heuristic.updateMap(stateObs);
	}
	
	private void calculateOneStepFitness(StateObservation so, geneType g) {
		StateObservation copySO = so.copy();
		copySO.advance(g.permutation.get(0));
		g.oneStepFitness = heuristic.evaluateState(copySO);
		g.fitness = heuristic.evaluateState(copySO);
	}

	private void calculateFitness(StateObservation so, geneType g) {
		StateObservation copySO = so.copy();
		copySO.advance(g.permutation.get(0));
		g.oneStepFitness = heuristic.evaluateState(copySO);
		g.fitness = heuristic.evaluateState(copySO);

		for (int i = 1; i < g.permutation.size(); i++)
		{
			copySO.advance(g.permutation.get(i));
			if (copySO.isGameOver())
			{
				break;
			}
		}
		g.oneStepFitness = heuristic.evaluateState(copySO);
		g.fitness = heuristic.evaluateState(copySO);
	}
	
	public ArrayList<Types.ACTIONS> GA(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		long remaining = elapsedTimer.remainingTimeMillis();

		geneType [] genes = new geneType[pSize];
		for (int i = 0; i < pSize; i++)
		{
			geneType newGene = new geneType();
			for (int j = 0; j < actionLength; j++)
			{
				if (j == 0)
				{
					newGene.permutation.add(actions[i%actionNumber]);
				}
				else 
				{
					int idx = rnd.nextInt(actionNumber);
					newGene.permutation.add(actions[idx]);
				}
			}
			calculateOneStepFitness(so, newGene);
			genes[i] = newGene;
		}
		Arrays.sort(genes);

        int numIters = 0;
		long remainingLimit = 10;
        while(remaining > remainingLimit)
        {
            /** mating selection*/
            int parent1 = rnd.nextInt(pSize);
            int parent2 = rnd.nextInt(pSize);
            while (parent1 == parent2)
            {
            	parent2 = rnd.nextInt(pSize);
            }
            /** crossover*/
            geneType child1 = new geneType();
            geneType child2 = new geneType();
            int cutPoint = 1+rnd.nextInt(actionLength-1);
            for (int i = 0; i < actionLength; i++)
            {
            	if (i < cutPoint)
            	{
            		child1.permutation.add(genes[parent1].permutation.get(i));
            		child2.permutation.add(genes[parent2].permutation.get(i));
            	}
            	else 
            	{
            		child1.permutation.add(genes[parent1].permutation.get(i));
            		child2.permutation.add(genes[parent1].permutation.get(i));
				}
            }
            /** mutation*/
            int a = 1+rnd.nextInt(actionLength-1);
            int b = 1+rnd.nextInt(actionLength-1);
            child1.permutation.set(a, actions[rnd.nextInt(actionNumber)]);
            child2.permutation.set(b, actions[rnd.nextInt(actionNumber)]);
            /** environmental selection*/
            calculateFitness(so, child1);
            calculateFitness(so, child2);
            
            if (child1.fitness > child2.fitness)
            {
            	genes[pSize-1] = child1;
            }
            else 
            {
            	genes[pSize-1] = child2;
			}
            Arrays.sort(genes);
            numIters++;
            remaining = elapsedTimer.remainingTimeMillis();
        }
//        System.out.println(numIters);
//        for (int i = 0; i < pSize; i++)
//		{
//        	System.out.println(genes[i].oneStepFitness);
//			System.out.println(genes[i].fitness);
//		}
        
		return genes[0].permutation;
	}

}
