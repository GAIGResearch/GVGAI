package tracks.singlePlayer.florabranchi.mtcs;

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

  public JFrame treeViewerFrame = new JFrame();

  private JGraphXAdapter<TreeNode, DefaultEdge> jgAdapter;


  public TreeViewer() throws HeadlessException {

  }

  Graph<TreeNode, DefaultEdge> buildSampleTree() {

    // create a JGraphT graph
    ListenableGraph<TreeNode, DefaultEdge> g =
        new DefaultListenableGraph<>(new DefaultDirectedGraph<>(DefaultEdge.class));

    // create a visualization using JGraph, via an adapter
    jgAdapter = new JGraphXAdapter<>(g);

    setPreferredSize(DEFAULT_SIZE);
    mxGraphComponent component = new mxGraphComponent(jgAdapter);
    component.setConnectable(false);
    component.getGraph().setAllowDanglingEdges(false);
    getContentPane().add(component);
    resize(DEFAULT_SIZE);

    TreeNode node1 = new TreeNode(0, null, null);
    TreeNode node2 = new TreeNode(0, node1, null);

    // add the vertices
    g.addVertex(node1);
    g.addVertex(node2);

    // that's all there is to it!...

    return g;
  }

  @Override
  public void init() {
    buildSampleTree();
  }

  public void showTree() {

    treeViewerFrame.getContentPane().add(this);
    treeViewerFrame.setTitle("JGraphT Adapter to JGraphX Demo");
    treeViewerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    treeViewerFrame.pack();
    treeViewerFrame.setVisible(true);
  }
}