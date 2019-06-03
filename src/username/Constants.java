package username;

/**
 * A class containing constants that are used by the level generator.
 */
final class Constants {

	/**
	 * The minimum width of the level, excluding its border.
	 */
	static final int MIN_WIDTH = 6;

	/**
	 * The maximum width of the level, excluding its border.
	 */
	static final int MAX_WIDTH = 18;

	/**
	 * How much the width of the level can randomly change, as a multiplier.
	 */
	static final double RANDOM_WIDTH = 0.25;

	/**
	 * The minimum height of the level, excluding its border.
	 */
	static final int MIN_HEIGHT = 6;

	/**
	 * The maximum height of the level, excluding its border.
	 */
	static final int MAX_HEIGHT = 18;

	/**
	 * How much the height of the level can randomly change, as a multiplier.
	 */
	static final double RANDOM_HEIGHT = 0.25;

	/**
	 * Hide the constructor.
	 */
	private Constants() {}
}
