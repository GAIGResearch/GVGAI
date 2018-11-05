package tutorialGeneration.criticalPathing;

import java.util.ArrayList;
import java.util.List;

import tutorialGeneration.AtDelfiGraph;
import tutorialGeneration.Mechanic;
import tutorialGeneration.Node;

public class FirstFrameCriticalPather extends CriticalPather {
	/***
	 * This class creates critical paths by greedily choosing the lowest frames at each
	 * step in the search
	 */
	
	/***
	 * Constructs the First Frame Critical Pather 
	 * @param graph
	 */
	public FirstFrameCriticalPather(AtDelfiGraph graph) {
		super(graph);
	}

	@Override
	public List<Mechanic> findCriticalPathVictory(String agent) {
		List<Mechanic> criticalPath = new ArrayList<Mechanic>();
		
		List<Mechanic> currentChoices = new ArrayList<Mechanic>();
		
		// get all the mechanics of the avatars
		for(Node avatar : this.getGraph().getAvatars()) {
			currentChoices.addAll(avatar.getMechanics());
		}
		boolean terminate = false;
		while(!terminate) {
			Mechanic earliestMech = null;
			int earliestFrame = 100000;
			// loop thru all the mechs
			for (Mechanic mech : currentChoices) {
				// if the mech has an earlier frame then the current earliest, replace 
				if (mech.getFrames().get(agent)[0] < earliestMech.getFrames().get(agent)[0]) {
					earliestMech = mech;
				}
			}
			// reset currentChoices
			currentChoices = earliestMech.getOutputs();
			// put this round in the critical path
			criticalPath.add(earliestMech);
			
			// check if the earliest was a terminal
			if (earliestMech.isTerminal()) {
				terminate = true;
			}
		}
		return criticalPath;
	}

	@Override
	public List<Mechanic> findCriticalPathLoss(String agent) {
		ArrayList<Mechanic> criticalPath = new ArrayList<Mechanic>();
		
		
		
		return criticalPath;
	}
	
}
