package agents.Return42.heuristics.features.controller;

import agents.Return42.GameStateCache;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.List;

public class PortalController extends FeatureController {

    @Override
    public boolean isUseful(GameStateCache state) {
        return state.getPortalsPositions() != null;
    }

    @Override
    public void check(GameStateCache state) {
        List<Observation>[] portals = state.getPortalsPositions();
        if (portals == null) {
        	active = false;
        	return;
        }
        
        for(List<Observation> p : portals) {
            if(p.size() > 0) {
                Vector2d pp = new Vector2d(p.get(0).position);
                Vector2d dir = pp.subtract(state.getAvatarPosition()).mul(1.0 / state.getBlockSize());
                if(dir.mag() == 1) {
                    StateObservation next = state.getState();
                    if(dir.x > 0) {
                        next.advance(Types.ACTIONS.ACTION_RIGHT);
                        active = !next.getAvatarPosition().equals(state.getAvatarPosition());
                    } else if(dir.x < 0) {
                        next.advance(Types.ACTIONS.ACTION_LEFT);
                        active = !next.getAvatarPosition().equals(state.getAvatarPosition());
                    } else {
                        if(dir.y > 0) {
                            next.advance(Types.ACTIONS.ACTION_DOWN);
                            active = !next.getAvatarPosition().equals(state.getAvatarPosition());
                        } else if(dir.y < 0) {
                            next.advance(Types.ACTIONS.ACTION_UP);
                            active = !next.getAvatarPosition().equals(state.getAvatarPosition());
                        }
                    }
                }
            }
        }
    }
}
