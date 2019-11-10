package tutorialGeneration;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.google.gson.JsonObject;

public class AtDelfiGraph {
	/**
	 * a simple verbose flag
	 */
	private boolean verbose = true;
	
	/**
	 * a flag to show the graph visualization
	 */
	private boolean nodeVisualization = true;
	private boolean mechanicVisualization = true;
		
	/**
	 * A list of all the nodes in the AtDelfi Mechanic graph
	 */
	private List<Node> allNodes;
	
	private List<String> firstSpriteTargetActions = Arrays.asList("KillSprite", "KillIfHasLess", "KillIfHasMore", 
			"KillIfOtherHasMore", "KillIfFromAbove", "FlipDirection", "ReverseDirection", "AttractGaze", "TurnAround", "WrapAround",
			"BounceForward", "ChangeResource", "AddHealthPoints", "AddHealthPointsToMax", "Align",
			"SubtractHealthPoints", "CloneSprite", "StepBack");
	private List<String> bothSpriteTargetActions = Arrays.asList("KillBoth", "PullWithIt", "CollectResource");
	private List<String> stypePlusTargetActions = Arrays.asList("TransformTo", "TransformToSingleton", "TransformToAll");
	private List<String> stypeTargetActions = Arrays.asList("Spawn", "KillAll", "SpawnIfHasMore", "SpawnIfHasLess", "SpawnBehind");
	private List<String> noTargetActions = Arrays.asList("UndoAll");
	/**
	 * Information parsed from the VGDL File
	 */
	private GameDescription gd;
	private SLDescription sl;
	private GameAnalyzer ga;
	private LevelAnalyzer la;
	
	/**
	 * The name of the game
	 */
	private String name;
	
	/**
	 * represents the entity which the player controls in the game
	 */
	private List<Node> avatars;
	private List<Node> sprites;
	private List<Node> conditions;
	private List<Node> actions;
	private List<Node> terminations;
	
	private List<Mechanic> mechanics;
	
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
	private Graph mechanicGraph;

	/**
	 * colors for the nodes
	 */
	private String spriteColor = "#FCEC8C";
	private String conditionColor = "#E77E43";
	private String actionColor = "#743C2E";
	private String critPathColor = "#F4A742";
	private String mechanicColor = "#FFFFFF";
	/**
	 * attributes for the nodes
	 */
	private String spriteAttributes = "shape:circle;fill-color:" + spriteColor +";size:100px;text-color:#000000;text-size:12;text-alignment:center;";
	private String conditionAttributes = "shape:diamond;fill-color:" + conditionColor + ";size:100px;text-color:#000000;text-size:12;text-alignment:center;";
	private String actionAttributes = "shape:box;fill-color:" + actionColor + ";size:75px;text-color:#FFFFFF;text-size:12;text-alignment:center;";
	
	private String critPathAttributes = "shape:rounded-box;fill-color:" + critPathColor +";size:250px, 25px;text-color:#000000;text-size:12;text-alignment:center;";
	private String mechanicAttributes = "shape:rounded-box;stroke-mode:plain;fill-color:" + mechanicColor +";size:250px, 25px;text-color:#000000;text-size:12;text-alignment:center;";
	
	/**
	 * Constructs a new AtDelfi Graph
	 * @param gd the GameDescription of the game
	 * @param sl the SLDescription of the game
	 * @param ga the GameAnalyzer of the game
	 * @param la the LevelAnalyzer of the game
	 * @param gameName the name of the game
	 */
	
	public AtDelfiGraph(GameDescription gd, SLDescription sl, GameAnalyzer ga, LevelAnalyzer la, String gameName) {
		this.gd = gd;
		this.sl = sl;
		this.ga = ga;
		this.la = la;
		
		this.name = gameName;
		
		avatars = new ArrayList<Node>();
		sprites = new ArrayList<Node>();
		conditions = new ArrayList<Node>();
		actions = new ArrayList<Node>();
		terminations = new ArrayList<Node>();
		allNodes = new ArrayList<Node>();
		
		mechanics = new ArrayList<Mechanic>();

		graph = new MultiGraph("Node Graph");
		
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
	}
	
	public Graph build() {
		if (verbose)
			System.out.println("Building Mechanic Graph...");
		readSpriteSet();
		parseAllSpriteMechanics();
		readInteractionSet();
		readTerminationSet();
		allNodes.addAll(sprites);
		allNodes.addAll(conditions);
		allNodes.addAll(actions);
		
//		if(nodeVisualization) {
//			visualizeNodeGraph();
//		}
//		if(mechanicVisualization) {
//			visualizeMechanicGraph();
//		}
		
		return graph;

	}

	public void visualizeNodeGraph() {

		// create all nodes
		for(Node node : allNodes) {
			// add this as a node in the visualization graph
			createGraphNode(getVisualNodeType(node), node.getName(), node.getId());
		}
		// create all edges
		for (Node node : allNodes) {
			for (Node output : node.getOutputs()) {
				addEdge(node.getId(), output.getId(), this.graph);
			}
		}
		// space the nodes out a bit
		spaceAllNodes(graph);
	}
	
	public void visualizeMechanicGraph(String agent, int level) {
		mechanicGraph = new MultiGraph("Mechanic Graph");
		for (Mechanic mech : mechanics) {
			MultiNode n = createMechanicNode(mechanicGraph, mech, agent, level);
		}
		
		for(Mechanic mech : mechanics) {
			for (Mechanic output : mech.getOutputs()) {
				addEdge(mech.getId(), output.getId(), mechanicGraph);
			}
		}
		spaceAllNodes(mechanicGraph);
		
		mechanicGraph.display();
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
		
		// sneak the score and time nodes into existence
		makeScoreAndTimeNodes();
		
		if(verbose) {
			System.out.println("ID \t:\t Name \t\t:\t Type");
			for(Node sprite : this.sprites) {
				System.out.println(sprite.getId() + " \t:\t " + sprite.getName() + "    \t:\t " + sprite.getType());
			}
			System.out.println("Avatars:");
			for (Node avatar : this.avatars) {
				System.out.println(avatar.getId() + " : " + avatar.getName());
			}
		}
		
			
			
	}
	
	public void makeScoreAndTimeNodes() {
		Node score = new Node("Score", "n/a", "Sprite");
		Node time = new Node("Time", "n/a", "Sprite");
		
		// add to sprites
		sprites.add(score);
		sprites.add(time);
	}
	
	public void spaceAllNodes(Graph graph) {
		int counter = 0;
		Iterator<? extends MultiNode> nodes = graph.getNodeIterator();
		while(nodes.hasNext()) {
			MultiNode node = nodes.next();
			Iterator<? extends MultiNode> nodes2 = graph.getNodeIterator();
			while(nodes2.hasNext()) {
				MultiNode node2 = nodes2.next();
				if(!node.equals(node2)) {
					addHiddenEdge(Integer.parseInt(node.getId()), Integer.parseInt(node2.getId()), counter, graph);
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
				if (!sprite1.getName().equals("Time") && !sprite1.getName().equals("Score") 
						&& !sprite2.getName().equals("Time") && !sprite2.getName().equals("Score")) {
					ArrayList<InteractionData> intDataList = gd.getInteraction(sprite1.getName(), sprite2.getName());
					for (InteractionData intData : intDataList) {
						if(sprite1 != sprite2)
							classifyInteractionData(sprite1, sprite2, intData);
					}
				}
			}
		}
	}
	
	public void readTerminationSet() {
		if(verbose)
			System.out.println("Reading Termination Set...");
		List<TerminationData> terminations = gd.getTerminationConditions();
		for(TerminationData termination : terminations) {
			classifyTerminationData(termination);
		}
		
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
		Node action4 = null;
		List<Node> actionList = new ArrayList<Node>();
		
		String readibleAction = intData.type;

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
			if(intData.type.equals("TransformTo")) {
				action1 = new Node("Transformee", "n/a", "Action");
				action3 = new Node("TransformTo", "n/a", "Action");
			} else if(intData.type.equals("TransformToSingleton")) {
				// TODO fix this so this method is covered. action2 will incorrectly point at sprite2 for now
				action1 = new Node("Transformee", "n/a", "Action");
				action2 = new Node("TransformTo","n/a", "Action");
				action3 = new Node("Spawn", "n/a", "Action");
			} 
			else if(intData.type.equals("TransformToAll")) {
				System.out.println("TransformToAll");
				action3 = new Node("Transformee", "n/a", "Action");
				action4 = new Node("TransformTo", "n/a", "Action");	
			}
		} else if(stypeTargetActions.contains(intData.type)) {
			if(intData.type.contains("Spawn")) {
				action1 = new Node("Spawn", "n/a", "Action");
			} else if(intData.type.equals("KillAll")) {
				action1 = new Node("KillAll", "n/a", "Action");
			} else {
				action1 = new Node("KillSprite", "n/a", "Action");
			}
		} else if(noTargetActions.contains(intData.type)) {
			if(intData.type.contains("UndoAll")) {
				action1 = new Node("UndoAll", "n/a", "Action");
			}
		}
		else {
			if(intData.type.equals("TeleportToExit")) {
				Node stype = null;
				if(sprite1.getType().equals("Portal")){
					stype = findSpriteNode(sprite1.getAttributes().get("stype"));	
				} else {
					stype = findSpriteNode(sprite2.getAttributes().get("stype"));	
				}
				condition = new Node("Collides", "n/a", "Condition");
				action4 = new Node("TeleportsTo", "n/a", "Action");
				action1 = new Node("Teleportee", "n/a", "Action");
				
				actionList.add(action4);
				action4.addOutput(stype);

				action4.addInput(condition);
				stype.addInput(action4);
				
				conditions.add(condition);
				actions.add(action4);
			} 
		}

		if(action1 != null) {
			actionList.add(action1);
			actions.add(action1);			
			action1.addOutput(sprite1);
			sprite1.addInput(action1);
		}
		if(action2 != null) {
			actionList.add(action2);
			actions.add(action2);
			action2.addOutput(sprite2);
			sprite2.addInput(action2);
		}
		if(action3 != null) {
			Node output = findSpriteNode(intData.sprites.get(0)); 
			actionList.add(action3);
			actions.add(action3);
			
			action3.addOutput(output);
			output.addInput(action3);
		}
		// score change mechanics
		if(Integer.parseInt(intData.scoreChange) != 0) {
			// now search for Score node
			Node score = findSpriteNode("Score");
			Node scoreChangeAction = new Node("ScoreChange", "n/a", "Action");
			
			actions.add(scoreChangeAction);
			scoreChangeAction.addAttribute("score", intData.scoreChange);
			scoreChangeAction.addOutput(score);
			score.addInput(scoreChangeAction);
			actionList.add(scoreChangeAction);
		}
		
		for(Node action : actionList) {
			condition.addOutput(action);
		}
		

		if(verbose)
			try{
				System.out.println("Mechanic: " + sprite1.getName() + " " + sprite2.getName() + " collides " + intData.type);
			} catch (Exception e)
			{
				System.out.println("Special Case: " + intData.type);
			}
		
		// make mechanic
		List<Node> sprites = Arrays.asList(new Node[]{sprite1, sprite2});
		List<Node> conditions = Arrays.asList(new Node[]{condition});
		createMechanic(sprites, conditions, actionList, readibleAction);
	}
	
	public void parseAllSpriteMechanics() {
		for (Node sprite : sprites) {
			parseSpriteMechanics(sprite);
		}
		for (Node avatar : avatars) {
			parseAvatarMechanics(avatar);
		}
	}
		
	public void parseSpriteMechanics(Node sprite) {
		if(sprite.getType().equals("SpawnPoint") || sprite.getType().equals("Bomber")) {
			
			String name = "";
			if(sprite.getAttributes().containsKey("cooldown")) {
				name = "Every " + sprite.getAttributes().get("cooldown") + " Ticks";
			} else {
				name = "Every Tick";
			}
			
			Node spawnee = findSpriteNode(sprite.getAttributes().get("stype"));
			
			Node condition = new Node(name, "n/a", "Condition");
			Node action = new Node("Spawn", "n/a", "Action");
			
			conditions.add(condition);
			actions.add(action);
			
			sprite.addOutput(condition);
			condition.addOutput(action);
			action.addOutput(spawnee);
			
			spawnee.addInput(action);
			action.addInput(condition);
			condition.addInput(sprite);
			
			Mechanic spriteMechanic = new Mechanic(false, this);
			spriteMechanic.setConditions(Arrays.asList(condition));
			spriteMechanic.setActions(Arrays.asList(action));
			spriteMechanic.addSprite(sprite);
			spriteMechanic.addSprite(spawnee);
			mechanics.add(spriteMechanic);
		}
	}
	public void parseAvatarMechanics(Node avatar) {
		// short conditional for what kind of avatar it is
		if(avatar.getType().equals("ShootAvatar") || avatar.getType().equals("OngoingShootAvatar") || avatar.getType().equals("FlakAvatar")) {
			Node condition = new Node("Press Space", "Player Input", "Condition");
			Node action = new Node("Spawn", "n/a", "Action");
			Node output = findSpriteNode(avatar.getAttributes().get("stype"));

			conditions.add(condition);
			actions.add(action);
			
			avatar.addOutput(condition);
			condition.addInput(avatar);
			condition.addOutput(action);
			action.addInput(condition);
			action.addOutput(output);
			output.addInput(action);
			
			Mechanic mech = new Mechanic(false, this);
			mech.addAction(action);
			mech.addCondition(condition);
			mech.addSprite(avatar);
			mech.setReadibleAction(action.getName());
			mechanics.add(mech);
		}
	}
	
	public void classifyTerminationData(TerminationData termination) {
		System.out.println("Termination Found");
			
		Node condition = new Node(termination.type, "Terminal Condition", "Condition");
		condition.addAttribute("limit", "" + termination.limit);
		
		Node action = new Node((termination.win.equals("True") ? "Win" : "Lose"), "Termination", "Action");
		
		terminations.add(action);
		condition.addOutput(action);
		action.addInput(condition);
		
		actions.add(action);
		conditions.add(condition);
		
		List<Node> mechConditions = new ArrayList<Node>();
		mechConditions.add(condition);
		
		List<Node> mechActions = new ArrayList<Node>();
		mechActions.add(action);

		// check if the portal will die on its own. If it does, we don't include it as a condition
		List<Node> mechSprites = new ArrayList<Node>();
		for (String name : termination.sprites) {
			Node sprite = findSpriteNode(name);
			mechSprites.add(sprite);
			if(!sprite.getAttributes().containsKey("total") || sprite.getAttributes().get("total").equals("0")) {
				sprite.addOutput(condition);
				condition.addInput(sprite);
			}
		}
		
		// if this is a time condition, then it wont have any sprites associated with it by default, so add the Time and the avatar nodes to it
		boolean flag = false;
		if (condition.getName().equals("Timeout")) {
			Node time = findSpriteNode("Time");
			mechSprites.add(time);
			time.addOutput(condition);
			condition.addInput(time);
			for(Node avatar : this.getAvatarEntities()) {
				avatar.addOutput(condition);
				condition.addInput(avatar);
				mechSprites.add(avatar);

			}
			flag = true;

		} 
			createMechanic(mechSprites, mechConditions, mechActions, action.getName(), termination.win.equals("True"), flag);
	}
	
	public void createMechanic(List<Node> sprites2, List<Node> conditions2, List<Node> actionList, String readibleAction) {
		Mechanic mechanic = new Mechanic(false, this);
		mechanic.setSprites(sprites2);
		mechanic.setConditions(conditions2);
		mechanic.setActions(actionList);
		mechanic.setReadibleAction(readibleAction);
		
		mechanics.add(mechanic);
		
	}
	
	public void createMechanic(List<Node> sprites2, List<Node> conditions2, List<Node> actionList, String readibleAction, boolean isWin, boolean isTimeout) {
		Mechanic mechanic = new Mechanic(true, this);
		mechanic.setSprites(sprites2);
		mechanic.setConditions(conditions2);
		mechanic.setActions(actionList);
		mechanic.setReadibleAction(readibleAction);
		
		mechanics.add(mechanic);
		
		if (isTimeout) {
			
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
	
	public MultiNode findVisualGraphNode(Graph graph, int id) {
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
	
	public MultiNode createMechanicNode(Graph graph, Mechanic mech, String agent, int level) {
		String details = mechanicAttributes;
		MultiNode c = graph.addNode("" + mech.getId());
		
		String beginLabel = "";
		for (Node sprite : mech.getSprites()) {
			beginLabel += sprite.getName() + " ";
		}
		beginLabel += mech.getConditions().get(0).getName() + " ";
		c.addAttribute("ui.label", beginLabel + mech.getReadibleAction()); //+ " : " + mech.getFrames().get(agent)[level]);

//		c.addAttribute("ui.label", beginLabel + mech.getReadibleAction());
		c.addAttribute("ui.style", details);
		return c;
	}
	
	public Edge addEdge(int idOne, int idTwo, Graph graph) {
		if(idOne != idTwo) {
			MultiNode first = findVisualGraphNode(graph, idOne);
			MultiNode second = findVisualGraphNode(graph,idTwo);
			Edge e = graph.addEdge(first.getId() + ":" + second.getId(), first, second, true);
			e.addAttribute("layout.weight", 25);
			return e;
		}
		return null;
	}
	
	public Edge addHiddenEdge(int idOne, int idTwo, int counter, Graph graph) {

		MultiNode first = findVisualGraphNode(graph, idOne);
		MultiNode second = findVisualGraphNode(graph, idTwo);
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
	

	public void insertFrameInformation(VisualDemonstrationInterfacer vdi) {

			ArrayList<String> agents = vdi.getAgents(this.name);
			//System.out.println(agents.size());
			int levelCount = vdi.getLevelCount(this.name);
			int playthroughCount = vdi.getPlaythroughCount(this.name);

			for(Mechanic mech: mechanics) {

				// keeps track of avg frames by agent-level 
				HashMap<String, int[]> frames = new HashMap<String, int[]>();


				for (String agent : agents) {
					int[] framesForAgent = new int[levelCount];
					for(int i = 0; i < levelCount; i++) {
						try {
							framesForAgent[i] = vdi.mechAgentLevelQuery(mech, agent, i, 1);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							framesForAgent[i] = -1;
						}
						frames.put(agent, framesForAgent);
					
					}
				
					mech.setFrames(frames);
				}
			}						
	}
		
	public void colorizeCriticalPath(List<Mechanic> criticalPath) {
		if(mechanicVisualization) {
			for (Mechanic mech : criticalPath) {
				MultiNode mechNode = findVisualGraphNode(this.mechanicGraph, mech.getId());
				mechNode.changeAttribute("ui.style", this.critPathAttributes);
				System.out.println(mech);
			}
		}
	}

	
	public List<Mechanic> getMechanics() {
		return mechanics;
	}
	
	
	public List<Node> getConditions() {
		return conditions;
	}
	
	public List<Node> getActions() {
		return actions;
	}
	
	public List<Node> getAvatars() {
		return avatars;
	}

	/**
	 * @return the nodeVisualization
	 */
	public boolean isNodeVisualization() {
		return nodeVisualization;
	}

	/**
	 * @param nodeVisualization the nodeVisualization to set
	 */
	public void setNodeVisualization(boolean nodeVisualization) {
		this.nodeVisualization = nodeVisualization;
	}

	/**
	 * @return the mechanicVisualization
	 */
	public boolean isMechanicVisualization() {
		return mechanicVisualization;
	}

	/**
	 * @param mechanicVisualization the mechanicVisualization to set
	 */
	public void setMechanicVisualization(boolean mechanicVisualization) {
		this.mechanicVisualization = mechanicVisualization;
	}
}