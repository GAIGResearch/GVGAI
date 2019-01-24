package agents.mrtndwrd;

import ontology.Types;
import core.game.StateObservation;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;

/** Option that represents (one of the) normal actions */
public class ActionOption extends Option implements Serializable
{
	/** The action that will (always) be taken by this option */
	private Types.ACTIONS action;

	public ActionOption(double gamma, Types.ACTIONS action)
	{
		super(gamma);
		this.action = action;
	}

	/** Copy constructor */
	public ActionOption(double gamma, int step, double cumulativeReward, Types.ACTIONS action)
	{
		super(gamma, step, cumulativeReward);
		this.action = action;
	}

	/** Return the action that this option represents */
	public Types.ACTIONS act(StateObservation so)
	{
		// Set finished to true, since this option has to take the action once
		// and then will be finished
		if(this.step > 0)
			System.out.println("WARNING action option not terminated?");
		this.step++;
		this.finished = true;
		return this.action;
	}

	public boolean isFinished(StateObservation so)
	{
		// Ignore so, all states are finite states for this option
		return this.step > 0;
	}

	/** Reset all values */
	public void reset()
	{
		this.step = 0;
	}

	protected void readObject(ObjectInputStream aInputStream) 
		throws ClassNotFoundException, IOException 
	{
		//always perform the default de-serialization first
		super.readObject(aInputStream);
		action = (Types.ACTIONS) aInputStream.readObject();
	}

	protected void writeObject(ObjectOutputStream aOutputStream)
		throws IOException 
	{
		// perform the default serialization for all non-transient, non-static
		// fields
		aOutputStream.defaultWriteObject();
		aOutputStream.writeObject(action);
	}
	@Override
	protected String getSubtype()
	{
		return this.action.toString();
	}

	@Override
	public Option copy()
	{
		return new ActionOption(gamma, step, cumulativeReward, action);
	}

	public String toString()
	{
		return "ActionOption(" + this.action + ")";
	}

	public int hashCode()
	{
		return this.action.hashCode();
	}

	public boolean equals(Object o)
	{
		if(o instanceof ActionOption)
		{
			ActionOption oa = (ActionOption) o;
			return this.action == oa.action;
		}
		return false;
	}
}
