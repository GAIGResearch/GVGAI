package agents.thorbjrn;

import tools.Vector2d;

public class Vector2dHelper {
	public static int hashCodeOfVector2d(Vector2d pos) {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(pos.x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(pos.y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
