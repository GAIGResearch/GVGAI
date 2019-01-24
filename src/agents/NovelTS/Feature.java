package agents.NovelTS;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import core.game.Observation;

import java.util.Random;
import java.util.ArrayList;
import tools.Vector2d;

public class Feature {
    private ArrayList<Atom> closeObject;
    private ArrayList<Atom> fullStateAtom;
    private Atom playerFeature;

    public Feature(StateObservation state) {
        closeObject = new ArrayList<Atom>();
        this.playerFeature = extractAvartarFeature(state);
        this.fullStateAtom = extractFeature(state);
    }


    public ArrayList<Atom> getFeatures() {
        return fullStateAtom;
    }

    public Atom getAvatarFeatures() {
        return playerFeature;
    }

    public ArrayList<Atom> getCloseObject() {
        return closeObject;
    }

    private ArrayList<Atom> extractFeature(StateObservation so) {
        ArrayList<Atom> features = new ArrayList<Atom>();
        ArrayList<Observation> obsArray[][] = so.getObservationGrid();
        int width = obsArray.length;
        int height = obsArray[0].length;
        for (int x = 0; x < width; x++) {
            for (int y  = 0; y < height; y++) {
                ArrayList<Observation> observations = obsArray[x][y];
                for (Observation obs: observations) {
                    Vector2d pos = obs.position;
                    Atom atom = new Atom((int)pos.x, (int)pos.y, obs.itype);
                    int distance = Utils.compareDistance(atom, playerFeature) / IWPlayer.blocksize;
                    if (distance <= 1) {
                        closeObject.add(atom);
                    }
                    features.add(atom);
                }
            }
        }
        return features;
    }

    private static Atom extractAvartarFeature(StateObservation so) {
        Vector2d pos = so.getAvatarPosition();
        Vector2d orientation = so.getAvatarOrientation();
        int x = (int)pos.x ;
        int y = (int) pos.y;
        int rotationX = (int) orientation.x + 1;
        int rotationY = (int) orientation.y + 1;

        return new Atom(x, y, (rotationY * 3 + rotationX) * 200);
    }
}
