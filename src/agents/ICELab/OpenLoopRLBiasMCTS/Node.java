package agents.ICELab.OpenLoopRLBiasMCTS;

import java.util.ArrayList;
//import java.awt.*;
//import java.util.Comparator;
import java.util.HashMap;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import agents.ICELab.Agent;
import agents.ICELab.GameInfo;

public class Node {
    // open loop
    public Types.ACTIONS                action        = null;
    public double                       averageReward = 0;
    public int                          nVisits       = 0;
    public int                          depth         = 0;
    public Node                         prev          = null;
    public HashMap<Types.ACTIONS, Node> children      = new HashMap<Types.ACTIONS, Node>();
    public double						reward		= 0;

    public Vector2d avatarPos = null;

    public double bestRewardWeight = 0.5;
    public double bestReward       = Double.NEGATIVE_INFINITY;

    // temporary
    public StateObservation state      = null;
    public Pheromones       pheromones = null;

    // mcts


    public boolean isDestroyable = true;

    
    private HashMap<Integer, Integer> distances;
    private HashMap<Integer, Path> paths;
    // to record if the distances information is updated and reliable
    private boolean distancesIsValid = false;
    
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
        //this.prev = null;
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
        Utils.logger.fine(action.name() + " depth: " + depth );
        state = prev.state.copy();
        //state = prev.state;
        state.advance(action);
        Agent.memory.addInformation(prev.state, state, action);
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
        this.reward = reward;
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
        		Utils.logger.info(child.action.name() + "'s bestReward: " + child.bestReward
        				+ " depth: " + child.depth + " prev.depth: " + ((prev== null)? -1:prev.depth) + " nVisits: " + child.nVisits);
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
            this.state = obs.copy();
            Node child = NodePool.get().init(action, this);
            children.put(action, child);
        }
    }
    
    HashMap<Integer,Integer> extractDistances(StateObservation so){
		HashMap<Integer,Integer> distances = new HashMap<Integer,Integer>();
		paths = new HashMap<Integer,Path>();
		
		ArrayList<ArrayList<Observation>[]> sprites = new ArrayList<ArrayList<Observation>[]>();

		sprites.add(so.getResourcesPositions(so.getAvatarPosition()));
		sprites.add(so.getNPCPositions(so.getAvatarPosition()));
		sprites.add(so.getImmovablePositions(so.getAvatarPosition()));
		sprites.add(so.getMovablePositions(so.getAvatarPosition()));
		sprites.add(so.getPortalsPositions(so.getAvatarPosition()));
		
		Pathfinder pf = new Pathfinder();
		pf.updateMap(so);
		
		for (ArrayList<Observation>[] type : sprites){
			if (type != null)
				for (int i = 0; i < type.length; i++){
					if (type[i].size() > 0){
						for(int j = 0; type[i].size() > j; j++){
							int itemType = type[i].get(j).itype;
							Path path = pf.getAStarPath(so, Utils.toTileCoord(type[i].get(j).position));
							//System.out.println(itemType + " dist.:" + distance );
							if (path != null && path.getLength() != -1) {
								paths.put(itemType, path);
								distances.put(itemType, path.getLength());
								break;
							}
						}
					}
				}
		}
			return distances; 	
	}

    
	public HashMap<Integer, Integer> getDistances() {
		if (!distancesIsValid || distances == null) {
			distances = extractDistances(state);
			distancesIsValid = true;
		}
		return distances;
	}

	public HashMap<Integer, Path> getPaths() {
		if (!distancesIsValid || paths == null) {
			distances = extractDistances(state);
			distancesIsValid = true;
		}
		return paths;
	}


}
