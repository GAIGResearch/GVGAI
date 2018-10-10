package video.constants;

import java.util.ArrayList;
/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 12/02/2018
 * @author Tiago Machado
 */
public class InteractionsList {
	
	public static String [] interactionList = new String[]
			{
					Interactions.KILLSPRITE,
					Interactions.KILL_IF_HAS_MORE,
					Interactions.KILL_IF_HAS_LESS,
					Interactions.KILL_IF_FROM_ABOVE,
					Interactions.KILL_IF_OTHER_HAS_MORE,
					Interactions.TRANSFORM_TO_SINGLETON,
					Interactions.SPAWN_IF_HAS_MORE,
					Interactions.SPAWN_IF_HAS_LESS,
					Interactions.CLONE_SPRITE,
					Interactions.TRANSFORM_TO,
					Interactions.UNDO_ALL,
					Interactions.REVERSE_DIRECTION,
					Interactions.ATTRACT_GAZE,
					Interactions.TURN_AROUND,
					Interactions.WRAP_AROUND,
					Interactions.TELEPORT_TO_EXIT,
					Interactions.PULL_WITH_IT,
					Interactions.BOUNCE_FORWARD,
					Interactions.COLLECT_RESOURCE,
					Interactions.CHANGE_RESOURCE,
					Interactions.STEP_BACK,
					Interactions.WALL_STOP,
					Interactions.FLIP_DIRECTION
			};
	
}
