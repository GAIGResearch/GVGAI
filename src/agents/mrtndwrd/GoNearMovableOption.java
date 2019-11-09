package agents.mrtndwrd;

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
public class GoNearMovableOption extends GoToMovableOption implements Serializable
{
	/** How near is "near" (in path size) */
	public final int THRESHOLD = 4;

	public GoNearMovableOption(double gamma, Lib.GETTER_TYPE type, int itype, int obsID, StateObservation so)
	{
		super(gamma, type, itype, obsID, so);
	}

	/** Copy constructor */
	public GoNearMovableOption(double gamma, int step, double cumulativeReward, Lib.GETTER_TYPE type, int itype, int obsID, SerializableTuple<Integer, Integer> goal)
	{
		super(gamma, step, cumulativeReward, type, itype, obsID, goal);
	}

	@Override
	public Option copy()
	{
		return new GoNearMovableOption(gamma, step, cumulativeReward, type, itype, obsID, goal);
	}

	@Override
	public String toString()
	{
		return String.format("GoNearMovableOption(%s,%d,%d)", type, itype, obsID);
	}

	@Override
	public boolean isFinished(StateObservation so)
	{
		if(this.finished)
			return true;

		// There might be some other cases in which this.finished has not been
		// set yet:
		if (!this.goalExists(so) || currentPath.size() < THRESHOLD)
		{
			setFinished();
			return true;
		}
		return super.isFinished(so);
	}
}

