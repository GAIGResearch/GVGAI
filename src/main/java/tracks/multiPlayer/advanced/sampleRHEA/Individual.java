package tracks.multiPlayer.advanced.sampleRHEA;

import java.util.Random;

public class Individual implements Comparable{

    protected int[] actions; // actions in individual. length of individual = actions.length
    private int nLegalActions; // number of legal actions
    protected double value;
    private Random gen;

    Individual(int L, int nLegalActions, Random gen) {
        actions = new int[L];
        for (int i = 0; i < L; i++) {
            actions[i] = gen.nextInt(nLegalActions);
        }
        this.nLegalActions = nLegalActions;
        this.gen = gen;
    }

    public void setActions (int[] a) {
        System.arraycopy(a, 0, actions, 0, a.length);
    }

    /**
     * Returns new individual
     * @param MUT - number of genes to mutate
     * @return - new individual, mutated from this
     */
    Individual mutate(int MUT) {
        Individual b = this.copy();
        b.setActions(actions);

        int count = 0;
        if (nLegalActions > 1) { // make sure you can actually mutate
            while (count < MUT) {
                int a; // index of action to mutate

                // random mutation of one action
                a = gen.nextInt(b.actions.length);

                int s;
                s = gen.nextInt(nLegalActions); // find new action
                b.actions[a] = s;

                count++;
            }
        }

        return b;
    }

    /**
     * Modifies individual
     * @param CROSSOVER_TYPE - type of crossover
     */
    public void crossover (Individual parent1, Individual parent2, int CROSSOVER_TYPE) {
        if (CROSSOVER_TYPE == Agent.POINT1_CROSS) {
            // 1-point
            int p = gen.nextInt(actions.length - 3) + 1;
            for ( int i = 0; i < actions.length; i++) {
                if (i < p)
                    actions[i] = parent1.actions[i];
                else
                    actions[i] = parent2.actions[i];
            }

        } else if (CROSSOVER_TYPE == Agent.UNIFORM_CROSS) {
            // uniform
            for (int i = 0; i < actions.length; i++) {
                if (gen.nextFloat() >= 0.5)
                    actions[i] = parent1.actions[i];
                else
                    actions[i] = parent2.actions[i];
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        Individual a = this;
        Individual b = (Individual)o;
        return Double.compare(b.value, a.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Individual)) return false;

        Individual a = this;
        Individual b = (Individual)o;

        for (int i = 0; i < actions.length; i++) {
            if (a.actions[i] != b.actions[i]) return false;
        }

        return true;
    }

    public Individual copy () {
        Individual a = new Individual(this.actions.length, this.nLegalActions, this.gen);
        a.value = this.value;
        a.setActions(this.actions);

        return a;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("" + value + ": ");
        for (int action : actions) s.append(action).append(" ");
        return s.toString();
    }
}
