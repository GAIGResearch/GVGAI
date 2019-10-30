 package agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch;

import agents.ICELab.OpenLoopRLBiasMCTS.Node;

public abstract class TreeSearch {
    public Node origin = null;
    public TreeSearch(Node origin) {
        this.origin = origin;
    }
    public abstract void search();
    public abstract void roll(Node origin);
}
