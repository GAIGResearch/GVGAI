package tracks.singlePlayer.advanced.sampleRS;
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
