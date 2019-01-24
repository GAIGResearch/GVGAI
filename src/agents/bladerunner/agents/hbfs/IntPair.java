package agents.bladerunner.agents.hbfs;

public class IntPair implements Comparable<IntPair> {

	public int a;
	public int b;

	public IntPair(int a, int b) {
		super();
		this.a = a;
		this.b = b;
	}

	@Override
	public int compareTo(IntPair o) {
		if (a > o.a) {
			return 1;
		} else if (a == o.a) {
			if (b > o.b) {
				return 1;
			} else if (b == o.b) {
				return 0;
			} else { // b < o.b
				return -1;
			}
		} else { // a < o.a
			return -1;
		}
	}

	@Override
	public boolean equals(Object obj) {
		IntPair p = (IntPair) obj;
		return p.a == a && p.b == b;
	}

}
