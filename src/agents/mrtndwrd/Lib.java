package agents.mrtndwrd;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Lib
{
	public static final double HUGE_NEGATIVE = -1000000000.0;
	public static final double HUGE_POSITIVE =  1000000000.0;
	/** Only the closest MAX_OBSERVATIONS observations are turned into options */
	public static final int MAX_OBSERVATIONS =  5;

	// This will be used by agents that make files. This postfix can be adjusted
	// by for example the Test class (that's why it's not final...), in order to
	// be able to run several agents at the same time
	public static String filePostfix = "";

	public static String observationToString(Observation obs)
	{
		String s = "";
		s += "obsID: " + obs.obsID + "\n";
		s += "cagegory: " + obs.category + "\n";
		s += "itype: " + obs.itype + "\n";
		s += "position: " + obs.position + "\n";
		s += "reference: " + obs.reference + "\n";
		s += "sqDist: " + obs.sqDist + "\n\n";
		return s;
	}

	public static void printObservationGrid(ArrayList<Observation>[][] observationGrid)
	{
		for (int x = 0; x < observationGrid.length; x++)
		{
			for (int y = 0; y<observationGrid[x].length; y++)
			{
				for (Observation obs : observationGrid[x][y])
				{
					System.out.print(obs.itype);
				}
				System.out.print(" ");
			}
			System.out.println();
		}
	}

	/** Very simple state evaluation, taken from the MCTS code */
	public static double simpleValue(StateObservation a_gameState) 
	{
		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();
		double rawScore = a_gameState.getGameScore();

		if(gameOver && win == Types.WINNER.PLAYER_LOSES)
			rawScore += HUGE_NEGATIVE;

		if(gameOver && win == Types.WINNER.PLAYER_WINS)
			rawScore += HUGE_POSITIVE;

		// Make quicker better
		return rawScore;
	}

	/** Get the nearest observation from an array of ArrayLists of observations,
	 * assuming that all ArrayLists are already ordered. These ArrayList Arrays
	 * come from functions like getNPCPositions()
	 */
	public static Observation getNearestObservation(ArrayList<Observation>[] obala)
	{
		// Sometimes obala isn't instantiated at all!
		if(obala == null)
		{
			return null;
		}
		
		// Hopefully this initializes to a not-null value
		Observation nearest = null;
		for(ArrayList<Observation> obal : obala)
		{
			// if there's observations in this entry
			if(obal.size() > 0)
			{
				// If we don't have a nearest, make this observation nearest
				if(nearest != null)
				{
					// Take the nearest observation of both
					if(nearest.sqDist > obal.get(0).sqDist)
					{
						nearest = obal.get(0);
					}
				}
				else
					nearest = obal.get(0);
			}
		}
		return nearest;
	}

	public static void writeHashMapToFile(HashMap h, String f)
	{
		try
		{
			FileOutputStream fos = new FileOutputStream(f);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(h);
			oos.close();
			System.out.println("HashMap written to file");
		}
		catch (Exception e)
		{
			System.out.println("Couldn't write the hash map to file!");
			e.printStackTrace();
		}
	}

	public static Object loadObjectFromFile(String f)
	{
		try
		{
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object o = ois.readObject();
			System.out.printf("Table loaded from file %s\n", f);
			return o;
		}
		catch (FileNotFoundException e)
		{
			// No file (yet) present:
			System.out.println("No hashMap loaded");
		}
		catch (Exception e)
		{
			// Something's definitely wrong now!
			System.out.println("Couldn't load the hash map from file!");
			e.printStackTrace();
			System.out.println("Returning null");
		}
		// if all else fails
		return null;
	}

	public static enum GETTER_TYPE
	{
		NPC, MOVABLE, IMMOVABLE, RESOURCE, PORTAL, NPC_KILL
	}


	/** Adds options for all actions in the arraylist actions
	 * @param actions all actions possible in this game
	 * @param possibleOptions A list that may allready contain options. New
	 * options are added to this list
	 */
	public static void setOptionsForActions(ArrayList<Types.ACTIONS> actions, ArrayList<Option> possibleOptions)
	{
		for(Types.ACTIONS action : actions)
		{
			possibleOptions.add(new ActionOption(Agent.GAMMA, action));
		}
	}

	/** Adds options to the list possibleOptions
	 * @param so the stateobservation is used to find stuff that a path can lead
	 * to
	 * @param possibleOptions A list that may allready contain options. New
	 * options are added to this list
	 * @param optionObsIds the ids of all observations that options lead to.
	 * This is useful for removing options that lead to non-existing sprites
	 */
	public static void setOptions(StateObservation so, ArrayList<Option> possibleOptions, HashSet<Integer> optionObsIDs)
	{
		// Holds the ObsIDs that were already present in this.optionObsIDs
		HashSet<Integer> keepObsIDs = new HashSet<Integer>();
		// Holds the new obsIDs
		HashSet<Integer> newObsIDs = new HashSet<Integer>();
		

		ArrayList<Types.ACTIONS> act = so.getAvailableActions();
		// Only create path planning options if up, down, left and right are
		// available
		if(act.contains(Types.ACTIONS.ACTION_UP) &&
				act.contains(Types.ACTIONS.ACTION_DOWN) &&
				act.contains(Types.ACTIONS.ACTION_LEFT) &&
				act.contains(Types.ACTIONS.ACTION_RIGHT))
		{
			Vector2d avatarPosition = so.getAvatarPosition();
			// Set options for all types of sprites that exist in this game. If they
			// don't exist, the getter will return null and no options will be
			// created.
			if(so.getNPCPositions() != null)
			{
				createOptions(so.getNPCPositions(avatarPosition), GETTER_TYPE.NPC, so, keepObsIDs, newObsIDs, possibleOptions, optionObsIDs);
				// We can use a weapon! Try to make kill-options
				// if(so.getAvailableActions().contains(Types.ACTIONS.ACTION_USE))
				// 	createOptions(so.getNPCPositions(), Lib.GETTER_TYPE.NPC_KILL, so, keepObsIDs, newObsIDs, possibleOptions, optionObsIDs);

			}
			if(so.getMovablePositions() != null)
				createOptions(so.getMovablePositions(avatarPosition), GETTER_TYPE.MOVABLE, so, keepObsIDs, newObsIDs, possibleOptions, optionObsIDs);
			//if(so.getImmovablePositions() != null)
			//	createOptions(so.getImmovablePositions(), Lib.GETTER_TYPE.IMMOVABLE, so, keepObsIDs, newObsIDs, possibleOptions, optionObsIDs);
			if(so.getResourcesPositions() != null)
				createOptions(so.getResourcesPositions(avatarPosition), GETTER_TYPE.RESOURCE, so, keepObsIDs, newObsIDs, possibleOptions, optionObsIDs);
			if(so.getPortalsPositions() != null)
				createOptions(so.getPortalsPositions(avatarPosition), GETTER_TYPE.PORTAL, so, keepObsIDs, newObsIDs, possibleOptions, optionObsIDs);

			// Remove all "old" obsIDs from this.optionObsIDs. optionObsIDs will
			// then only contain obsolete obsIDs
			optionObsIDs.removeAll(keepObsIDs);

			// Now remove all options that have the obsIDs in optionObsIDs.
			// We use the iterator, in order to ensure removing while iterating is
			// possible
			for (Iterator<Option> it = possibleOptions.iterator(); it.hasNext();)
			{
				Option option = it.next();
				// Remove the options that are still in optionObsIDs.
				if(optionObsIDs.contains(option.getObsID()))
				{
					it.remove();
				}
			}

			// Now all options are up-to-date. this.optionObsIDs should be updated
			// to represent the current options list:
			optionObsIDs.clear();
			optionObsIDs.addAll(newObsIDs);
		}
	}

	/** Adds new obsIDs to newObsIDs and ID's that should be kept to
	 * keepObsIDs, based on the ID's in the ArrayList observations
	 * Also creates options for all new obsIDs in possibleOptions and
	 * optionObsIDs
	 */
	public static void createOptions(ArrayList<Observation>[] observations,
			GETTER_TYPE type,
			StateObservation so,
			HashSet<Integer> keepObsIDs,
			HashSet<Integer> newObsIDs,
			ArrayList<Option> possibleOptions,
			HashSet<Integer> optionObsIDs)
	{
		// Loop through all types of NPCs
		for(List<Observation> observationType : observations)
		{
			// Prune to only the nearest observations
			if(observationType.size() > MAX_OBSERVATIONS)
				observationType = observationType.subList(0, MAX_OBSERVATIONS);
			// Loop through all the NPC's of this type
			for(Observation observation : observationType)
			{
				// This is considered to be a wall
				if(observation.itype == 0)
					continue;
				// Check if this is a new obsID
				if(! optionObsIDs.contains(observation.obsID))
				{
					// Create option for this obsID
					if(type == GETTER_TYPE.NPC || type == GETTER_TYPE.MOVABLE)
					{
						possibleOptions.add(new GoToMovableOption(Agent.GAMMA, 
							type, observation.itype, observation.obsID, so));
						//if(type == Lib.GETTER_TYPE.NPC)
						//	possibleOptions.add(new GoNearMovableOption(Agent.GAMMA, 
						//		type, observation.itype, observation.obsID, so));
					}
					// else if (type == Lib.GETTER_TYPE.NPC_KILL)
					// 	possibleOptions.add(new UseSwordOnMovableOption(Agent.GAMMA, 
					// 		type, observation.itype, observation.obsID, so));
					else
						possibleOptions.add(new GoToPositionOption(Agent.GAMMA, 
							type, observation.itype, observation.obsID, so));
				}
				else
				{
					// Add to the list of options that should be kept
					keepObsIDs.add(observation.obsID);
				}
				newObsIDs.add(observation.obsID);
			}
		}
	}
}
