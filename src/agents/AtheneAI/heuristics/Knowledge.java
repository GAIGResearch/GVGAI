package agents.AtheneAI.heuristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import agents.AtheneAI.search.ehc.EHCPlayer;
import agents.AtheneAI.search.waysolving.A_Star;
import agents.AtheneAI.search.waysolving.Grid;
import agents.AtheneAI.search.waysolving.GridElement;
import agents.AtheneAI.util.GameTypeChecker;
import agents.AtheneAI.util.Util;

/**
 * Class for collecting, retrieving and updating knowledge about sprites.
 * 
 * TODO: when agent loses/wins his position is (-1, -1), check for problems!
 * TODO: check everywhere for NPEs etc. TODO: check for waysolving over movables
 * (see boloadventures level 0: should reach portal and vulcano here!!)
 */
public class Knowledge {

	// Sprite Categories
	private static final int CATEGORY_RESOURCES = 1;
	private static final int CATEGORY_PORTALS = 2;
	private static final int CATEGORY_NPC = 3;
	private static final int CATEGORY_IMMOVABLE = 4;
	private static final int CATEGORY_MOVABLE = 6;

	// configuration
	private static final int NR_COLLISIONS_TO_BE_UNINTERESTING = 3;
	private static final int EHC_MAX_ITERATION_TIME_IN_MS = 300;
	private static final int NR_OBS_UNREACHABLE_FOR_TYPE_UNREACHABLE = 3;
	private static final int DO_SINGLE_MOVE_UNTIL = 5;
	private static final int DO_ANOTHER_ITERATION_UNTIL = 100;

	// events to be detected after collision
	public static final int WIN = 0;
	public static final int LOSS = 1;
	public static final int SCORE_DOWN = 2;
	public static final int SCORE_UP = 3;
	public static final int INVENTORY_DECREASED = 4;
	public static final int INVENTORY_INCREASED = 5;
	public static final int NEW_IMMOV_TYPE = 6;
	public static final int NEW_RES_TYPE = 7;

	// number of events that should be detected (size of an experience array for
	// each type)
	public static final int TOTAL_OBSERVABLE_EVENTS = 8;

	// way costs of sprite types
	public static final int WAY_COSTS_FREE = 1;
	public static final int WAY_COSTS_USE = 2;
	public static final int WAY_COSTS_MOVABLE = 50;
	public static final int WAY_COSTS_BLOCKED = 1000;
	public static final int VALID_FOR_ALL_AGENT_TYPES = -1;
	private static HashMap<Integer, HashMap<Integer, Integer>> wayCosts = new HashMap<Integer, HashMap<Integer, Integer>>();

	// Accumulated Knowledge
	private static Map<Integer, Map<Integer, int[]>> experience = new HashMap<Integer, Map<Integer, int[]>>();
	private static HashMap<Integer, Double> spawners = new HashMap<Integer, Double>();
	private static HashMap<Integer, Double> exits = new HashMap<Integer, Double>();
	private static int blockSize = 0;
	private static ArrayList<ACTIONS> availableActions = null;

	// information about current game
	private HashMap<Integer, HashMap<Integer, Integer>> collisions = new HashMap<Integer, HashMap<Integer, Integer>>();
	private HashMap<Integer, HashMap<Integer, List<Integer>>> unreachableObsIDs = new HashMap<Integer, HashMap<Integer, List<Integer>>>();
	private HashMap<Integer, List<Integer>> unreachableTypeIDs = new HashMap<Integer, List<Integer>>();
	private HashMap<Integer, List<Integer>> spawnedObjects = new HashMap<Integer, List<Integer>>();;
	private int ehcIterations = 0;
	private int aStarIterations = 0;
	private double ehcAvgTime = 0;
	private double ehcAcumTime = 0;
	private double aStarAvgTime = 0;
	private double aStarAcumTime = 0;
	private StateObservation currentState = null;
	private StateObservation startStateBackup = null;
	private Observation mostInterestingObservation = null;
	private int[] currentTarget = new int[] { -1, -1 };
	private int spawnerDetections = 0;
	private int gameTicksSimulated = 0;
	private ElapsedCpuTimer timer = null;

	// true to get detailed information printed
	private boolean debug = false;

	public static void initKnowledge(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, boolean catchingGame) {
		new Knowledge(stateObs, elapsedTimer, catchingGame);
		printExperience();
		printWayCosts();
		printSpawners();
	}

	/**
	 * Constructor
	 */
	private Knowledge(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, boolean catchingGame) {

		// backup the start state to begin again when a move terminates the game
		startStateBackup = stateObs.copy();
		currentState = stateObs;
		timer = elapsedTimer;
		currentTarget = new int[] { -1, -1 };
		blockSize = stateObs.getBlockSize();
		availableActions = stateObs.getAvailableActions(true);

		// getting this here addresses cases where portals disappear (TODO: however the
		// case that portals appear in the end is not handled!)
		ArrayList<Observation>[] portals = stateObs.getPortalsPositions();

		if (!catchingGame) {
			boolean exploreObjects = true;

			// keep some time left
			while (elapsedTimer.remainingTimeMillis() > Knowledge.DO_ANOTHER_ITERATION_UNTIL) {

				if (exploreObjects) {
					exploreObjects = doAnotherIteration();

					if (!exploreObjects && debug)
						System.out.println("doing just NIL and monitoring gamestate from now on ............");

					// TODO: try again after some time!!
					// TODO: try again after the agent died!! (this can be done manually!!)

				} else {

					// this will cause the avatar to do NIL, so that we are able to further monitor
					// the gamestate.
					makeSingleMove(null, null, true);
				}
			}

			// assign a probability to be a spawner to each portal type
			if (portals != null) {
				for (ArrayList<Observation> portalType : portals) {
					if (portalType != null && !portalType.isEmpty()) {
						if (spawners.containsKey(portalType.get(0).itype)) {
							spawners.put(portalType.get(0).itype,
									Math.min(1.0, (spawners.get(portalType.get(0).itype) / portalType.size())
											/ this.spawnerDetections));
						} else {
							spawners.put(portalType.get(0).itype, (double) 0);
						}
					}
				}
			}

		} else {
			EHCPlayer ehcPlayer = new EHCPlayer(availableActions);

			// keep some time left
			while (elapsedTimer.remainingTimeMillis() > Knowledge.DO_ANOTHER_ITERATION_UNTIL) {
				int agentType = Util.getAgentType(stateObs);

				// determine best action and advance
				ACTIONS action = ehcPlayer.getBestAction(stateObs, elapsedTimer,
						getInterestingMovables(stateObs, agentType));
				StateObservation lastState = stateObs.copy();
				stateObs.advance(action);
				gameTicksSimulated++;

				// test if there was a collision
				boolean collision = !stateObs.getHistoricEventsHistory().isEmpty()
						&& stateObs.getHistoricEventsHistory().last().gameStep == lastState.getGameTick();

				if (collision) {
					Event e = stateObs.getHistoricEventsHistory().last();
					int collidedWithTypeID = e.passiveTypeId;

					if (debug)
						System.out.println("there was a collision with typeID=" + collidedWithTypeID);

					evaluateCollision(lastState, stateObs, e);

					// count collisions per type for their future interestingness
					addCollision(agentType, collidedWithTypeID);
				}

				// if game was finished by the move, begin simulation again
				if (stateObs.isGameOver()) {
					startAgain();
				}
			}
		}

		printCollisions();
		System.out.println("remaining time after Knowledge was created: " + elapsedTimer.remainingTimeMillis() + "ms");
		System.out.println("simulated gameticks: " + gameTicksSimulated);
	}

	/**
	 * Selects next object to chase and determines how to reach it. Returns false if
	 * there are no more objects found to chase or not enough time left, otherwise
	 * true.
	 */
	private boolean doAnotherIteration() {
		A_Star aStar = new A_Star(currentState, this.timer, Knowledge.DO_ANOTHER_ITERATION_UNTIL);
		List<ACTIONS> actionPathToObject = null;
		int agentType = Util.getAgentType(currentState);

		if (debug) {
			System.out.println("************** starting new iteration (gametick = " + currentState.getGameTick()
					+ ", simulated tick = " + this.gameTicksSimulated + ") **************");
			Util.printDebugGameState(currentState);
		}

		// what object is most interesting to collide with?
		getMostInterestingObservation(currentState, agentType);

		// take time for current iteration
		ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

		// do A* if interesting immovable found and time is sufficient
		if (mostInterestingObservation != null) {
			if (debug)
				System.out.println("most interesting observation is typeID=" + mostInterestingObservation.itype
						+ " obsID=" + mostInterestingObservation.obsID);

			if (mostInterestingObservation.category != Knowledge.CATEGORY_NPC
					&& timer.remainingTimeMillis() > aStarAvgTime) {

				Vector2d startPos = currentState.getAvatarPosition();
				Vector2d targetPos = mostInterestingObservation.position;
				if (aStar.solve((int) (startPos.x / blockSize), (int) (startPos.y / blockSize),
						(int) (targetPos.x / blockSize), (int) (targetPos.y / blockSize), true, false)) {
					actionPathToObject = aStar.getActionSequence(GameTypeChecker.doubleMovesNeeded(currentState),
							Util.getDirection(currentState.getAvatarOrientation()));
					if (debug)
						System.out.println("A* found a path");

				} else {
					if (debug)
						System.out.println("A* found no path");
					setObservationUnreachable(agentType, mostInterestingObservation.itype,
							mostInterestingObservation.obsID);
					return true;
				}
			}

			// do EHC if interesting movable id found and time is sufficient
			else if (mostInterestingObservation.category == Knowledge.CATEGORY_NPC
					&& timer.remainingTimeMillis() > ehcAvgTime) {
				if (debug)
					System.out.println("EHC will be applied");
			}

			else {
				if (debug)
					System.out.println("time runs out");
				return false;
			}
		}

		// nothing more to explore or not enough time
		else {
			if (debug)
				System.out.println(
						"nothing interesting to explore anymore, simulated gameticks so far = " + gameTicksSimulated);
			return false;
		}

		// enough time to make a sigle move?
		while (timer.remainingTimeMillis() > Knowledge.DO_SINGLE_MOVE_UNTIL) {
			if (!makeSingleMove(actionPathToObject, elapsedTimerIteration, false)) {
				break;
			}
		}

		// update time estimations
		if (mostInterestingObservation.category == Knowledge.CATEGORY_NPC) {
			ehcAcumTime += (elapsedTimerIteration.elapsedMillis());
			ehcIterations++;
			ehcAvgTime = ehcAcumTime / ehcIterations;
		} else {
			aStarAcumTime += (elapsedTimerIteration.elapsedMillis());
			aStarIterations++;
			aStarAvgTime = aStarAcumTime / aStarIterations;
		}

		return true;
	}

	/**
	 * Makes a single move and identifies collisions. Returns false if the current
	 * target should be abandoned and another object should be chased, otherwise
	 * true.
	 */
	private boolean makeSingleMove(List<ACTIONS> actionPathToObject, ElapsedCpuTimer elapsedTimerIteration,
			boolean justDoNil) {
		boolean lastMoveOnPath = false;
		int agentType = Util.getAgentType(currentState);

		// select a move to make
		ACTIONS action = ACTIONS.ACTION_NIL;
		if (!justDoNil && mostInterestingObservation != null
				&& mostInterestingObservation.category == Knowledge.CATEGORY_NPC) {

			// if EHC takes too long, movables are probably not reachable
			if (elapsedTimerIteration.elapsedMillis() > EHC_MAX_ITERATION_TIME_IN_MS) {
				setTypeUnreachable(agentType, mostInterestingObservation.itype);
				return false;
			}
			action = ehc(currentState.copy(), mostInterestingObservation.itype);

		} else if (!justDoNil && mostInterestingObservation != null) {

			// are there more actions to make on the current A* path?
			if (actionPathToObject != null && !actionPathToObject.isEmpty()) {
				action = actionPathToObject.get(0);
				actionPathToObject.remove(0);

				if (actionPathToObject.isEmpty()) {
					lastMoveOnPath = true;
				}

			} else {
				return false;
			}
		}

		// make the selected move
		StateObservation lastState = currentState.copy();
		currentState.advance(action);
		gameTicksSimulated++;

		// do some things regulary
		if ((gameTicksSimulated % 100) < 50) {
			detectSpawners(lastState, currentState);
		}
		if (lastState.getGameTick() % 50 == 0) {
			updateWaycostsForNewMovables(currentState);
		}

		// if it was the last move on the current A* path, check if we reached the
		// target and update waycosts
		boolean steppedOnTarget = false;
		if (lastMoveOnPath) {

			int posX = (int) currentState.getAvatarPosition().x / blockSize;
			int posY = (int) currentState.getAvatarPosition().y / blockSize;
			steppedOnTarget = currentTarget[0] == posX && currentTarget[1] == posY;

			int newWayCosts = Knowledge.WAY_COSTS_BLOCKED;

			if (steppedOnTarget) {

				if (debug)
					System.out.println("stepped on target");

				// agent could reach the target, which also means he has not died!
				newWayCosts = Knowledge.WAY_COSTS_FREE;

				// agent could not step on target but stands directly next to it
			} else if (Util.manhattanDistInBlocks(currentTarget[0], currentTarget[1], posX, posY) == 1) {

				// maybe we can destroy the object with USE?!
				if (availableActions.contains(ACTIONS.ACTION_USE)) {
					StateObservation temp = currentState.copy();

					// try the USE-Action on the object
					temp.advance(ACTIONS.ACTION_USE);

					// now retry the previous action
					temp.advance(action);

					posX = (int) temp.getAvatarPosition().x / blockSize;
					posY = (int) temp.getAvatarPosition().y / blockSize;
					if (currentTarget[0] == posX && currentTarget[1] == posY) {
						if (debug)
							System.out.println("stepped on target after applying USE");
						newWayCosts = Knowledge.WAY_COSTS_USE;
					}
				}

			} else {

				// sth completely went wrong -> ignore
				setObservationUnreachable(agentType, mostInterestingObservation.itype,
						mostInterestingObservation.obsID);
			}

			// clear unreachable lists if an immovable id assumed to be blocked has become
			// free
			if (getWayCosts(agentType, mostInterestingObservation.itype) == Knowledge.WAY_COSTS_BLOCKED
					&& newWayCosts != Knowledge.WAY_COSTS_BLOCKED) {
				if (debug)
					System.out.println("updated waycosts, clearing unreachables");
				this.unreachableObsIDs.clear();
				this.unreachableTypeIDs.clear();
			}

			// update
			setWayCosts(agentType, mostInterestingObservation.itype, newWayCosts);
		}

		// test if there was a collision
		boolean collision = !currentState.getHistoricEventsHistory().isEmpty()
				&& currentState.getHistoricEventsHistory().last().gameStep == lastState.getGameTick();

		if (collision || steppedOnTarget) {
			int collidedWithTypeID = 0;

			// evaluate collision for features
			Event e;
			if (collision) {
				e = currentState.getHistoricEventsHistory().last();
				collidedWithTypeID = e.passiveTypeId;
			} else {
				collidedWithTypeID = mostInterestingObservation.itype;
				e = new Event(lastState.getGameTick(), false, agentType, collidedWithTypeID, 0,
						mostInterestingObservation.obsID, lastState.getAvatarPosition());
			}
			if (debug)
				System.out.println("there was a collision with typeID=" + collidedWithTypeID);
			evaluateCollision(lastState, currentState, e);

			// count collisions per type for their future interestingness
			addCollision(agentType, collidedWithTypeID);

			// test if EHC iteration can be finished
			if (!justDoNil && mostInterestingObservation != null
					&& mostInterestingObservation.category == Knowledge.CATEGORY_NPC
					&& collidedWithTypeID == mostInterestingObservation.itype) {
				return false;
			}
		}

		// finish Iteration if this was the last move
		if (lastMoveOnPath)
			return false;

		// if game was finished by the move, begin simulation again
		if (currentState.isGameOver()) {
			startAgain();
			return false;
		}

		return true;
	}

	/**
	 * Heuristic for walking towards the nearest interseting npc of the current
	 * gamestate.
	 */
	private ACTIONS ehc(StateObservation state, int typeID) {
		int nearestDistance = 100000;
		ACTIONS bestAction = ACTIONS.ACTION_NIL;

		for (ACTIONS action : availableActions) {

			if (action == ACTIONS.ACTION_ESCAPE || action == ACTIONS.ACTION_USE) {
				continue;
			}

			StateObservation stateCopy = state.copy();
			stateCopy.advance(action);

			ArrayList<Observation>[] npcs = stateCopy.getNPCPositions(stateCopy.getAvatarPosition());

			if (npcs != null) {
				for (ArrayList<Observation> npcType : npcs) {
					if (npcType != null && !npcType.isEmpty() && npcType.get(0).itype == typeID) {
						int distance = Util.manhattanDistInBlocks(stateCopy.getAvatarPosition(),
								npcType.get(0).position, blockSize);

						if (distance < nearestDistance) {
							nearestDistance = distance;
							bestAction = action;
						}
					}
				}
			}
		}

		return bestAction;
	}

	/**
	 * Call this regulary to detect spawners. Spawners are "bad" portals so we
	 * should not step on them and should not assume them to be exits later in the
	 * real game. Here only the frequency of being detected as a spawner for each
	 * portal type is accumulated. At the end of init, this is transformed into a
	 * probability for each portal type to be a spawner.
	 * 
	 * TODO: also immovables can be spawners! (e.g. boloadventures)
	 */
	private void detectSpawners(StateObservation state1, StateObservation state2) {
		this.spawnerDetections++;

		ArrayList<Observation>[] portalsState2 = state2.getPortalsPositions();
		ArrayList<Observation>[][] observationsState2 = state2.getObservationGrid();

		if (portalsState2 != null) {
			for (ArrayList<Observation> portalType : portalsState2) {
				if (portalType != null && !portalType.isEmpty()) {
					for (Observation portal : portalType) {

						int col = (int) portal.position.x / state2.getBlockSize();
						int row = (int) portal.position.y / state2.getBlockSize();
						ArrayList<Observation> observationsOnPortalState2 = observationsState2[col][row];

						if (observationsOnPortalState2.size() > 1) {

							// Spawners spawn either dynamic movables or npcs
							ArrayList<Observation>[] movablesState1 = state1.getMovablePositions();
							ArrayList<Observation>[] npcsState1 = state1.getNPCPositions();

							// System.out.println("things on portal itype=" + portal.itype + ", position=" +
							// portal.position + ":");
							for (Observation o : observationsOnPortalState2) {
								Observation sameObsInEarlierState = null;

								// spawners are assumed to spawn only dynamic movables or npcs
								if (o.category != Knowledge.CATEGORY_MOVABLE && o.category != Knowledge.CATEGORY_NPC) {
									continue;
								}

								// this partially prevents some weird effects
								if (spawnedObjects.containsKey(o.itype)) {
									if (spawnedObjects.get(o.itype).contains(o.obsID)) {
										// System.out.println("already spawned");
										continue;
									}
								}

								if (o.category == Knowledge.CATEGORY_MOVABLE) {
									// System.out.println("(movable) itype=" + o.itype + ", obsID=" + o.obsID + ",
									// position=" + o.position);

									if (movablesState1 != null) {
										for (ArrayList<Observation> movType : movablesState1) {
											if (movType != null && !movType.isEmpty()) {
												if (movType.get(0).itype != o.itype)
													continue;
												for (Observation mov : movType) {
													if (o.obsID == mov.obsID) {
														// System.out.println("found in earlier state, position=" +
														// mov.position);
														sameObsInEarlierState = mov;
														break;
													}
												}
											}
										}
									}
								}

								else if (o.category == Knowledge.CATEGORY_NPC) {
									// System.out.println("(npc) itype=" + o.itype + ", obsID=" + o.obsID + ",
									// position=" + o.position);
									if (npcsState1 != null) {
										for (ArrayList<Observation> npcType : npcsState1) {
											if (npcType != null && !npcType.isEmpty()) {
												if (npcType.get(0).itype != o.itype)
													continue;
												for (Observation npc : npcType) {
													if (o.obsID == npc.obsID) {
														// System.out.println("found in earlier state, position=" +
														// npc.position);
														sameObsInEarlierState = npc;
														break;
													}
												}
											}
										}
									}
								}

								if (sameObsInEarlierState == null) {

									// this partially prevents some weird effects
									if (!spawnedObjects.containsKey(o.itype)) {
										spawnedObjects.put(o.itype, new LinkedList<Integer>());
									}
									spawnedObjects.get(o.itype).add(o.obsID);

									// this marks this portal to be detected as a spawner
									if (spawners.containsKey(portal.itype)) {
										spawners.put(portal.itype, spawners.get(portal.itype) + 1);
									} else {
										spawners.put(portal.itype, (double) 1);
									}
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Call this regulary to update waycosts for dynamic movables which are
	 * appearing at some point in time. Otherwise the waycosts for these movables
	 * will not be known and therefore assumed to be BLOCKED.
	 */
	private void updateWaycostsForNewMovables(StateObservation stateObs) {
		ArrayList<Observation>[] movables = stateObs.getMovablePositions();
		if (movables != null) {
			for (ArrayList<Observation> movType : movables) {
				if (movType != null && !movType.isEmpty()) {
					setWayCosts(Knowledge.VALID_FOR_ALL_AGENT_TYPES, movType.get(0).itype, Knowledge.WAY_COSTS_MOVABLE);
				}
			}
		}
	}

	/**
	 * Returns the most interesting Observation in the given state observation.
	 */
	private void getMostInterestingObservation(StateObservation stateObs, int agentType) {
		int leastCollisions = Knowledge.NR_COLLISIONS_TO_BE_UNINTERESTING;
		Observation mostInterestingObs = null;
		int[] target = new int[] { -1, -1 };

		ArrayList<Observation>[] immovables = stateObs.getImmovablePositions(stateObs.getAvatarPosition());
		if (immovables != null) {
			for (ArrayList<Observation> immovableType : immovables) {
				if (immovableType != null && !immovableType.isEmpty()
						&& !isTypeUnreachable(agentType, immovableType.get(0).itype)) {
					if (getNrOfCollisions(agentType, immovableType.get(0).itype) < leastCollisions) {
						for (Observation immovable : immovableType) {
							if (!isObservationUnreachable(agentType, immovable.itype, immovable.obsID)) {
								leastCollisions = getNrOfCollisions(agentType, immovable.itype);
								mostInterestingObs = immovable;
								break;
							}
						}
						if (leastCollisions == 0)
							break;
					}
				}
			}
		}

		ArrayList<Observation>[] portals = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
		if (portals != null && leastCollisions != 0) {
			for (ArrayList<Observation> portalType : portals) {
				if (portalType != null && !portalType.isEmpty()
						&& !isTypeUnreachable(agentType, portalType.get(0).itype)) {
					if (getNrOfCollisions(agentType, portalType.get(0).itype) < leastCollisions) {
						for (Observation portal : portalType) {
							if (!isObservationUnreachable(agentType, portal.itype, portal.obsID)) {
								leastCollisions = getNrOfCollisions(agentType, portal.itype);
								mostInterestingObs = portal;
								break;
							}
						}
						if (leastCollisions == 0)
							break;
					}
				}
			}
		}

		ArrayList<Observation>[] ressources = stateObs.getResourcesPositions(stateObs.getAvatarPosition());
		if (ressources != null && leastCollisions != 0) {
			for (ArrayList<Observation> resType : ressources) {
				if (resType != null && !resType.isEmpty() && !isTypeUnreachable(agentType, resType.get(0).itype)) {
					if (getNrOfCollisions(agentType, resType.get(0).itype) < leastCollisions) {
						for (Observation res : resType) {
							if (!isObservationUnreachable(agentType, res.itype, res.obsID)) {
								leastCollisions = getNrOfCollisions(agentType, res.itype);
								mostInterestingObs = res;
								break;
							}
						}
						if (leastCollisions == 0)
							break;
					}
				}
			}
		}

		ArrayList<Observation>[] npcs = stateObs.getNPCPositions(stateObs.getAvatarPosition());
		if (npcs != null && leastCollisions != 0) {
			for (ArrayList<Observation> npcType : npcs) {
				if (npcType != null && !npcType.isEmpty() && !isTypeUnreachable(agentType, npcType.get(0).itype)) {
					if (getNrOfCollisions(agentType, npcType.get(0).itype) < leastCollisions) {
						leastCollisions = getNrOfCollisions(agentType, npcType.get(0).itype);
						mostInterestingObs = npcType.get(0);
						if (leastCollisions == 0)
							break;
					}
				}
			}
		}

		this.mostInterestingObservation = mostInterestingObs;
		if (mostInterestingObservation != null) {
			target[0] = (int) mostInterestingObservation.position.x / blockSize;
			target[1] = (int) mostInterestingObservation.position.y / blockSize;
		}
		currentTarget = target;
	}

	/**
	 * Determines interesting Movables.
	 */
	public ArrayList<Integer> getInterestingMovables(StateObservation state, int agentType) {
		ArrayList<Integer> interestingMovablesIds = new ArrayList<Integer>();
		ArrayList<Observation>[] movables = state.getMovablePositions();
		if (movables != null) {
			for (ArrayList<Observation> movableType : movables) {
				if (movableType != null && !movableType.isEmpty()) {
					if (this.getNrOfCollisions(agentType,
							movableType.get(0).itype) < Knowledge.NR_COLLISIONS_TO_BE_UNINTERESTING) {
						interestingMovablesIds.add(movableType.get(0).itype);
					}
				}
			}
		}
		return interestingMovablesIds;
	}

	/**
	 * Call after the avatar died in the simulation.
	 */
	private void startAgain() {
		if (debug)
			System.out.println("game is over, starting again...");
		currentState = startStateBackup.copy();
		unreachableObsIDs.clear();
		unreachableTypeIDs.clear();
		spawnedObjects.clear();
	}

	/*
	 * *****************************************************************************
	 * ******************* Getter & Setter
	 */

	/**
	 * Returns the learned way costs for a field with the given Immovable type on it
	 * for an agent with the given agent id. Assumes field is blocked for unknown
	 * types.
	 */
	public static int getWayCosts(int agentType, int spriteType) {
		int result = Knowledge.WAY_COSTS_BLOCKED;

		if (spriteType == GridElement.NO_OBSTACLE) {
			result = Knowledge.WAY_COSTS_FREE;

		} else if (wayCosts.containsKey(agentType)) {
			if (wayCosts.get(agentType).containsKey(spriteType)) {
				result = wayCosts.get(agentType).get(spriteType);
			}
		} else if (wayCosts.containsKey(VALID_FOR_ALL_AGENT_TYPES)) {
			if (wayCosts.get(VALID_FOR_ALL_AGENT_TYPES).containsKey(spriteType)) {
				result = wayCosts.get(VALID_FOR_ALL_AGENT_TYPES).get(spriteType);
			}
		}
		return result;
	}

	/**
	 * Updates the wayCosts for a field with the given Immovable type on it for an
	 * agent with the given agent id.
	 */
	private void setWayCosts(int agentType, int spriteType, int costs) {
		if (!wayCosts.containsKey(agentType)) {
			wayCosts.put(agentType, new HashMap<Integer, Integer>());
		}
		wayCosts.get(agentType).put(spriteType, costs);
	}

	/**
	 * Adds a unique obsID to be unreachable. If x unique obsIDs of the same type
	 * are unreachable, sets the whole type to be unreachable.
	 */
	private void setObservationUnreachable(int agentType, int spriteType, int obsID) {
		if (debug)
			System.out.println("setting individual observation unreachable: obsID=" + obsID + ", typeID=" + spriteType);

		if (!unreachableObsIDs.containsKey(agentType)) {
			unreachableObsIDs.put(agentType, new HashMap<Integer, List<Integer>>());
		}

		if (unreachableObsIDs.get(agentType).containsKey(spriteType)) {
			if (unreachableObsIDs.get(agentType).get(spriteType).size() >= NR_OBS_UNREACHABLE_FOR_TYPE_UNREACHABLE
					- 1) {
				setTypeUnreachable(agentType, spriteType);
			}
			unreachableObsIDs.get(agentType).get(spriteType).add(obsID);
		} else {
			List<Integer> l = new LinkedList<Integer>();
			l.add(obsID);
			unreachableObsIDs.get(agentType).put(spriteType, l);
		}
	}

	/**
	 * Returns true if an observation is unreachable for a given agent type,
	 * otherwise false.
	 */
	private boolean isObservationUnreachable(int agentType, int spriteType, int obsID) {
		if (unreachableObsIDs.containsKey(agentType)) {
			if (unreachableObsIDs.get(agentType).containsKey(spriteType)) {
				return unreachableObsIDs.get(agentType).get(spriteType).contains(obsID);
			}
		}
		return false;
	}

	/**
	 * Sets complete type unreachable for a given agent id, so that the agent will
	 * not try to reach objects of this type again before the list is cleared.
	 */
	private void setTypeUnreachable(int agentType, int spriteType) {
		if (debug)
			System.out.println("setting whole type unreachable: typeID=" + spriteType);
		if (!unreachableTypeIDs.containsKey(agentType)) {
			unreachableTypeIDs.put(agentType, new LinkedList<Integer>());
		}
		unreachableTypeIDs.get(agentType).add(spriteType);
	}

	/**
	 * Returns true if spriteType is unreachable for a given agent type, otherwise
	 * false.
	 */
	private boolean isTypeUnreachable(int agentType, int spriteType) {
		if (unreachableTypeIDs.containsKey(agentType)) {
			return unreachableTypeIDs.get(agentType).contains(spriteType);
		}
		return false;
	}

	/**
	 * Remembers how many times an agent with the given id collided with a sprite
	 * with the given id.
	 */
	private void addCollision(int agentType, int spriteType) {
		if (!collisions.containsKey(agentType)) {
			collisions.put(agentType, new HashMap<Integer, Integer>());
		}
		if (collisions.get(agentType).containsKey(spriteType)) {
			collisions.get(agentType).put(spriteType, collisions.get(agentType).get(spriteType) + 1);
		} else {
			collisions.get(agentType).put(spriteType, 1);
		}
	}

	/**
	 * Returns how many times an agent with the given id collided with a sprite with
	 * the given id.
	 */
	private int getNrOfCollisions(int agentType, int spriteType) {
		if (collisions.containsKey(agentType)) {
			if (collisions.get(agentType).containsKey(spriteType)) {
				return collisions.get(agentType).get(spriteType);
			}
		}
		return 0;
	}

	/*
	 * *****************************************************************************
	 * ******************* Debugging Prints
	 */

	/**
	 * Print collisions
	 */
	private void printCollisions() {
		System.out.println("**** Collisions **********");
		for (int agent : collisions.keySet()) {
			System.out.println("-----Agent type Id: " + agent + "-----");
			Map<Integer, Integer> c = collisions.get(agent);
			for (Integer i : c.keySet()) {
				System.out.println("collisions with type " + i + ": " + c.get(i));
			}
		}
		System.out.println("***************************");
	}

	/**
	 * Print the experience map
	 */
	private static void printExperience() {
		System.out.println("**** Gained Knowledge ****");
		for (int agent : experience.keySet()) {
			System.out.println("-----Agent type Id: " + agent + "-----");
			Map<Integer, int[]> experienceMap = experience.get(agent);
			System.out.println(
					"Sprite Type\tWin\t\tLoss\t\tScore Down\tScore Up\tInventory Down\tInventory Up\tNew Immov\tNew Resource");
			for (int key : experienceMap.keySet()) {
				System.out.print(key + "\t\t");
				for (int occurences : experienceMap.get(key)) {
					System.out.print(occurences + "\t\t");
				}
				System.out.println("");
			}
		}
		System.out.println("***************************");
	}

	/**
	 * Print the learned waycosts
	 */
	private static void printWayCosts() {
		System.out.println("**** Learned Waycosts ****");
		for (int agent : wayCosts.keySet()) {
			System.out.println("-----Agent type Id: " + agent + "-----");
			Map<Integer, Integer> costs = wayCosts.get(agent);
			for (Integer i : costs.keySet()) {
				System.out.println("type " + i + " has waycosts " + costs.get(i));
			}
		}
		System.out.println("***************************");
	}

	/**
	 * Print spawners
	 */
	private static void printSpawners() {
		System.out.println("**** Spawners ************");
		for (Integer i : spawners.keySet()) {
			System.out.println("probability of portal type " + i + " to be a spawner: " + spawners.get(i));
		}
		System.out.println("***************************");
	}

	/**
	 * Print exits
	 */
	private static void printExits(int agentType) {
		System.out.println("**** Exits for type " + agentType + " ****");
		for (Integer i : exits.keySet()) {
			System.out.println("probability of type " + i + " to be an exit: " + exits.get(i));
		}
		System.out.println("***************************");
	}

	/*
	 * *****************************************************************************
	 * ******************* Experience Generation
	 */

	/**
	 * Update the experience after a new collision. A collision must have happened
	 * directly in the lastState, otherwise no update happens
	 * 
	 * @param lastState
	 *            - state where a collision happened
	 * @param newState
	 *            - state right after collision
	 */
	public static void updateExperience(StateObservation lastState, StateObservation newState) {
		if (lastState != null && newState != null) {
			boolean collision = !newState.getHistoricEventsHistory().isEmpty()
					&& newState.getHistoricEventsHistory().last().gameStep == newState.getGameTick();
			if (collision) {
				evaluateCollision(lastState, newState, newState.getHistoricEventsHistory().last());
			}
		}
	}

	/**
	 * Here the changes from last to new game state are evaluated and a new value is
	 * assigned to the object with whom the agent collided.
	 */
	private static void evaluateCollision(StateObservation lastState, StateObservation newState, Event collision) {
		boolean[] changed = detectChanges(lastState, newState);
		int hasChanges = sumArray(changed);

		// if any changes happened
		if (hasChanges > 0) {
			// holds an experience map for one agent type
			Map<Integer, int[]> experienceMap = null;

			int agentTypeId = collision.activeTypeId;
			int passiveTypeId = collision.passiveTypeId;

			if (experience.containsKey(agentTypeId)) {
				experienceMap = experience.get(agentTypeId);
			} else {
				experienceMap = new HashMap<Integer, int[]>();
			}

			// holds an experience array for one passive object type
			int[] experienceArray = null;

			if (experienceMap.containsKey(passiveTypeId)) {
				experienceArray = experienceMap.get(passiveTypeId);
			} else {
				experienceArray = new int[Knowledge.TOTAL_OBSERVABLE_EVENTS];
			}

			for (int i = 0; i < changed.length; i++) {
				if (changed[i]) {
					experienceArray[i]++;
				}
			}
			experienceMap.put(passiveTypeId, experienceArray);
			experience.put(agentTypeId, experienceMap);
		}
	}

	/**
	 * sum up the values of a boolean array
	 */
	private static int sumArray(boolean[] array) {
		int sum = 0;
		for (boolean a : array) {
			if (a) {
				sum++;
			}
		}
		return sum;
	}

	/**
	 * sum up the values of an array of integers
	 */
	private static int sumArray(int[] array) {
		int sum = 0;
		for (int a : array) {
			sum += a;
		}
		return sum;
	}

	/**
	 * Here we look for changes in two given states. May be extended with further
	 * change types.
	 */
	private static boolean[] detectChanges(StateObservation lastState, StateObservation newState) {

		boolean[] changed = new boolean[Knowledge.TOTAL_OBSERVABLE_EVENTS];

		changed[Knowledge.LOSS] = newState.getGameWinner() == Types.WINNER.PLAYER_LOSES
				&& newState.getGameWinner() != lastState.getGameWinner();
		changed[Knowledge.WIN] = newState.getGameWinner() == Types.WINNER.PLAYER_WINS
				&& newState.getGameWinner() != lastState.getGameWinner();
		changed[Knowledge.SCORE_DOWN] = newState.getGameScore() < lastState.getGameScore();
		changed[Knowledge.SCORE_UP] = newState.getGameScore() > lastState.getGameScore();
		changed[Knowledge.INVENTORY_DECREASED] = inventoryDecreased(lastState.getAvatarResources(),
				newState.getAvatarResources());
		changed[Knowledge.INVENTORY_INCREASED] = inventoryIncreased(lastState.getAvatarResources(),
				newState.getAvatarResources());
		changed[Knowledge.NEW_IMMOV_TYPE] = detectNewTypes(lastState.getImmovablePositions(),
				newState.getImmovablePositions());
		changed[Knowledge.NEW_RES_TYPE] = detectNewTypes(lastState.getResourcesPositions(),
				newState.getResourcesPositions());

		return changed;
	}

	/**
	 * Returns wether the inventory of the agent has increased from last to new
	 * state.
	 */
	private static boolean inventoryIncreased(HashMap<Integer, Integer> lastStateInventory,
			HashMap<Integer, Integer> newStateInventory) {
		for (Integer i : newStateInventory.keySet()) {
			int newAmount = newStateInventory.get(i);
			if (!lastStateInventory.containsKey(i)) {
				return true;
			} else if (lastStateInventory.get(i) < newAmount) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns wether the inventory of the agent has increased from last to new
	 * state.
	 */
	private static boolean inventoryDecreased(HashMap<Integer, Integer> lastStateInventory,
			HashMap<Integer, Integer> newStateInventory) {
		for (Integer i : lastStateInventory.keySet()) {
			int oldAmount = lastStateInventory.get(i);
			if (!newStateInventory.containsKey(i)) {
				return true;
			} else if (newStateInventory.get(i) < oldAmount) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the number of objects increased from last to new state.
	 */
	private static boolean detectNewTypes(ArrayList<Observation>[] lastStateArray,
			ArrayList<Observation>[] newStateArray) {
		if (newStateArray != null) {
			if (lastStateArray != null) {
				return newStateArray.length > lastStateArray.length;
			} else
				return true;
		} else
			return false;
	}

	/*
	 * *****************************************************************************
	 * ******************* Experience Interface
	 */

	/**
	 * returns the experienceMap for a given agent type
	 * 
	 * @param agentTypeId
	 * @return experienceMap - mapping from passive objects to experience arrays,
	 *         for a given agent type
	 */
	public static Map<Integer, int[]> getExperienceMap(int agentTypeId) {
		if (experience.containsKey(agentTypeId)) {
			return experience.get(agentTypeId);
		}
		return null;
	}

	/**
	 * returns the experienceArray for a given agent type and passive object
	 * 
	 * @param agentTypeId
	 * @param passiveTypeId
	 * @return experienceArray - an array where indices are the events and values
	 *         mean, how often this event happened after the collision of the agent
	 *         of given type with a passive object of given type. If agent or
	 *         passive object are not known - returns null.
	 */
	public static int[] getExperienceArray(int agentTypeId, int passiveTypeId) {
		Map<Integer, int[]> experienceMap = getExperienceMap(agentTypeId);
		if (experienceMap != null) {
			if (experienceMap.containsKey(passiveTypeId)) {
				return experienceMap.get(passiveTypeId);
			}
		}
		return null;
	}

	/**
	 * for a given state returns a list of observations (immovables, movables, npc
	 * etc), that have only one instance of their type in the state and that haven't
	 * been seen before (not present in the experience map)
	 * 
	 * @param state
	 *            - current state, for which the list is evaluated
	 * @param agentTypeId
	 *            - type of an agent whose experience we consider
	 * @return a list of observations, that have only one instance of their type and
	 *         are not known to the given agent The list is empty, if no objects
	 *         satisfying the criteria are found
	 */
	public static ArrayList<Observation> getSingleUnkownObjects(StateObservation state, int agentTypeId) {
		ArrayList<Observation> singleObjects = getSingleObjects(state);
		ArrayList<Observation> singleUnknownObjects = new ArrayList<Observation>();

		if (!experience.containsKey(agentTypeId)) {
			return singleObjects;
		} else {
			Map<Integer, int[]> experienceMap = experience.get(agentTypeId);
			for (Observation observation : singleObjects) {
				if (!experienceMap.containsKey(observation.itype)
						|| sumArray(experienceMap.get(observation.itype)) == 0) {
					singleUnknownObjects.add(observation);
				}
			}
		}
		return singleUnknownObjects;
	}

	/**
	 * for a given state returns a list of observations (immovables, movables, npc
	 * etc), that have only one instance of their type in the state
	 * 
	 * @param state
	 *            - current state, for which the list is evaluated
	 * @return a list of observations, that have only one instance of their type The
	 *         list is empty, if no objects satisfying the criteria are found
	 */
	public static ArrayList<Observation> getSingleObjects(StateObservation state) {
		ArrayList<Observation> singleObjects = new ArrayList<Observation>();
		singleObjects.addAll(getSingleObjects(state.getImmovablePositions()));
		singleObjects.addAll(getSingleObjects(state.getMovablePositions()));
		singleObjects.addAll(getSingleObjects(state.getNPCPositions()));
		singleObjects.addAll(getSingleObjects(state.getPortalsPositions()));
		singleObjects.addAll(getSingleObjects(state.getResourcesPositions()));
		return singleObjects;
	}

	/**
	 * for a given list of observations (structure returned by getNPCPositions and
	 * similar functions) returns Observations (objects), that exist in one instance
	 * only (alone)
	 * 
	 * @param observations
	 * @return a list of observations that have only one instance
	 */
	public static ArrayList<Observation> getSingleObjects(ArrayList<Observation>[] observations) {
		ArrayList<Observation> singleObjects = new ArrayList<Observation>();
		if (observations != null) {
			for (ArrayList<Observation> typeObjects : observations) {
				if (typeObjects != null) {
					// if there is only one object of a particular type
					if (typeObjects.size() == 1) {
						singleObjects.add(typeObjects.get(0));
					}
				}
			}
		}
		return singleObjects;
	}

	/**
	 * Returns "benefit" of colliding with a passive object. Use this method if you
	 * do not know the agentType. Benefit is between -1 (bad) and +1 (good), 0 =
	 * unknown.
	 */
	public static double getBenefit(int passiveTypeId) {
		return getBenefit(Knowledge.VALID_FOR_ALL_AGENT_TYPES, passiveTypeId);
	}

	/**
	 * Returns "benefit" of colliding with a passive object. Benefit is between -1
	 * (bad) and +1 (good), 0 = unknown.
	 */
	public static double getBenefit(int agentTypeId, int passiveTypeID) {

		// if agent Id is not known - return 0
		if (!experience.containsKey(agentTypeId)) {
			return 0;
		}

		Map<Integer, int[]> experienceMap = experience.get(agentTypeId);

		// if passive object is not known - return 0
		if (!experienceMap.containsKey(passiveTypeID)) {
			return 0;
		}

		if (experienceMap.get(passiveTypeID)[Knowledge.WIN] > 0) {
			return 1;
		}
		if (experienceMap.get(passiveTypeID)[Knowledge.LOSS] > 0) {
			return -1;
		}
		if (experienceMap.get(passiveTypeID)[Knowledge.SCORE_UP] > 0) {
			return 0.5;
		}
		if (experienceMap.get(passiveTypeID)[Knowledge.SCORE_DOWN] > 0) {
			return -0.5;
		}
		if (experienceMap.get(passiveTypeID)[Knowledge.INVENTORY_DECREASED] > 0) {
			return 0.5;
		}
		if (experienceMap.get(passiveTypeID)[Knowledge.INVENTORY_DECREASED] > 0) {
			return -0.5;
		}
		if (Knowledge.spawners.containsKey(passiveTypeID) && Knowledge.spawners.get(passiveTypeID) > 0) {
			return -Knowledge.spawners.get(passiveTypeID);
		}
		if (experienceMap.get(passiveTypeID)[Knowledge.NEW_IMMOV_TYPE] > 0) {
			return 0.25;
		}
		if (experienceMap.get(passiveTypeID)[Knowledge.NEW_RES_TYPE] > 0) {
			return 0.25;
		}
		return 0;
	}

	/**
	 * Returns a list of movable ids that are considered to be good based on
	 * knowledge.
	 */
	public static ArrayList<Integer> getGoodMovables(StateObservation state) {
		ArrayList<Integer> goodMovablesIds = new ArrayList<Integer>();
		ArrayList<Observation>[] movables = state.getMovablePositions();
		if (movables != null) {
			for (ArrayList<Observation> movableType : movables) {
				if (movableType != null && !movableType.isEmpty()) {
					if (Knowledge.isGoodObservation(movableType.get(0).itype, Util.getAgentType(state))) {
						goodMovablesIds.add(movableType.get(0).itype);
					}
				}
			}
		}
		return goodMovablesIds;
	}

	/**
	 * Movable (or any other observation) is considered good if nothing bad happens
	 * when we collide with it or if we don't know anything about it (better to take
	 * risk than miss a potentially good object)
	 */
	public static boolean isGoodObservation(int typeId, int agentId) {
		return getBenefit(agentId, typeId) >= 0;
	}

	/**
	 * Movable (or any other observation) is considered good if nothing bad happens
	 * when we collide with it or if we don't know anything about it (better to take
	 * risk than miss a potentially good object)
	 */
	public static boolean isBadObservation(int typeId, int agentId) {
		return getBenefit(agentId, typeId) < 0;
	}

	/**
	 * Call this to determine the probabilities of portals and immovable types to be
	 * an exit. This depends on the agent type do you have to call it regulary with
	 * the current type.
	 */
	private static void determinePossibleExits(StateObservation stateObs) {
		exits.clear();
		int agentType = Util.getAgentType(stateObs);

		ArrayList<Observation>[] portals = stateObs.getPortalsPositions();
		if (portals != null) {
			for (ArrayList<Observation> portalType : portals) {
				if (portalType != null && !portalType.isEmpty()) {
					int id = portalType.get(0).itype;
					if (Knowledge.getBenefit(agentType, id) == 1) {
						exits.put(id, 1.0);
					} else if (Knowledge.getBenefit(agentType, id) == -1) {
						exits.put(id, 0.0);
					} else {
						exits.put(id, 0.5 / portalType.size());
					}
				}
			}
		}

		ArrayList<Observation>[] immovables = stateObs.getImmovablePositions();
		if (immovables != null) {
			for (ArrayList<Observation> immovType : immovables) {
				if (immovType != null && !immovType.isEmpty()) {
					int id = immovType.get(0).itype;
					if (Knowledge.getBenefit(agentType, id) == 1) {
						exits.put(id, 1.0);
					} else if (Knowledge.getBenefit(agentType, id) == -1) {
						exits.put(id, 0.0);
					} else {
						exits.put(id, 0.5 / immovType.size());
					}
				}
			}
		}

		printExits(agentType);
	}

	/*
	 * *****************************************************************************
	 * ******************* Distance Heuristic
	 */

	private static Grid grid = null;
	private static int[] goal = null;

	/**
	 * Call this to (re-)calculate the distances of all fields to the 'goal' for the
	 * given game state. Returns true if a 'goal' was found and you can use the
	 * heuristik by calling getDistanceHeuristicValue(...).
	 */
	public static boolean updateDistanceHeuristic(StateObservation state, ElapsedCpuTimer elapsedTimer, int limit) {

		System.out.println("updating distance heuristic");
		A_Star aStar = new A_Star(state, elapsedTimer, limit);
		goal = getGoal(state);
		if (goal[0] >= 0 && goal[1] >= 0) {
			aStar.solve(goal[0], goal[1], (int) state.getAvatarPosition().x / blockSize,
					(int) state.getAvatarPosition().y / blockSize, true, true);
			grid = aStar.getGrid();

			// when you uncomment this you are probably exceeding time!!
			// grid.printDebugDistances();
			// grid.printObsacles();
			// printValues();

			System.out.println("remaining time is " + elapsedTimer.remainingTimeMillis() + "ms");
			return true;
		}
		System.out.println("no goal found");
		grid = null;
		return false;
	}

	/**
	 * Call to get the heuristic value for a given position. The returned value will
	 * be in [0,1], where 0 means the field is far away from the 'goal' and 1 means
	 * the 'goal' is at the given position. If there is no path found to the 'goal'
	 * the approximate distance (manhattan) will be used for computing a value.
	 */
	public static double getDistanceHeuristicValue(Vector2d position) {

		if (grid == null) {
			return 0;
		}

		int[] gridDimensions = grid.getGridDimensions();
		int maxDistance = gridDimensions[0] * gridDimensions[1] / 3;
		int maxValue = 1;

		int row = (int) position.x / blockSize;
		int col = (int) position.y / blockSize;

		int distance = grid.getElementAt(row, col).getDistance();
		if (distance > maxDistance) {
			return 0;
		} else if (distance < 0) {
			distance = Util.manhattanDistInBlocks(col, row, goal[0], goal[1]);
		}
		return (maxDistance - distance) * ((double) maxValue / (double) maxDistance);

	}

	/**
	 * Determines the current 'goal' for the heuristic.
	 * 
	 * TODO: adapt this to not only look for exits.
	 */
	private static int[] getGoal(StateObservation stateObs) {
		int[] goal = new int[] { -1, -1 };
		Observation obs = null;
		double maxProbability = 0;

		determinePossibleExits(stateObs);

		ArrayList<Observation>[] portals = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
		ArrayList<Observation>[] immovables = stateObs.getImmovablePositions(stateObs.getAvatarPosition());

		if (portals != null) {
			for (ArrayList<Observation> portalType : portals) {
				if (portalType != null && !portalType.isEmpty()) {
					if (exits.containsKey(portalType.get(0).itype)) {
						double probability = exits.get(portalType.get(0).itype);
						if (probability > maxProbability) {
							obs = portalType.get(0);
						}
					}
				}
			}
		}

		if (immovables != null && obs == null) {
			for (ArrayList<Observation> movableType : immovables) {
				if (movableType != null && !movableType.isEmpty()) {
					if (exits.containsKey(movableType.get(0).itype)) {
						double probability = exits.get(movableType.get(0).itype);
						if (probability > maxProbability) {
							obs = movableType.get(0);
						}
					}
				}
			}
		}

		if (obs != null) {
			goal[0] = (int) obs.position.x / blockSize;
			goal[1] = (int) obs.position.y / blockSize;
			System.out.println("current goal is: typeID=" + obs.itype + ", obsID=" + obs.obsID);
		}

		return goal;
	}

	/**
	 * Prints the heuristic values for each GridElement.
	 */
	private static void printValues() {
		System.out.println("------------Values----------");
		for (GridElement[] array : grid.grid) {
			System.out.println();
			for (GridElement element : array) {
				double d = getDistanceHeuristicValue(
						new Vector2d(element.getRow() * blockSize, element.getColumn() * blockSize));
				System.out.printf("%.2f\t", d);
			}
		}
		System.out.println("\n----------------------------");
	}

}