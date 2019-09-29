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

  /**
   * Expands tree given a number of rollouts.
   */
  public void expandTree(double rollouts,
                         final StateObservation stateObservation) {
    rootNode = createRootNode();
    MCTNode currentNode = rootNode;
    for (int i = 0; i < rollouts; i++) {

      if (!currentNode.isFullyExpanded()) {
        if (currentNode.getChildrenNodes().isEmpty()) {

          // Expand children if first time
          currentNode.expandPossibleChildren(stateObservation);
        }

        // Rollout
        final MCTNode nodeToRollout = currentNode.getBestConfidenceNode();
        final StateObservation stateCopy = stateObservation.copy();
        nodeToRollout.rolloutNode(stateCopy);
        i++;

      } else {
        // Keep looking for leaves
        final MCTNode mostPromissingNode = currentNode.getBestPlayNode();
        System.out.println(String.format("Selected node %s to expand", mostPromissingNode.getNodeId()));
        currentNode = mostPromissingNode;
      }
    }

    final MCTNode bestNode = rootNode.getBestPlayNode();
    this.bestFoundAction = bestNode.getActionToGetToNode();

    System.out.println("Finished building tree");
    MCTNode.reset();
    System.out.println(String.format("ACTION ----- Best found action: %s -  avg %f", this.bestFoundAction.toString(), bestNode.getChildrenNodesReward()));
  }

  private MCTNode createRootNode() {
    return new MCTNode(null, Types.ACTIONS.ACTION_NIL);
  }

}
