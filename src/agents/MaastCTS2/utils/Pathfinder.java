package agents.MaastCTS2.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import agents.MaastCTS2.Globals;

/**
 * A*-based pathfinder which computes connectivity of the map upon initialization so that it
 * can instantly tell when two cells are not connected.
 * 
 * <p> Only considers objects of the ''wall'' type (type = 0) to be obstacles. Some other objects
 * could in fact also be obstacles, but that would need to be learned by the agent at runtime, which
 * is difficult.
 * 
 * <p> Uses the Manhattan Distance as a heuristic, because the player only has access to lateral
 * direction moves. May not be entirely accurate, since I believe physics could also result in diagonal
 * movements, but seems good enough. 
 * 
 * <p> Does not actually memorize and return paths since I don't expect to require that functionality,
 * but only uses pathfinding algorithms to determine the LENGTH of the shortest path
 * 
 * <p> TODO Rectangular Symmetry Reduction seems like it would be a useful optimization to implement
 * if I find time. Also looked into JPS / JPS+, but those do not seem to be directly applicable under
 * the assumption of a 4-connected grid (as opposed to an 8-connected grid).
 * 
 * <p> Goal Bounding may be interesting to look at too, but it has an expensive preprocessing step which
 * we may not be able to safely fit within a single second of init time on all levels.
 *
 * @author Dennis Soemers
 */
public class Pathfinder {
	
	/**
	 * A node in our pathfinding graph. This graph will only contain traversable nodes
	 * (during initialization we'll detect non-traversable cells and not create nodes for them)
	 *
	 * @author Dennis Soemers
	 */
	private static class GridNode{
		public final int x;
		public final int y;
		
		public GridNode[] neighbors;
		
		public GridNode(int x, int y){
			this.x = x;
			this.y = y;
			this.neighbors = null;
		}
	}
	
	/**
	 * A node for the A* search algorithm, to place in the ''Open List''
	 * 
	 * <p> Consists of a GridNode (representation of the location), and
	 * a cost so far (the ''g'' cost)
	 *
	 * @author Dennis Soemers
	 */
	private static class AStarNode implements Comparable<AStarNode>{
		public final GridNode position;
		public final int g;
		public final int f;
		
		public AStarNode(GridNode position, int g, int f){
			this.position = position;
			this.g = g;
			this.f = f;
		}
		
		@Override
	    public int compareTo(AStarNode n){
			if(f < n.f){
				// we have a smaller f score
				return -1;
			}
			else if(f == n.f){
				// equal f score; break ties according to g
				if(g > n.g){
					// larger g-part, so we're closer
					return -1;
				}
				else if(g < n.g){
					// smaller g-part, so we're further away
					return 1;
				}
			}
			else{
				// larger f score
				return 1;
			}
			
			return 0;
	    }
	}
	
	/**
	 * A connectivity map where, if 2 cells have the same value, they are connected to each other.
	 * Connectivity in this context means that it is possible from one cell to reach the other, NOT
	 * necessarily that they are direct neighbors.
	 * 
	 * <p> A value of 0 means not set (which should not be possible after preprocessing), and a negative
	 * value means that it is a wall.
	 */
	private int[][] connectivityMap;
	
	/**
	 * Grid of nodes to use for pathfinding. We'll put null in cells containing walls
	 */
	private GridNode[][] nodeGrid;
	
	/** 
	 * If true, we need to use A*. If false, we can simply return Manhattan distance
	 * <br> Should only be false in cases where we didn't detect any obstacles at all except for
	 * the outside boundaries of the map
	 */
	private boolean pathfinderNecessary;
	
	/** Useful for drawing when debugging */
	//static ArrayList<Color> randomColors = new ArrayList<Color>();
	
	/**
	 * Performs some preprocessing. TODO currently assuming we can safely fit our preprocessing in the
	 * 1 second we have for initialization, so not keeping track of time here. Should check if this is
	 * reasonable, and think of what to do if there is a risk of running out of time
	 * 
	 * @param stateObs
	 */
	public void init(StateObservation stateObs){
		ArrayList<Observation>[][] obsGrid = stateObs.getObservationGrid();
		final int width = obsGrid.length;
		final int height = obsGrid[0].length;
		final int maxIdxX = width - 1;
		final int maxIdxY = height - 1;
		connectivityMap = new int[width][height];
		pathfinderNecessary = false;	// we'll set this to true if we find obstacles
		
		// compute connectivity
		int nextConnectivityValue = 1;
		
		// find the next cell for which connectivity has not been set yet
		for(int i = 0; i < width; ++i){
			for(int j = 0; j < height; ++j){
				if(connectivityMap[i][j] == 0){
					// connectivity not set yet, so start a floodfill from this cell
					ArrayList<OrderedIntPair> cells = new ArrayList<OrderedIntPair>();
					cells.add(new OrderedIntPair(i, j));
					
					while(!cells.isEmpty()){
						OrderedIntPair cell = cells.remove(cells.size() - 1);
						int x = cell.first;
						int y = cell.second;
						
						if(containsWall(obsGrid, x, y)){
							// found a wall, so set connectivity negative and don't add children
							connectivityMap[x][y] = -1;
							
							if(x > 0 && x < maxIdxX && y > 0 && y < maxIdxY){
								// this wall is not on the outside boundary of the level, so we'll need pathfinding
								pathfinderNecessary = true;
							}
						}
						else{
							// not a wall; set connectivity
							connectivityMap[x][y] = nextConnectivityValue;
							
							// add neighbors							
							if(x > 0){
								if(connectivityMap[x - 1][y] == 0){
									// connectivity of this neighbor hasn't already been set
									cells.add(new OrderedIntPair(x - 1, y));
								}
							}
							
							if(y < maxIdxY){
								if(connectivityMap[x][y + 1] == 0){
									// connectivity of this neighbor hasn't already been set
									cells.add(new OrderedIntPair(x, y + 1));
								}
							}
							
							if(x < maxIdxX){
								if(connectivityMap[x + 1][y] == 0){
									// connectivity of this neighbor hasn't already been set
									cells.add(new OrderedIntPair(x + 1, y));
								}
							}
							
							if(y > 0){
								if(connectivityMap[x][y - 1] == 0){
									// connectivity of this neighbor hasn't already been set
									cells.add(new OrderedIntPair(x, y - 1));
								}
							}
						}
					}
					
					// finished floodfill; increment connectivity value for the next area if we had at least one non-wall
					if(connectivityMap[i][j] > 0){
						++nextConnectivityValue;
					}
				}
			}
		}
		
		// connectivity has been computed; now create nodes
		nodeGrid = new GridNode[width][height];
		for(int x = 0; x < width; ++x){
			for(int y = 0; y < height; ++y){
				if(connectivityMap[x][y] > 0){
					// not a wall here
					nodeGrid[x][y] = new GridNode(x, y);
				}
			}
		}
		
		// now that we have a grid of nodes, tell each of them which nodes are their neighbors, so that
		// we do not need to compute that every time over while pathfinding
		for(int x = 0; x < width; ++x){
			for(int y = 0; y < height; ++y){
				GridNode node = nodeGrid[x][y];
				
				if(node != null){	// a non-wall location
					int connectivity = connectivityMap[x][y];
					
					boolean left = (x > 0) && (connectivityMap[x - 1][y] == connectivity);
					boolean right = (x < maxIdxX) && (connectivityMap[x + 1][y] == connectivity);
					boolean down = (y > 0) && (connectivityMap[x][y - 1] == connectivity);
					boolean up = (y < maxIdxY) && (connectivityMap[x][y + 1] == connectivity);
					
					int numNeighbors = 0;
					if(left) ++numNeighbors;
					if(right) ++numNeighbors;
					if(down) ++numNeighbors;
					if(up) ++numNeighbors;
					
					node.neighbors = new GridNode[numNeighbors];
					
					int idx = 0;
					if(left) node.neighbors[idx++] = nodeGrid[x - 1][y];
					if(right) node.neighbors[idx++] = nodeGrid[x + 1][y];
					if(down) node.neighbors[idx++] = nodeGrid[x][y - 1];
					if(up) node.neighbors[idx++] = nodeGrid[x][y + 1];
				}
			}
		}
	}
	
	/**
	 * Computes the distance for moving from the given start cell to the given destination.
	 * 
	 * <p> Returns the Manhattan distance in cases where a pathfinding algorithm cannot be used
	 * (for instance, because one or both of the given cells lie outside our grid).
	 * 
	 * <p> Returns the maximum possible Manhattan distance on the map when the two given cells
	 * are known not to be connected.
	 * 
	 * <p> Returns the Manhattan distance quickly if the level is considered not to have any
	 * movement-blocking obstacles (which should be exactly the correct distance in such a case)
	 * 
	 * <p> In all other cases, uses A* to compute the distance.
	 * 
	 * <p> Will return the given maxAllowedDistance as soon as it is known that no path shorter than maxAllowedDistance
	 * will be found.
	 * 
	 * <p> TODO not taking into account portals which may result in shorter distances than Manhattan distance
	 * <br> TODO not yet keeping track of processing time, may want to return partial results earlier if computation
	 * takes long (but I expect to be able to implement A* well enough to have a negligible computation time)
	 * <br> TODO could consider caching previously found paths (remember that path from s to g has same cost has path
	 * from g to s)
	 * 
	 * @param startCell
	 * @param destination
	 * @param maxAllowedDistance
	 * @param blockedGrid
	 * @return
	 */
	public int computeDistance(OrderedIntPair startCell, OrderedIntPair destination, int maxAllowedDistance, boolean[][] blockedGrid){
		final int startX = startCell.first;
		final int startY = startCell.second;
		final int goalX = destination.first;
		final int goalY = destination.second;
		
		final int manhattanDist = Globals.manhattanDistance(startCell, destination);
		final int mapWidth = nodeGrid.length;
		final int mapHeight = nodeGrid[0].length;
		
		final int worstCaseDist = mapWidth * mapHeight;
		
		if(manhattanDist >= maxAllowedDistance){
			// not gonna find an improvement here
			return manhattanDist;
		}
		
		if(startX < 0 || startY < 0 || goalX < 0 || goalY < 0 ||
			startX >= mapWidth || startY >= mapHeight || goalX >= mapWidth || goalY >= mapHeight){
			// if either of the cells is not in our grid, return twice manhattan distance as heuristic (twice to punish moving out of map)
			return 2 * manhattanDist;
		}
		
		if(connectivityMap[startX][startY] != connectivityMap[goalX][goalY]){
			// the start is not connected to the goal
			return 2 * worstCaseDist;
		}
		
		if(connectivityMap[startX][startY] <= 0){
			// connectivity of start position is either not set, or is a wall
			// either case really shouldn't be possible, but to be safe, checking here
			return 2 * worstCaseDist;
		}
		
		if(!pathfinderNecessary){
			// we didn't find any obstacles inside the level, so no need to do pathfinding
			return manhattanDist;
		}
		
		// ---------------------------------------------------------
		// that's the end of all the special checks, time to do A*
		// ---------------------------------------------------------
		
		// a stack (LIFO queue) that we'll push nodes to that have an equal f-cost to their parent node
		// this is the highest-priority part of the ''Open List''
		ArrayList<AStarNode> fastStack = new ArrayList<AStarNode>();
		fastStack.add(new AStarNode(nodeGrid[startX][startY], 0, manhattanDist));
		
		// some unsorted queues, where a bin indexed by 0 < i < 5 contains ''open'' nodes with an
		// f-cost equal to f_0 + i, where f_0 is the heuristic cost of the start node
		// index 0 will not be used, since such elements automatically only appear on the fast stack
		// 
		// as soon as we would need a queue at index 5, we'll instead shift all the stacks to the left.
		// we probably don't really need 5 stacks, not sure, but afraid of off-by-one errors. This should be safe?
		@SuppressWarnings("unchecked")
		ArrayDeque<AStarNode>[] openBins = new ArrayDeque[5];
		for(int i = 1; i < openBins.length; ++i){
			openBins[i] = new ArrayDeque<AStarNode>();
		}
		int binOffset = 0;
		
		// we'll assume maps are kinda small-ish, so it's fine to just use an entire grid for the closed list
		boolean[][] closedList = new boolean[mapWidth][mapHeight];
		
		// not counting nodes in fastStack since we don't mind if fastStack grows big
		int numOpenNodes = 0;
		
		//System.out.println();
		while(true){
			// the next node to look at
			AStarNode current = null;

			if(!fastStack.isEmpty()){
				// try fast stack first
				current = fastStack.remove(fastStack.size() - 1);
				//System.out.println("popped from fast stack");
			}
			else{
				// next try our bins
				for(int i = 1; i < openBins.length; ++i){
					if(!openBins[i].isEmpty()){
						current = openBins[i].pop();
						--numOpenNodes;
						//System.out.println("popped from bin " + i);
						//System.out.println("current f = g + h = " + current.g + " + " + (current.f - current.g));
						break;
					}
				}
			}
			
			if(current == null){
				// no remaining open nodes
				break;
			}
			
			GridNode currentPos = current.position;
			int currentX = currentPos.x;
			int currentY = currentPos.y;
			
			if(currentX == goalX && currentY == goalY){
				// found the goal
				return current.f;
			}
			
			if(Globals.manhattanDistance(destination, new OrderedIntPair(currentX, currentY)) <= 1){
				// we'll count this as finding the goal too, in case our goal is a blocked cell
				return current.f + 1;	// + 1 because it's an adjacent cell, and not actually the goal yet
			}
			
			if(current.f >= maxAllowedDistance){
				// won't find a better distance anymore
				return maxAllowedDistance;
			}
			
			if(numOpenNodes > 30 && current.f > 2 * manhattanDist && current.f > manhattanDist + 5){
				// any path we'll find will be at least twice as long as the manhattan distance,
				// and at least 5 steps longer than manhattan distance
				// this is scary because it is likely the location is unreachable due to non-wall obstacles,
				// and we don't want to spend time pathfinding over the entire map, so we'll just return
				//
				// the check for numOpenNodes is there because, in games like labyrinth, we actually don't
				// mind long paths, we'll still find them quickly because our open list doesn't grow big because
				// there are very few traversable cells
				return worstCaseDist + manhattanDist;
			}
			
			// close the current node
			closedList[currentX][currentY] = true;
			
			// all of our neighbors will share this g-cost
			final int neighborG = current.g + 1;
			
			for(GridNode neighbor : currentPos.neighbors){
				int neighborX = neighbor.x;
				int neighborY = neighbor.y;
				
				if(blockedGrid[neighborX][neighborY]){
					// skipping this neighbor since it seems to be blocked by a non-wall object
					continue;
				}
				
				if(!closedList[neighborX][neighborY]){	// we assume our heuristic is consistent, so never need to re-visit closed nodes
					int neighborF = neighborG + Globals.manhattanDistance(new OrderedIntPair(neighborX, neighborY), destination);
					AStarNode newNode = new AStarNode(neighbor, neighborG, neighborF);
					
					if(neighborF == current.f){
						// this node can go to the fast stack
						fastStack.add(newNode);
					}
					else{
						// try to put the node in one of our bins
						int bin = neighborF - manhattanDist - binOffset;
						
						if(bin < openBins.length){
							openBins[bin].add(newNode);
							++numOpenNodes;
							//System.out.println("neighbor f = g + h = " + newNode.g + " + " + (newNode.f - newNode.g));
							//System.out.println("inserted node to bin " + bin);
						}
						else{
							//System.out.println("neighbor f = g + h = " + newNode.g + " + " + (newNode.f - newNode.g));
							//System.out.println("wanted to insert node in bin " + bin);
							
							int binIdx = 1;
							
							while(binIdx < openBins.length && openBins[binIdx].isEmpty()){
								++binIdx;
							}
							
							//System.out.println("bin " + binIdx + " not empty!");
							
							// binIdx should never be 1 here because we have uniform-cost grid with costs of 1.
							// not checking to verify it because I have no idea what we'd do if it happened anyway
							
							// binIdx now points to the first non-empty bin. We'll shift it and all bins after it to the ''left''
							for(int i = binIdx; i < openBins.length; ++i){
								openBins[i - binIdx + 1] = openBins[i];
								openBins[i] = new ArrayDeque<AStarNode>(openBins[i].size());
								//System.out.println("shifted bin " + i + " to bin " + (i - binIdx + 1));
							}
							
							//System.out.println("binOffset was " + binOffset);
							binOffset += (binIdx - 1);
							//System.out.println("set binOffset to " + binOffset);
							
							bin = neighborF - manhattanDist - binOffset;
							openBins[bin].add(newNode);
							++numOpenNodes;
							//System.out.println("inserted node to bin " + bin);
						}
					}
				}
			}
		}
		
		// if we reach this point the object must be unreachable because it is blocked by non-wall objects
		// in case all objects are unreachable, we'll prefer getting as close as possible, therefore
		// add manhattanDist to the worstCaseDist
		return worstCaseDist + manhattanDist;
	}
	
	public boolean isPathfinderNecessary(){
		return pathfinderNecessary;
	}
	
	/**
	 * Forces the pathfinder to be considered necessary, even if no obstacles were observed anywhere
	 */
	public void setPathfinderNecessary(){
		pathfinderNecessary = true;
	}
	
	/**
	 * Returns true iff the cell with the given x and y coordinates contains a wall in the given
	 * observation grid.
	 * 
	 * @param obsGrid
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean containsWall(ArrayList<Observation>[][] obsGrid, int x, int y){
		for(Observation obs : obsGrid[x][y]){
			if(obs.itype == 0){		// a wall
				return true;
			}
		}
		
		return false;
	}
	
	/*public void drawConnectivity(Graphics2D g){
		for(int x = 0; x < connectivityMap.length; ++x){
			for(int y = 0; y < connectivityMap[0].length; ++y){
				int connectivity = connectivityMap[x][y];
				
				Color color = new Color(255, 255, 255, 200);
				if(connectivity < 0){
					color = new Color(0, 0, 0, 150);
				}
				else{
					while(randomColors.size() <= connectivity){
						// create a new random color
						randomColors.add(new Color((int)(Math.random() * 255),
												(int)(Math.random() * 255),
												(int)(Math.random() * 255),
												150));
					}
					
					color = randomColors.get(connectivity);
				}
				g.setColor(color);
				
				Vector2d pos = Globals.knowledgeBase.cellToPosition(x,  y);
				g.fillRect((int) (pos.x), (int) (pos.y), 
						Globals.knowledgeBase.getPixelsPerBlock(), Globals.knowledgeBase.getPixelsPerBlock());
				
				g.setColor(Color.BLACK);
				//g.drawString("" + connectivity, (int) (pos.x), (int) (pos.y));
			}
		}
	}*/

}
