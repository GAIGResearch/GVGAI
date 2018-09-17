package tutorialGeneration;

import java.util.ArrayList;
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
	private String spriteAttributes = "shape:circle;fill-color:" + spriteColor +";size:100px;text-alignment:center;text-color:#000000;text-size:15;";
	private String conditionAttributes = "shape:diamond;fill-color: " + conditionColor + " ;size: 100px;text-alignment: center;text-color:#000000;text-size:13;";
	private String actionAttributes = "shape:box;fill-color: " + actionColor + " ;size: 75px;text-alignment: center;text-color:#FFFFFF;text-size:15;";
	
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
		Node action = new Node(intData.type, "n/a", "Action");
		// add these to the respective lists
		conditions.add(condition);
		actions.add(action);
		
		// add input/output for action/condition
		condition.addOutput(action);
		action.addInput(condition);
		
		// add input/output for sprites/condition
		sprite1.addOutput(condition);
		sprite2.addOutput(condition);
		condition.addInput(sprite1);
		condition.addInput(sprite2);
		
		// create output for action if applicable
		
		if(verbose) 
			System.out.println("Mechanic Generated: " + sprite1.getName() + " " + condition.getName() 
			+ " " + sprite2.getName() + " " + action.getName());

	}
	
	public void actionDecisionTree(Node sprite1, Node sprite2, Node action, InteractionData intData) {
		if (intData.sprites.size() > 0) {
			for (String spriteName : intData.sprites) {
				Node output = findSpriteNode(spriteName);
			}
		}
	}
	
	public Node findSpriteNode(String spriteName) {
		for (Node sprite : sprites) {
			if (spriteName.equals(sprite.getName())) {
				return sprite;
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
