package agents.Return42.heuristics.features;

import agents.Return42.GameStateCache;
import core.game.Observation;

import java.util.List;

public class KillNpc extends Feature {

    private final double npcCounterFactor;

    public KillNpc(double npcCounterFactor) {
        this.npcCounterFactor = npcCounterFactor;
    }

    @Override
    public boolean isUseful(GameStateCache state) {
        return state.getNPCPositions() != null;
    }

    @Override
    public double evaluate(GameStateCache state) {
        List<Observation>[] npcPositions = state.getNPCPositions();
        if(npcPositions != null) {
            int npcCounter = 0;

            for(List<Observation> npcs : npcPositions) {
                npcCounter += npcs.size();
            }
            return -npcCounter * getWeight();
        }
        return 0;
    }

    @Override
    public double getWeight() {
        return npcCounterFactor * weight;
    }
}
