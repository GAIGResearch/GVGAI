package agents.jaydee;

import java.util.LinkedList;
import java.util.List;

public class NodePool {
    private static List<Node> available = new LinkedList<Node>();

    public static Node get() {
        Node node;
        if (available.size() > 0) {
            node = available.remove(0);
        } else {
            node = new Node();
        }

        return node;
    }

    public static void releaseNode(Node node) {
        if (node.isDestroyable) {
            if (node.children != null) {
                for (Node n : node.children.values()) {
                    if (n != null) {
                        releaseNode(n);
                    }
                }
            }
            node.release();
            available.add(node);
        }
    }
}