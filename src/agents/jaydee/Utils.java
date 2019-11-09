package agents.jaydee;

import tools.Vector2d;

import java.awt.*;
import java.util.Comparator;

public class Utils {
	public static double[][] copy2DArray(double[][] arr) {
		int n = arr.length;
		double[][] copy = new double[n][];
		for (int i = 0; i < n; i++) {
			copy[i] = new double[arr[i].length];
			System.arraycopy(arr[i], 0, copy[i], 0, arr[i].length);
		}
		return copy;
	}

	public static double[][] copyInto2DArray(double[][] dst, double[][] src) {
		int n = src.length;
		double[][] copy = new double[n][];
		for (int i = 0; i < n; i++) {
			System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
		}
		return copy;
	}

	public static double min(double[][] arr) {
		double min = Double.POSITIVE_INFINITY;
		for (double[] anArr : arr) {
			for (double anAnArr : anArr) {
				if (anAnArr < min) {
					min = anAnArr;
				}
			}
		}
		return min;
	}

	public static double max(double[][] arr) {
		double max = Double.NEGATIVE_INFINITY;
		for (double[] anArr : arr) {
			for (double anAnArr : anArr) {
				if (anAnArr > max) {
					max = anAnArr;
				}
			}
		}
		return max;
	}

	public static Point toTileCoord(Vector2d coord) {
		return toTileCoord(coord.x, coord.y);
	}

	public static Point toTileCoord(double x, double y) {
		Point point = new Point();
		point.x = (int) Math.round(x / GameInfo.blocksize);
		point.y = (int) Math.round(y / GameInfo.blocksize);
		if (point.x < 0)
			point.x = 0;
		if (point.y < 0)
			point.y = 0;
		if (point.x > GameInfo.width - 1)
			point.x = GameInfo.width - 1;
		if (point.y > GameInfo.height - 1)
			point.y = GameInfo.height - 1;
		return point;
	}

	public static Comparator<Node> heuristicComparator = new Comparator<Node>() {
		@Override
		public int compare(Node o1, Node o2) {
			double f1 = o1.bestReward;
			double f2 = o2.bestReward;
			if (f1 < f2) {
				return -1;
			} else if (f1 > f2) {
				return 1;
			} else {
				return 0;
			}
		}
	};
}
