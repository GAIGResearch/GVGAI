package agents.bladerunner.agents.misc;

/**
 * A data structure to store pairs.
 * 
 * @author Benjamin Ellenberger
 *
 * @param <FIRST>
 * @param <SECOND>
 */
public class Pair<FIRST, SECOND> implements Comparable<Pair<FIRST, SECOND>> {

	public final FIRST first;
	public final SECOND second;

	public Pair(FIRST first, SECOND second) {
		this.first = first;
		this.second = second;
	}

	public static <FIRST, SECOND> Pair<FIRST, SECOND> of(FIRST first,
			SECOND second) {
		return new Pair<FIRST, SECOND>(first, second);
	}

	@Override
	public int compareTo(Pair<FIRST, SECOND> o) {
		int cmp = compare(first, o.first);
		return cmp == 0 ? compare(second, o.second) : cmp;
	}

	@SuppressWarnings("unchecked")
	private static int compare(Object o1, Object o2) {
		return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? +1
				: ((Comparable<Object>) o1).compareTo(o2);
	}

	@Override
	public int hashCode() {
		return 31 * hashcode(first) + hashcode(second);
	}

	private static int hashcode(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair))
			return false;
		if (this == obj)
			return true;
		return equal(first, ((Pair<?, ?>) obj).first)
				&& equal(second, ((Pair<?, ?>) obj).second);
	}

	private boolean equal(Object o1, Object o2) {
		return o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2));
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ')';
	}
}
