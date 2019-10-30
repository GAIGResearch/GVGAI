package agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch;

import core.game.StateObservation;
import agents.ICELab.Agent;
import agents.ICELab.OpenLoopRLBiasMCTS.Node;

public class MCTS extends TreeSearch {
    public static final int MAX_SELECTION_DEPTH = 20;
    public static final double UCB_EXPLORATION = 0.1;

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
