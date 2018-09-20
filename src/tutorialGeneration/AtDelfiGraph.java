package tutorialGeneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;


import core.game.GameDescription;
import core.game.SLDescription;
import core.game.GameDescription.InteractionData;
import core.game.GameDescription.SpriteData;
import core.game.GameDescription.TerminationData;
import tools.GameAnalyzer;
import tools.LevelAnalyzer;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.*;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.view.Viewer;

public class AtDelfiGraph {
	/**
	 * a simple verbose flag
	 */
	private boolean verbose = true;
	
	/**
	 * a flag to show the graph visualization
	 */
	private boolean graphVisualization = true;
		
	/**
	 * A list of all the nodes in the AtDelfi Mechanic graph
	 */
	private List<Node> allNodes;
	
	private List<String> firstSpriteTargetActions = Arrays.asList("KillSprite", "KillIfHasLess", "KillIfHasMore", 
			"KillIfOtherHasMore", "KillIfFromAbove", "FlipDirection", "ReverseDirection", "AttractGaze", "TurnAround", "WrapAround",
			"BounceForward", "ChangeResource", "AddHealthPoints", "AddHealthPointsToMax", "Align", "TeleportToExit",
			"SubtractHealthPoints", "CloneSprite", "StepBack");
	private List<String> bothSpriteTargetActions = Arrays.asList("KillBoth", "PullWithIt", "CollectResource");
	private List<String> stypePlusTargetActions = Arrays.asList("TransformTo", "TransformToSingleton");
	private List<String> stypeTargetActions = Arrays.asList("Spawn", "KillAll", "SpawnIfHasMore", "SpawnIfHasLess", "SpawnBehind");
	/**
	 * Information parsed from the VGDL File
	 */
	private GameDescription gd;
	private SLDescription sl;
	private GameAnalyzer ga;
	private LevelAnalyzer la;
	
	/**
	 * represents the entity which the player controls in the game
	 */
	private List<Node> avatars;
	private List<Node> sprites;
	private List<Node> conditions;
	private List<Node> actions;
	
	
	/**
	 * Enums for node types in the visualization graph
	 */
	public enum NodeType {
		SPRITE, CONDITION, ACTION, UKNOWN
	};
	/**
	 * the visualization graph
	 */
	private Graph graph;

	/**
	 * colors for the nodes
	 */
	private String spriteColor = "#FCEC8C";
	private String conditionColor = "#E77E43";
	private String actionColor = "#743C2E";
			
	/**
	 * attributes for the nodes
	 */
	private String spriteAttributes = "shape:circle;fill-color:" + spriteColor +";size:100px;text-color:#000000;text-size:12;text-alignment:center;";
	private String conditionAttributes = "shape:diamond;fill-color:" + conditionColor + ";size:100px;text-color:#000000;text-size:12;text-alignment:center;";
	private String actionAttributes = "shape:box;fill-color:" + actionColor + ";size:75px;text-color:#FFFFFF;text-size:12;text-alignment:center;";
	
	/**
	 * Constructs a new AtDelfi Graph
	 * @param gd the GameDescription of the game
	 * @param sl the SLDescription of the game
	 * @param ga the GameAnalyzer of the game
	 * @param la the LevelAnalyzer of the game
	 */
	
	public AtDelfiGraph(GameDescription gd, SLDescription sl, GameAnalyzer ga, LevelAnalyzer la) {
		this.gd = gd;
		this.sl = sl;
		this.ga = ga;
		this.la = la;
		
		avatars = new ArrayList<Node>();
		sprites = new ArrayList<Node>();
		conditions = new ArrayList<Node>();
		actions = new ArrayList<Node>();
		allNodes = new ArrayList<Node>();

		graph = new MultiGraph("Mechanic Graph");
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
	}
	
	public void build() {
		if (verbose)
			System.out.println("Building Mechanic Graph...");
		readSpriteSet();
		readInteractionSet();
		allNodes.addAll(sprites);
		allNodes.addAll(conditions);
		allNodes.addAll(actions);
		
		if(graphVisualization) {
			visualizeGraph();
			graph.display();
		}

	}

	public void visualizeGraph() {

		// create all nodes
		for(Node node : allNodes) {
			// add this as a node in the visualization graph
			createGraphNode(getVisualNodeType(node), node.getName(), node.getId());
		}
		// create all edges
		for (Node node : allNodes) {
			for (Node output : node.getOutputs()) {
				addEdge(node.getId(), output.getId());
			}
		}
		// space the nodes out a bit
		spaceAllNodes();
	}
	
	public void readSpriteSet() {
		if (verbose) {
			System.out.println("Creating all game entity nodes...");
		}
		ArrayList<SpriteData> allSpriteData = gd.getAllSpriteData();
		
		for(SpriteData current : allSpriteData) {
			// Create the given sprite
			createSpriteEntity(current);
		}
		
		if(verbose)
			System.out.println("ID \t:\t Name \t\t:\t Type");
			for(Node sprite : this.sprites) {
				System.out.println(sprite.getId() + " \t:\t " + sprite.getName() + "    \t:\t " + sprite.getType());
			}
			System.out.println("Avatars:");
			for (Node avatar : this.avatars) {
				System.out.println(avatar.getId() + " : " + avatar.getName());
			}
	}
	
	public void spaceAllNodes() {
		int counter = 0;
		Iterator<? extends MultiNode> nodes = graph.getNodeIterator();
		while(nodes.hasNext()) {
			MultiNode node = nodes.next();
			Iterator<? extends MultiNode> nodes2 = graph.getNodeIterator();
			while(nodes2.hasNext()) {
				MultiNode node2 = nodes2.next();
				if(!node.equals(node2)) {
					addHiddenEdge(Integer.parseInt(node.getId()), Integer.parseInt(node2.getId()), counter);
					counter++;
				}
			}
		}
	}
	
	public void readInteractionSet() {
		if (verbose) {
			System.out.println("Reading Interaction Set...");
		}
		
		for(Node sprite1 : sprites) {
			for (Node sprite2 : sprites) {
				ArrayList<InteractionData> intDataList = gd.getInteraction(sprite1.getName(), sprite2.getName());
				for (InteractionData intData : intDataList) {
					if(sprite1 != sprite2)
						classifyInteractionData(sprite1, sprite2, intData);
				}
			}
		}
	}
	
	public void readTerminationSet() {
		if(verbose)
			System.out.println("Reading Termination Set...");
	}
	public void createSpriteEntity(SpriteData current) {
		if(verbose) {
			System.out.println("Sprite Created: "  + current.name);
		}
		Node sprite = new Node(current.name, current.type, "Sprite");
		this.sprites.add(sprite);
		

		// add this node to the avatar list
		if (current.isAvatar){
			this.avatars.add(sprite);
		}
		
		// add attributes to this node
		sprite.addAttribute("isAvatar", "" + current.isAvatar);
		sprite.addAttribute("isNPC", "" + current.isNPC);
		sprite.addAttribute("isPortal", "" + current.isPortal);
		sprite.addAttribute("isResource", "" + current.isResource);
		sprite.addAttribute("isSingleton", "" + current.isSingleton);
		sprite.addAttribute("isStatic", "" + current.isStatic);
		HashMap<String,String> currParams = current.getParameters();
	    Iterator it = currParams.entrySet().iterator();
	    while(it.hasNext()) {
	        HashMap.Entry pair = (HashMap.Entry)it.next();	    
	        sprite.addAttribute(pair.getKey().toString(), pair.getValue().toString());
	        if(verbose)
	        	System.out.println("Attribute Found... " + pair.getKey().toString() + " = " + pair.getValue().toString());
	    }
	    
	    
	}
	
	public void classifyInteractionData(Node sprite1, Node sprite2, InteractionData intData) {
		Node condition = new Node("Collides", "n/a", "Condition");
		// add these to the respective lists
		conditions.add(condition);
		
		// add input/output for sprites/condition
		sprite1.addOutput(condition);
		sprite2.addOutput(condition);
		condition.addInput(sprite1);
		condition.addInput(sprite2);

		actionDecisionTree(sprite1, sprite2, condition, intData);
	
	}
	
	public void actionDecisionTree(Node sprite1, Node sprite2, Node condition, InteractionData intData) {
		Node action1 = null;
		Node action2 = null;
		Node action3 = null;
		List<Node> actionList = new ArrayList<Node>();

		if (firstSpriteTargetActions.contains(intData.type)) {
			// this actions targets only the first sprite
			action1 = new Node(intData.type, "n/a", "Action");		
		} else if(bothSpriteTargetActions.contains(intData.type)) {
			if(intData.type.equals("KillBoth")) {
				action1 = new Node("KillSprite", "n/a", "Action");
				action2 = new Node("KillSprite", "n/a", "Action");
			} else if(intData.type.equals("PullWithIt")) {
				action1 = new Node("Pulled", "n/a", "Action");
				action2 = new Node("Puller", "n/a", "Action");
			} else if(intData.type.equals("CollectResource")) {
				action1 = new Node("KillSprite", "n/a", "Action");
				action2 = new Node("IncreaseResource", "n/a", "Action");
			}	else {
				action1 = new Node("NothingImportant", "n/a", "Action");
			}
		} else if(stypePlusTargetActions.contains(intData.type)) {
			// this actions targets some stype output in addition to possibly one of the first two sprites
			if(intData.type.equals("TransformTo")) {
				action1 = new Node("Transformee", "n/a", "Action");
				action3 = new Node("TransformTo", "n/a", "Action");
			} else if(intData.type.equals("KillAll")) {
				action1 = new Node("KillAll", "n/a", "Action");
			} 
		} else if(stypePlusTargetActions.contains(intData.type)) {
			if(intData.type.equals("TransformTo")) {
				action1 = new Node("Transformee", "n/a", "Action");
				action3 = new Node("TransformTo", "n/a", "Action");
			} else if(intData.type.equals("TransformToSingleton")) {
				// TODO fix this so this method is covered. action2 will incorrectly point at sprite2 for now
				action1 = new Node("Transformee", "n/a", "Action");
				action2 = new Node("TransformTo","n/a", "Action");
				action3 = new Node("Spawn", "n/a", "Action");
			}
		} else if(stypeTargetActions.contains(intData.type)) {
			if(intData.type.contains("Spawn")) {
				action1 = new Node("Spawn", "n/a", "Action");
			} else {
				action1 = new Node("KillSprite", "n/a", "Action");
			}
		}
		if(action1 != null) {
			actionList.add(action1);
			actions.add(action1);			
			action1.addOutput(sprite1);
			sprite1.addInput(action1);
			
//			createGraphNode(NodeType.ACTION, action1.getName(), action1.getId());
//			addEdge(action1.getId(), sprite1.getId());
		}
		if(action2 != null) {
			actionList.add(action2);
			actions.add(action2);
			action2.addOutput(sprite2);
			sprite2.addInput(action2);
			
//			createGraphNode(NodeType.ACTION, action2.getName(), action2.getId());
//			addEdge(action2.getId(), sprite2.getId());	
		}
		if(action3 != null) {
			Node output = findSpriteNode(intData.sprites.get(0)); 
			actionList.add(action3);
			actions.add(action3);
			
			action3.addOutput(output);
			output.addInput(action3);
			
//			createGraphNode(NodeType.ACTION, action3.getName(), action3.getId());
//			addEdge(action3.getId(), output.getId());
		}
		
		for(Node action : actionList) {
			condition.addOutput(action);
//			addEdge(condition.getId(), action.getId());
		}
		if(verbose) 
			System.out.println("Mechanic: " + sprite1.getName() + " " + sprite2.getName() + " collides " + action1.getName());

	}
	
	public Node findSpriteNode(String spriteName) {
		for (Node sprite : sprites) {
			if (spriteName.equals(sprite.getName())) {
				return sprite;
			}
		}
		return null;
	}
	
	public MultiNode findVisualGraphNode(int id) {
		Iterator<? extends MultiNode> nodes = graph.getNodeIterator();
		while(nodes.hasNext()) {
			MultiNode node = nodes.next();
			if(node.getId().equals("" + id)) {
				return node;
			}
		}
		return null;
	}
	
	public MultiNode createGraphNode(NodeType t, String name, int id) {
		String details = "";
		if(t == NodeType.SPRITE) {
			details = spriteAttributes;
		} else if(t == NodeType.CONDITION) {
			details = conditionAttributes;
		} else {
			details = actionAttributes;
		}
		
		MultiNode c = graph.addNode("" + id);
		c.addAttribute("ui.label", name);
		c.addAttribute("ui.style", details);
		
		return c;
	}
	
	public Edge addEdge(int idOne, int idTwo) {
		MultiNode first = findVisualGraphNode(idOne);
		MultiNode second = findVisualGraphNode(idTwo);
		Edge e = graph.addEdge(first.getId() + ":" + second.getId(), first, second, true);
		e.addAttribute("layout.weight", 25);
		return e;
	}
	
	public Edge addHiddenEdge(int idOne, int idTwo, int counter) {

		MultiNode first = findVisualGraphNode(idOne);
		MultiNode second = findVisualGraphNode(idTwo);
		Edge e = graph.addEdge("" + counter, first, second, true);
		e.addAttribute("layout.weight", 25);
		e.addAttribute("ui.hide");
		return e;
	}
	
	/**
	 * @return the sl
	 */
	public SLDescription getSl() {
		return sl;
	}
	
	/**
	 * @param sl the sl to set
	 */
	public void setSl(SLDescription sl) {
		this.sl = sl;
	}
	/**
	 * @return the ga
	 */
	public GameAnalyzer getGa() {
		return ga;
	}
	/**
	 * @param ga the ga to set
	 */
	public void setGa(GameAnalyzer ga) {
		this.ga = ga;
	}
	/**
	 * @return the la
	 */
	public LevelAnalyzer getLa() {
		return la;
	}
	/**
	 * @param la the la to set
	 */
	public void setLa(LevelAnalyzer la) {
		this.la = la;
	}
	/**
	 * @return the gd
	 */
	public GameDescription getGd() {
		return gd;
	}
	/**
	 * @param gd the gd to set
	 */
	public void setGd(GameDescription gd) {
		this.gd = gd;
	}
	/**
	 * @return the avatarEntities
	 */
	public List<Node> getAvatarEntities() {
		return avatars;
	}
	/**
	 * @param avatarEntities the avatarEntities to set
	 */
	public void setAvatarEntities(List<Node> avatarEntities) {
		this.avatars = avatarEntities;
	}

	/**
	 * @return the allNodes
	 */
	public List<Node> getAllNodes() {
		return allNodes;
	}

	/**
	 * @param allNodes the allNodes to set
	 */
	public void setAllNodes(List<Node> allNodes) {
		this.allNodes = allNodes;
	}

	/**
	 * @return the sprites
	 */
	public List<Node> getSprites() {
		return sprites;
	}

	/**
	 * @param sprites the sprites to set
	 */
	public void setSprites(List<Node> sprites) {
		this.sprites = sprites;
	}
	
	public NodeType getVisualNodeType(Node n) {
		if (n.getCategory().equals("Sprite")) 
			return NodeType.SPRITE;
		else if (n.getCategory().equals("Condition")) 
			return NodeType.CONDITION;
		else if (n.getCategory().equals("Action"))
			return NodeType.ACTION;
		else
			return NodeType.UKNOWN;
	}
}
