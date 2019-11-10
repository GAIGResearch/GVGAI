package tutorialGeneration.criticalPathing;

import java.util.ArrayList;
import java.util.List;

import tutorialGeneration.AtDelfiGraph;
import tutorialGeneration.Mechanic;

public class BFSPather extends CriticalPather {
	/***
	 * This class creates critical paths by greedily choosing the lowest frames at each
	 * step in the search
	 */
	
	/***
	 * Constructs the First Frame Critical Pather 
	 * @param graph
	 */
	public BFSPather(AtDelfiGraph graph) {
		super(graph);
	}

	@Override
	public List<Mechanic> findCriticalPath(String agent, boolean isWin, int level) {
		List<Mechanic> criticalPath = new ArrayList<Mechanic>();
		List<Node> currentChoices = new ArrayList<Node>();
		
		// get all the mechanics of the avatars
		for(Mechanic mech : this.getGraph().getAvatars().get(0).getMechanics()) {
			Node node = new Node(mech);
			node.parent = null;
			currentChoices.add(node);
		}
		
		boolean terminate = false;

		Node end = null;
		int count = 0;
		// terminate when its time or when currentChoices has no choices
		while(!terminate && currentChoices.size() > 0) {
			System.out.println(count++);
			// remove from the top of the queue
			Node current = currentChoices.remove(0);

			if (current.mech.isWin()) {
				end = current;
				terminate = true;
			}
			else {
				// loop thru all of current's children mechanics
				for(Mechanic childMech : current.mech.getOutputs()) {
					Node child = new Node(childMech);
					// check if child is visited, add to choices if not
					if (!childMech.isVisted()) {
						childMech.setVisted(true);
						child.parent = current;
						currentChoices.add(child);
					}
				}
			}
		}
		
		// build critical path based on the found terminal mechanic
		while(end.parent != null) {
			criticalPath.add(end.mech);
			end = end.parent;
		}
		criticalPath.add(end.mech);
		return criticalPath;
	}
}


class Node {
	public Node parent;
	public Mechanic mech;
	
	public Node(Mechanic mech) {
		this.mech = mech;
	}
}
