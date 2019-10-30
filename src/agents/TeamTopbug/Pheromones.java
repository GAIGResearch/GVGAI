package agents.TeamTopbug;

import java.awt.Point;

import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class Pheromones {
	public double[][] grid;
	public static final double PHEROMONES_AVATAR = 1;
	public static final double PHEROMONES_DECAY = 0.99;
	public static final double PHEROMONES_DIFFUSE = 0.4;

	public Pheromones() {
		grid = new double[GameInfo.width][GameInfo.height];
	}

	public Pheromones(Node node, Pheromones prev) {
		grid = new double[GameInfo.width][GameInfo.height];

		Vector2d avatarPosition = node.avatarPos;
		Point tilePos = Utils.toTileCoord(avatarPosition);

		double[][] buffered = Utils.copy2DArray(prev.grid);

		buffered[tilePos.x][tilePos.y] = PHEROMONES_AVATAR;

		// todo: test if double diffuse is good or not
		diffuse(grid, buffered);
		Utils.copyInto2DArray(buffered, grid);
		diffuse(grid, buffered);
		Utils.copyInto2DArray(buffered, grid);
		diffuse(grid, buffered);

	}

	public static void diffuse(double[][] dst, double[][] src) {
		for (int i = 0; i < GameInfo.width; i++) {
			for (int j = 0; j < GameInfo.height; j++) {
				int n = 0;
				double d = 0;
				if (i > 0) {
					n++;
					d += src[i - 1][j];
				}
				if (i < GameInfo.width - 1) {
					n++;
					d += src[i + 1][j];
				}
				if (j > 0) {
					n++;
					d += src[i][j - 1];
				}
				if (j < GameInfo.height - 1) {
					n++;
					d += src[i][j + 1];
				}
				if (n == 0) {
					n = 1;
					d = 0;
				}
				dst[i][j] = PHEROMONES_DIFFUSE * (d / n) + (1 - PHEROMONES_DIFFUSE) * PHEROMONES_DECAY * src[i][j];

			}
		}
	}

	public static class Benchmark {
		public static void main(String[] args) {
			int w = 20;
			int h = 20;
			int n = 1000000000;
			double[][] grid0 = new double[w][h];
			double[][] grid1 = new double[w][h];
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			for (int i = 0; i < n; i++) {
				diffuse(grid0, grid1);
			}
			System.out.println(timer.elapsed() / (1000 * 1000) / (double) n + " seconds per diffuse");
		}
	}
}
