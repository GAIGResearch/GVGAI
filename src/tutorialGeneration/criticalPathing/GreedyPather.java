package tutorialGeneration.criticalPathing;

import java.util.ArrayList;
import java.util.List;

import tutorialGeneration.AtDelfiGraph;
import tutorialGeneration.Mechanic;
import tutorialGeneration.Node;

public class GreedyPather extends CriticalPather {
	/***
	 * This class creates critical paths by greedily choosing the lowest frames at each
	 * step in the search
	 */
	
	/***
	 * Constructs the First Frame Critical Pather 
	 * @param graph
	 */
	public GreedyPather(AtDelfiGraph graph) {
		super(graph);
	}

	@Override
	public List<Mechanic> findCriticalPath(String agent, boolean isWin, int level) {
		List<Mechanic> criticalPath = new ArrayList<Mechanic>();
		
		List<Mechanic> currentChoices = new ArrayList<Mechanic>();
		
		// get all the mechanics of the avatars
		currentChoices.addAll(this.getGraph().getAvatars().get(0).getMechanics());
		
		boolean terminate = false;
		int count = 0;
		int earliestFrame = 100000;
		int floor = 0;

		// terminate when its time or when currentChoices has no choices
		while(!terminate && currentChoices.size() > 0) {
			System.out.println(count++);
			Mechanic earliestMech = null;
			// loop thru all the mechs
			for (Mechanic mech : currentChoices) {
				// if the mech has an earlier frame then the current earliest, replace 
				if (!mech.isVisted() && mech.getFrames().get(agent)[level] < earliestFrame 
//						&& mech.getFrames().get(agent)[level] != -1 
						&& mech.getFrames().get(agent)[level] != 0
						&& mech.getFrames().get(agent)[level] >= floor) {
					earliestMech = mech;
					earliestFrame = mech.getFrames().get(agent)[level];
				}
			}
			
			// break if earliestMech is null
			if(earliestMech == null) {
				break;
			}
			earliestMech.setVisted(true);
			floor = earliestFrame;
			earliestFrame = 100000;
			

			// reset currentChoices
//			currentChoices = new ArrayList<Mechanic>();
			for(Mechanic mech : earliestMech.getOutputs()) {
				if(!mech.isVisted() 
//						&& mech.getFrames().get(agent)[level] != -1 
						&& mech.getFrames().get(agent)[level] != 0
						&& mech.getFrames().get(agent)[level] >= floor) {
					currentChoices.add(mech);
				}
			}
//			currentChoices.addAll(this.getGraph().getAvatars().get(0).getMechanics());

			
			
			// put this round in the critical path
			criticalPath.add(earliestMech);
			// check if the earliest was a terminal
			if (earliestMech.isTerminal() && earliestMech.isWin() == isWin) {
				terminate = true;
			}
		}
		return criticalPath;
	}

//	@Override
//	public List<Mechanic> findCriticalPathLoss(String agent) {
//		ArrayList<Mechanic> criticalPath = new ArrayList<Mechanic>();
//		
//		
//		
//		return criticalPath;
//	}
	
}
