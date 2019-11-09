package agents.bladerunner.agents.misc.pathplanning;

import java.util.ArrayList;
import java.util.PriorityQueue;

import agents.bladerunner.Agent;
import ontology.Types;

/**
 * An implementation of A*
 * 
 * @author Benjamin Ellenberger
 *
 *         //Example use: 
 *         PathPlanner.updateStart(1, 1); // Not necessary, just if you need a way to that position only.
 *         PathPlanner.updateGoal(26,8);
 *
 *         PathPlanner.updateWays();
 *
 *         ArrayList<Types.ACTIONS> path = PathPlanner.getPathToGoal(1, 1);
 *
 *         for (Types.ACTIONS action : path) System.out.println("Next action: "
 *         + action); System.out.println(PathPlanner.getNextStepToGoal(1, 1));
 *         System.out.println(PathPlanner.getStepsQtyToGoal(1,1));
 *         System.out.println(PathPlanner.getMaximumSteps());
 */
public class PathPlanner {

	/** Initial pipe length for quick allocation */
	public int INITIAL_PIPE_LENGTH = 250;

	/** Initial rejection length for quick allocation */
	public int INITIAL_REJECTION_SET_SIZE = 250;

	//
	/** open set */
	public PriorityQueue<PathPlannerNode> pipe = new PriorityQueue<PathPlannerNode>(INITIAL_PIPE_LENGTH);

	/** visited set */
	public ArrayList<PathPlannerNode> visited = new ArrayList<PathPlannerNode>(INITIAL_REJECTION_SET_SIZE);

	/** root node of the search */
	public PathPlannerNode hbfsRoot = null;
	//
	/** Number of elements processed */
	public int processedElementsQty = 0;

	/** goal position */
	private int goalX = 0;
	private int goalY = 0;

	/** start position */
	private int startX = 0;
	private int startY = 0;

	/** If only a single path instead of a full path gradient is needed */
	private boolean onlySinglePathNeeded = false;

	/** If it has found a path at all */
	private boolean pathFound = false;

	private int maximumSteps = -1;
	
	public PathPlanner() {
		cleanHbfs();
	}

	private void cleanHbfs() {
		pipe.clear();
		visited.clear();
		hbfsRoot = null;
		pathFound = false;
		processedElementsQty = 0;
		maximumSteps = -1;
	}

	public void updateStart(int startX, int startY) {
		this.startX = startX;
		this.startY = startY;
	}

	public void updateGoal(int goalX, int goalY) {
		this.goalX = goalX;
		this.goalY = goalY;
	}

	public Types.ACTIONS getNextStepToGoal(int x, int y) {
		for (PathPlannerNode n : visited) {
			if (n.x == x && n.y == y) {
				return n.actionToParent;
			}
		}
		return Types.ACTIONS.ACTION_NIL;
	}

	public double getStepsQtyToGoal(int x, int y) {
		double highestDistance = 0;
		for (PathPlannerNode n : visited) {
			highestDistance = (highestDistance < n.getDistanceFromGoal()) ? n.getDistanceFromGoal() : highestDistance;
			if (n.x == x && n.y == y) {
				return n.getDistanceFromGoal();
			}
		}
		return highestDistance + 1;
	}

	public double getDistanceToGoal(int x, int y) {
		return euclidianDistance(goalX, goalY, x, y);
	}

	public int getMaximumSteps() {
		if (maximumSteps == -1) {
			for (PathPlannerNode n : visited) {
				maximumSteps = (int) ((maximumSteps < n.getDistanceFromGoal()) ? n.getDistanceFromGoal() : maximumSteps);
			}
		}
		return maximumSteps;
	}

	public ArrayList<Types.ACTIONS> getPathToGoal(int x, int y) {
		ArrayList<Types.ACTIONS> path = new ArrayList<>();
		for (PathPlannerNode n : visited) {
			if (n.x == x && n.y == y) {
				path = n.getActionSequence();
			}
		}

		return path;
	}

	private boolean performHbfs() {

		if (pipe.isEmpty()) {
			return true;
		}

		// get the first Node from non-searched Node list, sorted by lowest
		// distance from our goal as guessed by our heuristic
		PathPlannerNode current = pipe.remove();

		// check if our current Node location is the start node. If it is, we
		// are done.
		if (current.x == startX && current.y == startY) {
//			if (Agent.isVerbose) {
//				System.out.println();
//				System.out.println("PathHBFS::Goal found.");
//			}
			pathFound = true;
		}
		if (onlySinglePathNeeded && pathFound) {
			return true;
		}

		// move current node to the closed (already searched) list
		visited.add(current);

		// go through all the current node neighbors and calculate if one should
		// be our next step
		for (PathPlannerNode neighbor : current.getNeighbors()) {

			// calculate how long the path is if we chose this neighbor as the
			// next step in the path
			double neighborDistanceFromStart = current.getDistanceFromGoal() + 1;
			double totalDistance = neighborDistanceFromStart + euclidianDistance(neighbor, startX, startY);

			// if child node has been evaluated and the newer fullDistance is
			// higher, skip
			int i = visited.indexOf(neighbor);
			if (i != -1) {
				neighbor = visited.get(i);
				if (totalDistance >= neighbor.getTotalDistance()) {
					continue;
				}
			}

			for (PathPlannerNode n : pipe) {
				if (n.equals(neighbor)) {
					neighbor = n;
				}
			}

			// if child node is not in queue or new fullDistance is lower
			if ((!pipe.contains(neighbor)) || (totalDistance < neighbor.getTotalDistance())) {

				neighbor.parent = current;
				neighbor.setDistanceFromStart(neighborDistanceFromStart);
				neighbor.setTotalDistance(totalDistance);

				if (!pipe.contains(neighbor)) {
					pipe.add(neighbor);
//					if (Agent.isVerbose) {
//						System.out.print(".");
//					}
				}

			}
		}

//		if (Agent.isVerbose) {
//			System.out.print("|");
//		}
		return false;

	}


	public void displayPathState() {
		displayPathState(null);
	}

	public void displayPathState(PathPlannerNode node) {
		if (node == null) {
			node = pipe.peek();
		}
		if (node == null) {
			if (Agent.isVerbose) {
				System.out.println("PathHBFS::#Pipe Empty");
				System.out.format("PathHBFS::Pipe:%5d|R.Set:%5d|LongestDistance:%3.2f|Speed:%3d", pipe.size(), visited.size(),
						maximumSteps, processedElementsQty);
			}
			return;
		}
		if (Agent.isVerbose) {
			System.out.println();
			System.out.format("PathHBFS::Pipe:%5d|R.Set:%5d|Depth:%3d|TotDistance:%3.2f|Speed:%3d", pipe.size(), visited.size(),
					node.depth, node.getTotalDistance(), processedElementsQty);
		}
	}

	/**
	 * Picks an action. This function is called every game step to request an
	 * action from the player.
	 * 
	 * @param so
	 *            Observation of the current state.
	 * @param elapsedTimer
	 *            Timer when the action returned is due.
	 * @return An action for the current state
	 */
	public void updateWays() {
		// clean up from previous runs
		cleanHbfs();

		// add goal node to the pipe
		hbfsRoot = new PathPlannerNode(0, goalX, goalY);
		hbfsRoot.setDistanceFromStart(0);
		hbfsRoot.setTotalDistance(euclidianDistance(hbfsRoot, startX, startY));
		pipe.add(hbfsRoot);

		// if the search has terminated
		boolean hasTerminated = false;

		while (!hasTerminated) {
			hasTerminated = performHbfs();
			processedElementsQty++;
		}
//		System.out.println();
//		displayPathState();
//		System.out.println();
	}

	/**
	 * Euclidean cost between state a and state b
	 */
	@SuppressWarnings("unused")
	private double euclidianDistance(PathPlannerNode a, PathPlannerNode b) {
		float x = a.x - b.x;
		float y = a.y - b.y;
		return Math.sqrt(x * x + y * y);
	}

	/**
	 * Euclidean cost between state a and and a position
	 */
	private double euclidianDistance(PathPlannerNode a, int goalX, int goalY) {
		float x = a.x - goalX;
		float y = a.y - goalY;
		return Math.sqrt(x * x + y * y);
	}

	/**
	 * Euclidean cost between state a and and a position
	 */
	private double euclidianDistance(int startX, int startY, int goalX, int goalY) {
		float x = startX - goalX;
		float y = startY - goalY;
		return Math.sqrt(x * x + y * y);
	}

	public boolean isOnlySinglePathNeeded() {
		return onlySinglePathNeeded;
	}

	public void setOnlySinglePathNeeded(boolean onlySinglePathNeeded) {
		this.onlySinglePathNeeded = onlySinglePathNeeded;
	}

	public boolean hasPathFound() {
		return pathFound;
	}
}
