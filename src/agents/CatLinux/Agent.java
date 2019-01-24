package agents.CatLinux;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

/**
 * Created with IntelliJ IDEA.
 * User: Jialin Liu
 * Date: 23/05/15
 * Time: 10:17
 *
 * GA with bandit
 */
public class Agent extends AbstractPlayer {
    public static ArrayList<Types.ACTIONS> legal_actions;
    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    private int NUM_ACTIONS;
    private int ELITISM = 2;
    private int TOURNAMENT_SIZE = 3;
    private int SIMULATION_DEPTH = 10;
    private int POPULATION_SIZE = 10;
    private double MUT_PROBA = 0.1;
    private int buffer_init_size = 4;

    private GAIndividual population[];
    protected Random rdm_generator;

    private int bestActionIdx;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        rdm_generator = new Random();
        legal_actions = stateObs.getAvailableActions();
        NUM_ACTIONS = legal_actions.size();
        bestActionIdx = 0;
        initPopulation();
        evaluatePopulation(stateObs);
        sortPopulationByFitness();
    }

    private void initPopulation() {
        population = new GAIndividual[POPULATION_SIZE];
        // Randomize initial population
        for (int i = 0; i < population.length; i++) {
            population[i] = new GAIndividual(SIMULATION_DEPTH, NUM_ACTIONS, rdm_generator);
        }
    }

    private void updatePopulation() {
        for (int i = 0; i < population.length; i++) {
            int j;
            for (j=0;j < population[i].genome.length-1; j++) {
                population[i].genome[j] = population[i].genome[j+1];
            }
            population[i].genome[j] = rdm_generator.nextInt(NUM_ACTIONS);
        }
    }

    private void updatePartPopulation() {
        int i;
        for (i=0; i<buffer_init_size; i++) {
            int j;
            for (j=0; j<population[i].genome.length-1; j++) {
                population[i].genome[j] = population[i].genome[j+1];
            }
            population[i].genome[j] = rdm_generator.nextInt(NUM_ACTIONS);
        }
        for(i=buffer_init_size;i< population.length; i++) {
            for (int j=0; j<population[i].genome.length; j++) {
                population[i].genome[j] = rdm_generator.nextInt(NUM_ACTIONS);
            }
        }
    }

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     *
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        legal_actions = stateObs.getAvailableActions();
        NUM_ACTIONS = legal_actions.size();
        getAction(stateObs, elapsedTimer);
        return legal_actions.get(bestActionIdx);
    }

    public void getAction(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        int numIters = 0;
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit) {
            // generate offspring
            if(numIters==0) {
                //updatePopulation();
                updatePartPopulation();
            } else {
                GAIndividual[] nextPop = new GAIndividual[population.length];
                int i;
                for(i = 0; i < ELITISM; ++i)
                {
                    nextPop[i] = population[i];
                }
                for(;i<population.length;++i)
                {
                    nextPop[i] = breed();
                    mutate(nextPop[i]);
                }
                this.population = nextPop;
            }
            // evaluate the genomes
            evaluatePopulation(stateObs);
            sortPopulationByFitness();
            numIters++;
            this.bestActionIdx = this.population[0].genome[0];
            int legalActionsSize = stateObs.getAvailableActions().size();
            if(this.bestActionIdx >= legalActionsSize){
                this.bestActionIdx = rdm_generator.nextInt(legalActionsSize);
            }
            acumTimeTaken += (elapsedTimer.elapsedMillis());
            avgTimeTaken = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }
    }

    public GAIndividual breed() {
        GAIndividual gai1 = getParent(null);        //First parent.
        GAIndividual gai2 = getParent(gai1);        //Second parent.
        return uniformCross(gai1, gai2);
    }

    public GAIndividual uniformCross(GAIndividual parentA, GAIndividual parentB) {

        int[] newInd = new int[parentA.genome.length];

        for(int i = 0; i < parentA.genome.length; ++i)
        {
            if(rdm_generator.nextFloat() < 0.5f)
            {
                newInd[i] = parentA.genome[i];
            }else{
                newInd[i] = parentB.genome[i];
            }
        }
        return new GAIndividual(newInd);
    }

    public GAIndividual getParent(GAIndividual first)
    {
        GAIndividual best = null;
        int[] tour= new int[TOURNAMENT_SIZE];
        for(int i = 0; i < TOURNAMENT_SIZE; ++i)
            tour[i] = -1;

        int i = 0;
        while(tour[TOURNAMENT_SIZE-1] == -1)
        {
            int part = (int) (rdm_generator.nextFloat()*population.length);
            boolean valid = population[part] != first;  //Check it is not the same selected first.
            for(int k = 0; valid && k < i; ++k)
            {
                valid = (part != tour[k]);                 //Check it is not in the tournament already.
            }

            if(valid)
            {
                tour[i++] = part;
                if(best == null || (population[part].getFitness() > best.getFitness()))
                    best = population[part];
            }
        }
        return best;
    }

    public void mutate(GAIndividual individual) {

        for (int i = 0; i < individual.genome.length; i++) {
            if(rdm_generator.nextDouble() < MUT_PROBA)
            {
                double p = rdm_generator.nextDouble();
                individual.genome[i] = rdm_generator.nextInt(NUM_ACTIONS);
            }
        }
    }

    public void evaluatePopulation(StateObservation stateObs) {
        for (int i = 0; i < population.length; i++) {
            StateObservation stateCopy = stateObs.copy();
            GAIndividual current_individual = population[i];
            current_individual.evaluate(stateCopy, rdm_generator);
        }
    }

    private void sortPopulationByFitness() {
        for (int i = 0; i < population.length; i++) {
            for (int j = i + 1; j < population.length; j++) {
                if (population[i].getFitness() < population[j].getFitness()) {
                    GAIndividual gcache = population[i];
                    population[i] = population[j];
                    population[j] = gcache;
                }
            }
        }
    }

    public static double evaluateState(StateObservation stateObs) {
        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        double rawScore = stateObs.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        return rawScore;
    }

    @Override
    public void draw(Graphics2D g)
    {
        //g.drawString("Num Simulations: " + numSimulations, 10, 20);
    }
}
