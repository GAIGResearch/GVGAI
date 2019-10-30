package agents.bladerunner.agents.hbfs;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import agents.bladerunner.Agent;
import agents.bladerunner.agents.misc.ObservationTools;
import agents.bladerunner.agents.misc.ObservationTools.DefaultAnalysis;

/* Node Class
 Computes hashcodes and heuristic.
 See HBFSAgent for details.
 */

public class HBFSNode implements Comparable<HBFSNode> {

	public StateObservation so;
	public ACTIONS causingAction;
	public HBFSNode parent;
	public int depth;

	// opportunityScore - how many reachable places are there?
	// trapped tile score - how many tiles are trapped?
	// attention score - how close does this path get to attended tiles, where
	// attention is a function of tile scarcity?
	// tabulate possible tile interactions
	// reward all changes in tile occurrence distribution
	private double score = -1;
	private double eventScore = -1;
	private double tileDiversityScore = -1;
	private double loadScore = -1;
	private double transformScore = -1;
	private int totalLoad = -1;
	private int hash = -1;

	public HBFSNode(StateObservation so, ACTIONS causingAction, HBFSNode parent, int depth) {
		super();
		this.so = so;
		this.causingAction = causingAction;
		this.parent = parent;
		this.depth = depth;
	}

	// Computes the heuristic score for this path
	// It is a weighted sum of
	// + depth
	// - how many events have been created
	// - 1.75^(how many different tile interactions have been seen)
	// - how did the total number of tiles change (positive for decrease)
	//
	// depth has a positive weight, the other 3 weights are negative
	// paths with minimal values of the heuristic are considered for expansion
	// (see HBFSAgent)
	// public double scoreNode(HBFSNode arg0) {
	//
	// loadScore = HBFSAgent.rootLoad - arg0.getLoad();
	// Set<IntPair> typeIds = new TreeSet<IntPair>();
	// eventScore = 0;
	// for (Event ev : arg0.so.getEventsHistory()) {
	// eventScore += scoreEvent(ev);
	// typeIds.add(new IntPair(ev.activeTypeId, ev.passiveTypeId));
	// }
	// tileDiversityScore = Math.pow(1.75, typeIds.size());
	//
	// double positionScore = 0;
	//
	// return HBFSAgent.wDepth * arg0.depth + HBFSAgent.wEvents * eventScore
	// + +HBFSAgent.wTileDiversity * tileDiversityScore
	// + HBFSAgent.wPosition * positionScore + HBFSAgent.wLoad
	// * loadScore;
	// }

	public double scoreNode(HBFSNode arg0) {

		if (arg0.parent == null) {
			return 0;
		}

		DefaultAnalysis a = ObservationTools.analyze(HBFSAgent.rootObservationList, arg0.parent.so, arg0.so);

		loadScore = Math.abs(HBFSAgent.rootLoad - arg0.getLoad());
		Set<IntPair> typeIds = new TreeSet<IntPair>();
		eventScore = 0;
		for (Event ev : arg0.so.getHistoricEventsHistory()) {
			eventScore += scoreEvent(ev);
			typeIds.add(new IntPair(ev.activeTypeId, ev.passiveTypeId));
		}
		tileDiversityScore = Math.pow(1.75, typeIds.size());

		double positionScore = 0;

		transformScore = a.tileTransforms;
		transformScore = a.tileCreations + a.tileDestructions;

		return HBFSAgent.wDepth * arg0.depth + HBFSAgent.wEvents * eventScore
				+ +HBFSAgent.wTileDiversity * tileDiversityScore + HBFSAgent.wPosition * positionScore
				+ HBFSAgent.wLoad * loadScore + HBFSAgent.wTransforms * transformScore;

	}

	// Computes hash code for the StateObservation. Used to organize the list of
	// visited states.
	// Rotating hash for sequences of small values:
	// http://burtleburtle.net/bob/hash/doobs.html
	public int computeHash() {
		int sequenceLength = so.getWorldDimension().height * so.getWorldDimension().width + 2;
		if (HBFSAgent.RESPECT_AGENT_ORIENTATION)
			sequenceLength += 2;
		if (HBFSAgent.REPSECT_AGENT_SPEED)
			sequenceLength += 1;

		ArrayList<Observation>[][] grid = so.getObservationGrid();
		totalLoad = 0;
		hash = sequenceLength;
		int posIndex = 0;
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				hash = (hash << 4) ^ (hash >> 28) ^ (1 + posIndex++); // 9.158E-4
				// hash = (hash << 4) ^ (hash >> 28) ^ 1; // 0.011
				for (Observation o : grid[i][j]) {
					hash = (hash << 4) ^ (hash >> 28) ^ (2 + o.itype);
				}
				totalLoad += grid[i][j].size();
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

		if (HBFSAgent.TRACK_HASHING)
			HBFSAgent.hashList.add(hash);

		return hash;
	}

	public double getScore() {
		if (score != -1) {
			return score;
		}
		score = scoreNode(this);
		if (HBFSAgent.maxScoreDifference < this.depth - this.score) {
			HBFSAgent.maxScoreDifference = Math.max(this.depth - this.score, HBFSAgent.maxScoreDifference);
			HBFSAgent.correspondingScore = this.score;
		}
		return score;
	}

	public double updateScore() {
		score = scoreNode(this);
		if (HBFSAgent.maxScoreDifference < this.depth - this.score) {
			HBFSAgent.maxScoreDifference = Math.max(this.depth - this.score, HBFSAgent.maxScoreDifference);
			HBFSAgent.correspondingScore = this.score;
		}
		return score;
	}

	public double getTileDiversityScore() {
		if (tileDiversityScore != -1) {
			return tileDiversityScore;
		}
		getScore();
		return tileDiversityScore;
	}

	public double getEventScore() {
		if (eventScore != -1) {
			return eventScore;
		}
		getScore();
		return eventScore;
	}

	public int getLoad() {
		if (totalLoad != -1) {
			return totalLoad;
		}
		computeHash();
		return totalLoad;
	}

	public double getTransformScore() {
		if (transformScore != -1) {
			return transformScore;
		}
		getScore();
		return transformScore;
	}

	public double getLoadScore() {
		if (loadScore != -1) {
			return loadScore;
		}
		getScore();
		return loadScore;
	}

	public double scoreEvent(Event ev) {
		double rt = 0;
		if (ev.passiveTypeId != 0 && ev.activeTypeId != 0) {
			rt = rt + 1;
		}
		return rt;
	}

	@Override
	public boolean equals(Object obj) {
		HBFSAgent.equalCalls++;
		if (Agent.isVerbose && HBFSAgent.equalCalls % HBFSAgent.callReportFrequency == 1) {
			System.out.print(".");
		}
		if (hashCode() != obj.hashCode())
			return false;

		if (HBFSAgent.TRACK_HASHING)
			HBFSAgent.hashesEqual++;

		HBFSNode n = (HBFSNode) obj;
		if (!n.so.getAvatarPosition().equals(so.getAvatarPosition())) {
			if (HBFSAgent.TRACK_HASHING)
				HBFSAgent.hashCollisions++;
			return false;
		}
		if (HBFSAgent.RESPECT_AGENT_ORIENTATION) {
			if (!n.so.getAvatarOrientation().equals(so.getAvatarOrientation())) {
				if (HBFSAgent.TRACK_HASHING)
					HBFSAgent.hashCollisions++;
				return false;
			}
		}
		if (HBFSAgent.REPSECT_AGENT_SPEED) {
			if (n.so.getAvatarSpeed() != so.getAvatarSpeed()) {
				if (HBFSAgent.TRACK_HASHING)
					HBFSAgent.hashCollisions++;
				return false;
			}
		}

		ArrayList<Observation>[][] grid = so.getObservationGrid();
		ArrayList<Observation>[][] ngrid = n.so.getObservationGrid();

		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				if (grid[i][j].size() != ngrid[i][j].size()) {
					if (HBFSAgent.TRACK_HASHING)
						HBFSAgent.hashCollisions++;
					return false;
				}
				for (int k = 1; k < grid[i][j].size(); k++) {
					if (grid[i][j].get(k).itype != ngrid[i][j].get(k).itype) {
						if (HBFSAgent.TRACK_HASHING)
							HBFSAgent.hashCollisions++;
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		if (hash != -1) {
			return hash;
		}
		return computeHash();
	}

	public int compareTo(HBFSNode o) {
		int rt = Double.compare(getScore(), o.getScore());
		HBFSAgent.compareCalls++;
		if (Agent.isVerbose && HBFSAgent.compareCalls % (2 * HBFSAgent.callReportFrequency) == 1) {
			System.out.print("-");
		}
		return rt;
	}

	public Stack<ACTIONS> getActionSequence() {
		Stack<ACTIONS> seq = new Stack<ACTIONS>();
		HBFSNode current = this;
		while (true) {
			if (current.causingAction != null) {
				seq.push(current.causingAction);
			}
			if (current.parent != null) {
				current = current.parent;
			} else {
				break;
			}
		}
		return seq;
	}

	public void displayActionSequence() {
		Stack<ACTIONS> s = getActionSequence();
		if (Agent.isVerbose) {
			System.out.print("Actions: ");
		}
		for (ACTIONS a : s) {
			if (Agent.isVerbose) {
				System.out.print(a + ";");
			}
		}
		if (Agent.isVerbose) {
			System.out.println();
		}
	}

	public static void setRootLoad(int load) {
		HBFSAgent.rootLoad = load;
	}

	public static void displayStateObservation(StateObservation so) {
		ArrayList<Observation>[][] grid = so.getObservationGrid();
		System.out.println("HBFS::#Grid:      " + grid.length + " X " + grid[1].length);
		System.out.println("Actions:   " + so.getAvailableActions());
		System.out.println("Immovable: " + arrayListToString(so.getImmovablePositions()));
		System.out.println("Movable:   " + arrayListToString(so.getMovablePositions()));
		System.out.println("NPCs:      " + so.getNPCPositions());
		System.out.println("Resources: " + so.getResourcesPositions());
		System.out.println("A.Res. :   " + so.getAvatarResources());
		System.out.println("Events:    " + so.getHistoricEventsHistory().size());
		int eventScore = 0;
		for (Event ev : so.getHistoricEventsHistory()) {
			if (ev.activeTypeId == ev.passiveTypeId && ev.passiveTypeId != 0) {
				eventScore += 1;
			}
		}
		System.out.println("Event Score:   " + eventScore);
		if (so.getHistoricEventsHistory().size() > 0)
			System.out.println("Last Event:" + so.getHistoricEventsHistory().last().gameStep + "; "
					+ so.getHistoricEventsHistory().last().fromAvatar + "; ptid:" + so.getHistoricEventsHistory().last().passiveTypeId
					+ "; atid:" + so.getHistoricEventsHistory().last().passiveTypeId + "; pos:"
					+ so.getHistoricEventsHistory().last().position);
		System.out.println("Position:  " + so.getAvatarPosition());

		int sequenceLength = so.getWorldDimension().height * so.getWorldDimension().width + 2;
		int hash = sequenceLength;
		int totalLoad = 0;
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				for (Observation o : grid[i][j]) {
					hash = (hash << 4) ^ (hash >> 28) ^ o.itype;
				}
				totalLoad += grid[i][j].size();
			}
		}
		System.out.println("Total Load: " + totalLoad);
	}

	private static String arrayListToString(ArrayList<Observation>[] a) {
		if (a == null)
			return "null";
		String rt = "[" + a.length + "] ";
		for (ArrayList<Observation> e : a) {
			rt = rt + e.size() + "";
			if (!e.isEmpty()) {
				rt = rt + "<" + e.get(0).itype + ">";
			}
			rt = rt + " | ";
		}
		return rt;
	}

}