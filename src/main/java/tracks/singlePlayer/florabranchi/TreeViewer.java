package tracks.singlePlayer.florabranchi;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;

import org.jgrapht.Graph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultListenableGraph;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import tracks.singlePlayer.florabranchi.models.ViewerNode;

public class TreeViewer extends JApplet {

  private static final long serialVersionUID = 2202072534703043194L;

  private static final Dimension DEFAULT_SIZE = new Dimension(530, 320);

  private JGraphXAdapter<ViewerNode, DefaultEdge> jgxAdapter;

  public TreeViewer() throws HeadlessException {

  }

  Graph<ViewerNode, DefaultEdge> buildTree(List<ViewerNode> viewerNodes) {
/*    ListenableGraph<ViewerNode, DefaultEdge> g = new DefaultListenableGraph<>(new DefaultDirectedGraph<>(DefaultEdge.class));

    // create a visualization using JGraph, via an adapter
    jgxAdapter = new JGraphXAdapter<>(g);

    setPreferredSize(DEFAULT_SIZE);
    mxGraphComponent component = new mxGraphComponent(jgxAdapter);
    component.setConnectable(false);
    component.getGraph().setAllowDanglingEdges(false);
    getContentPane().add(component);
    resize(DEFAULT_SIZE);

    // add the vertices
    g.addVertex(viewerNodes.get(0));
    g.addVertex(viewerNodes.get(1));

    // positioning via jgraphx layouts
    mxCircleLayout layout = new mxCircleLayout(jgxAdapter);

    layout.execute(jgxAdapter.getDefaultParent());

    return g;*/
    return null;
  }


  @Override
  public void init() {
    ListenableGraph<ViewerNode, DefaultEdge> g =
        new DefaultListenableGraph<>(new DefaultDirectedGraph<>(DefaultEdge.class));

    // create a visualization using JGraph, via an adapter
    jgxAdapter = new JGraphXAdapter<>(g);

    setPreferredSize(DEFAULT_SIZE);
    mxGraphComponent component = new mxGraphComponent(jgxAdapter);
    component.setConnectable(false);
    component.getGraph().setAllowDanglingEdges(false);
    getContentPane().add(component);
    resize(DEFAULT_SIZE);

    ViewerNode v1 = new ViewerNode(1, 1, new ArrayList<>());
    ViewerNode v2 = new ViewerNode(1, 1, new ArrayList<>());
    ViewerNode v3 = new ViewerNode(1, 1, new ArrayList<>());
    ViewerNode v4 = new ViewerNode(1, 1, new ArrayList<>());

    // add some sample data (graph manipulated via JGraphX)
    g.addVertex(v1);
    g.addVertex(v2);
    g.addVertex(v3);
    g.addVertex(v4);

    g.addEdge(v1, v2);
    g.addEdge(v1, v3);
    g.addEdge(v1, v4);


    // positioning via jgraphx layouts
    mxCircleLayout layout = new mxCircleLayout(jgxAdapter);

    // center the circle
    int radius = 100;
    layout.setX0((DEFAULT_SIZE.width / 2.0) - radius);
    layout.setY0((DEFAULT_SIZE.height / 2.0) - radius);
    layout.setRadius(radius);
    layout.setMoveCircle(true);

    layout.execute(jgxAdapter.getDefaultParent());
    // that's all there is to it!...
  }

  public void showTree(final List<ViewerNode> viewerNodes,
                       final JFrame treeViewerFrame) {
    //buildTree(viewerNodes);
  }
}