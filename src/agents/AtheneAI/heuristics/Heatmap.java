package agents.AtheneAI.heuristics;

import tools.Vector2d;
import core.game.StateObservation;

public class Heatmap {
	public int decay;
	public int rebound;
	public int delay;
	public int maxScore;
	public int[][] scoreVector;
	public int[][] delayVector;
	public double sectorXsize;
	public double sectorYsize;

	public Heatmap(double maxSize, double minSize, int _delay, int _decay,
			int _rebound, int _maxScore, StateObservation obs) {
		decay = _decay;
		rebound = _rebound;
		delay = _delay;
		maxScore = _maxScore;

		int sectorsX = 0;
		int sectorsY = 0;
		for (int i = 1; i <= obs.getWorldDimension().getWidth(); ++i) {

			double widthOfSectors = obs.getWorldDimension().getWidth() / i;

			if (widthOfSectors > minSize && widthOfSectors < maxSize) {
				sectorXsize = widthOfSectors;
				sectorsX = i;
				break;
			}
		}
		for (int i = 1; i <= obs.getWorldDimension().getHeight(); ++i) {

			double heightOfSectors = obs.getWorldDimension().getHeight() / i;

			if (heightOfSectors > minSize && heightOfSectors < maxSize) {
				sectorYsize = heightOfSectors;
				sectorsY = i;
				break;
			}
		}
		scoreVector = new int[sectorsX][sectorsY];
		delayVector = new int[sectorsX][sectorsY];

		for (int i = 0; i < scoreVector.length; ++i) {
			for (int j = 0; j < scoreVector[i].length; ++j) {
				scoreVector[i][j] = maxScore;
				delayVector[i][j] = delay;
			}
		}
	}

	public int getScore(Vector2d pos) {
		int x = getSectorX(pos.x);
		int y = getSectorY(pos.y);
		return scoreVector[x][y];
	}

	public void addVisit(Vector2d pos) {
		int x = getSectorX(pos.x);
		int y = getSectorY(pos.y);
		for (int i = 0; i < scoreVector.length; ++i) {
			for (int j = 0; j < scoreVector[i].length; ++j) {
				if (i == x && j == y) {
					scoreVector[i][j] -= decay;
					delayVector[i][j] = delay;
				} else {
					if (delayVector[i][j] == 0 && scoreVector[i][j] < maxScore) {
						scoreVector[i][j] += rebound;
					} else {
						delayVector[i][j] -= 1;
					}
				}
			}
		}
	}

	private int getSectorX(double x) {
	    for (int i = 1; i < scoreVector.length; ++i) {
		if (i * sectorXsize > x) {
			return i - 1;
		}
	    }
	    return scoreVector.length - 1;
	}

	private int getSectorY(double y) {
		for (int i = 1; i < scoreVector[0].length; ++i) {
			if (i * sectorYsize > y) {
				return i - 1;
			}
		}
		return scoreVector[0].length - 1;
	}
}
