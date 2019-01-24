package agents.ICELab;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.Vector2d;

import java.util.ArrayList;

public class GameInfo {
    public static int             width       = -1;
    public static int             height      = -1;
    public static int             blocksize   = -1;
    public static Types.ACTIONS[] actions     = null;
    public static int             NUM_ACTIONS = 0;
    public static int             avatarType = 0;
    public static boolean		  isDetermin = false;
    public static boolean		  OneDimension =false;
    public static boolean		  USE_ACTION = false;
//    public static int             Max_GameTick = 0;

    static public void init(StateObservation so) {
        ArrayList<Observation>[][] observationGrid = so.getObservationGrid();
        width = observationGrid.length;
        height = observationGrid[0].length;
        blocksize = so.getBlockSize();
        avatarType = so.getAvatarType();
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> actionList = so.getAvailableActions();

        actionList.add(Types.ACTIONS.ACTION_NIL); // allow action NIL

        NUM_ACTIONS = actionList.size();
        actions = new Types.ACTIONS[NUM_ACTIONS];
        int i = 0;
        int Dimension = 0;
        for (Types.ACTIONS action : actionList) {
        		//System.out.println(action.name() + " ordinal:" + action.ordinal());
            actions[i++] = action;
            if(action == Types.ACTIONS.ACTION_DOWN || action == Types.ACTIONS.ACTION_UP 
            		||action == Types.ACTIONS.ACTION_LEFT || action == Types.ACTIONS.ACTION_RIGHT){
            	Dimension++;
            }else if(action == Types.ACTIONS.ACTION_USE){
            	USE_ACTION = true;
            }
        }
        if(Dimension<=2){
        	OneDimension = true;
        	System.out.println("OneDGame");
        }
        isDetermin = UseHashCode(so);

    }
    
    
    
	public static boolean UseHashCode(StateObservation stateObs){
		StateObservation so = stateObs.copy();
		long initialHash;
		so.advance(ACTIONS.ACTION_NIL);
		initialHash = GenerateHashCode(so);
		int judge_step=30;
		int a=0;
		outside: for(a=0;a<judge_step;a++) {
			so.advance(ACTIONS.ACTION_NIL);
			if(initialHash!=GenerateHashCode(so)){
				break outside;	
			}
		}
		if(a==judge_step){
			return true;
			
		}else {
			return false;
		}
	}
	
	
	public static long GenerateHashCode(StateObservation so)
	{
		long result = 17;
		long prime = 31;
		ArrayList<Observation>[][] list;
		Vector2d orient;
		orient = so.getAvatarOrientation();
		
		result = result * prime + so.getAvatarType();
		result = result * prime + (int)(so.getAvatarPosition().x/blocksize);		
		result = result * prime + (int)(so.getAvatarPosition().x/blocksize);
		result = result * prime + (int)((orient.x * 2) + orient.y);

		list = so.getObservationGrid();
		for (int i = 0; i < list.length; i++)
		{
			for (int j = 0; j < list[i].length; j++)
			{
				result = result * prime + (i * list[i].length + j);
				for (Observation obj : list[i][j])
				{
					if (obj.category != Types.TYPE_AVATAR)
					{
						result = result * prime + obj.obsID;
						result = result * prime + obj.itype;
					}
				}
			}
		}
		return result;
	}
}
