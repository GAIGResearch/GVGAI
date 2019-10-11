package tracks.singlePlayer.florabranchi.mtcs;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;

import org.jgrapht.Graph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultListenableGraph;

import java.awt.*;

import javax.swing.*;

public class TreeViewer extends JApplet {

  private static final long serialVersionUID = 2202072534703043194L;

  private static final Dimension DEFAULT_SIZE = new Dimension(530, 320);

  private JGraphXAdapter<TreeNode, DefaultEdge> jgxAdapter;


  public TreeViewer() throws HeadlessException {

  }

  Graph<TreeNode, DefaultEdge> buildSampleTree() {

    // create a JGraphT graph
    ListenableGraph<TreeNode, DefaultEdge> g =
        new DefaultListenableGraph<>(new DefaultDirectedGraph<>(DefaultEdge.class));

    // create a visualization using JGraph, via an adapter
    jgxAdapter = new JGraphXAdapter<>(g);

    setPreferredSize(DEFAULT_SIZE);
    mxGraphComponent component = new mxGraphComponent(jgxAdapter);
    component.setConnectable(false);
    component.getGraph().setAllowDanglingEdges(false);
    getContentPane().add(component);
    resize(DEFAULT_SIZE);

    TreeNode node1 = new TreeNode(0, null, null);
    TreeNode node2 = new TreeNode(0, node1, null);

    // add the vertices
    g.addVertex(node1);
    g.addVertex(node2);

    // positioning via jgraphx layouts
    mxCircleLayout layout = new mxCircleLayout(jgxAdapter);

    // center the circle
    int radius = 100;
    layout.setX0((DEFAULT_SIZE.width / 2.0) - radius);
    layout.setY0((DEFAULT_SIZE.height / 2.0) - radius);
    layout.setRadius(radius);
    layout.setMoveCircle(true);

    layout.execute(jgxAdapter.getDefaultParent());

    return g;
  }

  @Override
  public void init() {
    buildSampleTree();
  }

  public void showTree(JFrame treeViewerFrame) {

    treeViewerFrame.getContentPane().add(this);
    treeViewerFrame.setTitle("JGraphT Adapter to JGraphX Demo");
    treeViewerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    treeViewerFrame.pack();
    treeViewerFrame.setVisible(true);
  }
}