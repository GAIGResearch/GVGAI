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
	
	private HashMap<String, int[]> frames;
	
	private boolean isTerminal;
	
	
	private boolean visted;
	
	public Mechanic(boolean isTerminal) {
		this.setId(Mechanic.idCounter++);

		setVisted(false);
		sprites = new ArrayList<Node>();
		conditions = new ArrayList<Node>();
		actions = new ArrayList<Node>();
		
		this.setTerminal(isTerminal);
		
		setFrames(new HashMap<String, int[]>());
	}
	
	public void addSprite(Node sprite) {
		sprites.add(sprite);
	}
	
	public void addCondition(Node condition) {
		conditions.add(condition);
	}
	
	public void addAction(Node action) {
		actions.add(action);
	}
	
	public List<Node> getSprites() {
		return sprites;
	}
	
	public void setSprites(List<Node> sprites2) {
		this.sprites = sprites2;
	}
	
	public List<Node> getConditions() {
		return conditions;
	}
	
	public void setConditions(List<Node> conditions) {
		this.conditions = conditions;
	}
	
	public List<Node> getActions() {
		return actions;
	}
	
	public void setActions(List<Node> actions) {
		this.actions = actions;
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
		if(this.getActions().get(0).equals("Win")) {
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
