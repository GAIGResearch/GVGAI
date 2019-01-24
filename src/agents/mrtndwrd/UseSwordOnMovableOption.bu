package mrtndwrd;

import ontology.Types;
import core.game.StateObservation;
import core.game.Observation;
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
public class UseSwordOnMovableOption extends GoToMovableOption implements Serializable
{
	public UseSwordOnMovableOption(double gamma, Lib.GETTER_TYPE type, int itype, int obsID, StateObservation so)
	{
		super(gamma, type, itype, obsID, so);
	}

	/** Copy constructor */
	public UseSwordOnMovableOption(double gamma, int step, double cumulativeReward, Lib.GETTER_TYPE type, int itype, int obsID, SerializableTuple<Integer, Integer> goal)
	{
		super(gamma, step, cumulativeReward, type, itype, obsID, goal);
	}

	@Override
	public Option copy()
	{
		return new UseSwordOnMovableOption(gamma, step, cumulativeReward, type, itype, obsID, goal);
	}

	@Override
	public String toString()
	{
		return String.format("UseSwordOnMovableOption(%s,%d,%d)", type, itype, obsID);
	}

	@Override
	public Types.ACTIONS act(StateObservation so)
	{
		setGoalLocation(so);
		// Save the calculation hassle if the goal is already set to null
		// unfortunately this speed-hack creates redundancy the rest of this
		// function
		if(this.goal == null && this.step > 0)
		{
			System.out.printf("Goal = null, step = %d, this = %s\n", step, this);
			this.step++;
			return Types.ACTIONS.ACTION_NIL;
		}
		SerializableTuple<Integer, Integer> avatarPosition = Agent.aStar.vectorToBlock(so.getAvatarPosition());
		//System.out.printf("Avatar position: %s, goal position: %s\n", avatarPosition, this.goal);
		// Check if you're next to the enemy
		if(AStar.distance(avatarPosition, this.goal) < 3)
		{
			// Check if you're aimed at the enemy
			if(so.getAvatarOrientation().equals(AStar.direction(avatarPosition, this.goal)))
			{
				// FIRE!
				//System.out.println("FIRE!!!!");
				this.step++;
				return Types.ACTIONS.ACTION_USE;
			}
			// Else: Move away from the enemy
			//else if(Agent.aStar.distance(avatarPosition, this.goal) == 1)
			else
			{
				this.step++;
				return AStar.moveAway(avatarPosition, this.goal);
			}
		}
		// Too far away, just go to the enemy
		return super.act(so);
	}
}
