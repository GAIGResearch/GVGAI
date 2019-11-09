package agents.muzzle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

public class Agent extends AbstractPlayer {

    private boolean elitist;

    private Random randomGen;

    private ArrayList<Double> fitnessList;
    private ArrayList<LinkedList<Types.ACTIONS>> plans;

    private int numberOfActions;

    private Types.ACTIONS[] actionMapping;
    private HashMap<Types.ACTIONS, Integer> reverseActionMapping;

    private static int POPULATION_SIZE = 8;
    private static double ELITE_FRACTION = 0.4;
    private static double RANDOM_FRACTION = 0.05;
    private static int PLAN_LENGTH = 15;
    private static double MUTATION_RATE = 0.05;

    private static double GAMMA = 0.1;

    public Agent(StateObservation observation, ElapsedCpuTimer elapsedTimer){
        randomGen = new Random();
        fitnessList = new ArrayList<>();

        this.elitist = true;
        this.numberOfActions = observation.getAvailableActions().size();

        actionMapping = new Types.ACTIONS[numberOfActions];
        reverseActionMapping = new HashMap<Types.ACTIONS, Integer>();

        int k = 0;
        for (Types.ACTIONS action : observation.getAvailableActions()) {
            actionMapping[k] = action;
            reverseActionMapping.put(action, k);
            k++;
        }

        plans = new ArrayList<>(POPULATION_SIZE);
        for(int i = 0; i < (int)Math.floor(POPULATION_SIZE); i++) {
            plans.add(samplePlan(PLAN_LENGTH));
        }
        simulatePlans(observation);

    }

    /** Mutate a plan, changing a position to a random action if with probability MUTATION_RATE*/
    public LinkedList<Types.ACTIONS> mutate(LinkedList<Types.ACTIONS> plan){
        for(int i = 0; i < plan.size(); i++){
            if(MUTATION_RATE > 0 && randomGen.nextDouble() < MUTATION_RATE){
                plan.set(i,actionMapping[randomGen.nextInt(actionMapping.length)]);
            }
        }

        return plan;
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        advancePlans();
        simulatePlans(stateObs);

        int n = plans.size();
        while(elapsedTimer.remainingTimeMillis() > 10) {
            ArrayList<LinkedList<Types.ACTIONS>> newPlans = new ArrayList<>();
            ArrayList<Double> newFitness = new ArrayList<>();

            while(plans.size() < 2 * n) {
                if(elapsedTimer.remainingTimeMillis() < 5) {
                    break;
                }

                for (int i = 0; i < plans.size() / 2; i++) {
                    if(elapsedTimer.remainingTimeMillis() < 8) {
                        break;
                    }
                    int x = 0;
                    int a = selectProportionate();
                    int b = a;
                    while (b == a) {
                        x++;

                        if (x < 10) {
                            b = selectProportionate();
                        } else {
                            b = randomGen.nextInt(plans.size());
                            if (a == 0) {
                                b = 1;
                            } else {
                                b = a - 1;
                            }
                        }
                    }

                    LinkedList<Types.ACTIONS> newPlan = crossover(plans.get(a), plans.get(b));
                    if(randomGen.nextDouble() < RANDOM_FRACTION) {
                        newPlan = samplePlan(PLAN_LENGTH);
                    } else {
                        mutate(newPlan);
                    }
                    double planFitness = simulatePlan(stateObs, newPlan);

                    newFitness.add(planFitness);
                    newPlans.add(newPlan);
                }
            }

            plans.addAll(newPlans);
            fitnessList.addAll(newFitness);

            while(plans.size() > n) {
                int minFitnessIndex = 0;
                double minFitness = Double.POSITIVE_INFINITY;
                for(int i = 0; i < fitnessList.size(); i ++){
                    if(fitnessList.get(i) < minFitness){
                        minFitness = fitnessList.get(i);
                        minFitnessIndex = i;
                    }
                }
                fitnessList.remove(minFitnessIndex);
                plans.remove(minFitnessIndex);
            }

        }

        int maxFitnessIndex = 0;
        double maxFitness = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < fitnessList.size(); i ++){
            if(fitnessList.get(i) > maxFitness){
                maxFitness = fitnessList.get(i);
                maxFitnessIndex = i;
            }
        }

        return plans.get(maxFitnessIndex).get(0);
    }

    private void simulatePlans(StateObservation observation){
        fitnessList = new ArrayList<>();

        for (LinkedList<Types.ACTIONS> plan : plans) {
            fitnessList.add(simulatePlan(observation, plan));
        }
    }

    private int selectProportionate(){
        double totalFitness = 0;
        for(Double d: fitnessList){
            totalFitness += d;
        }

        double r = randomGen.nextDouble() * totalFitness;

        double sumFitness = 0;
        int i = 0;
        for(double f : fitnessList) {
            sumFitness += f;

            if(r <= sumFitness) {
                break;
            }

            i++;
        }

        return i;
    }

    /** Single point crossover */
    private LinkedList<Types.ACTIONS> crossover(LinkedList<Types.ACTIONS> planA, LinkedList<Types.ACTIONS> planB) {
        int point = 1 + randomGen.nextInt(planA.size()-2);

        LinkedList<Types.ACTIONS> result = new LinkedList<>(planA.subList(0, point));
        result.addAll(planB.subList(point,planB.size()));

        return result;
    }

    /** Cut the first action of each plan and extend each plan by a random one */
    private void advancePlans(){
        for (LinkedList<Types.ACTIONS> plan : plans){
            plan.remove(0);
            plan.add(sampleAction());
        }
    }

    /**
     * Simulate a plan and return the fitness function of it's objective function
     * */
    private double simulatePlan(StateObservation observation, LinkedList<Types.ACTIONS> plan){
        StateObservation obs = observation.copy();

        double reward = 0.0;

        int i = 0;
        HistoryAwareHeuristic objectiveFunction = new HistoryAwareHeuristic(obs);
        for(Types.ACTIONS action : plan){
            obs.advance(action);

            reward += objectiveFunction.evaluateState(obs) * Math.pow(GAMMA,i);
        }

        //System.out.println(reward);
        return fitnessFunction(reward/PLAN_LENGTH);
    }

    /**
     * Fitness function, normalizes the result of the objective function to a value
     * between 0 and 1.
     *
     * Currently uses the standard sigmoid function.
     * */
    private double fitnessFunction(double objectiveResult) {

//        System.out.println(((objectiveResult/(1+2*Math.abs(objectiveResult)))+1)/2);
//        System.out.println(1/(1+Math.exp(-1*objectiveResult)));
//        System.out.println("=----===-");
        //System.out.println(objectiveResult);
        return 1/(1+Math.exp(-1*(objectiveResult)));
    }

    private Types.ACTIONS sampleAction() {
        return actionMapping[randomGen.nextInt(actionMapping.length)];
    }

    private LinkedList<Types.ACTIONS> samplePlan(int length){
        LinkedList<Types.ACTIONS> result = new LinkedList<>();

        for(int i = 0; i < length; i++){
            result.add(sampleAction());
        }

        return result;
    }
}
