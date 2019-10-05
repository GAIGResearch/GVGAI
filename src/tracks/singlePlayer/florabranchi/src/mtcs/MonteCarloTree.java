package tracks.singlePlayer.florabranchi.src.mtcs;

import core.game.StateObservation;
import ontology.Types;

public class MonteCarloTree {

  private MCTNode rootNode;

  private Types.ACTIONS bestFoundAction = Types.ACTIONS.ACTION_NIL;

  public MonteCarloTree() {

  }

  public Types.ACTIONS getBestFoundAction() {
    return bestFoundAction;
  }

  public void monteCarloSearch(double iterations,
                               final StateObservation stateObservation) {

    rootNode = createRootNode(stateObservation);
    for (int i = 0; i < iterations; i++) {
      MCTNode selectedNode = executeTreePolicy();
      //System.out.println(String.format("Rollouting selected node %s", selectedNode.getNodeId()));
      selectedNode.rolloutNode();
    }

    final MCTNode bestNode = rootNode.getBestChild();
    this.bestFoundAction = bestNode.getActionToGetToNode();

    System.out.println("Finished building tree");
    MCTNode.reset();
    System.out.println(String.format("ACTION ----- Best found action: %s -  avg %f", this.bestFoundAction.toString(),
        bestNode.getSimulationReward() / bestNode.getTimesVisited()));
  }

  public MCTNode executeTreePolicy() {
    //System.out.println(String.format("Executing tree policy"));
    MCTNode selectedNode = rootNode;
    do {
      if (!selectedNode.isFullyExtended()) {
        //System.out.println(String.format("Expanding children of node %s", selectedNode.getNodeId()));
        return selectedNode.expand();
      } else {
        selectedNode = selectedNode.getBestChild();
        //System.out.println(String.format("Selected children %s", selectedNode.getNodeId()));
      }
    } while (!selectedNode.isFullyExtended());
    return selectedNode;
  }

  private MCTNode createRootNode(StateObservation initialState) {
    return new MCTNode(null, Types.ACTIONS.ACTION_NIL, initialState);
  }

}
