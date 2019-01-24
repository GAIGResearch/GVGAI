package agents.bladerunner.agents.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;
import agents.bladerunner.agents.hbfs.HBFSAgent;
import agents.bladerunner.agents.misc.pathplanning.PathPlanner;

public class ObservationTools {

	// Analysis of either the root node or the node2 of the node transition
	// root->...->node1->node2
	public static class DefaultAnalysis {
		public int load; // total number of tiles
		public int tileDestructions;
		public int tileCreations;
		public int tileTransforms; // total number of tile transforms w.r.t.
									// root (a
		// tile vanishes or transforms into another one,
		// movement does not count)
		public int tileMovements; // total number of tile movements w.r.t. root
									// (a tile
		// moves from one pos. to another)
		public int relevantEvents; // not so important; total number of relevant
									// events
		// w.r.t. root (all except irrelevant events)
		public int irrelevantEvents; // not so important; total number of
										// relevant
		// events w.r.t. root (events that involve walls
		// etc...)
		public int trappedTiles; // TODO: (maybe) if you want the newly trapped
									// tiles
		// you have to subtract the root trapped tiles, one
		// could just calculate them when the root is set,
		// but I was not sure if you want that.
		public double ResourceValue; // an attractivity weighted (not jet
										// implemented)

		// sum of resources minus the resources that
		// were already there

		// double transformationScore; //If we somehow find out if a
		// transformation is good (maybe with a more sophisticated
		// iTypeAttractivity-mao), we could reward the agent with that, even if
		// it does not get any score in the game for that step

		public void print() {
			System.out.println("ObservationTools: ");
			System.out.println("load: " + load);
			System.out.println("tileDestructions: " + tileDestructions);
			System.out.println("tileCreations: " + tileCreations);
			System.out.println("tileTransforms: " + tileTransforms);
			System.out.println("tileMovements: " + tileMovements);
			System.out.println("relevantEvents: " + relevantEvents);
			System.out.println("irrelevantEvents: " + irrelevantEvents);
			System.out.println("trappedTiles: " + trappedTiles);
			System.out.println("added ResourceValue: " + ResourceValue);
			// System.out.println("transformationScore: "+ transformationScore);
			System.out.println();

		}
	}

	private static HashMap<Integer, Integer> rootObsList;
	private static StateObservation rootso;

	// Computes hash code for the StateObservation. Used to organize the list of
	// visited states.
	// Rotating hash for sequences of small values:
	// http://burtleburtle.net/bob/hash/doobs.html
	public static int getHash(StateObservation so) {
		int sequenceLength = so.getWorldDimension().height * so.getWorldDimension().width + 2;
		if (HBFSAgent.RESPECT_AGENT_ORIENTATION)
			sequenceLength += 2;
		if (HBFSAgent.REPSECT_AGENT_SPEED)
			sequenceLength += 1;
		int hash = sequenceLength;
		ArrayList<Observation>[][] grid = so.getObservationGrid();
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				for (Observation o : grid[i][j]) {
					hash = (hash << 4) ^ (hash >> 28) ^ o.itype;
				}
			}
		}
		hash = (hash << 4) ^ (hash >> 28) ^ ((int) so.getAvatarPosition().x);
		hash = (hash << 4) ^ (hash >> 28) ^ ((int) so.getAvatarPosition().y);

		if (HBFSAgent.RESPECT_AGENT_ORIENTATION) {
			hash = (hash << 4) ^ (hash >> 28) ^ ((int) so.getAvatarOrientation().x);
			hash = (hash << 4) ^ (hash >> 28) ^ ((int) so.getAvatarOrientation().y);
		}

		if (HBFSAgent.REPSECT_AGENT_SPEED) {
			hash = (hash << 4) ^ (hash >> 28) ^ ((int) so.getAvatarSpeed());
		}

		// hash = hash % HBFSAgent.prime;
		return hash;
	}

	public static int getLoad(StateObservation so) {
		int load = 0;
		ArrayList<Observation>[][] grid = so.getObservationGrid();
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				load += grid[i][j].size();
			}
		}
		return load;
	}

	// Analysis for root node
	// Potential speed up: First check whether events took place, if so, update
	// the state observation
	// Alpha:I do not see, how this Analysis can be done without a parent, so I
	// created three methods:
	// One to set the root, one to compare to the root and one to compare two
	// arbitrary StateObservations
	// I do not see any way to speed it up, as there are changes that happen
	// without an (official history) event (e.g. Block or Lemming in Portal )
	// So tile transformations is more general than events.

	/*
	 * getAnalysis with only one observation as parameter automatically compares to
	 * the root please set root before you use this
	 */
	public static DefaultAnalysis getAnalysis(StateObservation so) {
		DefaultAnalysis analysis;
		analysis = analyze(rootObsList, rootso, so);
		return analysis;
	}

	/*
	 * getAnalysis with two observation as parameter compares the two observations
	 */
	public static DefaultAnalysis getAnalysis(StateObservation so, StateObservation parent) {
		DefaultAnalysis analysis;
		analysis = analyze(getObsList(parent), parent, so);
		return analysis;
	}

	/*
	 * sets the root observation
	 */
	public static void setRoot(StateObservation so) {
		rootObsList = getObsList(so);
		rootso = so;
	}

	/*
	 * returns a HashMap of non wall ObservationIDs and the corresponding iTypes
	 */
	public static HashMap<Integer, Integer> getObsList(StateObservation so) {
		HashMap<Integer, Integer> ObsList = new HashMap<Integer, Integer>();
		ArrayList<Observation>[][] grid = so.getObservationGrid();
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				for (Observation obs : grid[i][j]) {
					if (obs.itype != 0) {
						ObsList.put(obs.obsID, obs.itype);
					}
				}
			}
		}
		return ObsList;
	}

	/*
	 * This is Jakob's Trap Heuristic from the MCTS Node
	 */
	public static int getnTrapped(StateObservation a_gameState) {

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
		//
		return isTrapped;
	}

	/*
	 * potential TODO: The movements are only calculated for the movables, not for
	 * npcs or resources, if you think that it is necessary please add this
	 */
	private static int getMovements(StateObservation parentSo, StateObservation so) {
		int nMov = 0;

		HashMap<Integer, Vector2d> obsList = new HashMap<Integer, Vector2d>();
		ArrayList<Observation>[] movPositions = so.getMovablePositions();
		if (movPositions != null) {
			for (ArrayList<Observation> movPos : movPositions) {
				for (Observation obs : movPos) {
					obsList.put(obs.obsID, obs.position);
				}
			}
		}
		HashMap<Integer, Vector2d> parentObsList = new HashMap<Integer, Vector2d>();
		ArrayList<Observation>[] parentMovPositions = parentSo.getMovablePositions();
		if (parentMovPositions != null) {
			for (ArrayList<Observation> movPos : parentMovPositions) {
				for (Observation obs : movPos) {
					parentObsList.put(obs.obsID, obs.position);
				}
			}
		}

		for (int key : obsList.keySet()) {
			if (parentObsList.containsKey(key)) {
				if (obsList.get(key) != parentObsList.get(key)) {
					nMov += 1;
				}
			}

		}

		return nMov;
	}

	/*
	 * this method does the actual analysis
	 */
	public static DefaultAnalysis analyze(HashMap<Integer, Integer> rootObsList, StateObservation parentSo,
			StateObservation so) {
		DefaultAnalysis analysis = new DefaultAnalysis();
		HashMap<Integer, Integer> obsList = getObsList(so);
		int currObsNumber = obsList.size();
		int rootObsNumber = rootObsList.size();
		// compare the obsLists
		HashSet<Integer> commonObs = new HashSet<Integer>(obsList.keySet());
		commonObs.retainAll(rootObsList.keySet());
		int commonObsNumber = commonObs.size();
		// are there cases, where the iType changes?
		int transforms = 0;
		for (int key : commonObs) {
			if (obsList.get(key) != rootObsList.get(key)) {
				transforms += 1;
			}
		}

		// handle the events
		int nEvents = so.getHistoricEventsHistory().size();
		int nRelEvents = 0;
		for (Event e : so.getHistoricEventsHistory()) {
			if (e.passiveTypeId != 0) {
				nRelEvents += 1;
			}
		}

		int nParEvents = parentSo.getHistoricEventsHistory().size();
		int nRelParEvents = 0;
		for (Event e : parentSo.getHistoricEventsHistory()) {
			if (e.passiveTypeId != 0) {
				nRelParEvents += 1;
			}
		}

		// check the resources
		double weightedResValue = 0;
		for (int res : so.getAvatarResources().keySet()) {
			weightedResValue += so.getAvatarResources().get(res); // *ResAttractivity.get(res)
																	// //it can
																	// be
																	// weighted
																	// by a
																	// resource
		}
		for (int res : parentSo.getAvatarResources().keySet()) {
			weightedResValue = -so.getAvatarResources().get(res); // *ResAttractivity.get(res)
																	// //it can
																	// be
																	// weighted
																	// by a
																	// resource
		}

		analysis.load = obsList.size(); // total number of tiles
		analysis.tileCreations = currObsNumber - commonObsNumber;
		analysis.tileDestructions = rootObsNumber - commonObsNumber;
		analysis.tileTransforms = transforms; // TODO: these are only real itype
												// transforms, be aware, that
												// they do not always happen
												// when you expect them to
		// total number of tile transforms w.r.t. root (a tile vanishes or
		// transforms into another one, movement does not count)
		analysis.tileMovements = getMovements(parentSo, so); // total number of
																// tile
																// movements
																// w.r.t. root
																// (a tile moves
																// from one pos.
																// to another)
		analysis.relevantEvents = nRelEvents - nRelParEvents; // not so
																// important;
																// total number
																// of relevant
																// events w.r.t.
																// root (all
																// except
																// irrelevant
																// events)
		analysis.irrelevantEvents = (nEvents - nRelEvents) - (nParEvents - nRelParEvents); // not
																							// so
																							// important;
																							// total
																							// number
																							// of
																							// relevant
																							// events
																							// w.r.t.
																							// root
																							// (events
																							// that
																							// involve
																							// walls
																							// etc...)
		analysis.trappedTiles = ObservationTools.getnTrapped(so);
		analysis.ResourceValue = weightedResValue;
		// analysis.transformationScore; //if we can use that, I can try to
		// implement it

		return analysis;
	}

	HashMap<Integer, Integer> getRessourceDifference(StateObservation so) {
		HashMap<Integer, Integer> ressourceDifference = new HashMap<>();

		for (int k : so.getAvatarResources().keySet()) {
			if (PersistentStorage.previousAvatarRessources.containsKey(k)) {
				ressourceDifference.put(k,
						so.getAvatarResources().get(k) - PersistentStorage.previousAvatarRessources.get(k));
			} else {
				ressourceDifference.put(k, so.getAvatarResources().get(k));
			}
		}

		// PersistentStorage.previousAvatarRessources = so.getAvatarResources();

		return ressourceDifference;
	}

	public static int getRessourceDifferenceIndicator(StateObservation so) {
		int indicator = 0;
		for (int k : so.getAvatarResources().keySet()) {
			if (PersistentStorage.previousAvatarRessources.containsKey(k)) {
				indicator += so.getAvatarResources().get(k) - PersistentStorage.previousAvatarRessources.get(k);
			} else {
				indicator += so.getAvatarResources().get(k);
			}
		}

		// PersistentStorage.previousAvatarRessources = so.getAvatarResources();

		return indicator;
	}

	private static PathPlanner pathPlanner = null;

	public static PathPlanner getPathPlanner() {
		if (pathPlanner == null) {
			pathPlanner = new PathPlanner();
		}
		return pathPlanner;

	}
}
