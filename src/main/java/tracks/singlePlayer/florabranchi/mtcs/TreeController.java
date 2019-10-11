package tracks.singlePlayer.florabranchi.mtcs;

import core.game.StateObservation;
import javafx.util.Pair;

public class TreeController {

  public TreeNode rootNode;


  public void buildTree(int iterations) {

    rootNode = new TreeNode(0, null, null);

    for (int i = 0; i < iterations; i++) {
      
      Pair<MonteCarloNode, StateObservation> selectedNode = executeTreePolicy();

      //System.out.println(String.format("Rollouting selected node %s", selectedNode.getNodeId()));

      selectedNode.getKey().rolloutNode(selectedNode.getValue());
    }

  }

  Pair<MonteCarloNode, StateObservation> executeTreePolicy() {

    return null;
  }

  public void rollout() {

  }

  public void updateTree() {

  }


}
