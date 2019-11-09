package tutorialGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {

	public static int idCounter;
	
	private int id;
	
	private String name;
	private String readibleName;
	private String type;
	private String category;

	private List<Node> inputs;
	private List<Node> outputs;
	private List<String> parents;
	
	private Map<String,String> attributes;
	
	private List<Mechanic> mechanics;

	public Node(String name, String type, String category) {
		this.id = Node.idCounter++;
		
		this.name = name;
		this.type = type;
		this.category = category;
		this.inputs = new ArrayList<Node>();
		this.outputs = new ArrayList<Node>();
		this.parents = new ArrayList<String>();
		this.setMechanics(new ArrayList<Mechanic>());
		
		this.attributes = new HashMap<String, String>();
		
		this.addAttribute("isAvatar", "false");
		this.addAttribute("isPortal", "false");
		this.addAttribute("isNPC", "false");
		this.addAttribute("isResource", "false");
		this.addAttribute("isSingleton", "false");
		this.addAttribute("isStatic", "false");
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the readibleName
	 */
	public String getReadibleName() {
		return readibleName;
	}

	/**
	 * @param readibleName the readibleName to set
	 */
	public void setReadibleName(String readibleName) {
		this.readibleName = readibleName;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the inputs
	 */
	public List<Node> getInputs() {
		return inputs;
	}

	/**
	 * @param inputs the inputs to set
	 */
	public void setInputs(List<Node> inputs) {
		this.inputs = inputs;
	}
	
	public void addInput(Node input) {
		this.inputs.add(input);
	}

	/**
	 * @return the outputs
	 */
	public List<Node> getOutputs() {
		return outputs;
	}

	/**
	 * @param outputs the outputs to set
	 */
	public void setOutputs(List<Node> outputs) {
		this.outputs = outputs;
	}
	
	public void addOutput(Node output) {
		this.outputs.add(output);
	}

	/**
	 * @return the parents
	 */
	public List<String> getParents() {
		return parents;
	}

	/**
	 * @param parents the parents to set
	 */
	public void setParents(List<String> parents) {
		this.parents = parents;
	}

	/**
	 * @return the attributes
	 */
	public Map<String,String> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String,String> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * adds a new key, value pair to the attributes map
	 * @param key the key to set
	 * @param value the value of the key
	 */
	public void addAttribute(String key, String value) {
		this.attributes.put(key, value);
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
	 * @return the mechanics
	 */
	public List<Mechanic> getMechanics() {
		return mechanics;
	}

	/**
	 * @param mechanics the mechanics to set
	 */
	public void setMechanics(List<Mechanic> mechanics) {
		this.mechanics = mechanics;
	}
	
	public void addMechanic(Mechanic mechanic) {
		this.mechanics.add(mechanic);
	}
}
