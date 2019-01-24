package agents.bladerunner.agents.misc.pathplanning;

import java.util.ArrayList;

import ontology.Types.ACTIONS;
import agents.bladerunner.Agent;
import agents.bladerunner.agents.misc.PersistentStorage;

/**
 * Node Class Computes hashcodes and heuristic.
 * 
 * @see {@link PathPlanner} for details.
 */
public class PathPlannerNode implements Comparable<PathPlannerNode> {

	/** The action that lead to this position. */
	public ACTIONS actionToParent;

	/** The parent of this node in the shortest path. */
	public PathPlannerNode parent;

	/** The search depth so far. */
	public int depth;

	/** X and Y coordinates of the point */
	public int x, y;

	/** Accumulated distance so far */
	private double distanceFromStart = 0;

	/** Estimated distance from start to goal. */
	private double totalDistance = 0;

	/** Default constructor */
	public PathPlannerNode() {

	}

	/** Overloaded constructor */
	public PathPlannerNode(int depth, int x, int y) {
		super();
		this.depth = depth;
		this.x = x;
		this.y = y;

	}

	/** Overloaded constructor */
	public PathPlannerNode(ACTIONS actionToParent, PathPlannerNode parent, int depth) {
		super();
		this.actionToParent = actionToParent;
		this.parent = parent;
		this.depth = depth;
		switch (actionToParent) {
		case ACTION_DOWN:
			this.x = parent.x;
			this.y = parent.y - 1;
			break;
		case ACTION_ESCAPE:
			this.x = parent.x;
			this.y = parent.y;
			break;
		case ACTION_LEFT:
			this.x = parent.x + 1;
			this.y = parent.y;
			break;
		case ACTION_NIL:
			this.x = parent.x;
			this.y = parent.y;
			break;
		case ACTION_RIGHT:
			this.x = parent.x - 1;
			this.y = parent.y;
			break;
		case ACTION_UP:
			this.x = parent.x;
			this.y = parent.y + 1;
			break;
		case ACTION_USE:
			this.x = parent.x;
			this.y = parent.y;
			break;
		default:
			break;

		}
	}

	ArrayList<PathPlannerNode> getNeighbors() {
		ArrayList<PathPlannerNode> neighbors = new ArrayList<>();

		if (!PersistentStorage.adjacencyMap.isObstacle(x + 1, y)) {
			neighbors.add(new PathPlannerNode(ACTIONS.ACTION_LEFT, this, depth + 1));
		}

		if (!PersistentStorage.adjacencyMap.isObstacle(x, y + 1)) {
			neighbors.add(new PathPlannerNode(ACTIONS.ACTION_UP, this, depth + 1));

		}

		if (!PersistentStorage.adjacencyMap.isObstacle(x - 1, y)) {
			neighbors.add(new PathPlannerNode(ACTIONS.ACTION_RIGHT, this, depth + 1));
		}

		if (!PersistentStorage.adjacencyMap.isObstacle(x, y - 1)) {
			neighbors.add(new PathPlannerNode(ACTIONS.ACTION_DOWN, this, depth + 1));
		}
		return neighbors;
	}

	// Overloaded constructor
	public PathPlannerNode(PathPlannerNode other) {
		this.x = other.x;
		this.y = other.y;
		this.actionToParent = other.actionToParent;
		this.depth = other.depth;
	}

	@Override
	public boolean equals(Object obj) {
		if (hashCode() != obj.hashCode()) {
			return false;
		}

		return true;
	}

	public ArrayList<ACTIONS> getActionSequence() {
		ArrayList<ACTIONS> seq = new ArrayList<ACTIONS>();
		PathPlannerNode current = this;
		while (true) {
			if (current.actionToParent != null) {
				seq.add(current.actionToParent);
			}
			if (current.parent != null) {
				current = current.parent;
			} else {
				break;
			}
		}
		return seq;
	}

	public void displayActionSequence() {
		System.out.print("PathHBFS::");
		ArrayList<ACTIONS> s = getActionSequence();
		if (Agent.isVerbose) {
			System.out.print("Actions: ");
		}
		for (ACTIONS a : s) {
			if (Agent.isVerbose) {
				System.out.print(a + ";");
			}
		}
		if (Agent.isVerbose) {
			System.out.println();
		}
	}

	// Override the CompareTo function for the HashMap usage
	@Override
	public int hashCode() {
		return this.x + 34245 * this.y;
	}

	@Override
	public int compareTo(PathPlannerNode o) {
		return Double.compare(totalDistance, o.totalDistance);
	}

	public double getDistanceFromGoal() {
		return distanceFromStart;
	}

	public void setDistanceFromStart(double distanceFromStart) {
		this.distanceFromStart = distanceFromStart;
	}

	public double getTotalDistance() {
		return totalDistance;
	}

	public void setTotalDistance(double totalDistance) {
		this.totalDistance = totalDistance;
	}
}