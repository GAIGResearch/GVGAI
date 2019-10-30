package agents.mrtndwrd;

import core.game.StateObservation;
import core.game.Observation;
import tools.Vector2d;
import ontology.Types;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Comparator;

@SuppressWarnings("unchecked")
public class AStar
{
	/** The size of (movement) blocks in the current game */
	private static int blockSize;

	/** Max x coordinate (min is assumed to be 0) */
	public int maxX;

	/** Max y coordinate (min is assumed to be 0) */
	public int maxY;

	/** Dynamic set of observation iTypes that are identified as inpenetrable */
	private DefaultHashMap<Integer, Integer> wallITypeScore;

	private Tuple<Integer, Integer> goal;

	private DefaultHashMap<Tuple<Integer, Integer>, Double> gScore = new DefaultHashMap<Tuple<Integer, Integer>, Double> (0.);

	private Map<Tuple<Integer, Integer>, Double> fScore = new HashMap<Tuple<Integer, Integer>, Double> ();

	/** Set of nodes to check out, ordered by fScore */
	private PriorityQueue<Tuple<Integer, Integer>> openSet;

	/** Used to find out the actual best path, when the goal is reached */
	private HashMap<Tuple<Integer, Integer>, Tuple<Integer, Integer>> cameFrom;

	/** Set of nodes that has been checked */
	HashSet<Tuple<Integer, Integer>> closedSet;

	private ArrayList<Observation>[][] lastObservationGrid;

	/** Initialize the grid. Assumes 0's in the observationGrid are walls */
	public AStar(StateObservation so)
	{
		blockSize = so.getBlockSize();
		setLastObservationGrid(so.getObservationGrid());

		openSet = new PriorityQueue<Tuple<Integer, Integer>>(10, new TupleComparator());
		closedSet = new HashSet<Tuple<Integer, Integer>>();
		cameFrom = new HashMap<Tuple<Integer, Integer>, Tuple<Integer, Integer>>();
		this.wallITypeScore = new DefaultHashMap<Integer, Integer>(0);

		// X, vertical coordinates, is the inner array
		maxX = so.getObservationGrid().length-1;
		// Y, the horizontal coordinates, is the outer array
		maxY = so.getObservationGrid()[0].length-1;
	}

	public void setLastObservationGrid(ArrayList<Observation>[][] observationGrid)
	{
		this.lastObservationGrid = observationGrid;
	}

	/** Creates a tuple of integers representing block coordinates from a
	 * vector2d representing field coordinates 
	 */
	public static SerializableTuple<Integer, Integer> vectorToBlock(Vector2d vector)
	{
		return new SerializableTuple<Integer, Integer>((int) vector.x/blockSize, (int)vector.y/blockSize);
	}

	public ArrayList<SerializableTuple<Integer, Integer>> aStar (Vector2d start, Vector2d goal)
	{
		return aStar (vectorToBlock(start), vectorToBlock(goal));
	}

	/** Run a* with cartesian coordinates. Block size should already be
	 * eliminated from these positions */
	public ArrayList<SerializableTuple<Integer, Integer>> aStar(Tuple<Integer, Integer> start, Tuple<Integer, Integer> goal)
	{
		// Reset everything:
		openSet.clear();
		closedSet.clear();
		gScore.clear();
		fScore.clear();
		cameFrom.clear();

		this.goal = goal;
		openSet.add(start);
		gScore.put(start, 0.);
		fScore.put(start, fScore(start));
		Tuple<Integer, Integer> current;

		while(openSet.size() != 0)
		{
			// current := the node in openset having the lowest f_score[] value
			current = openSet.poll();
			if(current.equals(goal))
			{
				return reconstructPath(current);
			}
			closedSet.add(current);
			double currentGScore = gScore.get(current);
			for(Tuple<Integer, Integer> neighbour : getNeighbours(current))
			{
				if(closedSet.contains(neighbour))
					continue;

				// Add the newly applied distance and the wallScore to this
				// path's gScore (we'll try to go through anything with a high
				// wall score if that's the only way)
				double tentativeGScore = currentGScore
					+ distance(current, neighbour) + wallScore(neighbour);

				if(!openSet.contains(neighbour) || tentativeGScore < currentGScore)
				{
					cameFrom.put(neighbour, current);
					gScore.put(neighbour, tentativeGScore);
					fScore.put(neighbour, fScore(neighbour));
					if(!openSet.contains(neighbour))
						openSet.add(neighbour);
				}
			}
		}
		// Nothing found...
		return new ArrayList<SerializableTuple<Integer, Integer>>();
	}

	/** Recreates the path. The first entry is the goal, the last entry is the
	 * current location */
	private ArrayList<SerializableTuple<Integer, Integer>> reconstructPath(Tuple<Integer, Integer> end)
	{
		ArrayList<SerializableTuple<Integer, Integer>> path = new ArrayList<SerializableTuple<Integer, Integer>>();
		Tuple<Integer, Integer> current = new SerializableTuple<Integer, Integer>(end);

		do
		{
			path.add(new SerializableTuple<Integer, Integer>(current));
			current = cameFrom.get(current);
		}
		while (current != null);

		return path;
	}

	/** Creates a collection of neighbours reachable from node 'node' */
	private Collection<Tuple<Integer, Integer>> getNeighbours(Tuple<Integer, Integer> node)
	{
		Collection<Tuple<Integer, Integer>> neighbours = 
			new ArrayList<Tuple<Integer, Integer>> (4);
		Tuple<Integer, Integer> nt;
		if(node.x < maxX)
		{
			nt = new Tuple<Integer, Integer>(node.x + 1, node.y);
			neighbours.add(nt);
		}
		if(node.x > 0)
		{
			nt = new Tuple<Integer, Integer>(node.x - 1, node.y);
			neighbours.add(nt);
		}
		if(node.y < maxY)
		{
			nt = new Tuple<Integer, Integer>(node.x, node.y + 1);
			neighbours.add(nt);
		}
		if(node.y > 0)
		{
			nt = new Tuple<Integer, Integer>(node.x, node.y - 1);
			neighbours.add(nt);
		}
		return neighbours;
	}

	/** Calculates fscore, which is the gScore + euclidian distance from node to
	 * goal */
	private double fScore(Tuple<Integer, Integer> node)
	{
		return gScore.get(node) + heuristic(node);
	}

	/** Heuristic based on the euclidian distance and the chance of the current
	 * node being a wall */
	private double heuristic(Tuple<Integer, Integer> node)
	{
		return distance(node, goal) + wallScore(node);
	}

	private int wallScore(Tuple<Integer, Integer> node)
	{
		return wallScore(node.x, node.y);
	}

	private int wallScore(int x, int y)
	{
		// IMPOSSIBLE!
		if(x < 0 || y < 0 || x > maxX || y > maxY)
			return 999999999;
		int score = 0;
		for(Observation obs : lastObservationGrid[x][y])
		{
			score += wallITypeScore.get(obs.itype);
		}
		return score;
	}

	public static double distance(Tuple<Integer, Integer> node, 
		Tuple<Integer, Integer> goal)
	{
		// sqrt(x^2 + y^2)
		return Math.sqrt(Math.pow(node.x - goal.x, 2) + 
			Math.pow(node.y - goal.y, 2));
	}

	/** Compare tuples by fScore */
	public class TupleComparator implements 
		Comparator<Tuple<Integer, Integer>>
	{
		public int compare (Tuple<Integer, Integer> n1,
			Tuple<Integer, Integer> n2)
		{
			double f1 = fScore(n1);
			double f2 = fScore(n2);
			if(f1 > f2)
				return 1;
			else if(f2 > f1)
				return -1;
			return 0;
		}
	}

	/** Returns the action that leads from start to end. */
	public static Types.ACTIONS neededAction(Tuple<Integer, Integer> start, 
		Tuple<Integer, Integer> end)
	{
		if(start.x > end.x)
		{
			return Types.ACTIONS.ACTION_LEFT;
		}
		else if(start.x < end.x)
		{
			return Types.ACTIONS.ACTION_RIGHT;
		}
		else if(start.y > end.y)
		{
			return Types.ACTIONS.ACTION_UP;
		}
		else if(start.y < end.y)
		{
			return Types.ACTIONS.ACTION_DOWN;
		}
		// System.out.println("Probably same location, returning action nil");
		return Types.ACTIONS.ACTION_NIL;
	}

	public static SerializableTuple<Integer, Integer> applyAction(SerializableTuple<Integer, Integer> location,
			Types.ACTIONS action)
	{
		SerializableTuple<Integer, Integer> endLocation = new SerializableTuple<Integer, Integer>(location);
		if(action == Types.ACTIONS.ACTION_LEFT)
			endLocation.x -= 1;
		else if(action == Types.ACTIONS.ACTION_RIGHT)
			endLocation.x += 1;
		else if(action == Types.ACTIONS.ACTION_UP)
			endLocation.y -= 1;
		else if(action == Types.ACTIONS.ACTION_DOWN)
			endLocation.y += 1;
		return endLocation;
	}

	/** Get orientation: up = (0, -1), down = (0, 1), left = (-1, 0), 
	 * right = (1, 0) */
	public static Vector2d direction(SerializableTuple<Integer, Integer> from, 
			SerializableTuple<Integer, Integer> to)
	{
		double i, j;
		if(from.x > to.x)
			j = -1;
		else if (from.x < to.x)
			j = 1;
		else
			j = 0;
		if(from.y > to.y)
			i = -1;
		else if (from.y < to.y)
			i = 1;
		else
			i = 0;
		return new Vector2d(j, i);
	}

	/** Returns the action that moves away from the goal. Opposite of
	 * "neededAction" */
	public static Types.ACTIONS moveAway(SerializableTuple<Integer, Integer> location,
			SerializableTuple<Integer, Integer> goal)
	{
		if(location.x < goal.x)
		{
			return Types.ACTIONS.ACTION_LEFT;
		}
		else if(location.x > goal.x)
		{
			return Types.ACTIONS.ACTION_RIGHT;
		}
		else if(location.y < goal.y)
		{
			return Types.ACTIONS.ACTION_UP;
		}
		else if(location.y > goal.y)
		{
			return Types.ACTIONS.ACTION_DOWN;
		}
		// System.out.println("Probably same location, returning action left");
		return Types.ACTIONS.ACTION_LEFT;
	}

	public void checkForWalls(StateObservation state, Types.ACTIONS action, StateObservation nextState)
	{
		SerializableTuple<Integer, Integer> startLocation = vectorToBlock(state.getAvatarPosition());
		Vector2d startOrientation = state.getAvatarOrientation();
		SerializableTuple<Integer, Integer> endLocation = vectorToBlock(nextState.getAvatarPosition());
		Vector2d endOrientation = nextState.getAvatarOrientation();
		ArrayList<Observation> observations;
		SerializableTuple<Integer, Integer> expectedEndLocation = applyAction(startLocation, action);
		boolean killSprite = false;
		if(nextState.isGameOver() && nextState.getGameWinner() 
						== Types.WINNER.PLAYER_LOSES)
		{
			killSprite = true;
		}

		// If orientation changed, we assume that no movement was made
		// if location didn't change, there was probably something in the
		// way
		if(killSprite || 
				(startOrientation.equals(endOrientation) && 
					startLocation.equals(endLocation) 
					&& !action.equals(Types.ACTIONS.ACTION_NIL)
					&& !action.equals(Types.ACTIONS.ACTION_USE)))
		{
			// Killsprites are worse than walls
			// Get the sprite itypes on the endLocation:
			if(expectedEndLocation.x < maxX && expectedEndLocation.y < maxY && 
					expectedEndLocation.x >= 0 && expectedEndLocation.y >= 0)
			{
				observations = nextState.getObservationGrid()
					[expectedEndLocation.x][expectedEndLocation.y];
				for(Observation obs : observations)
				{
					// Increase the wall-score of this iType
					wallITypeScore.put(obs.itype, 
							wallITypeScore.get(obs.itype) + 1);
				}
			}
		}
	}

	/** Print all the walls! */
	public String toString()
	{
		String s = "Walls: \n";
		// First, loop through possible y coordinates (since y is vertical)
		for (int y=0; y<=maxY; y++)
		{
			// Now loop through x (horizontal)
			for (int x=0; x<=maxX; x++)
			{
				s += String.format("%5d ", wallScore(x, y));
			}
			s += "\n";
		}
		return s;
	}
}
