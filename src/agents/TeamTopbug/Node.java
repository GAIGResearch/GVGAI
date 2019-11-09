package agents.TeamTopbug;

import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.awt.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

public class Node {
    // open loop
    public Types.ACTIONS                action        = null;
    public double                       averageReward = 0;
    public int                          nVisits       = 0;
    public int                          depth         = 0;
    public Node                         prev          = null;
    public HashMap<Types.ACTIONS, Node> children      = new HashMap<Types.ACTIONS, Node>();

    public Vector2d avatarPos = null;

    public double bestRewardWeight = 0.5;
    public double bestReward       = Double.NEGATIVE_INFINITY;

    // temporary
    public StateObservation state      = null;
    public Pheromones       pheromones = null;

    // mcts


    public boolean isDestroyable = true;

    // node at gamestart
    public Node() {
        pheromones = new Pheromones();
    }

    // node expand
    public Node(Types.ACTIONS action, Node prev) {
        init(action, prev);
    }

    public Node init(Types.ACTIONS action, Node prev) {
        this.averageReward = 0;
        this.nVisits = 0;
        this.prev = null;
        this.children = new HashMap<Types.ACTIONS, Node>();

        this.bestRewardWeight = 0.5;
        this.bestReward = Double.NEGATIVE_INFINITY;


        this.state = null;
        this.pheromones = null;

        this.isDestroyable = true;

        this.action = action;
        this.prev = prev;

        update();
        updateAverageReward();
        updateBestReward();

        return this;
    }

    public void update() {
        depth = prev.depth + 1;
        //state = prev.state.copy();
        state = prev.state;
        state.advance(action);
        if(state.isGameOver() && (prev != null)) {
            avatarPos = prev.avatarPos;
        } else {
            avatarPos = state.getAvatarPosition().copy();
        }
        pheromones = new Pheromones(this, prev.pheromones);
    }

    /**
     * Marks this Node as visited
     */
    public void updateAverageReward() {

        double reward = Heuristic.evaluate(this);
        if (this.nVisits == 0) { // this happens for recently expanded childs
            this.averageReward = reward;
            this.nVisits = 1;
        } else {
            this.nVisits++;
            this.averageReward = (this.averageReward * (this.nVisits - 1) + reward) / this.nVisits;
        }

        // update bestReward of parent and its ancestors (backpropagation)
        this.updateBestReward();
    }

    public void updateBestReward() {
        Node current = this;
        do {
            if (!current.children.isEmpty()) {
                // node has children

                // determine best child
                double bestChild = Double.NEGATIVE_INFINITY;
                for (Node child : current.children.values()) {
                    if (child.bestReward > bestChild) {
                        bestChild = child.bestReward;
                    }
                }

                // update bestReward
                current.bestReward = current.bestRewardWeight * bestChild + (1 - current.bestRewardWeight) * current.averageReward;
            } else {
                // no children, just use averageReward as bestReward
                current.bestReward = current.averageReward;
            }

            current = current.prev;
        } while (current != null && (current.depth >= 0));
    }

    public Node select() {
        Double bestValue = Double.NEGATIVE_INFINITY;
        Node best = null;
        for (Node child : children.values()) {
            if (child.bestReward > bestValue) {
                bestValue = child.bestReward;
                best = child;
            }
        }
        return best;
    }

    public void release() {
        prev = null;
        state = null;
        children = null;
        pheromones = null;
    }

    public void expand() {
        StateObservation obs = this.state;
        for (Types.ACTIONS action : GameInfo.actions) {
            // performance todo: mehrere backpropagations sind nicht notwendig,
            // lieber erst einmal alle kinder erzeugen und dann einmal backpropagation
            this.state = obs.copy();
            Node child = NodePool.get().init(action, this);
            children.put(action, child);
        }
    }


}
