package agents.YBCriber;

import java.util.HashMap;
import tools.Vector2d;

public class Properties {
	double access = 1; // 1 = not
	int uAccess = 0;

	double destroyed = 0; // 1 = can be destroyed
	int uDestroyed = 0;

	double kill = 0; // 1 = kills
	int uKill = 0;

	double movable = 0; // 1 = movable
	int uMovable = 0;

	double resources = 0; // total number
	int uResources = 0;

	double score = 0;
	int uScore = 0;

	HashMap<Integer, Integer> M = new HashMap<Integer, Integer>();
	int uM = 0;

	int spawn = 0;

	// TODO: use an array and each position is one of these?

	public Properties(Integer access2, Double score2, Integer kill2, Integer movable2, Integer resources2,
			Integer destroyed2, Integer spawned) {
		update(access2, score2, kill2, movable2, resources2, destroyed2, spawned);
	}

	public Properties(Vector2d v) {
		this(null, null, null, null, null, null, null);
		M.put(encode(v), 1);
		++uM;
	}

	public void update(Integer access2, Double score2, Integer kill2, Integer movable2, Integer resources2,
			Integer destroyed2, Integer spawned) {
		if (access2 != null) {
			access = (access * uAccess + access2) / (uAccess + 1);
			++uAccess;
		}
		if (destroyed2 != null) {
			destroyed = (destroyed * uDestroyed + destroyed2) / (uDestroyed + 1);
			++uDestroyed;
		}
		if (kill2 != null) {
			kill = (kill * uKill + kill2) / (uKill + 1);
			++uKill;
		}
		if (movable2 != null) {
			movable = (movable * uMovable + movable2) / (uMovable + 1);
			++uMovable;
		}
		if (resources2 != null) {
			resources = (resources * uResources + resources2) / (uResources + 1);
			++uResources;
		}
		if (score2 != null) {
			score = (score * uScore + score2) / (uScore + 1);
			++uScore;
		}
		if (spawned != null && spawned == 1)
			spawn = 1;
	}

	public void updateM(Vector2d v) {
		++uM;
		int x = encode(v);
		if (!M.containsKey(x))
			M.put(x, 1);
		else {
			int y = M.get(x);
			M.put(x, y + 1);
		}
	}

	public int encode(Vector2d v) {
		return (int) (v.y + Agent.h) * Agent.Hashpos + (int) v.x + Agent.w;
	}

	static Vector2d decode(int x) {
		return new Vector2d(x % Agent.Hashpos - Agent.w, x / Agent.Hashpos - Agent.h);
	}
}
