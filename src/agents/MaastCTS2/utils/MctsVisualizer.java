package agents.MaastCTS2.utils;

public class MctsVisualizer{
}

/*
import it.uniroma1.dis.wsngroup.gexf4j.core.EdgeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.Graph;
import it.uniroma1.dis.wsngroup.gexf4j.core.Mode;
import it.uniroma1.dis.wsngroup.gexf4j.core.Node;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.Attribute;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeClass;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeList;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.StaxGraphWriter;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.viz.ColorImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import agents.MaastCTS2.Agent;
import agents.MaastCTS2.Globals;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.MctNode;

public class MctsVisualizer {
	
	public static void generateGraphFile(MctNode root){
		System.out.println();
		System.out.println("Starting generation graph file...");
		
		MctsController controller = (MctsController) Agent.controller;
		
		Gexf gexf = new GexfImpl();
		Calendar date = Calendar.getInstance();
		
		gexf.getMetadata()
			.setLastModified(date.getTime())
			.setCreator("Dennis Soemers")
			.setDescription("MCTS Search Tree in GVG-AI");
		gexf.setVisualization(true);

		Graph graph = gexf.getGraph();
		graph.setDefaultEdgeType(EdgeType.DIRECTED).setMode(Mode.STATIC);
		
		AttributeList attrList = new AttributeListImpl(AttributeClass.NODE);
		graph.getAttributeLists().add(attrList);
		
		Attribute depthAttribute = attrList.createAttribute("0", AttributeType.INTEGER, "depth");
		Attribute avgScoreAttribute = attrList.createAttribute("1", AttributeType.DOUBLE, "avg. score");
		Attribute numVisitsAttribute = attrList.createAttribute("2", AttributeType.DOUBLE, "num visits");
		Attribute isNovelAttribute = attrList.createAttribute("3", AttributeType.BOOLEAN, "passed novelty test");
		Attribute tdValueAttribute = attrList.createAttribute("4", AttributeType.DOUBLE, "TD value");
		
		HashMap<MctNode, Node> nodesMap = new HashMap<MctNode, Node>();
		int nodeID = 0;
		int edgeID = 0;
		
		ArrayList<MctNode> mctsNodes = new ArrayList<MctNode>();
		mctsNodes.add(root);
		
		while(!mctsNodes.isEmpty()){
			MctNode mctsNode = mctsNodes.remove(0);
			double avgScore = mctsNode.getTotalScore() / mctsNode.getNumVisits();
			avgScore = Globals.normalise(avgScore, controller.MIN_SCORE, controller.MAX_SCORE);
			Node graphNode = nodesMap.get(mctsNode);
			
			if(graphNode == null){
				graphNode = graph.createNode("" + nodeID);
				graphNode.setLabel("" + mctsNode.getNumVisits());
				graphNode.getAttributeValues().addValue(depthAttribute, "" + mctsNode.getDepth());
				graphNode.getAttributeValues().addValue(avgScoreAttribute, "" + avgScore);
				graphNode.getAttributeValues().addValue(numVisitsAttribute, "" + mctsNode.getNumVisits());
				graphNode.getAttributeValues().addValue(isNovelAttribute, "" + mctsNode.isNovel());
				graphNode.getAttributeValues().addValue(tdValueAttribute, "" + mctsNode.getTdValue());
				graphNode.setColor(getColorForScore(avgScore));
				++nodeID;
				nodesMap.put(mctsNode, graphNode);
			}			
			
			for(MctNode child : mctsNode.getChildren()){
				double avgChildScore = child.getTotalScore() / child.getNumVisits();
				avgChildScore = Globals.normalise(avgChildScore, controller.MIN_SCORE, controller.MAX_SCORE);
				mctsNodes.add(child);
				Node childGraphNode = graph.createNode("" + nodeID);
				childGraphNode.setLabel("" + child.getNumVisits());
				childGraphNode.getAttributeValues().addValue(depthAttribute, "" + child.getDepth());
				childGraphNode.getAttributeValues().addValue(avgScoreAttribute, "" + avgChildScore);
				childGraphNode.getAttributeValues().addValue(numVisitsAttribute, "" + child.getNumVisits());
				childGraphNode.getAttributeValues().addValue(isNovelAttribute, "" + child.isNovel());
				childGraphNode.getAttributeValues().addValue(tdValueAttribute, "" + child.getTdValue());
				childGraphNode.setColor(getColorForScore(avgChildScore));
				++nodeID;
				nodesMap.put(child, childGraphNode);
				
				graphNode.connectTo("" + edgeID, child.getActionFromParent().toString().substring("ACTION_".length()), EdgeType.DIRECTED, childGraphNode);
				++edgeID;
			}
		}
		
		StaxGraphWriter graphWriter = new StaxGraphWriter();
		File f = new File("D:/Apps/gvg-master-thesis/Results/MCTS_Tree_GVG_AI.gexf");
		Writer out;
		try {
			out =  new FileWriter(f, false);
			graphWriter.writeToStream(gexf, out, "UTF-8");
			System.out.println(f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Finished generation graph file!");
		System.out.println();
	}
	
	private static ColorImpl getColorForScore(double avgScore){
		final java.awt.Color yellow = java.awt.Color.YELLOW;
		final java.awt.Color red = java.awt.Color.RED;
		final java.awt.Color green = java.awt.Color.GREEN;
		
		ColorImpl color = new ColorImpl();
		
		if(avgScore == 0.5){
			color.setB(yellow.getBlue());
			color.setG(yellow.getGreen());
			color.setR(yellow.getRed());
		}
		else if(avgScore < 0.5){
			double redRatio = 1.0 - 2.0 * avgScore;
			double yellowRatio = 1.0 - redRatio;
			
			color.setB((int)(redRatio * red.getBlue() + yellowRatio * yellow.getBlue()));
			color.setG((int)(redRatio * red.getGreen() + yellowRatio * yellow.getGreen()));
			color.setR((int)(redRatio * red.getRed() + yellowRatio * yellow.getRed()));
		}
		else{
			double greenRatio = avgScore;
			double yellowRatio = 1.0 - greenRatio;
			
			color.setB((int)(greenRatio * green.getBlue() + yellowRatio * yellow.getBlue()));
			color.setG((int)(greenRatio * green.getGreen() + yellowRatio * yellow.getGreen()));
			color.setR((int)(greenRatio * green.getRed() + yellowRatio * yellow.getRed()));
		}
		
		return color;
	}

}
*/