package agents.bladerunner.agents.misc;

import java.util.ArrayList;

import ontology.Types.ACTIONS;

import tools.Vector2d;
import core.game.Observation;
import core.game.StateObservation;

public class AdjacencyMap {

	private boolean[][] adjacencyMap;
	private int adjacencyMapWidth;
	private int adjacencyMapHeight;
	private int blockSize;

	/**
	 * Initialize Adjacency map with state observation, it automatically gets
	 * the World dimensions and sets all positions
	 * 
	 * @param StateObservation
	 * 
	 */
	public AdjacencyMap(StateObservation stateObs) {

		ArrayList<Observation>[][] grid = stateObs.getObservationGrid();
		adjacencyMapWidth = grid.length;
		adjacencyMapHeight = grid[0].length;
		adjacencyMap = new boolean[adjacencyMapWidth][adjacencyMapHeight];

		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				for (Observation o : grid[i][j]) {
					if (adjacencyMap[i][j] == true) {
						break;
					}
					switch (o.itype) {
					case 0:
						adjacencyMap[i][j] = false; // Avatar
					case 1:
						adjacencyMap[i][j] = true; // Walls
						break;
					default:
						adjacencyMap[i][j] = false; // Others
						break;
					}

				}
			}
		}
	}

	public void print() {
		System.out.print(" ");
		for (int i = 0; i < adjacencyMapWidth; i++) {
			System.out.print(i%10);
		}
		System.out.println();
		
		for (int j = 0; j < adjacencyMapHeight; j++) {
			System.out.print(j%10);
			for (int i = 0; i < adjacencyMapWidth; i++) {
				System.out.print(adjacencyMap[i][j] ? "#" : " ");
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
	}

	public boolean[][] getRawAdjacencyMap() {
		return adjacencyMap;
	}

	public boolean isActionPossible(int X, int Y, ACTIONS action) {
		switch (action) {
		case ACTION_DOWN:
			return isObstacle(X, Y + 1);
		case ACTION_ESCAPE:
			return true; // TODO: I think you can always do that? What is escape actually?
		case ACTION_LEFT:
			return isObstacle(X - 1, Y);
		case ACTION_NIL:
			return true;
		case ACTION_RIGHT:
			return isObstacle(X + 1, Y);
		case ACTION_UP:
			return isObstacle(X, Y - 1);
		case ACTION_USE:
			return true;
		default:
			break;

		}
		return false;
	}

	public boolean isActionPossibleFromWorldPixelPos(double pixelX,
			double pixelY, ACTIONS action) {
		int X = floorDiv((int) (pixelX + 0.1), blockSize);
		int Y = floorDiv((int) (pixelY + 0.1), blockSize);
		return isActionPossible(X, Y, action);
	}

	public boolean isActionPossibleFromWorldPosition(Vector2d pos,
			ACTIONS action) {
		return isActionPossibleFromWorldPixelPos(pos.x, pos.y, action);
	}

	public boolean isObstacle(int X, int Y) {
		if (X < 0 || Y < 0 || X >= adjacencyMapWidth || Y >= adjacencyMapHeight) {
			return true;
		}
		return adjacencyMap[X][Y];
	}

	public boolean getObstaclewithWorldPixelPos(int pixelX, int pixelY) {
		int X = floorDiv((int) (pixelX + 0.1), blockSize);
		int Y = floorDiv((int) (pixelY + 0.1), blockSize);
		return isObstacle(X, Y);
	}

	public boolean getObstacleAtWorldPosition(Vector2d posVec) {
		int X = floorDiv((int) (posVec.x + 0.1), blockSize);
		int Y = floorDiv((int) (posVec.y + 0.1), blockSize);
		return isObstacle(X, Y);
	}

	public void setObstacle(int X, int Y, boolean value) {
		if (X >= 0 && Y >= 0 && X < adjacencyMapWidth && Y < adjacencyMapHeight) {
			adjacencyMap[X][Y] = value;
		}
	}

	public void setObstacleAtWorldPixelPos(double pixelX, double pixelY,
			boolean value) {
		int X = floorDiv((int) (pixelX + 0.1), blockSize);
		int Y = floorDiv((int) (pixelY + 0.1), blockSize);
		setObstacle(X, Y, value);
	}

	public void setObstacleAtWorldPosition(Vector2d posVec, boolean value) {
		setObstacleAtWorldPixelPos(posVec.x, posVec.y, value);
	}

	public void addOtherMap(AdjacencyMap mapToAdd) {
		boolean[][] addMap = mapToAdd.getRawAdjacencyMap();
		for (int i = 0; i < adjacencyMapWidth; i++) {
			for (int j = 0; j < adjacencyMapHeight; j++) {
				adjacencyMap[i][j] |= addMap[i][j];
			}
		}
	}

	public void subtractOtherMap(AdjacencyMap mapToSubtract) {
		boolean[][] addMap = mapToSubtract.getRawAdjacencyMap();
		for (int i = 0; i < adjacencyMapWidth; i++) {
			for (int j = 0; j < adjacencyMapHeight; j++) {
				adjacencyMap[i][j] &= !addMap[i][j];
			}
		}
	}

	/**
	 * Returns the largest (closest to positive infinity) long value that is
	 * less than or equal to the algebraic quotient. There is one special case,
	 * if the dividend is the Long.MIN_VALUE and the divisor is -1, then integer
	 * overflow occurs and the result is equal to the Long.MIN_VALUE. Normal
	 * integer division operates under the round to zero rounding mode
	 * (truncation). This operation instead acts under the round toward negative
	 * infinity (floor) rounding mode. The floor rounding mode gives different
	 * results than truncation when the exact result is negative.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int floorDiv(int x, int y) {
		int r = x / y;
		// if the signs are different and modulo not zero, round down
		if ((x ^ y) < 0 && (r * y != x)) {
			r--;
		}
		return r;
	}

}
