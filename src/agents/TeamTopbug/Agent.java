package agents.TeamTopbug;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;
import java.util.Stack;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import agents.TeamTopbug.TreeSearch.GA;
import agents.TeamTopbug.TreeSearch.TreeSearch;

public class Agent extends AbstractPlayer {
	public static Random random = new Random();
	public static AnyTime anyTime = new AnyTime();

	public Node gameStart;
	public Node origin;

	public TreeSearch search;

	public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		GameInfo.init(stateObs);
		gameStart = NodePool.get();
		gameStart.state = stateObs;
		gameStart.avatarPos = stateObs.getAvatarPosition();
		gameStart.nVisits = 1;
		gameStart.depth = 0;

		origin = gameStart;

		anyTime.beginInit(elapsedTimer);

		// choose our search algorithm
		// search = new MCTS(origin);
		search = new GA(origin);
		// search = new DBS(origin);

		// fill search tree
		search.search();
	}

	@Override
	public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		anyTime.begin(elapsedTimer);

		origin.state = stateObs;
		origin.depth = 0;
		if (origin.prev != null) {
			origin.prev.depth = -1;
		}
		// fill search tree
		search.search();

		// select action
		Node selected = origin.select();
		Types.ACTIONS action = selected.action;

		// release garbage & roll search tree
		selected.isDestroyable = false;
		origin.release();
		origin = selected;

		// roll search algorithm
		search.roll(origin);

		System.out.println(action);

		return action;
	}

	@Override
	public void draw(Graphics2D g) {
		int s = (int) (GameInfo.blocksize * 0.5);
		double[][] arr = origin.pheromones.grid;
		double min = Utils.min(arr);
		double max = Utils.max(arr);

		// draw pheromones
		for (int i = 0; i < GameInfo.width; i++) {
			for (int j = 0; j < GameInfo.height; j++) {
				int x = i * GameInfo.blocksize;
				int y = j * GameInfo.blocksize;

				// calculate color
				float here = 0;
				if ((max - min) != 0)
					here = (float) ((arr[i][j] - min) / (max - min));
				g.setColor(new Color(here, here, here));

				g.fillOval(x + GameInfo.blocksize - s, y + GameInfo.blocksize - s, s, s);

			}
		}

		// draw search space from origin
		Stack<Node> stack = new Stack<Node>();
		stack.push(origin);
		g.setColor(new Color(255, 0, 0));
		while (!stack.empty()) {
			Node current = stack.pop();
			// draw line from current to current.prev
			if (current.prev != null) {
				Vector2d prevPoint = current.prev.avatarPos;
				Vector2d point = current.avatarPos;
				g.drawLine((int) (prevPoint.x + s + 0.5), (int) (prevPoint.y + s + 0.5), (int) (point.x + s + 0.5),
						(int) (point.y + s + 0.5));
				// g.drawString(String.valueOf(current.averageReward), (int) (point.x + s +
				// 0.5), (int) (point.y + s + 0.5));
			}

			// traverse children
			if (current.children != null) {
				for (Node child : current.children.values()) {
					stack.push(child);
				}
			}
		}

	}
}
