package tracks.singlePlayer.florabranchi;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.swingViewer.Viewer;

import java.util.ArrayList;

import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeViewer2 {

  public static void main(String[] args) {

    Graph graph = new SingleGraph("Tutorial 1");
    SpriteManager sman = new SpriteManager(graph);

    final Node a = graph.addNode("A");
    //graph.addNode("A");
    a.addAttribute("ui.label", new ViewerNode(1, 2, new ArrayList<>()));

/*    final Sprite s1 = sman.addSprite("S1");
    s1.attachToNode("A");
    s1.setPosition(. 2, 0);
    s1.addAttribute("ui.label", "aaaaaaaaaaaaaaaaaaaaaaaa");*/

    graph.addNode("B");
    graph.addNode("C");
    graph.addEdge("AB", "A", "B");
    graph.addEdge("BC", "B", "C");
    graph.addEdge("CA", "C", "A");

    Viewer viewer = graph.display();

    viewer.enableAutoLayout();
  }
}
