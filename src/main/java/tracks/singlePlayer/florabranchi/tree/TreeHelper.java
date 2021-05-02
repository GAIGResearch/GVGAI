package tracks.singlePlayer.florabranchi.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ontology.Types;

public class TreeHelper {


  public TreeHelper(final List<Types.ACTIONS> availableActions) {
    //generateTreeNodeIds(11, availableActions);
  }

  public static Map<Integer, Map<Types.ACTIONS, Integer>> nodeIdsMap = new HashMap<>();
  public static Map<Integer, Integer> nodeDepthInfoMap = new HashMap<>();
  public static Map<Integer, Integer> nodeToParentMap = new HashMap<>();
  public static Map<Integer, Integer> nodesPerDepth = new HashMap<>();

  public static void main(String[] args) {

    List<Types.ACTIONS> availableActions = new ArrayList<>();
    availableActions.add(Types.ACTIONS.ACTION_USE);
    availableActions.add(Types.ACTIONS.ACTION_LEFT);
    availableActions.add(Types.ACTIONS.ACTION_RIGHT);

    TreeHelper helper = new TreeHelper(availableActions);
    System.out.println("did it work");
  }
  public int getNodeParent(final Integer id) {
    return nodeToParentMap.getOrDefault(id, -1);
  }

  public int getNodeDepth(final Integer id) {
    return nodeDepthInfoMap.getOrDefault(id, -1);
  }

  public int getTotalLayerNodes(final Integer depth) {
    return nodesPerDepth.getOrDefault(depth, -1);
  }


  public void generateTreeNodeIds(final int treeDepth,
                                  final List<Types.ACTIONS> availableActions) {


    int levelNodes = availableActions.size();

    int totalNodes = 0;
    for (int k = 0; k <= treeDepth; k++) {
      totalNodes += (int) Math.pow(levelNodes, k);
    }


    int nodeCount = 0;
    int firstLayerNode = 0;

    List<Integer> lastLayerNodesList = new ArrayList<>();
    List<Integer> currentLayerNodesList = new ArrayList<>();

    // Build tree from top
    for (int depth = 0; depth < treeDepth; depth++) {

      double currentLayerNodes = Math.pow(levelNodes, depth);
      nodesPerDepth.put(depth, (int) currentLayerNodes);

      firstLayerNode = nodeCount;

      for (int layerNode = 0; layerNode < currentLayerNodes; layerNode++) {
        currentLayerNodesList.add(nodeCount);
        nodeDepthInfoMap.put(nodeCount, depth);
        nodeCount++;
      }

      if (!lastLayerNodesList.isEmpty()) {

        for (Integer lastLayerNode : lastLayerNodesList) {

          // for each node in last
          // link to n = levelNodes edges in last layer
          addChildrenNodes(lastLayerNode, firstLayerNode, availableActions);
          nodeToParentMap.put(lastLayerNode, firstLayerNode);
          firstLayerNode += levelNodes;
        }
      }

      lastLayerNodesList = new ArrayList<>(currentLayerNodesList);
      currentLayerNodesList.clear();
    }
  }

  public Integer getNodeId(final Integer parentId,
                           final Types.ACTIONS relatedAction) {
    return nodeIdsMap.get(parentId).get(relatedAction);
  }

  public void addChildrenNodes(final Integer parentNode,
                               final Integer firstNode,
                               final List<Types.ACTIONS> availableActions) {

    int initialNodeId = firstNode;
    for (final Types.ACTIONS action : availableActions) {
      addNewNodeToMap(parentNode, initialNodeId, action);
      initialNodeId++;
    }
  }

  public void addNewNodeToMap(final int parentNodeId,
                              final int childrenId,
                              final Types.ACTIONS nodeAction) {


    final Map<Types.ACTIONS, Integer> actionsIntegerMap = nodeIdsMap.get(parentNodeId);
    if (actionsIntegerMap != null) {
      actionsIntegerMap.put(nodeAction, childrenId);
    } else {
      Map<Types.ACTIONS, Integer> nodeMap = new HashMap<>();
      nodeMap.put(nodeAction, childrenId);
      nodeIdsMap.put(parentNodeId, nodeMap);
    }
  }

  public List<Integer> getNodeChildren(final Integer nodeId) {
    final Map<Types.ACTIONS, Integer> orDefault = nodeIdsMap.getOrDefault(nodeId, new HashMap<>());
    if (!orDefault.isEmpty()) {
      return new ArrayList<>(orDefault.values());
    }
    return new ArrayList<>();
  }


}
