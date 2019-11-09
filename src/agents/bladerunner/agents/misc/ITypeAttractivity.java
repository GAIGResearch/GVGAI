package agents.bladerunner.agents.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import agents.bladerunner.Agent;
import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;

public class ITypeAttractivity extends HashMap<Integer, Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The attractivity of a certain IType.
	 */
	private HashMap<Integer, Double> spriteCategoryAttractivityValue;

	public ITypeAttractivity() {
		super();
	}

	/**
	 * creates a HashMap with all current unique iTypes in the game and its
	 * corresponding attraction values
	 * 
	 * @param StateObs
	 *            a StateObservation
	 */
	public ITypeAttractivity(StateObservation StateObs, int numActions) {
		super();

		ArrayList<Observation>[][] grid = StateObs.getObservationGrid();

		// Set prior Attraction values of categories
		spriteCategoryAttractivityValue = new HashMap<Integer, Double>();
		spriteCategoryAttractivityValue.put(Types.TYPE_AVATAR, -1.0);
		spriteCategoryAttractivityValue.put(Types.TYPE_RESOURCE, 0.4);
		spriteCategoryAttractivityValue.put(Types.TYPE_PORTAL, 0.1);
		if (numActions == 4 || numActions == 2) {
			spriteCategoryAttractivityValue.put(Types.TYPE_NPC, -0.2);
		} else {
			spriteCategoryAttractivityValue.put(Types.TYPE_NPC, 0.2);
		}
		spriteCategoryAttractivityValue.put(Types.TYPE_STATIC, 0.05);
		spriteCategoryAttractivityValue.put(Types.TYPE_FROMAVATAR, 0.0);
		spriteCategoryAttractivityValue.put(Types.TYPE_MOVABLE, 0.1);

		// go through observation grid and put all iTypes into the map
		for (ArrayList<Observation>[] Obsarray : grid) {
			for (ArrayList<Observation> Obslist : Obsarray) {
				for (Observation Obs : Obslist) {
					this.put(Obs.itype, this.spriteCategoryAttractivityValue.get(Obs.category));
				}
			}
		}
		this.put(0, -1.0); // Set Wall to -1
	}

	/**
	 * creates a HashMap with all current unique iTypes in the game and its
	 * corresponding attraction values
	 * 
	 * @param StateObs
	 *            a StateObservation
	 * @param SpriteCategoryPriors
	 *            Maps Category to a prior Attraction Value, contains Avatar=0,
	 *            Resource=1, Portal=2, NPC=3, Static=4, FromAvatar=5, Movable=6
	 * 
	 */
	public ITypeAttractivity(StateObservation StateObs, HashMap<Integer, Double> SpriteCategoryPriors) {
		super();

		ArrayList<Observation>[][] grid = StateObs.getObservationGrid();

		// Set prior Attraction values of categories
		spriteCategoryAttractivityValue = SpriteCategoryPriors;

		// go through observation grid and put all iTypes into the map
		for (ArrayList<Observation>[] Obsarray : grid) {
			for (ArrayList<Observation> Obslist : Obsarray) {
				for (Observation Obs : Obslist) {
					this.put(Obs.itype, this.spriteCategoryAttractivityValue.get(Obs.category));
				}
			}
		}
		this.put(0, -1.0); // Set Wall to -1
	}

	/**
	 * creates a HashMap with all current unique iTypes in the game and its
	 * corresponding attraction values
	 * 
	 * @param StateObs
	 *            a StateObservation
	 * @param SpriteCategoryPriors
	 *            Array of Attraction Values, in the following order Avatar=0,
	 *            Resource=1, Portal=2, NPC=3, Static=4, FromAvatar=5, Movable=6
	 * 
	 */
	public ITypeAttractivity(StateObservation StateObs, Double[] SpriteCategoryPriors) {
		super();

		ArrayList<Observation>[][] grid = StateObs.getObservationGrid();

		// Set prior Attraction values of categories
		spriteCategoryAttractivityValue = new HashMap<Integer, Double>();
		spriteCategoryAttractivityValue.put(Types.TYPE_AVATAR, SpriteCategoryPriors[Types.TYPE_AVATAR]);
		spriteCategoryAttractivityValue.put(Types.TYPE_RESOURCE, SpriteCategoryPriors[Types.TYPE_RESOURCE]);
		spriteCategoryAttractivityValue.put(Types.TYPE_PORTAL, SpriteCategoryPriors[Types.TYPE_PORTAL]);
		spriteCategoryAttractivityValue.put(Types.TYPE_NPC, SpriteCategoryPriors[Types.TYPE_NPC]);
		spriteCategoryAttractivityValue.put(Types.TYPE_STATIC, SpriteCategoryPriors[Types.TYPE_STATIC]);
		spriteCategoryAttractivityValue.put(Types.TYPE_FROMAVATAR, SpriteCategoryPriors[Types.TYPE_FROMAVATAR]);
		spriteCategoryAttractivityValue.put(Types.TYPE_MOVABLE, SpriteCategoryPriors[Types.TYPE_MOVABLE]);

		// go through observation grid and put all iTypes into the map
		for (ArrayList<Observation>[] Obsarray : grid) {
			for (ArrayList<Observation> Obslist : Obsarray) {
				for (Observation Obs : Obslist) {
					this.put(Obs.itype, this.spriteCategoryAttractivityValue.get(Obs.category));
				}
			}
		}
		this.put(0, -1.0); // Set Wall to -1
	}

	// METHODS
	public Double putIfAbsent(Observation obs) {
		if (!this.containsKey(obs.itype)) {
			this.put(obs.itype, this.spriteCategoryAttractivityValue.get(obs.category));
			if (Agent.isVerbose) {
				System.out.println("ITypeAttractivityMap::added iType " + obs.itype + " with value "
						+ this.spriteCategoryAttractivityValue.get(obs.category));
			}
		}
		return this.get(obs.itype);
	}

	/**
	 * updates the itypesAttractionValues based on a state Observation TODO: Maybe
	 * it would be good to make a difference between Avatar and FromAvatar Events.
	 * TODO: Maybe there is a better learning rule TODO: The updateVals have to be
	 * adjusted
	 * 
	 * @param StateObservation
	 *            State Observation
	 */
	public void updateAttraction(StateObservation stateObs, Double previousScore) {

		double updateVal = 0;
		double learningrate = 1;
		double scoreIncrease = stateObs.getGameScore() - previousScore;
		boolean gameOver = stateObs.isGameOver();
		Types.WINNER win = stateObs.getGameWinner();

		if (scoreIncrease == 0) {
			updateVal = 0.00;
		} else {
			if (scoreIncrease > 0) {
				updateVal = 0.2;
			}
			if (scoreIncrease < 0) {
				updateVal = -0.1;
			}
		}
		if (gameOver && win == Types.WINNER.PLAYER_LOSES) {
			updateVal = -0.2;
		}

		if (gameOver && win == Types.WINNER.PLAYER_WINS) {
			updateVal = 1;
		}

		Iterator<Event> eventIterator = stateObs.getHistoricEventsHistory().descendingIterator();
		while (eventIterator.hasNext()) {
			Event currEvent = eventIterator.next();

			/*
			 * System.out.print("Game Step:" + currEvent.gameStep + " ");
			 * System.out.print(currEvent.passiveTypeId + " ");
			 * System.out.print(currEvent.passiveSpriteId + " ");
			 * System.out.print(currEvent.position + " "); System.out.println(" ");
			 */

			// All Events that happened in the last game tick
			if (currEvent.gameStep < stateObs.getGameTick() - 1) {
				break;
			}

			// it seems not to be possible to get the corresponding Observation
			// (and the category), so any unknown iType is initialized with 0
			if (!this.containsKey(currEvent.passiveTypeId)) {
				this.put(currEvent.passiveTypeId, 0.0);
			}

			if (currEvent.passiveTypeId != 0) { // exclude walls=0
				double newVal = this.get(currEvent.passiveTypeId) + updateVal * learningrate;
				if (newVal < -1 && newVal != -2) {
					newVal = -1;
				}
				if (newVal > 1) {
					newVal = 1;
				}
				// System.out.println("change attraction value of iType" +
				// currEvent.passiveTypeId + "from" + A
				// this.get(currEvent.passiveTypeId) + "to" + newVal );
				this.put(currEvent.passiveTypeId, newVal);
			}
		}

	}

}
