package adrienctx;

import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

import java.util.ArrayList;

/**
 * Created by acouetoux on 30/06/16.
 */
public class MyStateObservation {

    public StateObservation stateObservation;

    public MyStateObservation(StateObservation _s){
        stateObservation = _s;
    }

    @Override
    public boolean equals(Object arg){
        MyStateObservation obj = (MyStateObservation) arg;
        Vector2d pos1 = this.stateObservation.getAvatarPosition();
        Vector2d pos2 = obj.stateObservation.getAvatarPosition();
        Vector2d orientation1 = this.stateObservation.getAvatarOrientation();
        Vector2d orientation2 = obj.stateObservation.getAvatarOrientation();

        if(!(pos1.equals(pos2))){
            return false;
        }
        if(!(orientation1.equals(orientation2))){
            return false;
        }

        ArrayList<Observation>[] obsList1 = this.stateObservation.getNPCPositions();
        ArrayList<Observation>[] obsList2 = obj.stateObservation.getNPCPositions();
        if(!areTwoObsListEqual(obsList1, obsList2)){
            return false;
        }
        obsList1 = this.stateObservation.getMovablePositions();
        obsList2 = obj.stateObservation.getMovablePositions();
        if(!areTwoObsListEqual(obsList1, obsList2)){
            return false;
        }
        obsList1 = this.stateObservation.getResourcesPositions();
        obsList2 = obj.stateObservation.getResourcesPositions();
        if(!areTwoObsListEqual(obsList1, obsList2)){
            return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        int code = 0;
        ArrayList<Observation>[] obsList1 = stateObservation.getNPCPositions();
        code += getSumOfDistances(obsList1);
        obsList1 = stateObservation.getMovablePositions();
        code += getSumOfDistances(obsList1);
        obsList1 = stateObservation.getResourcesPositions();
        code += getSumOfDistances(obsList1);

        return code;
    }

    private int getSumOfDistances(ArrayList<Observation>[] obsList){
        int i = 0;
        int j = 0;
        int sum = 0;
        if (obsList != null) {
            while (i < obsList.length) {
                while (j < obsList[i].size()) {
                    sum += (int)obsList[i].get(j).sqDist;
                    j++;
                }
                i++;
            }
        }
        return sum;
    }

    private boolean areTwoObsListEqual(ArrayList<Observation>[] obsList1, ArrayList<Observation>[] obsList2){
        int i = 0;
        int j = 0;
        if ((obsList1 != null) && (obsList2 != null)) {
            if (obsList1.length != obsList2.length) {
                return false;
            } else {
                while (i < obsList1.length) {
                    if (obsList1[i].size() != obsList2[i].size()) {
                        return false;
                    } else {
                        while (j < obsList1[i].size()) {
                            if (!obsList1[i].get(j).equals(obsList2[i].get(j))) {
                                return false;
                            }
                            j++;
                        }
                    }
                    i++;
                }
            }
        }
        return true;
    }
}
