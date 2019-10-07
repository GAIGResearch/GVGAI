package tracks.singlePlayer.florabranchi.src.mtcs;

import java.util.ArrayList;
import java.util.List;

import core.game.StateObservation;
import javafx.util.Pair;
import ontology.Types;

public class MonteCarloTree {

  private MonteCarloNode rootNode;

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
      Pair<MonteCarloNode, StateObservation> selectedNode = executeTreePolicy(stateObservation);
      //System.out.println(String.format("Rollouting selected node %s", selectedNode.getNodeId()));
      selectedNode.getKey().rolloutNode(selectedNode.getValue());
    }

    final MonteCarloNode bestNode = rootNode.getBestChild();
    this.bestFoundAction = bestNode.actionToGetToNode;

    System.out.println("Finished building tree");
    MonteCarloNode.reset();
    System.out.println(String.format("ACTION ----- Best found action: %s -  avg %f", this.bestFoundAction.toString(), bestNode.simulationReward / bestNode.timesVisited));
  }

  public Pair<MonteCarloNode, StateObservation> executeTreePolicy(final StateObservation stateObservation) {
    //System.out.println(String.format("Executing tree policy"));
    MonteCarloNode selectedNode = rootNode;
    List<Types.ACTIONS> actionsToGetToNode = new ArrayList<>();
    StateObservation stateAfterNodeAction = stateObservation.copy();
    do {
      if (!selectedNode.isFullyExtended()) {
        //System.out.println(String.format("Expanding children of node %s", selectedNode.getNodeId()));
        stateAfterNodeAction = advanceStateToNodeState(stateObservation, actionsToGetToNode);
        final MonteCarloNode nodeAfterExpansion = selectedNode.expand(stateAfterNodeAction);
        return new Pair<>(nodeAfterExpansion, stateAfterNodeAction);
      } else {
        selectedNode = selectedNode.getBestChild();
        actionsToGetToNode.add(selectedNode.actionToGetToNode);
        //System.out.println(String.format("Selected children %s", selectedNode.getNodeId()));
      }
    } while (!selectedNode.isFullyExtended());
    return new Pair<>(selectedNode, stateAfterNodeAction);
  }

  private MonteCarloNode createRootNode(StateObservation initialState) {
    return new MonteCarloNode(null, Types.ACTIONS.ACTION_NIL, initialState);
  }

  private StateObservation advanceStateToNodeState(final StateObservation initialState,
                                                   final List<Types.ACTIONS> actionsToGetToNode) {
    actionsToGetToNode.forEach(initialState::advance);
    return initialState;
  }

}
