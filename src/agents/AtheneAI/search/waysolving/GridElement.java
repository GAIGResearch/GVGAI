package agents.AtheneAI.search.waysolving;

/**
 * GridElement data structure.
 */
public class GridElement {
	public final static int DISTANCE_NOT_SET = -1;
	public final static int NO_OBSTACLE = -1;

	private int obstacleID;
	private int row;
	private int column;
	private int distance;
	private boolean isStart;
	private boolean isEnd;
	private boolean visited;
	private GridElement predecessor;

	/**
	 * Initiates the grid element with the specified row and column. 
	 * Sets other fields to default.
	 */
	public GridElement(int row, int column) {
		this.row = row;
		this.column = column;
		this.obstacleID = NO_OBSTACLE;
		this.distance = DISTANCE_NOT_SET;
		this.visited = false;
		this.isStart = false;
		this.isEnd = false;
		this.predecessor= null;
	}
	
	/**
	 * Resets the element so that another path can be searched 
	 * on the same grid (i.e. obstacle remains).
	 */
	public void resetElement() {
		this.distance = DISTANCE_NOT_SET;
		this.visited = false;
		this.predecessor= null;
		this.isStart = false;
		this.isEnd = false;
	}

	public GridElement getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(GridElement pre) {
		this.predecessor = pre;
	}
	
	public int getObstacleID() {
		if (isStart || isEnd) {
			return GridElement.NO_OBSTACLE; // TODO: That is not neccessarily the problem!!!
		}
		return obstacleID;
	}

	public void setObstacleID(int id) {
		this.obstacleID = id;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public int getDistance() {
		return distance;
	}

	public void setVisited(boolean b) {
		this.visited = b;	
	}

	public boolean getVisited() {
		return visited;
	}

	public void setStart(boolean b) {
		this.isStart = b;	
	}

	public boolean isStart() {
		return isStart;
	}

	public void setEnd(boolean b) {
		this.isEnd = b;	
	}

	public boolean isEnd() {
		return isEnd;
	}
	
}