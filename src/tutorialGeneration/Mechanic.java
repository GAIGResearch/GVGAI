package tutorialGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Mechanic {
	public static int idCounter;
	
	private int id;
	private List<Node> sprites;
	private List<Node> conditions;
	private List<Node> actions;
	private String readibleAction;
	private AtDelfiGraph graph;
	
	private HashMap<String, int[]> frames;
	
	private boolean isTerminal;
	
	
	private boolean visted;
	
	public Mechanic(boolean isTerminal, AtDelfiGraph graph) {
		this.setId(Mechanic.idCounter++);

		setVisted(false);
		sprites = new ArrayList<Node>();
		conditions = new ArrayList<Node>();
		actions = new ArrayList<Node>();
		
		this.graph = graph;
		this.setTerminal(isTerminal);
		
		setFrames(new HashMap<String, int[]>());
	}
	
	public void addSprite(Node sprite) {
		sprites.add(sprite);
		sprite.addMechanic(this);
	}
	
	public void addCondition(Node condition) {
		conditions.add(condition);
		condition.addMechanic(this);
	}
	
	public void addAction(Node action) {
		actions.add(action);
		action.addMechanic(this);
	}
	
	public List<Node> getSprites() {
		return sprites;
	}
	
	public void setSprites(List<Node> sprites2) {
		this.sprites = sprites2;
		
		for (Node sprite: sprites) {
			if(!sprite.getMechanics().contains(this))
				sprite.addMechanic(this);
		}
	}
	
	public List<Node> getConditions() {
		return conditions;
	}
	
	public void setConditions(List<Node> conditions) {
		this.conditions = conditions;
		
		for (Node cond: conditions) {
			if(!cond.getMechanics().contains(this))
				cond.addMechanic(this);
		}
	}
	
	public List<Node> getActions() {
		return actions;
	}
	
	public void setActions(List<Node> actions) {
		this.actions = actions;
		
		for (Node action: actions) {
			if(!action.getMechanics().contains(this))
				action.addMechanic(this);
		}
	}
	
	
	public void setReadibleAction(String readibleAction) {
		this.readibleAction = readibleAction;
	}
	
	public String getReadibleAction() {
		return readibleAction;
	}
	public List<Mechanic> getOutputs() {
		List<Mechanic> outputMechanics = new ArrayList<Mechanic>();
		for(Node action : actions) {
			for (Node sprite : action.getOutputs()) {
				for(Mechanic mech : sprite.getMechanics()) {
					if (!outputMechanics.contains(mech)) {
						outputMechanics.add(mech);
					}
				}
			}
		}
//		if (!this.isTerminal) {
//			for(Node sprite : this.getSprites()) {
//				if(graph.getAvatarEntities().contains(sprite)) {
//					// check for timeout mechanics, if they exist, and add as outputs to this mechanic
//					for(Mechanic mech : graph.getMechanics()) {
//						if(mech.conditions.get(0).getName().equals("Timeout")) {
//							outputMechanics.add(mech);
//						}
//					}
//				}
//			}
//		}
		
		return outputMechanics;
	}

	/**
	 * @return the frames
	 */
	public HashMap<String, int[]> getFrames() {
		return frames;
	}

	/**
	 * @param frames the frames to set
	 */
	public void setFrames(HashMap<String, int[]> frames) {
		this.frames = frames;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the isTerminal
	 */
	public boolean isTerminal() {
		return isTerminal;
	}

	/**
	 * @param isTerminal the isTerminal to set
	 */
	public void setTerminal(boolean isTerminal) {
		this.isTerminal = isTerminal;
	}
	
	public String toString() {
		String mechString = "";
		
		// all sprites
		mechString += "Sprites: ";
		for (Node sprite : sprites) {
			mechString += sprite.getName() + ", ";
		}
		mechString += "\n";
		
		// all conditions
		mechString += "Conditions: ";
		for (Node condition : conditions) {
			mechString += condition.getName() + ", ";
		}
		mechString += "\n";
		
		// all actions
		mechString += "Actions: ";
		for (Node action : actions) {
			mechString += action.getName() + ", ";
		}
		mechString += "\n";
				
		// all frames
		mechString += "Frames: ";
	    Iterator it = frames.entrySet().iterator();
	    while(it.hasNext()) {
	        HashMap.Entry pair = (HashMap.Entry)it.next();
	        mechString += "\n";
	        mechString += pair.getKey() + ": [";
	        for (int i : (int[]) pair.getValue()) {
	        	mechString += i + ", ";
	        }
	        mechString += "]\n";
	    }		
		return mechString;
	}
	
	public boolean isWin() {
		if(this.getActions().get(0).getName().equals("Win")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return the visted
	 */
	public boolean isVisted() {
		return visted;
	}

	/**
	 * @param visted the visted to set
	 */
	public void setVisted(boolean visted) {
		this.visted = visted;
	}
	
}
