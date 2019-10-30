package agents.Return42.heuristics.features;

import agents.Return42.GameStateCache;
import core.game.Observation;

import java.util.List;

public class FleeNpc extends Feature {

    private final double npcDistanceFactor;
    private final double npcCounterFactor;

    public FleeNpc(double npcDistanceFactor, double npcCounterFactor) {
        this.npcDistanceFactor = npcDistanceFactor;
        this.npcCounterFactor = npcCounterFactor;
    }

    @Override
    public boolean isUseful(GameStateCache state) {
        return state.getNPCPositions() != null && state.getAvailableActions().size() > 4;
    }

    @Override
    public double evaluate(GameStateCache state) {
        List<Observation>[] npcPositions = state.getNPCPositions();
        if(npcPositions != null) {
            double npcMinDistance = 0;
            int npcCounter = 0;

            for(List<Observation> npcs : npcPositions) {
                if(npcs.size() > 0) {
                    npcMinDistance += Math.sqrt(npcs.get(0).sqDist) / state.getBlockSize();
                    npcCounter += npcs.size();
                }
            }

            if(npcCounter > 0) {
                //flee before npc but try to kill
                return npcMinDistance * npcDistanceFactor - npcCounter * npcCounterFactor;
            }
        }
        return 0;
    }

    @Override
    public double getWeight() {
        return (npcDistanceFactor + npcCounterFactor) * weight;
    }
}
