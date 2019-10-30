package agents.bladerunner.agents.misc;

import java.awt.Dimension;

import tools.Vector2d;
import core.game.StateObservation;

public class RewardMap {

	private double[][] rewMap;
	private int rewMapWidth;
	private int rewMapHeight;
	private Dimension dim;
	private Dimension worldDim;
	private int blockSize;

	/**
	 * Initialize Reward map with state observation, it automatically gets the
	 * World dimensions and sets all rewards to initVal
	 * 
	 * @param StateObservation
	 * @param initVal
	 * 
	 */
	public RewardMap(StateObservation stateObs, double initVal) {
		blockSize = stateObs.getBlockSize();
		worldDim = stateObs.getWorldDimension();
		rewMapWidth = worldDim.width / blockSize;
		rewMapHeight = worldDim.height / blockSize;
		// System.out.println(blockSize+" "+worldDim+" "+rewMapWidth+" "+rewMapHeight);
		dim = new Dimension(rewMapWidth, rewMapHeight);

		rewMap = new double[rewMapWidth][rewMapHeight];
		for (int i = 0; i < rewMapWidth; i++) {
			for (int j = 0; j < rewMapHeight; j++) {
				rewMap[i][j] = initVal;
			}
		}
	}

	public void print() {
		for (int j = 0; j < rewMapHeight; j++) {
			for (int i = 0; i < rewMapWidth; i++) {
				double val = rewMap[i][j];
				if (val < 0) {
					System.out.printf("%.3f", val);
					System.out.print(" ");
				} else {
					System.out.print(" ");
					System.out.printf("%.3f", val);
					System.out.print(" ");
				}

			}
			System.out.println();
		}
		System.out.println();
		System.out.println();

	}

	public Dimension getDimension() {
		return dim;
	}

	public double[][] getRewardValues() {
		return rewMap;
	}

	public double getReward(int X, int Y) {
		if (X < 0 || Y < 0 || X >= rewMapWidth || Y >= rewMapHeight) {
			return 0;
		}
		return rewMap[X][Y];
	}

	public double getRewardwithWorldPixelPos(int pixelX, int pixelY) {
		int X = floorDiv((int) (pixelX + 0.1), blockSize);
		int Y = floorDiv((int) (pixelY + 0.1), blockSize);
		return getReward(X, Y);
	}

	public double getRewardAtWorldPosition(Vector2d posVec) {
		int X = floorDiv((int) (posVec.x + 0.1), blockSize);
		int Y = floorDiv((int) (posVec.y + 0.1), blockSize);
		return getReward(X, Y);
	}

	public void setReward(int X, int Y, double value) {
		if (X >= 0 && Y >= 0 && X < rewMapWidth && Y < rewMapHeight) {
			rewMap[X][Y] = value;
		}
	}

	public void setRewardAtWorldPixelPos(double pixelX, double pixelY, double value) {
		int X = floorDiv((int) (pixelX + 0.1), blockSize);
		int Y = floorDiv((int) (pixelY + 0.1), blockSize);
		setReward(X, Y, value);
	}

	public void setRewardAtWorldPosition(Vector2d posVec, double value) {
		setRewardAtWorldPixelPos(posVec.x,posVec.y,value);
	}

	public void incrementRewardAtWorldPosition(Vector2d posVec, double incValue) {
		int X = floorDiv((int) (posVec.x + 0.1), blockSize);
		int Y = floorDiv((int) (posVec.y + 0.1), blockSize);
		setReward(X, Y, getReward(X, Y) + incValue);
	}

	public void addOtherMap(RewardMap MapToAdd) {
		double[][] addMap = MapToAdd.getRewardValues();
		for (int i = 0; i < rewMapWidth; i++) {
			for (int j = 0; j < rewMapHeight; j++) {
				rewMap[i][j] += addMap[i][j];
			}
		}
	}

	/**
	 * Increment by the given value, but max to 1
	 * 
	 */
	public void incrementAll(double incValue) {
		for (int i = 0; i < rewMapWidth; i++) {
			for (int j = 0; j < rewMapHeight; j++) {
				if (rewMap[i][j] < 1) {
					rewMap[i][j] += incValue;
				}

			}
		}
	}

	public void decrementAtPos(Vector2d posVec, double incValue) {
		int X = floorDiv((int) (posVec.x + 0.1), blockSize);
		int Y = floorDiv((int) (posVec.y + 0.1), blockSize);
		if (X >= 0 && Y >= 0 && X < rewMapWidth && Y < rewMapHeight) {
			rewMap[X][Y] += incValue;
		}
	}
	
	/**
	 * Returns the largest (closest to positive infinity) long value that is
	 * less than or equal to the algebraic quotient. There is one special case,
	 * if the dividend is the Long.MIN_VALUE and the divisor is -1, then integer
	 * overflow occurs and the result is equal to the Long.MIN_VALUE. Normal
	 * integer division operates under the round to zero rounding mode
	 * (truncation). This operation instead acts under the round toward negative
	 * infinity (floor) rounding mode. The floor rounding mode gives different
	 * results than truncation when the exact result is negative.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int floorDiv(int x, int y) {
		int r = x / y;
		// if the signs are different and modulo not zero, round down
		if ((x ^ y) < 0 && (r * y != x)) {
			r--;
		}
		return r;
	}

}
