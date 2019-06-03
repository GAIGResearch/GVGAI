package username;

/**
 * A utilities class.
 */
final class Utils {

	/**
	 * Hide the constructor.
	 */
	private Utils() {}

	/**
	 * Clamps the value between the given bounds.
	 * @param min The lower bound.
	 * @param val The value which should be clamped.
	 * @param max The upper bound.
	 * @param <T> The type of the value.
	 * @return The clamped value
	 */
	static <T extends Comparable<T>> T clamp(T min, T val, T max) {
		if (val.compareTo(min) < 0) return min;
		if (val.compareTo(max) > 0) return max;
		return val;
	}
}
