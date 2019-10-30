package agents.YBCriber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import agents.YBCriber.pcollections.HashTreePSet;
import agents.YBCriber.pcollections.PSet;

public class BfsNode {
	private ArrayList<Integer> actionSeq;
	private PSet<Integer> parentSeenObs;
	private StateObservation parentSo;
	private StateObservation so;

	static int MAX_OBS = 100; // change!!

	BfsNode(StateObservation so) {
		this.actionSeq = new ArrayList<>();
		this.parentSeenObs = HashTreePSet.empty();
		this.parentSo = null;
		this.so = so;
	}

	BfsNode(BfsNode node) {
		this.actionSeq = new ArrayList<>(node.actionSeq);
		this.parentSeenObs = node.parentSeenObs;
		this.parentSo = node.parentSo;
		// always copy state obs in case it matters (e.g. when taking the worst possible
		// outcome)
		this.so = node.so;
	}

	BfsNode child(int action) {
		if (action < 0 || action >= Agent.actions.size()) {
			System.out.println("Invalid action");
			return null;
		}
		BfsNode next = new BfsNode(this);
		next.move(action);
		return next;
	}

	ArrayList<BfsNode> children() {
		ArrayList<Integer> perm = new ArrayList<>();
		for (int i = 0; i < Agent.actions.size(); ++i) {
			perm.add(i);
		}
		Collections.shuffle(perm);
		ArrayList<BfsNode> ch = new ArrayList<>();
		// ACTION_NIL has highest priority
		for (int i = 0; i < Agent.actions.size(); ++i) {
			if (Agent.actions.get(perm.get(i)) == Types.ACTIONS.ACTION_NIL) {
				ch.add(this.child(perm.get(i)));
			}
		}
		for (int i = 0; i < Agent.actions.size(); ++i) {
			if (Agent.actions.get(perm.get(i)) != Types.ACTIONS.ACTION_NIL) {
				ch.add(this.child(perm.get(i)));
			}
		}
		return ch;
	}

	// returns the sequence of actions performed from the root node to current node
	ArrayList<Integer> getActionSeq() {
		return new ArrayList<>(this.actionSeq);
	}

	// returns the depth of the current node in the tree (root has depth 0)
	int getDepth() {
		return this.actionSeq.size();
	}

	// similar to .getSo, but each call generates a new state obs generated from
	// parent state obs
	StateObservation getNewSo() {
		this.generateNewSo();
		return this.so;
	}

	// returns the IDs of seen observations up to (and including) parent
	Set<Integer> getParentSeenObs() {
		return this.parentSeenObs;
	}

	// returns current state obs (generates it from parent if necessary)
	StateObservation getSo() {
		if (this.so == null) {
			this.generateNewSo();
		}
		return this.so;
	}

	// returns the IDs of all observations in the state obs
	private static Set<Integer> getObservationsIds(StateObservation so) {
		Set<Integer> observationsIds = new HashSet<>();
		ArrayList<Observation>[][] grid = so.getObservationGrid();
		int type = Agent.getAvatarItype(grid, Agent.getAvatarGridPosition(so));
		boolean b = Agent.obsProperties.containsKey(type);
		for (int x = 0; x < grid.length; ++x) {
			for (int y = 0; y < grid[x].length; ++y) {
				for (Observation o : grid[x][y]) {
					if (observationsIds.size() < MAX_OBS) {
						if (!b || !Agent.obsProperties.get(type).containsKey(o.itype)
								|| (Agent.obsProperties.get(type).get(o.itype).destroyed > 0
										&& Agent.obsProperties.get(type).get(o.itype).spawn == 0))
							observationsIds.add(o.obsID);
					}
				}
			}
		}
		return observationsIds;
	}

	// generates a new current state obs from parent state obs
	private void generateNewSo() {
		if (this.actionSeq.isEmpty())
			return;

		this.so = this.parentSo.copy();
		int lastAction = this.actionSeq.get(this.actionSeq.size() - 1);
		this.so.advance(Agent.actions.get(lastAction));
		Agent.analyzeStates(this.parentSo, this.so, Agent.actions.get(lastAction));
	}

	// advances parent state obs, using current state obs if available
	// sets current state obs to null
	private void move(int action) {
		if (this.so == null) {
			this.generateNewSo();
		}
		this.parentSo = this.so;
		this.so = null;

		this.actionSeq.add(action);
		if (this.parentSeenObs.size() < MAX_OBS)
			this.parentSeenObs = this.parentSeenObs.plusAll(getObservationsIds(this.parentSo));
	}
}
