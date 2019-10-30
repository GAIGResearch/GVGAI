package agents.AtheneAI.search.waysolving;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import agents.AtheneAI.heuristics.Knowledge;
import agents.AtheneAI.util.Util;

/**
 * Finds the shortest path between two cells of a grid. Considers what objects
 * are on the cells (e.g. walls).
 */
public class A_Star {
	private LinkedList<GridElement> queue;
	private GridElement startElement;
	private GridElement endElement;
	private Grid grid;
	private boolean moveMovables;
	private boolean validActionSequence;
	private int agentType;
	private ElapsedCpuTimer timer;
	private int limit;

	/**
	 * Constructor
	 */
	public A_Star(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, int limit) {
		grid = new Grid(stateObs.getWorldDimension().height / stateObs.getBlockSize(),
				stateObs.getWorldDimension().width / stateObs.getBlockSize());
		queue = new LinkedList<GridElement>();
		startElement = null;
		endElement = null;
		moveMovables = false;
		validActionSequence = false;
		agentType = Util.getAgentType(stateObs);
		timer = elapsedTimer;
		this.limit = limit;

		updateGrid(stateObs);
	}

	/**
	 * Fills the Grid with the information in the given StateObservation.
	 */
	private void updateGrid(StateObservation stateObs) {

		ArrayList<Observation>[] immovables = stateObs.getImmovablePositions();
		if (immovables != null) {
			for (ArrayList<Observation> observations : immovables) {
				if (observations != null) {
					for (Observation observation : observations) {
						GridElement obstacle = grid.getElementAt(
								(int) (observation.position.y / stateObs.getBlockSize()),
								((int) observation.position.x / stateObs.getBlockSize()));
						if (obstacle != null)
							obstacle.setObstacleID(observation.itype);
					}
				}
			}
		}

		ArrayList<Observation>[] movables = stateObs.getMovablePositions();
		if (movables != null) {
			for (ArrayList<Observation> observations : movables) {
				if (observations != null) {
					for (Observation observation : observations) {
						GridElement obstacle = grid.getElementAt(
								(int) (observation.position.y / stateObs.getBlockSize()),
								((int) observation.position.x / stateObs.getBlockSize()));
						if (obstacle != null)
							obstacle.setObstacleID(observation.itype);
					}
				}
			}
		}
	}

	/**
	 * Tries to find the shortest path from start to target.
	 * 
	 * @param moveMovables
	 *            whether the agent is allowed to move objects on its way
	 * @return true if the problem was solved, false otherwise.
	 */
	public boolean solve(int startX, int startY, int targetX, int targetY, boolean moveMovables, boolean doNotBreak) {
		this.moveMovables = moveMovables;

		// delete information of previous paths (TODO: needed??)
		grid.reset();
		queue = new LinkedList<GridElement>();

		endElement = grid.getElementAt(targetY, targetX);
		endElement.setEnd(true);
		startElement = grid.getElementAt(startY, startX);
		startElement.setStart(true);
		startElement.setDistance(0);
		startElement.setVisited(true);

		queue.add(startElement);
		while (!queue.isEmpty() && timer.remainingTimeMillis() > limit) {

			GridElement minDistanceElement = removeMinFromQueue();

			if (minDistanceElement.isEnd() && !doNotBreak) {
				validActionSequence = true;
				return true;
			}

			expandNode(minDistanceElement);
		}
		validActionSequence = false;
		return false;
	}

	/**
	 * Returns the sequence of actions to go the shortest path from start to target.
	 */
	public List<ACTIONS> getActionSequence(boolean doubleMoves, ACTIONS initDirection) {
		LinkedList<ACTIONS> actions = new LinkedList<ACTIONS>();

		// return empty path if there was no path found in solve()
		if (!validActionSequence) {
			return actions;
		}

		GridElement current = endElement;
		GridElement previous = endElement.getPredecessor();

		// return a path also when actually no action must be performed!
		if (current.isStart() && current.isEnd()) {
			actions.add(ACTIONS.ACTION_NIL);
			return actions;
		}

		// extract (reversed) actions from found path
		LinkedList<ACTIONS> reversedActions = new LinkedList<ACTIONS>();
		while (previous != null) {
			if (current.getColumn() < previous.getColumn()) {
				reversedActions.add(ACTIONS.ACTION_LEFT);
			} else if (current.getColumn() > previous.getColumn()) {
				reversedActions.add(ACTIONS.ACTION_RIGHT);
			} else if (current.getRow() < previous.getRow()) {
				reversedActions.add(ACTIONS.ACTION_UP);
			} else if (current.getRow() > previous.getRow()) {
				reversedActions.add(ACTIONS.ACTION_DOWN);
			} else {
				reversedActions.add(ACTIONS.ACTION_NIL);
			}

			// insert use action if needed
			if (Knowledge.getWayCosts(agentType, current.getObstacleID()) == Knowledge.WAY_COSTS_USE) {
				reversedActions.add(ACTIONS.ACTION_USE);
			}

			current = previous;
			previous = previous.getPredecessor();
		}

		// bring reversed actions in the right order and insert double moves if needed
		ACTIONS currentDirection = initDirection;
		ACTIONS nextDirection = null;
		for (int i = reversedActions.size() - 1; i >= 0; i--) {

			ACTIONS currentAction = reversedActions.get(i);

			if (doubleMoves) {

				if (currentAction == ACTIONS.ACTION_USE) {
					nextDirection = reversedActions.get(i - 1);
				} else {
					nextDirection = currentAction;
				}

				// detect change in direction
				if (currentDirection != null && currentDirection != nextDirection) {
					actions.add(nextDirection);
					currentDirection = nextDirection;
				}
			}

			actions.add(currentAction);
		}

		return actions;
	}

	/**
	 * Adds neighbors of visited GridElements to the queue.
	 */
	private void expandNode(GridElement minDistanceElement) {

		minDistanceElement.setVisited(true);

		// Insert all neighbors of visited GridElement to the queue
		for (GridElement neighbor : grid.getNeighborsOf(minDistanceElement)) {

			// if neighbor was already visited -> move to next neighbor
			if (neighbor.getVisited()) {
				continue;
			}

			// estimate the expected distance to target when visiting this neighbor
			int distance = Knowledge.getWayCosts(agentType, neighbor.getObstacleID())
					+ minDistanceElement.getDistance();

			// neighbor already has a smaller distance -> move to next neighbor
			if (neighbor.getDistance() != GridElement.DISTANCE_NOT_SET && neighbor.getDistance() < distance) {
				continue;
			}

			// neighbor has no distance yet -> must be inserted to queue
			if (neighbor.getDistance() == GridElement.DISTANCE_NOT_SET) {

				// agent cannot go through blocked fields (unless it is the target, i.e. we only
				// assume it is blocked)
				if ((neighbor.isEnd()
						|| Knowledge.getWayCosts(agentType, neighbor.getObstacleID()) != Knowledge.WAY_COSTS_BLOCKED)

						// agent should only go through movables (push them) if this is allowed
						&& (moveMovables || Knowledge.getWayCosts(agentType,
								neighbor.getObstacleID()) != Knowledge.WAY_COSTS_MOVABLE)) {

					queue.add(neighbor);
				}

			}

			// update info in GridElement
			neighbor.setDistance(distance);
			neighbor.setPredecessor(minDistanceElement);
		}
	}

	/**
	 * Resturns the element in the queue with the least expected distance to the end
	 * element.
	 */
	private GridElement removeMinFromQueue() {
		int minIndex = 0;
		int minDistance = queue.get(0).getDistance() + manhattanDistance(queue.get(0), endElement);

		// search queue for GridElement with minimal expected distance
		for (int i = 1; i < queue.size(); i++) {

			int tempDistance = queue.get(i).getDistance() + manhattanDistance(queue.get(i), endElement);

			if (tempDistance < minDistance) {
				minIndex = i;
				minDistance = tempDistance;
			}
		}
		GridElement result = queue.get(minIndex);
		queue.remove(minIndex);
		return result;
	}

	/**
	 * Calculates the Manhattan Distance (in cells) between a and b.
	 */
	private int manhattanDistance(GridElement a, GridElement b) {
		return Math.abs(a.getColumn() - b.getColumn()) + Math.abs(a.getRow() - b.getRow());
	}

	public Grid getGrid() {
		return grid;
	}

}
