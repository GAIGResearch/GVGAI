package tutorialGeneration.criticalPathing;

import java.util.List;

import tutorialGeneration.AtDelfiGraph;
import tutorialGeneration.Mechanic;

public abstract class CriticalPather {

	
	private AtDelfiGraph graph;
	
	public CriticalPather(AtDelfiGraph graph) {
		this.setGraph(graph);
	}
	
	public abstract List<Mechanic> findCriticalPath(String agent, boolean isWin, int level);
//	public abstract List<Mechanic> findCriticalPath(boolean isWin);
	
	public void resetVisits() {
		for(Mechanic mech : getGraph().getMechanics()) {
			mech.setVisted(false);
		}
	}

	/**
	 * @return the graph
	 */
	public AtDelfiGraph getGraph() {
		return graph;
	}

	/**
	 * @param graph the graph to set
	 */
	public void setGraph(AtDelfiGraph graph) {
		this.graph = graph;
	}
}
