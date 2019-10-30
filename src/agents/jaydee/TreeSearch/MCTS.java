package agents.jaydee.TreeSearch;

import core.game.StateObservation;
import agents.jaydee.Agent;
import agents.jaydee.Node;

public class MCTS extends TreeSearch {
    public static final int MAX_SELECTION_DEPTH = 3;
    public static final double UCB_EXPLORATION = 2.0;

    public MCTS(Node origin) {
        super(origin);
    }

    @Override
    public void search() {
        StateObservation obs = origin.state;
        while (!Agent.anyTime.isTimeOver()) {
            Agent.anyTime.updatePerLoop();


            // select from tree starting from origin until MAX_SELECTION_DEPTH is reached
            Node selected = origin;
            selected.state = obs.copy();
            while (selected.depth < MAX_SELECTION_DEPTH) {
                if (selected.state.isGameOver()) {
                    //System.out.println("GAMEOVER BREAK");
                    break;
                }

                // if first time visit expand
                if (selected.children.isEmpty()) { 
                    selected.expand();
                    //System.out.println("EXPAND BREAK");
                    break;
                    // performance todo: wenn wir nach expand direkt weiter selecten kommen wir tiefer,
                    // dafuer kann MCTS nicht so gut entscheiden ob es sinn macht bis dahin zu kommen
                    // break einfach entfernen, dann selected er von hier aus weiter (innerhalb der kinder)
                    // evt. gibt es dann probleme mit stateObs.copy
                } else {
                    // select child
                    selected = select(selected);
                    // update current with actual stateObs
                    selected.update();
                    selected.updateAverageReward();
                }

            }

        }
    }

    @Override
    public void roll(Node origin) {
        this.origin = origin;

        // Agent performs roll on tree -> nothing to do here
    }

    protected Node select(Node node) {
        Double bestValue = Double.NEGATIVE_INFINITY;
        Node best = null;
        for (Node child : node.children.values()) {
            // todo: normalization
            double value = child.bestReward + UCB_EXPLORATION * Math.sqrt(Math.log(node.nVisits) / child.nVisits);

            if (value > bestValue) {
                bestValue = value;
                best = child;
            }
        }
        return best;
    }
}
