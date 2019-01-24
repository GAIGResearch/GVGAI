package agents.MH2015;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.TreeSet;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import agents.Heuristics.StateHeuristic;

public class Heuristic extends StateHeuristic {

	int worldWidth = 0;
	int worldHeight = 0;
	int worldBlockSize = 0;
	double[][] visitedCountMap;
	double distanceFactor;
	double unvisitedScore = 0;
	double resourceDistanceScore = 0;
	double npcDistanceScore = 0;

	private ArrayList<Integer> enemyTypeList;

	public Heuristic(StateObservation stateObs) {
		// TODO Auto-generated constructor stub
		worldBlockSize = stateObs.getBlockSize();
		Dimension worldDimension = stateObs.getWorldDimension();
		worldWidth = worldDimension.width / worldBlockSize;
		worldHeight = worldDimension.height / worldBlockSize;
		distanceFactor = worldWidth * worldHeight * worldBlockSize;
		initWorldMap(stateObs);

		enemyTypeList = new ArrayList<Integer>();
	}

	private void initWorldMap(StateObservation stateObs) {
		visitedCountMap = new double[worldWidth][worldHeight];
		// System.out.printf("map size %d %d", map.length, map[0].length);
		for (int i = 0; i < worldWidth; i++) {
			for (int j = 0; j < worldHeight; j++) {
				visitedCountMap[i][j] = 0.0;
			}
		}
	}

	public void updateMap(StateObservation stateObs) {

		Vector2d avatarPosition = stateObs.getAvatarPosition();
		int x = (int) (avatarPosition.x / worldBlockSize);
		int y = (int) (avatarPosition.y / worldBlockSize);

		if (x < 0 || x >= visitedCountMap.length || y < 0 || y >= visitedCountMap[0].length) {
			return;
		}
		visitedCountMap[x][y]++;

		// for (int b = 0; b < visitedCountMap[0].length; b++) {
		// for (int a = 0; a < visitedCountMap.length; a++) {
		// System.out.print(" " + visitedCountMap[a][b]);
		// }
		// System.out.println();
		// }
	}

	@Override
	public double evaluateState(StateObservation stateObs) {
		Vector2d avatarPosition = stateObs.getAvatarPosition();
		ArrayList<Observation>[] npcPositionsArrayLists = stateObs.getNPCPositions(avatarPosition);
		ArrayList<Observation>[] resourcesPositionsArrayLists = stateObs.getResourcesPositions(avatarPosition);
		TreeSet<Event> history = stateObs.getHistoricEventsHistory();

		double score = stateObs.getGameScore();
		if (stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS) {
			return 100000;
		} else if (stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
			if (history.size() > 0) {
				Event deathEvent = history.last();
				// System.out.printf("enemy type id:%d\n", deathEvent.passiveTypeId);
				if (!enemyTypeList.contains(deathEvent.passiveTypeId)) {
					enemyTypeList.add(deathEvent.passiveTypeId);
				}
			}
			return Double.NEGATIVE_INFINITY;
		}

		int x = (int) (avatarPosition.x / worldBlockSize);
		int y = (int) (avatarPosition.y / worldBlockSize);

		if (x < 0 || x >= visitedCountMap.length || y < 0 || y >= visitedCountMap[0].length) {
			unvisitedScore = 0;
		} else {
			unvisitedScore = (distanceFactor - visitedCountMap[x][y]) / distanceFactor;
		}

		// calculate NPC score.
		if (npcPositionsArrayLists != null) {
			double awayDistance = worldHeight * worldWidth;
			double chaseDistance = 0;
			for (ArrayList<Observation> npcs : npcPositionsArrayLists) {
				if (npcs.size() > 0) {
					int npcTypeId = npcs.get(0).itype;
					// if NPC is harmful, run away. if not, chase.
					if (enemyTypeList.contains(npcTypeId)) {
						double dis = Math.sqrt(npcs.get(0).sqDist);
						if (dis < awayDistance) {
							awayDistance = dis;
						}
					} else {
						double dis = Math.sqrt(npcs.get(0).sqDist);
						if (dis < chaseDistance) {
							chaseDistance = dis;
						}
					}
				}
			}
			npcDistanceScore = (awayDistance * 3 - chaseDistance) / distanceFactor;
		}

		// calculate resource score.
		// if (resourcesPositionsArrayLists != null) {
		// double collectDistance = worldHeight * worldWidth;
		// for (ArrayList<Observation> resources : resourcesPositionsArrayLists) {
		// if (resources.size() > 0) {
		// double dis = Math.sqrt(resources.get(0).sqDist);
		// if (dis < collectDistance) {
		// collectDistance = dis;
		// }
		// }
		// }
		// resourceDistanceScore = (distanceFactor -collectDistance) / distanceFactor;
		// }

		return score + unvisitedScore + npcDistanceScore;
	}

}
