package agents.ICELab.OpenLoopRLBiasMCTS;

public class MapNode implements Comparable<MapNode> {
	/** The x coordinate of the node */
	private int x;
	/** The y coordinate of the node */
	private int y;
	/** The path cost for this node */
	private int cost;
	/** The parent of this node, how we reached it in the search */
	private MapNode parent;
	/** The heuristic cost of this node */
	private int heuristic;
	/** The search depth of this node */
	private int depth;
	/** The portal type of the node, -1 if the node is not a portal*/
	private int portalType = -1;

	private boolean reachable = false;

	public MapNode(int x, int y) {
		this.x = x;
		this.y = y;
		parent = null;
		heuristic = 0;
	}

	/*public int setHeuristic(int tx, int ty)
	{
		int dx = tx - x;
		int dy = ty - y;
		return (int) Math.sqrt(dx*dx - dy*dy);
	}*/

	public void setHeuristic(int h)
	{
		this.heuristic = h;
		if (h < -9999)
			this.reachable = false;
	}

	public int getHeuristic()
	{
		return this.heuristic;
	}

	public int setParent(MapNode parent) {
		depth = parent.depth + 1;
		this.parent = parent;

		return depth;
	}

	public MapNode getParent() {
		return parent;
	}

	public int getCost()
	{
		return cost;
	}

	public void setCost(int cost)
	{
		this.cost = cost;
	}

	public int getDepth()
	{
		return depth;
	}

	public void setDepth(int depth)
	{
		this.depth = depth;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setReachable(boolean reachable){
		//System.out.println("(" + this.x + ", " + this.y + ")  set to " + ((reachable)? "reachable":"not reachable"));
		this.reachable = reachable;
	}

	public boolean getReachable(){
		return this.reachable;
	}

	public void setPortalType(int type){
		this.portalType = type;
	}

	public int getPortalType(){
		return this.portalType;
	}

	/**
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(MapNode other) {
		MapNode o = (MapNode) other;

		float f = heuristic + cost;
		float of = o.heuristic + o.cost;

		if (f < of) {
			return -1;
		} else if (f > of) {
			return 1;
		} else {
			return 0;
		}
	}

}
