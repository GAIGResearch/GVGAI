package agents.TeamTopbug;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;

import java.util.ArrayList;

public class GameInfo {
    public static int             width       = -1;
    public static int             height      = -1;
    public static int             blocksize   = -1;
    public static Types.ACTIONS[] actions     = null;
    public static int             NUM_ACTIONS = 0;


    static public void init(StateObservation so) {
        ArrayList<Observation>[][] observationGrid = so.getObservationGrid();
        width = observationGrid.length;
        height = observationGrid[0].length;
        blocksize = so.getBlockSize();

        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> actionList = so.getAvailableActions();

        actionList.add(Types.ACTIONS.ACTION_NIL); // allow action NIL

        NUM_ACTIONS = actionList.size();
        actions = new Types.ACTIONS[NUM_ACTIONS];
        int i = 0;
        for (Types.ACTIONS action : actionList) {
            actions[i++] = action;
        }

    }
}
