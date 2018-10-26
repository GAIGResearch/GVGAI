package tutorialGeneration;

import java.util.ArrayList;
import java.util.HashMap;
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
	
	public Mechanic(boolean isTerminal) {
		this.setId(Mechanic.idCounter++);

		sprites = new ArrayList<Node>();
		conditions = new ArrayList<Node>();
		actions = new ArrayList<Node>();
		
		this.isTerminal = isTerminal;
		
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
	
}
