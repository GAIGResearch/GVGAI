package agents.mrtndwrd;

import java.util.Map.Entry;

public class Tuple<X, Y> implements Entry<X, Y>
{
	public X x;
	public Y y;

	public Tuple(){}

	public Tuple(X x_, Y y_){
		x=x_;
		y=y_;
	}

	public X getKey()
	{
		return x;
	}

	public Y getValue()
	{
		return y;
	}

	public Y setValue(Y y)
	{
		this.y = y;
		return y;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + x.hashCode();
		hash = hash * 31 + y.hashCode();
		return hash;
	}

	public boolean equals(Object o)
	{
		try 
		{
			@SuppressWarnings("unchecked")
			Tuple<X, Y> t = (Tuple<X, Y>) o;
			return equals(t);
		}
		catch (Exception e)
		{
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public boolean equals(Tuple<X, Y> o)
	{
		return x.equals(o.x) && y.equals(o.y);
	}

	public String toString()
	{
		return "(" + x.toString() + ", " + y.toString() + ")";
	}
}
