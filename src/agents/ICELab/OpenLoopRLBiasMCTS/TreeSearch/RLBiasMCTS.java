package agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch;

import java.util.HashMap;

import core.game.StateObservation;
import ontology.Types;
import agents.ICELab.Agent;
import agents.ICELab.GameInfo;
import agents.ICELab.OpenLoopRLBiasMCTS.Node;
import agents.ICELab.OpenLoopRLBiasMCTS.Utils;

public class RLBiasMCTS extends TreeSearch {
    public static final int MAX_SELECTION_DEPTH = 20;
    public static final double UCB_EXPLORATION = 0.1;
    HashMap<Types.ACTIONS, Integer> count;
    RLBiasedActionSelector biasedSelector;

    public RLBiasMCTS(Node origin) {
        super(origin);
        biasedSelector = new RLBiasedActionSelector();
    }

    @Override
    public void search() {
        StateObservation obs = origin.state;
        count = new HashMap<Types.ACTIONS, Integer>();
        for (Types.ACTIONS action : GameInfo.actions)
        		count.put(action, 0);
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
                    if (selected.depth == 1)
                    		count.put(selected.action, count.get(selected.action) + 1);
                    // update current with actual stateObs
                    selected.update();
                    selected.updateAverageReward();
                    biasedSelector.updateQValues(selected);
                }

            }
        }
    }

    @Override
    public void roll(Node origin) {
        this.origin = origin;
        for (Types.ACTIONS action : count.keySet())
        		Utils.logger.info ("root->" + action.name() + " rolled " + count.get(action) + " times");
        // Agent performs roll on tree -> nothing to do here
    }

    protected Node select(Node node) {
    		if (node.depth == 0){
    			for (Types.ACTIONS action : GameInfo.actions)
    				if (count.get(action) == 0)
    					return node.children.get(action);
    		}
    		return biasedSelector.selectAction(node);
    }
}