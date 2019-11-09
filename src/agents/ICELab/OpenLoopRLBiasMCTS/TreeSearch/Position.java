package agents.ICELab.OpenLoopRLBiasMCTS.TreeSearch;

import java.util.Objects;

public class Position{
	public int x;
	public int y;
	public int type = -1;

	public Position(int x, int y){
		this.x = x;
		this.y = y;
	}

	public Position(int x, int y, int type){
		this.x = x;
		this.y = y;
		this.type = type;
	}

	public Position(Position p){
		this.x = p.x;
		this.y = p.y;
		this.type = p.type;
	}

	// calculate the Manhattan distance from another position
	public int manhattanDistance(Position p){
		return Math.abs(this.x - p.x) + Math.abs(this.y - p.y);
	}
	@Override
	public boolean equals (Object p){
		if (p instanceof Position)
			return (this.x == ((Position) p).x) && (this.y == ((Position) p).y) && (this.type == ((Position) p).type);
		else
			return false;
	}

	@Override
	public int hashCode(){
		return Objects.hash(this.x, this.y, this.type);
	}
}