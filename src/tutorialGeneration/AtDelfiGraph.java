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
			"SubtractHealthPoints", "CloneSprite");
	private List<String> bothSpriteTargetActions = Arrays.asList("KillBoth", "PullWithIt", "CollectResource");
	private List<String> stypeTargetActions = Arrays.asList("TransformTo", "KillAll", "TransformToSingleton", "SpawnIfHasMore",
			"SpawnIfHasLess", "SpawnBehind");
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
		SPRITE, CONDITION, ACTION
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
	private String spriteAttributes = "shape:circle;fill-color:" + spriteColor +";size:100px;text-color:#000000;text-size:12;";
	private String conditionAttributes = "shape:diamond;fill-color:" + conditionColor + ";size:100px;text-color:#000000;text-size:12;";
	private String actionAttributes = "shape:box;fill-color:" + actionColor + ";size:75px;text-color:#000000;text-size:12;";
	
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
		
		
		System.setProperty("org.grapphstream.ui.render", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		graph = new MultiGraph("Mechanic Graph");
	}
	
	public void build() {
		if (verbose)
			System.out.println("Building Mechanic Graph...");
		readSpriteSet();
		readInteractionSet();
		
		if(graphVisualization) {
			graph.display();
			spaceAllNodes();
		}
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
		
		// add this as a node in the visualization graph
		createGraphNode(NodeType.SPRITE, sprite.getName(), sprite.getId());
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
//		Node action = new Node(intData.type, "n/a", "Action");
		// add these to the respective lists
		conditions.add(condition);
//		actions.add(action);
		
		// add input/output for action/condition
//		condition.addOutput(action);
//		action.addInput(condition);
		
		// add input/output for sprites/condition
		sprite1.addOutput(condition);
		sprite2.addOutput(condition);
		condition.addInput(sprite1);
		condition.addInput(sprite2);
		
		// create nodes for condition and action
		createGraphNode(NodeType.CONDITION, condition.getName(), condition.getId());
//		createGraphNode(NodeType.ACTION, action.getName(), action.getId());

		addEdge(sprite1.getId(), condition.getId());
		addEdge(sprite2.getId(), condition.getId());
//		addEdge(condition.getId(), action.getId());
		
		// create output for action if applicable
		actionDecisionTree(sprite1, sprite2, condition, intData);
		
//		if(verbose) 
//			System.out.println("Mechanic Generated: " + sprite1.getName() + " " + condition.getName() 
//			+ " " + sprite2.getName() + " " + action.getName());
	}
	
	public void actionDecisionTree(Node sprite1, Node sprite2, Node condition, InteractionData intData) {
		if (firstSpriteTargetActions.contains(intData.type)) {
			// this actions targets only the first sprite
			Node action = new Node(intData.type, "n/a", "Action");
			actions.add(action);
			
			condition.addOutput(action);
			action.addOutput(sprite1);
			sprite1.addInput(action);
			
			createGraphNode(NodeType.ACTION, action.getName(), action.getId());
			addEdge(action.getId(), sprite1.getId());
			addEdge(condition.getId(), action.getId());
			
		} else if(bothSpriteTargetActions.contains(intData.type)) {
			List<Node> actions = new ArrayList<Node>();
			Node action1 = null;
			Node action2 = null;
			if(intData.type.equals("KillBoth")) {
				action1 = new Node("KillSprite", "n/a", "Action");
				action2 = new Node("KillSprite", "n/a", "Action");
			} else if(intData.type.equals("PullWithIt")) {
				action1 = new Node("Pulled", "n/a", "Action");
				action2 = new Node("Puller", "n/a", "Action");
			} else if(intData.type.equals("CollectResource")) {
				action1 = new Node("KillSprite", "n/a", "Action");
				action2 = new Node("IncreaseResource", "n/a", "Action");
			}
				
			actions.add(action1);
			actions.add(action2);

			action1.addOutput(sprite1);
			action2.addOutput(sprite2);
			sprite1.addInput(action1);
			sprite2.addInput(action2);
			
			createGraphNode(NodeType.ACTION, action1.getName(), action1.getId());
			createGraphNode(NodeType.ACTION, action2.getName(), action2.getId());
			
			addEdge(action1.getId(), sprite1.getId());
			addEdge(action2.getId(), sprite2.getId());	
			
			for(Node action : actions) {
				condition.addOutput(action);
				addEdge(condition.getId(), action.getId());
			}
			
		} else if(stypeTargetActions.contains(intData.type)) {
			// this actions targets only the second sprite
			Node action = new Node(intData.type, "n/a", "Action");
			actions.add(action);
			
			condition.addOutput(action);
			action.addOutput(sprite1);
			sprite2.addInput(action);
			
			createGraphNode(NodeType.ACTION, action.getName(), action.getId());
			addEdge(action.getId(), sprite1.getId());
			addEdge(condition.getId(), action.getId());
		}
		
//		Node output = findSpriteNode(spriteName);
//		
//		action.addOutput(output);
//		output.addInput(action);
//		
//		// add edge for action output
//		addEdge(action.getId(), output.getId());

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
}
