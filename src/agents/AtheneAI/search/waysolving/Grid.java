package agents.AtheneAI.search.waysolving;

import java.util.LinkedList;
import java.util.List;

/**
 * Grid data structure.
 */
public class Grid {

	public GridElement[][] grid;
	private int rows;
	private int columns;

	/**
	 * Initiates the grid with the specified height and width.
	 */
	public Grid(int rows, int columns) {
		this.rows = rows;
		this.columns = columns;
		this.grid = new GridElement[rows][columns];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				this.grid[i][j] = new GridElement(i, j);
			}
		}
	}

	/**
	 * Returns the element at the specified position, 
	 * or null if the coordinates are out of bounds.
	 */
	public GridElement getElementAt(int row, int column) {
		
		if (row >= 0 && row < rows && column >= 0 && column < columns){
			return grid[row][column];
		} else{
			return null;
		}
	}
	
	/**
	 * Returns the horizontal and vertical neighbors of a GridElement.
	 */
	public List<GridElement> getNeighborsOf(GridElement element) {
		LinkedList<GridElement> neighbors = new LinkedList<GridElement>();
		
		if(element.getRow() > 0){
			neighbors.add(getElementAt(element.getRow()-1,element.getColumn()));
		}
		if (element.getRow() < rows-1){
			neighbors.add(getElementAt(element.getRow()+1,element.getColumn()));
		}
		if (element.getColumn() > 0){
			neighbors.add(getElementAt(element.getRow(),element.getColumn()-1));
		}
		if (element.getColumn() < columns-1){
			neighbors.add(getElementAt(element.getRow(),element.getColumn()+1));
		}
		return neighbors;
	}

	/**
	 * Resets all GridElements so that another path can be searched 
	 * on the same grid (i.e. obstacles remain).
	 */
	public void reset() {
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				this.grid[i][j].resetElement();;
			}
		}
	}
	
	/**
	 * Returns width and height (in this order) of the grid in blocks.
	 */
	public int[] getGridDimensions(){
		return new int[]{columns, rows};
	}
	
	/**
	 * Prints the distances for each GridElement.
	 */
	public void printDebugDistances() {
		System.out.println("--------------Distances--------------");
		for (GridElement[] array : grid) {
			System.out.println();
			for (GridElement element : array) {
				System.out.print(element.getDistance() + "\t");
			}
		}
		System.out.println("\n-----------------------------------");
	}
	
	/**
	 * Prints the obstacle ids for each GridElement.
	 */
	public void printObsacles() {
		System.out.println("------------Obstacles----------");
		for (GridElement[] array : grid) {
			System.out.println();
			for (GridElement element : array) {
				System.out.print(element.getObstacleID() + "\t");
			}
		}
		System.out.println("\n----------------------------");
	}
}
