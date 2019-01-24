package agents.mrtndwrd;

import ontology.Types;
import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;

/** Option that represents planning a route using aStar.
 * The route is planned between walls, enemies and objects are not taken into
 * account. The static aStar variable in AbstractAgent is used. 
 */
public class GoToPositionOption extends Option implements Serializable
{
	/** The goal position of this option */
	protected SerializableTuple<Integer, Integer> goal;

	/** Specifies if this follows an NPC or a movable non-npc sprite */
	protected Lib.GETTER_TYPE type;

	/** Specifies the itype (sprite type id) in the getter of this type, e.g. getNPCPositions */
	protected int itype; 

	/** If this is true, the goal is a sprite that can be removed from the game.
	 * That means that this option is not possible anymore. If this is false,
	 * the goal is just an x/y location which is always possible to go to */
	protected boolean goalIsSprite;

	
	/** The last planned path, as long as this is followed, no replanning has to
	 * be done */
	ArrayList<SerializableTuple<Integer, Integer>> currentPath = new ArrayList<SerializableTuple<Integer, Integer>>();

	/** Initialize with a position to go to */
	public GoToPositionOption(double gamma, SerializableTuple<Integer, Integer> goal)
	{
		super(gamma);
		this.goal = goal;
		this.goalIsSprite = false;
	}

	/** Initialize with a position to go to */
	public GoToPositionOption(double gamma, int step, double cumulativeReward, SerializableTuple<Integer, Integer> goal)
	{
		super(gamma, step, cumulativeReward);
		this.goal = goal;
		this.goalIsSprite = false;
	}

	/** Initialize with a position to go to. Goal is converted to block
	 * coordinates */
	public GoToPositionOption(double gamma, Vector2d goal)
	{
		this(gamma, Agent.aStar.vectorToBlock(goal));
	}

	/** Initialize with something that has to be followed */
	public GoToPositionOption(double gamma, Lib.GETTER_TYPE type, int itype, int obsID, StateObservation so)
	{
		super(gamma);
		this.type = type;
		this.itype = itype;
		this.obsID = obsID;
		this.goal = getGoalLocationFromSo(so);
		this.goalIsSprite = true;
	}

	/** Initialize with something that has to be followed, including setting the
	 * goal location (so no StateObservation is needed) */
	public GoToPositionOption(double gamma, Lib.GETTER_TYPE type, int itype, int obsID, SerializableTuple<Integer, Integer> goal)
	{
		super(gamma);
		this.type = type;
		this.itype = itype;
		this.obsID = obsID;
		if(goal == null)
			System.out.println("WARNING! Setting goal to NULL in constructor!");
		this.goal = goal;
		this.goalIsSprite = true;
	}

	public GoToPositionOption(double gamma, int step, double cumulativeReward, Lib.GETTER_TYPE type, int itype, int obsID, SerializableTuple<Integer, Integer> goal)
	{
		super(gamma, step, cumulativeReward);
		this.type = type;
		this.itype = itype;
		this.obsID = obsID;
		if(goal == null)
			System.out.println("WARNING! Setting goal to NULL in constructor!");
		this.goal = goal;
		this.goalIsSprite = true;
	}

	/** "Empty" constructor. Only use this if you'll set the goal in a subclass!
	 * WARNING: Don't expect to be able to use the hashCode variable from this class.
	 * That means you'd have to override hashCode() from this class as well!
	 */
	protected GoToPositionOption(double gamma)
	{
		super(gamma);
		System.out.println("WARNING! Using empty constructor in " + this);
		this.currentPath = new ArrayList<SerializableTuple<Integer, Integer>>();
	}

	/** Returns the next action to get to this.goal. This function only plans
	 * around walls, not around other objects or NPC's. Only plans a new path if
	 * the current location is not anymore in the currentPath. 
	 */
	public Types.ACTIONS act(StateObservation so)
	{
		SerializableTuple<Integer, Integer> avatarPosition = Agent.aStar.vectorToBlock(so.getAvatarPosition());
		if(!goalExists(so) || avatarPosition.equals(this.goal))
		{
			setFinished();
		}
		// Increase step counter to keep track of everything.
		this.step++;
		// Do nothing if we're already done
		if(this.finished)
		{
			return Types.ACTIONS.ACTION_NIL;
		}

		int index = currentPath.indexOf(avatarPosition);

		// Sometimes: recalculate in case the world has changed.
		if(so.getGameTick() % 15 == 0 || index < 0)
		{
			// Plan a new path
			this.currentPath = Agent.aStar.aStar(avatarPosition, goal);
			// current location is at the end of the path
			index = currentPath.size()-1;
		}

		if(currentPath.size() == 0 || index < 0)
		{
			// No path available, we're done.
			setFinished();
			System.out.printf("No path available to go to %s in %s, returning NIL\n", this.goal, Agent.aStar);
			return Types.ACTIONS.ACTION_NIL;
		}

		// Return the action that is needed to get to the next path index.
		return Agent.aStar.neededAction(avatarPosition, currentPath.get(index - 1));
	}

	/** This option is finished if the avatar's position is the same as
	 * the goal location or if the game has ended (avatarPosition = -1, -1)
	 */
	public boolean isFinished(StateObservation so)
	{
		if(this.finished)
			return true;

		// There might be some other cases in which this.finished has not been
		// set yet:
		if (!this.goalExists(so) || currentPath.size() == 0)
		{
			setFinished();
			return true;
		}

		Vector2d avatarPosition = so.getAvatarPosition();
		// Option is also finished when the game is over or the goal is reached
		if(avatarPosition.x == -1 && avatarPosition.y == -1 ||
				this.goal == Agent.aStar.vectorToBlock(avatarPosition))
		{
			setFinished();
			return true;
		}

		// If all of the above fails, this is not finished yet!
		return false;
	}

	/** Returns the location of the thing that is tracked, based on type, itype
	 * and obsID */
	protected SerializableTuple<Integer, Integer> getGoalLocationFromSo(StateObservation so)
	{
		ArrayList<Observation> observations;
		observations = getObservations(so, itype);
		for (Observation o : observations)
		{
			if(o.obsID == this.obsID)
				return Agent.aStar.vectorToBlock(o.position);
		}

		// Probably this obs is already eliminated.
		return null;
	}

	/** Returns the observations from the right getter, according to this.type
	 * @param itype the itype inside the getter, of the observation type that is
	 * requested
	 */
	protected ArrayList<Observation> getObservations(StateObservation so, int itype)
	{
		ArrayList<Observation>[] observations = null;

		if(this.type == Lib.GETTER_TYPE.NPC || this.type == Lib.GETTER_TYPE.NPC_KILL)
			observations = so.getNPCPositions();
		if(this.type == Lib.GETTER_TYPE.MOVABLE)
			observations = so.getMovablePositions();
		if(this.type == Lib.GETTER_TYPE.IMMOVABLE)
			observations = so.getImmovablePositions();
		if(this.type == Lib.GETTER_TYPE.RESOURCE)
			observations = so.getResourcesPositions();
		if(this.type == Lib.GETTER_TYPE.PORTAL)
			observations = so.getPortalsPositions();
		if(observations == null)
		{
			System.out.printf("WARNING: Type %s NOT known in %s!\n", this.type, this);
			return new ArrayList<Observation>();
		}

		for(ArrayList<Observation> o : observations)
		{
			// check if iType matches
			if(o.size() > 0)
				if(o.get(0).itype == this.itype)
					return o;
		}
		return new ArrayList<Observation>();
	}

	/** DOES NOT SET this.finished! */
	public boolean goalExists(StateObservation so)
	{
		if(goalIsSprite && getGoalLocationFromSo(so) == null)
		{
			return false;
		}
		return true;
	}


	public void reset()
	{
		super.reset();
		this.currentPath = new ArrayList<SerializableTuple<Integer, Integer>>();
	}

	protected void readObject(ObjectInputStream aInputStream) 
		throws ClassNotFoundException, IOException 
	{
		//always perform the default de-serialization first
		super.readObject(aInputStream);
	}

	protected void writeObject(ObjectOutputStream aOutputStream)
		throws IOException 
	{
		// perform the default serialization for all non-transient, non-static
		// fields
		aOutputStream.defaultWriteObject();
	}

	@Override
	public Option copy()
	{
		if(this.type != null)
			return new GoToPositionOption(gamma, step, cumulativeReward, type, itype, obsID, goal);
		else
		{
			System.out.println("WARNING! Type = null!");
			return new GoToPositionOption(gamma, step, cumulativeReward, goal);
		}
	}

	@Override
	public String toString()
	{
		return String.format("GoToPositionOption(%s,%d,%d), goal at %s", 
				type, itype, obsID, goal);
	}

	@Override
	public int hashCode()
	{
		//System.out.println(this);
		return this.goal.hashCode();
	}

	protected String getSubtype()
	{
		return super.getSubtype() + this.itype;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof GoToPositionOption)
		{
			GoToPositionOption oa = (GoToPositionOption) o;
			if(this.goal != null && oa.goal != null)
				return this.goal.equals(oa.goal);
		}
		return false;
	}
}
