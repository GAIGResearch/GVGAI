package agents.NovelTS;
import tools.Vector2d;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import core.game.Observation;
import core.game.StateObservation;


/**
* The purpose of the serializer is to give a temperary aim for the Agent
* to achieve, this aim is depended on the current state and
* the previous knowleged based on the aim.
*
* The serializer will keep on getting feedback from the searching state
* and tries to estimate the best goal to reach
*
* Because of the time limitation, we will assume events could only
* be related if they next to each other.
*/
public class Serializer {
    private HashMap<Integer, Integer> heuristic;
    // private HashMap<Integer, Integer> heuristic;
    private int numVisit;
    private ArrayList<Integer> ids;

    public Serializer() {
        heuristic = new HashMap<Integer, Integer>();

        ids = new ArrayList<Integer>();
    }


    /**
    * Will check whether the object close by is interesting or not
    * interesting object are objects that cause reward
    *
    * it takes in a fully expaned node
    */
    public void updateHeuristic(Feature feature, double reward) {
        this.numVisit += 1;
        // System.out.println(feature.getCloseObject().size());
        for (Atom atom: feature.getCloseObject()) {
            int obj = atom.getData3();
            if (!heuristic.containsKey(obj)) {
                ids.add(obj);
            }
            if (reward == 0) {
                if (heuristic.get(obj) == 1)
                    heuristic.put(obj, 0);
            }
            else if (reward > 0) {
                heuristic.put(obj, 1);
            }
            else {
                heuristic.put(obj, -1);
            }
        }
    }

    public void reset() {
        for (Integer id: ids) {
            if (heuristic.get(id) == 0) {
                heuristic.put(id, 1);
            }
        }
    }

    /**
    * This is a simple heuristic that calculate the minimum moving dis(tance
    * to an interesting object.
    */
    public int getStep(Feature feature) {
        int distance = 3000;
        Atom player = feature.getAvatarFeatures();
        for (Atom atom: feature.getFeatures()) {
            int obj = atom.getData3();
            if (!heuristic.containsKey(obj)){
                heuristic.put(obj, 1);
                ids.add(obj);
            }
            int h = heuristic.get(obj);
            int steps;
            int d = Utils.compareDistance(atom, player)
                    / IWPlayer.blocksize;
            if (h >= 0){
                steps =  d * h;
            } else {
                // steps = 3000 - d;
                steps = 10000;
            }

            if (steps < distance && steps != 0) {
                distance = steps;
            }
        }
        if (distance == 300) {
            reset();
            return getStep(feature);
        }
        // System.out.println(score);
        return distance;
    }

    @Override
    public String toString() {
        String s = "";
        for (Integer id: ids) {
            s += "(" + id +", " + heuristic.get(id) + ")";
        }
        return s + " " + numVisit;
    }
}
