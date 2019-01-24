package agents.bladerunner.agents.mcts;

import java.util.ArrayList;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;
import agents.bladerunner.Agent;
import agents.bladerunner.agents.misc.ObservationTools;
import agents.bladerunner.agents.misc.PersistentStorage;

public class MCTSNode {
	public enum StateType {
		UNCACHED, LOSE, NORMAL, WIN
	}

	public static Random m_rnd;

	// strong rewards
	private static final double HUGE_NEGATIVE_REWARD = -Double.MAX_VALUE;
	private static final double HUGE_POSITIVE_REWARD = Double.MAX_VALUE;

	public static double fear_of_unknown = 0.99;
	public static double epsilon = 1e-6;
	public static double egreedyEpsilon = 0.05;
	public StateObservation state;
	public MCTSNode parent;
	public MCTSNode[] children;
	public double totValue;
	public int nVisits;

	public int m_depth;
	private static double[] lastBounds = new double[] { 0, 1 };
	private static double[] curBounds = new double[] { 0, 1 };
	public StateType stateType = StateType.UNCACHED;

	// keeps track of the reward at the start of the MCTS search
	// public double startingRew;

	public MCTSNode(Random rnd) {
		this(null, null, rnd);
	}

	public MCTSNode(StateObservation state, MCTSNode parent, Random rnd) {
		this.state = state;
		this.parent = parent;
		MCTSNode.m_rnd = rnd;
		children = new MCTSNode[PersistentStorage.actions.length];
		totValue = 0.0;
		if (parent != null) {
			m_depth = parent.m_depth + 1;
		} else {
			m_depth = 0;
		}

	}

	public MCTSNode(StateObservation state, MCTSNode parent) {
		this.state = state;
		this.parent = parent;
		children = new MCTSNode[PersistentStorage.actions.length];
		totValue = 0.0;
		if (parent != null) {
			m_depth = parent.m_depth + 1;
		} else {
			m_depth = 0;
		}

	}

	public int countNodes() {
		int n = 1;
		for (MCTSNode child : children) {
			if (child != null) {
				n += child.countNodes();
			}
		}
		return n;
	}

	public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

		lastBounds[0] = curBounds[0];
		lastBounds[1] = curBounds[1];
		int firstTry = 0;
		if (elapsedTimer.remainingTimeMillis() > 300) {
			PersistentStorage.MCTS_DEPTH_RUN = 20;
			firstTry = 1;
		}

		if (firstTry == 1) {
			while (elapsedTimer.remainingTimeMillis() > 50) {

				MCTSNode cur = this;
				while (!cur.state.isGameOver() && cur.m_depth < PersistentStorage.MCTS_DEPTH_RUN) {
					if (cur.notFullyExpanded()) {
						// form deeper trees
						cur = cur.expand();

					} else {
						cur = cur.uct();
						// cur = cur.egreedy();
					}
				}

				double delta = cur.rollOut();
				backUp(cur, delta + 1, 1);
				// backUpBest(selected, delta);
			}
			PersistentStorage.MCTS_DEPTH_RUN = PersistentStorage.MCTS_DEPTH_FIX;
		}

		else {
			while (elapsedTimer.remainingTimeMillis() > 10) {
				MCTSNode selected = treePolicy();
				double delta = selected.rollOut();
				backUp(selected, delta, 1);
				// backUpBest(selected, delta);
			}
		}

	}

	public MCTSNode treePolicy() {

		MCTSNode cur = this;
		while (!cur.state.isGameOver() && cur.m_depth < PersistentStorage.MCTS_DEPTH_RUN) {
			if (cur.notFullyExpanded()) {
				// expand with random actions of the unused actions.
				return cur.expand();
				// cur = cur.expand();

			} else {
				cur = cur.uct();
				// cur = cur.egreedy();
			}
		}

		return cur;
	}

	public MCTSNode expand() {

		int bestAction = 0;
		double bestValue = -1; // select a never used action
		for (int i = 0; i < children.length; i++) {
			double x = MCTSNode.m_rnd.nextDouble();
			if (x > bestValue && children[i] == null) {
				bestAction = i;
				bestValue = x;
			}
		}
		StateObservation nextState = state.copy();
		nextState.advance(PersistentStorage.actions[bestAction]);

		// build children for the newly tried action
		MCTSNode tn = new MCTSNode(nextState, this);
		children[bestAction] = tn;
		return tn;

	}

	public MCTSNode uct() {

		MCTSNode selectedNode = null;
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		for (int i = 0; i < children.length; i++) {
			double hvVal = children[i].totValue;
			double childValue = hvVal / (children[i].nVisits + MCTSNode.epsilon);

			// reward + UCT-exploration term. Not clear to me if this is useful
			// for the size of the tree that we have within our time constraints
			double uctValue = childValue
					+ PersistentStorage.K * Math.sqrt(Math.log(nVisits + 1) / (children[i].nVisits + MCTSNode.epsilon))
					+ MCTSNode.m_rnd.nextDouble() * MCTSNode.epsilon;

			// small sampleRandom numbers: break ties in unexpanded nodes
			if (uctValue > bestValue && !children[i].isLoseState()) {
				selected = i;
				bestValue = uctValue;
			}
		}
		if (selected == -1 || children[selected].isLoseState()) {
			if (Agent.isVerbose) {
				System.out.println("MCTS::##### Oh crap.  Death awaits with choice " + selected + ".");
			}
			selected = 0;
		}
		if (selected != -1) {
			// if we do the uct step it might be worthwhile also to update the
			// state believe, this way we create a real rollout from a newly
			// sampled state-pathway and not just from the very first one.

			selectedNode = children[selected];
			StateObservation nextState = state.copy();
			nextState.advance(PersistentStorage.actions[selected]);
			selectedNode.state = nextState;

		}
		if (selectedNode == null) {
			throw new RuntimeException("Warning! returning null: " + bestValue + " : " + children.length);
		}
		return selectedNode;
	}

	public MCTSNode egreedy() {

		MCTSNode selected = null;

		if (MCTSNode.m_rnd.nextDouble() < egreedyEpsilon) {
			// Choose randomly
			int selectedIdx = MCTSNode.m_rnd.nextInt(children.length);
			selected = children[selectedIdx];

		} else {
			// pick the best Q.
			double bestValue = -Double.MAX_VALUE;
			for (MCTSNode child : children) {
				double hvVal = child.totValue + MCTSNode.m_rnd.nextDouble() * MCTSNode.epsilon;

				// small sampleRandom numbers: break ties in unexpanded nodes
				if (hvVal > bestValue) {
					selected = child;
					bestValue = hvVal;
				}
			}

		}

		if (selected == null) {
			throw new RuntimeException("Warning! returning null: " + this.children.length);
		}

		return selected;
	}

	public double rollOut() {
		StateObservation rollerState = state.copy();

		// int thisDepth = this.m_depth;
		int thisDepth = 0; // here we guarantee "ROLLOUT_DEPTH" more rollout
		// after MCTS/expand is finished
		double previousScore;
		// rollout with random actions for "ROLLOUT_DEPTH" times
		while (!finishRollout(rollerState, thisDepth)) {
			previousScore = rollerState.getGameScore();
			int action = MCTSNode.m_rnd.nextInt(PersistentStorage.actions.length);
			rollerState.advance(PersistentStorage.actions[action]);
			PersistentStorage.iTypeAttractivity.updateAttraction(rollerState, previousScore);
			thisDepth++;
		}

		// why do a random action???
		// previousScore = rollerState.getGameScore();
		// int action = MCTSNode.m_rnd.nextInt(PersistentStorage.actions.length);
		// rollerState.advance(PersistentStorage.actions[action]);
		// PersistentStorage.iTypeAttractivity.updateAttraction(rollerState,
		// previousScore);

		// update our ItypeAttractivity (use the startingreward of the rollout, usefull?
		// )
		PersistentStorage.iTypeAttractivity.updateAttraction(rollerState, PersistentStorage.startingReward);

		// get current position and reward at that position due to the exploration map
		double explRew = PersistentStorage.rewMap.getRewardAtWorldPosition(rollerState.getAvatarPosition());

		Vector2d curPos = rollerState.getAvatarPosition();
		// System.out.println(PersistentStorage.MCTS_DEPTH_FIX +" "+
		// PersistentStorage.MCTS_DEPTH_RUN + " "+ m_depth);
		int nSteps = 1 + PersistentStorage.MCTS_DEPTH_FIX - (PersistentStorage.MCTS_DEPTH_RUN - m_depth);
		// counts the number of Blocks we moved
		double nonJitterRew = 0;
		if (curPos.x > 0) {
			nonJitterRew = Math.abs((MCTSAgent.startingPos.x - curPos.x))
					+ Math.abs((MCTSAgent.startingPos.y - curPos.y));
			nonJitterRew /= (rollerState.getBlockSize() * nSteps);
		}
		if (nonJitterRew >= 1)
			nonJitterRew = 0;

		// in 2D games is exploration and notJitter movements not that important
		double multiplierExploration = 1;
		if (PersistentStorage.actions.length < 4)
			multiplierExploration = 0.1;
		// get a reward based on the distance to the different Itypes.
		// this can be closest ones, or only positives....
		double distITypeRewNewDist = getNewExplITypeRewardNewDist(rollerState, nonJitterRew);

		// get a heuristic for wasting resources
		double ressourceReward = ObservationTools.getRessourceDifferenceIndicator(rollerState) * 0.05;

		// use a fraction of "explRew" as an additional reward (Not given by the
		// Gamestats) the multiplication is just taking care of ignoring this distItype
		// if we are stuck.
		double additionalRew = (multiplierExploration * explRew / 10 + distITypeRewNewDist / 3
				+ multiplierExploration * nonJitterRew / 20 + ressourceReward) / 2;

		// DecimalFormat df = new DecimalFormat("####0.0000");

		// System.out.println("explRew: " + df.format(explRew/10) + " ItypeDistRew: " +
		// df.format(distITypeRewNewDist/3) + " NonJitterRew: " +
		// df.format(nonJitterRew/20) + " res: "+ df.format(ressourceReward));
		// System.out.println(": " + curPos.x + " : " + curPos.y + " "+ nSteps );

		int useRelativeReward = 1;
		double normDelta = 0;
		if (useRelativeReward == 0) {

			double delta = value(rollerState) + additionalRew;
			if (delta < curBounds[0])
				curBounds[0] = delta;
			if (delta > curBounds[1])
				curBounds[1] = delta;

			normDelta = Utils.normalise(delta, lastBounds[0], lastBounds[1]);
		} else {
			// get the relative reward
			normDelta = (value(rollerState) - PersistentStorage.startingReward) + additionalRew;
		}
		int useTrappedHeuristics = 1;
		// if (useTrappedHeuristics == 1) {
		// normDelta += 0.1f * (PersistentStorage.numberOfBlockedMovables -
		// trapHeuristic(rollerState));
		// }

		// try to punish positions where we died in some rollouts
		if (normDelta < -100) {
			if (this.parent != null) {
				Vector2d lastPos = this.parent.state.getAvatarPosition();
				PersistentStorage.rewMap.setRewardAtWorldPosition(lastPos, -0.4);
			}
		}

		return normDelta;
	}

	public double getNewExplITypeRewardNewDist(StateObservation state, double nonJitterRew) {
		// creates a heuristic reward based on the distance of the rolloutstates from to
		// the various abjects

		// THIS IS REALLY UGLY BUT IM TO LAZY TO CLEAN THAT UP NOW AND IT WORKS OK ;)
		double totRew = 0;
		Vector2d pos = state.getAvatarPosition();

		int blockSize = state.getBlockSize();

		int avaX = floorDiv((int) (pos.x + 0.1), blockSize);
		int avaY = floorDiv((int) (pos.y + 0.1), blockSize);
		// double maxPath = (PersistentStorage.rewMap.getDimension().height +
		// PersistentStorage.rewMap.getDimension().width);

		int count1 = 0;

		ArrayList<Observation>[] npcPositions = null;
		npcPositions = state.getNPCPositions(pos);
		if (npcPositions != null) {
			for (ArrayList<Observation> npcs : npcPositions) {
				if (npcs.size() > 0) {
					// for(int i = 0; i< npcs.size(); i++){
					// only look at the closest rewarding/punishing npc
					for (int i = 0; i < 1; i++) {

						double npcAttractionValue = 0;

						try {
							npcAttractionValue = PersistentStorage.iTypeAttractivity.get(npcs.get(i).itype);
						} catch (NullPointerException e) {
							PersistentStorage.iTypeAttractivity.putIfAbsent(npcs.get(i));
							npcAttractionValue = PersistentStorage.iTypeAttractivity.get(npcs.get(i).itype);
						}

						if (MCTSAgent.pathPlannerMaps.containsKey(npcs.get(i).itype)) {
							// compute the current distance to the closest enemy
							double distIntSteps = MCTSAgent.pathPlannerMaps.get(npcs.get(i).itype)
									.getStepsQtyToGoal(avaX, avaY);
							double maxPath = MCTSAgent.pathPlannerMaps.get(npcs.get(i).itype).getMaximumSteps();
							double dist = distIntSteps / maxPath;

							if (npcAttractionValue < 0 && PersistentStorage.actions.length % 2 != 0
									&& npcAttractionValue > -1.5)
								totRew += Math.abs(npcAttractionValue) / (dist * dist + 0.05) * 1 / 50;
							else {
								// case of an enemy that killed us in a previous game
								if (npcAttractionValue < -1) {
									// dist = Math.sqrt(Math.abs(pos.x-npcPos.x) +
									// Math.abs(pos.y-npcPos.y))/blockSize;
									if (distIntSteps < 5) {
										// close repulsion from enemies
										totRew += -3;
									}
								} else {
									totRew += npcAttractionValue / (dist * dist + 0.05) * 1 / 50;
								}
							}
							count1++;
						}
					}
				}
			}
		}

		ArrayList<Observation>[] resPos = null;
		resPos = state.getResourcesPositions(pos);
		if (resPos != null) {
			for (ArrayList<Observation> res : resPos) {
				if (res.size() > 0) {
					// for(int i = 0; i< res.size(); i++){
					// only look at the closest rewarding/punishing npc
					for (int i = 0; i < 1; i++) {

						double resAttractionValue = 0;
						try {
							resAttractionValue = PersistentStorage.iTypeAttractivity.get(res.get(i).itype);
						} catch (NullPointerException e) {
							PersistentStorage.iTypeAttractivity.putIfAbsent(res.get(i));
							resAttractionValue = PersistentStorage.iTypeAttractivity.get(res.get(i).itype);
						}

						if (MCTSAgent.pathPlannerMaps.containsKey(res.get(i).itype)) {
							// compute the current distance to the closest enemy
							double distIntSteps = ((MCTSAgent.pathPlannerMaps).get(res.get(i).itype))
									.getStepsQtyToGoal(avaX, avaY);
							double maxPath = MCTSAgent.pathPlannerMaps.get(res.get(i).itype).getMaximumSteps();
							double dist = distIntSteps / maxPath;
							totRew += 3 * resAttractionValue / (dist * dist + 0.05) * 1 / 50;

							count1++;
						}
					}
				}
			}
		}

		// go towards the closest attracting movable:
		ArrayList<Observation>[] movPos = null;
		movPos = state.getMovablePositions(pos);
		if (movPos != null) {
			for (ArrayList<Observation> mov : movPos) {
				if (mov.size() > 0) {
					// for(int i = 0; i< res.size(); i++){
					// only look at the closest rewarding/punishing npc
					for (int i = 0; i < 1; i++) {

						double movAttractionValue = 0;
						try {
							movAttractionValue = PersistentStorage.iTypeAttractivity.get(mov.get(i).itype);
						} catch (NullPointerException e) {
							PersistentStorage.iTypeAttractivity.putIfAbsent(mov.get(i));
							movAttractionValue = PersistentStorage.iTypeAttractivity.get(mov.get(i).itype);
						}

						if (MCTSAgent.pathPlannerMaps.containsKey(mov.get(i).itype)) {
							// compute the current distance to the closest enemy
							if (movAttractionValue > 0) {
								double distIntSteps = MCTSAgent.pathPlannerMaps.get(mov.get(i).itype)
										.getStepsQtyToGoal(avaX, avaY);
								double maxPath = MCTSAgent.pathPlannerMaps.get(mov.get(i).itype).getMaximumSteps();
								double dist = distIntSteps / maxPath;

								totRew += 2 * movAttractionValue / (dist * dist + 0.05) * 1 / 50;
								count1++;
							}
						}

					}
				}
			}
		}

		// normalize this type of reward somehow
		if (count1 > 0)
			return totRew / count1;
		else
			return 0;
	}

	// old way to get an ItypeReward based on simple distance measure
	public double getNewExplITypeReward(StateObservation state, double nonJitterRew) {
		double totRew = 0;

		Vector2d pos = state.getAvatarPosition();
		double maxDist = Math.sqrt(Math.pow(PersistentStorage.rewMap.getDimension().height * state.getBlockSize(), 2)
				+ Math.pow(PersistentStorage.rewMap.getDimension().height * state.getBlockSize(), 2));

		int blockSize = state.getBlockSize();
		// int avaX = floorDiv((int) (pos.x + 0.1), blockSize);
		// int avaY = floorDiv((int) (pos.y + 0.1), blockSize);
		// PathPlanner pp = new PathPlanner();
		// pp.updateStart(avaX,avaY);
		//
		// double maxPath = (PersistentStorage.rewMap.getDimension().height +
		// PersistentStorage.rewMap.getDimension().width);

		int count1 = 0;

		// TODO: perhaps we should distinguish the cases where we want to kill the
		// enemies
		// Thus if we have the 5th or 3 actions we can also desire to go close ( or
		// align) with
		// the enemies: aligning seems better since it goes closer and is better for
		// shooting.
		ArrayList<Observation>[] npcPositions = null;
		npcPositions = state.getNPCPositions(pos);
		if (npcPositions != null) {
			for (ArrayList<Observation> npcs : npcPositions) {
				if (npcs.size() > 0) {
					// for(int i = 0; i< npcs.size(); i++){
					// only look at the closest rewarding/punishing npc
					for (int i = 0; i < 1; i++) {

						Vector2d npcPos = npcs.get(i).position;

						// int npcX = floorDiv((int) (npcPos.x + 0.1), blockSize);
						// int npcY = floorDiv((int) (npcPos.y + 0.1), blockSize);
						// pp.updateGoal(npcX, npcY);
						// pp.updateWays();
						// //compute the current distance to the closest enemy
						// double distIntSteps = pp.getDistance(avaX,avaY);
						// double dist = distIntSteps / maxPath;

						double npcAttractionValue = 0;

						try {
							npcAttractionValue = PersistentStorage.iTypeAttractivity.get(npcs.get(i).itype);
						} catch (NullPointerException e) {
							PersistentStorage.iTypeAttractivity.putIfAbsent(npcs.get(i));
							npcAttractionValue = PersistentStorage.iTypeAttractivity.get(npcs.get(i).itype);
						}

						double dist = Math.sqrt(Math.pow(pos.x - npcPos.x, 2) + Math.pow(pos.y - npcPos.y, 2))
								/ maxDist;

						if (npcAttractionValue < 0 && PersistentStorage.actions.length % 2 != 0
								&& npcAttractionValue > -1.5)
							totRew += Math.abs(npcAttractionValue) / (dist * dist + 0.1) * 1 / 10;
						else {
							// case of an enemy that killed us in a previous game
							if (npcAttractionValue < -1) {
								dist = Math.sqrt(Math.abs(pos.x - npcPos.x) + Math.abs(pos.y - npcPos.y)) / blockSize;
								if (dist < 0.2) {
									totRew += npcAttractionValue / (dist * dist + 0.1) * 1 / 10;
								}
							} else {
								totRew += npcAttractionValue / (dist * dist + 0.1) * 1 / 10;
							}
						}
						count1++;
					}
				}
			}
		}

		ArrayList<Observation>[] resPos = null;
		resPos = state.getResourcesPositions(pos);
		if (resPos != null) {
			for (ArrayList<Observation> res : resPos) {
				if (res.size() > 0) {
					// for(int i = 0; i< res.size(); i++){
					// only look at the closest rewarding/punishing npc
					for (int i = 0; i < 1; i++) {

						Vector2d resPosition = res.get(i).position;

						// int resX = floorDiv((int) (resPosition.x + 0.1), blockSize);
						// int resY = floorDiv((int) (resPosition.y + 0.1), blockSize);
						// pp.updateGoal(resX, resY);
						// pp.updateWays();
						// //compute the current distance to the closest enemy
						// double distIntSteps = pp.getDistance(avaX,avaY);
						// double dist = distIntSteps /maxPath;

						double resAttractionValue = 0;
						try {
							resAttractionValue = PersistentStorage.iTypeAttractivity.get(res.get(i).itype);
						} catch (NullPointerException e) {
							PersistentStorage.iTypeAttractivity.putIfAbsent(res.get(i));
							resAttractionValue = PersistentStorage.iTypeAttractivity.get(res.get(i).itype);
						}
						double dist = Math.sqrt(
								Math.pow((pos.x - resPosition.x), 2) + Math.pow(pos.y - resPosition.y, 2)) / maxDist;
						totRew += 3 * resAttractionValue / (dist * dist + 0.1) * 1 / 10;
						count1++;
					}
				}
			}
		}

		// go towards the closest attracting movable:
		ArrayList<Observation>[] movPos = null;
		movPos = state.getMovablePositions(pos);
		if (movPos != null) {
			for (ArrayList<Observation> mov : movPos) {
				if (mov.size() > 0) {
					// for(int i = 0; i< res.size(); i++){
					// only look at the closest rewarding/punishing npc
					for (int i = 0; i < 1; i++) {

						Vector2d movPosition = mov.get(i).position;

						// int movX = floorDiv((int) (movPosition.x + 0.1), blockSize);
						// int movY = floorDiv((int) (movPosition.y + 0.1), blockSize);
						// pp.updateGoal(movX, movY);
						// pp.updateWays();
						// //compute the current distance to the closest enemy
						// double distIntSteps = pp.getDistance(avaX,avaY);
						// double dist = distIntSteps /maxPath;

						double movAttractionValue = 0;
						try {
							movAttractionValue = PersistentStorage.iTypeAttractivity.get(mov.get(i).itype);
						} catch (NullPointerException e) {
							PersistentStorage.iTypeAttractivity.putIfAbsent(mov.get(i));
							movAttractionValue = PersistentStorage.iTypeAttractivity.get(mov.get(i).itype);
						}
						double dist = Math.sqrt(Math.pow(pos.x - movPosition.x, 2) + Math.pow(pos.y - movPosition.y, 2))
								/ maxDist;
						totRew += 2 * movAttractionValue / (dist * dist + 0.1) * 1 / 10;
						count1++;
					}
				}
			}
		}

		// normalize this type of reward somehow
		if (count1 > 0)
			return totRew / count1;
		else
			return 0;
	}

	public double value(StateObservation a_gameState) {

		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();
		double rawScore = a_gameState.getGameScore();

		if (gameOver && win == Types.WINNER.PLAYER_LOSES) {
			// return -Double.MAX_VALUE/10;
			return HUGE_NEGATIVE_REWARD;
		}

		if (gameOver && win == Types.WINNER.PLAYER_WINS) {
			return HUGE_POSITIVE_REWARD;
		}

		return rawScore;
	}

	public boolean finishRollout(StateObservation rollerState, int depth) {
		if (depth >= PersistentStorage.ROLLOUT_DEPTH) { // rollout end condition
			// occurs
			// "ROLLOUT_DEPTH" after the
			// MCTS/expand is finished
			return true;
		}
		if (rollerState.isGameOver()) { // end of game
			return true;
		}

		return false;
	}

	public void backUp(MCTSNode node, double result, int leaveNode) {
		// // add the rewards and visits the the chosen branch of the tree
		// MCTSNode n = node;
		//
		// while (n != null) {
		// n.nVisits++;
		// if(result < 0){
		// if(n.totValue > -Double.MAX_VALUE )
		// n.totValue += result;
		// else
		// n.totValue -= 100;
		// }
		// else{
		// if(n.totValue < Double.MAX_VALUE )
		// n.totValue += result;
		// else
		// n.totValue += 100;
		//
		// }
		//
		// n = n.parent;
		// // a little hack to compare deaths which are close by and those that
		// // are far away
		// if (result < 0)
		// result /= 2;
		// }

		MCTSNode n = node;

		while (n != null) {
			n.nVisits++;
			if (result < -1000) {

				if (leaveNode == 1) {
					n.totValue = n.totValue / 4 - 10000;
				} else {
					n.totValue = n.totValue / 4 - 10;
				}
			} else
				n.totValue += result;

			n = n.parent;
			leaveNode = 0;
			// a little hack to compare deaths which are close by and those that
			// are far away
			if (result < 0)
				result /= 2;
		}

	}

	public void backUpBest(MCTSNode node, double result) {
		// add the rewards and visits the the chosen branch of the tree
		MCTSNode n = node;
		while (n != null) {
			n.nVisits++;
			if (n.totValue < result) {
				n.totValue = result;
			}
			n = n.parent;
		}
	}

	public int mostVisitedAction() {
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		boolean allEqual = true;
		double first = -1;

		for (int i = 0; i < children.length; i++) {

			if (children[i] != null) {
				if (first == -1)
					first = children[i].nVisits;
				else if (first != children[i].nVisits) {
					allEqual = false;
				}

				if (children[i].nVisits + MCTSNode.m_rnd.nextDouble() * epsilon > bestValue) {
					bestValue = children[i].nVisits;
					selected = i;
				}
			}
		}

		if (selected == -1) {
			if (Agent.isVerbose) {
				System.out.println("MCTS::There are no visited actions!");
			}
			selected = 0;
		} else if (allEqual) {
			// If all are equal, we opt to choose for the one with the best Q.
			selected = bestAction();
		}
		selected = bestAction();
		return selected;
	}

	public int bestAction() {
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;

		for (int i = 0; i < children.length; i++) {

			// previous implementation lead to the tendency to choose the later
			// actions thats why the avatar
			// ended up in the top right corner in most cases.
			if (children[i] != null) {
				// we divide the reward by the number of times that we actually
				// tried that child ( the sqrt is there just for fun ;) )
				double disturbedChildRew = (children[i].totValue + (MCTSNode.m_rnd.nextDouble() - 0.5) * epsilon)
						/ (children[i].nVisits);
				if (disturbedChildRew > bestValue && !children[i].isDeadEnd(2)) {
					bestValue = disturbedChildRew;
					// bestValue = children[i].totValue;
					selected = i;
				}
			}
		}

		if (selected == -1) {
			if (Agent.isVerbose) {
				System.out.println("MCTS::Best action is no action!");
			}
		}
		return selected;
	}

	public boolean isDeadEnd(int max_depth) {
		MCTSNode cur = this;
		boolean allDeaths = true;

		// Base case
		if (max_depth == 0 || this.isLoseState() || this.stateType == StateType.WIN) {
			return this.isLoseState();
		} else {
			for (int i = 0; allDeaths && i < cur.children.length; i++) {
				if (cur.children[i] != null) {
					allDeaths = allDeaths && cur.children[i].isDeadEnd(max_depth - 1);
				} else {
					if (MCTSNode.m_rnd.nextDouble() > fear_of_unknown) {
						// Well, there's an unknown path, and we're not worried
						// - so let's guess it isn't a dead end!
						// if (Agent.isVerbose) {
						// System.out.println("MCTS::Overcame fear of unknown!");
						// }
						return false;
					}
				}
			}
			// Let the callers know if there is only death this way
			return allDeaths;
		}
	}

	public boolean isLoseState() {
		if (this.stateType == StateType.UNCACHED) {
			boolean gameOver = this.state.isGameOver();
			Types.WINNER win = this.state.getGameWinner();
			if (gameOver && win == Types.WINNER.PLAYER_LOSES) {
				this.stateType = StateType.LOSE;
				return true;
			} else {
				if (win == Types.WINNER.PLAYER_WINS) {
					this.stateType = StateType.WIN;
				} else {
					this.stateType = StateType.NORMAL;
				}
				return false;
			}
		} else {
			return this.stateType == StateType.LOSE;
		}
	}

	public boolean notFullyExpanded() {
		for (MCTSNode tn : children) {
			if (tn == null) {
				return true;
			}
		}
		return false;
	}

	public static double trapHeuristic(StateObservation a_gameState) {

		// return the number of movable objects that are apparently blocked, at
		// least for 1 move
		ArrayList<Observation>[] movePos = null;
		movePos = a_gameState.getMovablePositions();
		int isTrapped = 0;
		int isCompletelyFree = 0;
		double blockSquare = a_gameState.getBlockSize() * a_gameState.getBlockSize();
		if (movePos != null) {
			for (int j = 0; j < movePos.length; j++) {
				for (int i = 0; i < movePos[j].size(); i++) {
					Vector2d mPPos = movePos[j].get(i).position;

					ArrayList<Observation>[] trappedByImmovables = a_gameState.getImmovablePositions(mPPos);

					ArrayList<Observation>[] trappedByMovables = a_gameState.getMovablePositions(mPPos);

					if (trappedByImmovables != null && trappedByImmovables.length > 0) {
						// if surrounded by 3 objects, its trapped
						if (trappedByImmovables[0].size() >= 3) {
							if ((trappedByImmovables[0].get(0).sqDist - blockSquare) < 1
									&& (trappedByImmovables[0].get(1).sqDist - blockSquare) < 1
									&& (trappedByImmovables[0].get(2).sqDist - blockSquare) < 1) {
								isTrapped++;
							}
						}
						// if surrounded by a corner its trapped
						if (trappedByImmovables[0].size() >= 2) {
							if ((trappedByImmovables[0].get(0).sqDist - blockSquare) < 1
									&& (trappedByImmovables[0].get(1).sqDist - blockSquare) < 1
									&& Math.abs((trappedByImmovables[0].get(1).position.x
											- trappedByImmovables[0].get(0).position.x)
											* (trappedByImmovables[0].get(1).position.y
													- trappedByImmovables[0].get(0).position.y)) > 1) {
								isTrapped++;
							} else {
								// if surrounded by two immovable objects and a
								// movable object its trapped
								if (trappedByMovables != null && trappedByMovables.length > 0) {
									if (trappedByMovables[0].size() > 1) {
										if ((trappedByImmovables[0].get(0).sqDist - blockSquare) < 1
												&& (trappedByImmovables[0].get(1).sqDist - blockSquare) < 1
												&& (trappedByMovables[0].get(1).sqDist - blockSquare) < 1) {
											isTrapped++;
										}
									}
								}
							}
						}
					}

					if (trappedByImmovables != null && trappedByImmovables.length > 0 && trappedByMovables != null
							&& trappedByMovables.length > 0) {
						// reward movable objects that are not surrounded by
						// anything
						if (trappedByImmovables[0].size() > 0) {
							if (trappedByMovables[0].size() > 1) {
								if ((trappedByImmovables[0].get(0).sqDist - blockSquare) > 1
										&& (trappedByMovables[0].get(1).sqDist - blockSquare) > 1) {
									isCompletelyFree++;
								}
							} else {
								if (trappedByImmovables[0].get(0).sqDist - blockSquare > 1) {
									isCompletelyFree++;
								}

							}
						}
					}
				}
			}
		}
		// reward the completely free objects not as much as the trapped ones.
		return isTrapped - isCompletelyFree / 2;
	}

	public void correctDepth() {
		// should correct (subtract 1 of) the depth of the whole tree. Needed
		// after cut, but seems to be to slow
		MCTSNode root = this;
		root.m_depth -= 1;
		for (int i = 0; i < root.children.length; i++) {
			if (root.children[i] != null) {
				// search for ALL children being null!
				root.children[i].correctDepth();
			}
		}
	}

	public static int floorDiv(int x, int y) {
		int r = x / y;
		// if the signs are different and modulo not zero, round down
		if ((x ^ y) < 0 && (r * y != x)) {
			r--;
		}
		return r;
	}

}
