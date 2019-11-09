package agents.Return42.heuristics.features.controller;

import agents.Return42.GameStateCache;

public abstract class FeatureController {
    protected boolean active = true;

    public boolean isActive() {
        return active;
    }

    public abstract boolean isUseful(GameStateCache state);

    public abstract void check(GameStateCache state);

    public void reactivate() {
        active = true;
    }
}
