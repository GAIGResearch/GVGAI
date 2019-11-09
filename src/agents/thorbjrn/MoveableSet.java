package agents.thorbjrn;

import java.util.HashSet;

import tools.Vector2d;

public class MoveableSet {

	public Vector2d avatarPos;
//	public Vector2d avatarDir;
	public HashSet<Moveable> moveables = new HashSet<Moveable>();
	
	
	public MoveableSet(HashSet<Moveable> moveables, Vector2d avatarPos, Vector2d avatarDir) {
		this.moveables = moveables;
		this.avatarPos = avatarPos;
//		this.avatarDir = avatarDir;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((avatarPos == null) ? 0 : Vector2dHelper.hashCodeOfVector2d(avatarPos));
		result = prime * result
//				+ ((avatarDir == null) ? 0 : Vector2dHelper.hashCodeOfVector2d(avatarDir));
//		result = prime * result
				+ ((moveables == null) ? 0 : moveables.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MoveableSet other = (MoveableSet) obj;
		if (avatarPos == null) {
			if (other.avatarPos != null)
				return false;
		} else if (!avatarPos.equals(other.avatarPos))
			return false;
		if (moveables == null) {
			if (other.moveables != null)
				return false;
		} else if (!moveables.containsAll(other.moveables))
			return false;
		return true;
	}
}
