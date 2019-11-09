package agents.ICELab.OpenLoopRLBiasMCTS;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import core.game.Observation;
import core.game.StateObservation;
import agents.ICELab.Agent;
import agents.ICELab.OpenLoopRLBiasMCTS.Memory.MemoryItem;
public class Pathfinder {
	public int[] portalEntryTypes;
	private HashMap<Integer, ArrayList<MapNode>> portalExitMap = new HashMap<Integer, ArrayList<MapNode>>();;
	private ArrayList<Observation>[][] objs;
	private ArrayList<MapNode> open;
	private ArrayList<MapNode> close;
	private MapNode[][] mapNodes;
	private int width;
	private int height;
	private StateObservation so;

	public int getAStarDistance(StateObservation so, Point target){
		if (!mapNodes[target.x][target.y].getReachable())
			return -1;
		Point avatarPoint = Utils.toTileCoord(so.getAvatarPosition());
		Path path = findPath(avatarPoint.x, avatarPoint.y, target.x, target.y, so);
		if (path == null)
			return -1;
		return path.getLength();
	};
	public Path getAStarPath(StateObservation so, Point target){
		if (!mapNodes[target.x][target.y].getReachable())
			return null;
		Point avatarPoint = Utils.toTileCoord(so.getAvatarPosition());
		Path path = findPath(avatarPoint.x, avatarPoint.y, target.x, target.y, so);
		return path;
	};

	/*
	 * get all neighbours of a certain node
	 * if the node is a portal, the exits are considered neighbours instead of adjacent sprites
	 */
	private ArrayList<MapNode> getAllNeighbours(MapNode curr) {
		ArrayList<MapNode> neighbours = new ArrayList<MapNode>();
		int nx, ny;
		if (curr.getPortalType() != -1){
			// is portal, add portal exits as neighbors
			int exitType = Agent.memory.portalMemories.get(curr.getPortalType());
			ArrayList<MapNode> exits = portalExitMap.get(exitType);
			if (exits != null && exits.size() != 0)
				for (MapNode exit : exits)
					neighbours.add(exit);
		} else {
			// adjacent positions are added
			for (int x = -1; x < 2; x++)
			{
				for (int y = -1; y < 2; y++)
				{
					// continue if it is diagonal position
					if ((x == 0 && y == 0) || ((x != 0) && (y != 0)))
						continue;

					nx = x + curr.getX();
					ny = y + curr.getY();
					if (this.vaildPos(nx, ny))
						neighbours.add(mapNodes[nx][ny]);
				}
			}
		}
		return neighbours;
	}

	/*
	 * Update the portal exit list by reading the observation grid and portal memories
	 * Add portal entry data to nodes also
	 */
	private void updatePortalInformation(StateObservation so) {
		ArrayList<Observation>[] portals = so.getPortalsPositions();
		if (portals!= null && portals.length > 0){
			portalEntryTypes = new int [portals.length];
			for (int i = 0; i < portals.length; i++){
				if(portals[i].size() > 0)
					portalEntryTypes[i] = portals[i].get(0).itype;
			}
		}
		portalExitMap.clear();
		if (Agent.memory.portalMemories != null && Agent.memory.portalMemories.size() != 0){
	        Iterator<Map.Entry<Integer, Integer>> itEntries = Agent.memory.portalMemories.entrySet().iterator();
	        while(itEntries.hasNext())
	        {
	            Map.Entry<Integer, Integer> entry = itEntries.next();
	            ArrayList<MapNode> portalExitList = new ArrayList<MapNode>();
	            for (int x = 0; x < getWidth(); x++)
	            	for (int y = 0; y < getHeight(); y++){
	            		ArrayList<Observation> observations = so.getObservationGrid()[x][y];
	            		for (Observation o : observations){
	            			//System.out.println("o.itype == " + o.itype + " exit type: " + entry.getValue().intValue() + " equal == " + (o.itype == entry.getValue().intValue()));
	            			if (o.itype == entry.getValue().intValue())
	            				portalExitList.add(mapNodes[x][y]);
	            			if (o.itype == entry.getKey().intValue())
	            				mapNodes[x][y].setPortalType(entry.getKey());
	            		}

	            	}
	            //System.out.println("# portal exit type " + entry.getValue().intValue() + " == " + portalExitList.size());
	            this.portalExitMap.put(entry.getValue(), portalExitList);
	        }
	    }

	}


	public boolean vaildPos(int x, int y)
	{
		if (x >= width || x < 0)
		{
			return false;
		}
		if (y >= height || y < 0)
		{
			return false;
		}

		ArrayList<Observation>[][] grid = so.getObservationGrid();
		if (grid[x][y].size() > 0){
			for (int i =0; i < grid[x][y].size(); i++){
				int type = grid[x][y].get(i).itype;

				// if the AI holds memory of the type, check if it is traversable and safe
				if (Agent.memory.memories.containsKey(so.getAvatarType()) && Agent.memory.memories.get(so.getAvatarType()).containsKey(type)){
					MemoryItem mi = Agent.memory.memories.get(so.getAvatarType()).get(type);
					if(!mi.isTraversable() || mi.isHostile())
						return false;
				}
			}
		}

		return true;
	}

	public Path findPath(int fx, int fy, int tx, int ty, StateObservation so)
	{
		if (this.vaildPos(tx, ty) == false) {
			//System.out.println("(" + tx + ", " + ty + ") is not valid");
			return null;
		}

		if (mapNodes[tx][ty].getParent() == null || !mapNodes[tx][ty].getReachable()) {
			return null;
		}

		Path path = new Path();
		MapNode target = mapNodes[tx][ty];
		while (target != mapNodes[fx][fy]) {
			//System.out.print("( " + target.getX() + ", " + target.getY() + ") ");
			path.prependStep(target.getX(), target.getY());
			target = target.getParent();
			if (target == null){
				// no parent is found, meaning no path to the target could be found
				//System.out.print("No Path found ");
				path = null;
				break;
			}
		}
		//System.out.println("from ( " + fx + ", " + fy + ") ");
		//path.prependStep(fx,fy);

		return path;
	}

	public void updateMap(StateObservation so){

		open = new ArrayList<MapNode>();
		close = new ArrayList<MapNode>();
		this.so = so;
		objs = so.getObservationGrid();
		width = objs.length;
		height = objs[1].length;

		mapNodes = new MapNode[this.getWidth()][this.getHeight()];
		for (int i = 0; i < this.getWidth(); i++) {
			for (int j = 0; j < this.getHeight(); j++) {
				mapNodes[i][j] = new MapNode(i, j);
			}
		}
		portalExitMap = new HashMap<Integer, ArrayList<MapNode>>();

		int currentX = (int) so.getAvatarPosition().x/so.getBlockSize();
		int currentY = (int) so.getAvatarPosition().y/so.getBlockSize();
//		if (currentX < 0 || currentY < 0 || currentX > objs.length || currentY > objs[0].length)
		if (currentX < 0 || currentY < 0 || currentX >= objs.length || currentY >= objs[0].length)
    		return;
		// recreate the nodes map
		for (int i = 0; i < this.getWidth(); i++)
			for (int j = 0; j < this.getHeight(); j++)
				mapNodes[i][j] = new MapNode(i, j);

		updatePortalInformation(so);

    		MapNode curr;
    		mapNodes[currentX][currentY].setCost(0);
    		mapNodes[currentX][currentY].setDepth(0);
		close.clear(); open.clear();
		open.add(mapNodes[currentX][currentY]);

		while (open.size() != 0)
		{
			Collections.sort(open);
			curr = open.get(0);
			open.remove(curr);
			close.add(curr);

			ArrayList<MapNode> neighbours = getAllNeighbours(curr);

			for (MapNode neighbour: neighbours){
				int nextcost = curr.getCost() + this.GetMoveCost(curr.getX(), curr.getY(), neighbour.getX(), neighbour.getY()) + neighbour.getHeuristic();

				if (neighbour.getCost() > nextcost)
				{
					neighbour.setCost(nextcost);
					neighbour.setParent(curr);
					neighbour.setReachable(true);
				}
				if (!open.contains(neighbour) && !close.contains(neighbour))
				{
					neighbour.setCost(nextcost);
					neighbour.setParent(curr);
					neighbour.setReachable(true);
					open.add(neighbour);
				}
			}
		}
	}

	public MapNode getNode(int x, int y){
		return mapNodes[x][y];
	}

	public int GetMoveCost(int fx, int fy, int tx, int ty)
	{
		return 1;
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}
}
