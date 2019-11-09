package agents.mrtndwrd;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;

import java.util.Random;
import java.util.HashSet;
import java.util.ArrayList;

@SuppressWarnings("unchecked")
public class SingleTreeNode
{
	private static final double HUGE_NEGATIVE = -10000000.0;

	private static final double HUGE_POSITIVE =  10000000.0;

	/** This gets counted upon the score of the option currently being followed.
	 * It's less than the HUGE_NEGATIVE and HUGE_POSITIVE so that we'll switch
	 * options to avoid dying or to increase win-chance */
	private static final double AGENT_OPTION_EXTRA = 0.1;

	/** mctsSearch continues until there are only so many miliseconds left */
	public static final int REMAINING_LIMIT = 6;

	public static double epsilon = 1e-6;

	public static double egreedyEpsilon = 0.05;

	public StateObservation state;

	public SingleTreeNode parent;

	public SingleTreeNode[] children;

	public double totValue;

	public int nVisits;

	private ArrayList<Option> possibleOptions;

	private HashSet<Integer> optionObsIDs;

	/** When this is true, something just expanded this node. This will be reset
	 * by the treePolicy */
	private boolean expanded; 

	/** The option that is chosen in this node. This option is followed until it
	 * is finished, thereby representing a specific subtree in the whole */
	private Option chosenOption;

	/** The option that the agent is already following. This gets an extra score */
	private Option agentOption;

	public static Random random;
	/** The depth in the rollout of this node (initialized as parent.node+1) */
	public int nodeDepth;

	private boolean chosenOptionFinished;

	protected static double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};

	/** Root node constructor */
	public SingleTreeNode(ArrayList<Option> possibleOptions, HashSet<Integer> optionObsIDs, Random rnd, Option currentOption) 
	{
		this(null, null, null, possibleOptions, optionObsIDs, rnd);
		this.agentOption = currentOption;
	}

	/** normal constructor */
	public SingleTreeNode(StateObservation state, SingleTreeNode parent, Option chosenOption, ArrayList<Option> possibleOptions, HashSet<Integer> optionObsIDs, Random rnd)
	{
		this.state = state;
		this.parent = parent;
		this.random = rnd;
		this.possibleOptions = possibleOptions;
		this.optionObsIDs = optionObsIDs;
		this.chosenOption = chosenOption;
		this.expanded = false;
		// Create the possibility of chosing new options
		if(chosenOption == null || chosenOption.isFinished(state))
		{
			this.chosenOptionFinished = true;
			// Update the option ranking if needed
			if(chosenOption != null && chosenOption.isFinished(state))
				chosenOption.updateOptionRanking();

			children = new SingleTreeNode[possibleOptions.size()];
		}
		// The only child is the continuation of this option.
		else
		{
			this.chosenOptionFinished = false;
			children = new SingleTreeNode[1];
		}

		totValue = 0.0;
		if(parent != null)
			nodeDepth = parent.nodeDepth+1;
		else
			nodeDepth = 0;
	}

	public void mctsSearch(ElapsedCpuTimer elapsedTimer) 
	{
		double avgTimeTaken = 0;
		double acumTimeTaken = 0;
		long remaining = elapsedTimer.remainingTimeMillis();
		int numIters = 0;

		while(remaining > 2*avgTimeTaken && remaining > REMAINING_LIMIT)
		{
			ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

			// Select the node to explore (either expanding unexpanded node, or
			// selecting the best one with UCT)
			//System.out.printf("Remaining before treePolicy: %d\n", elapsedTimer.remainingTimeMillis());
			SingleTreeNode selected = treePolicy();

			// System.out.println("Selected: " + selected);

			// Get node value using a max-depth rollout
			//System.out.printf("Remaining before rollOut: %d\n", elapsedTimer.remainingTimeMillis());
			double delta = selected.rollOut();

			// Set values for parents of current node, using new rollout value
			//System.out.printf("Remaining before rollOut: %d\n", elapsedTimer.remainingTimeMillis());
			backUp(selected, delta);
			//System.out.printf("Remaining after backUp: %d\n", elapsedTimer.remainingTimeMillis());

			numIters++;
			acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;

			avgTimeTaken = acumTimeTaken/numIters;
			remaining = elapsedTimer.remainingTimeMillis();
		}
	}

	/** Expand the current treenode, if it's not fully expanded. Else, return
	 * the best node using uct
	 */
	public SingleTreeNode treePolicy() 
	{
		SingleTreeNode cur = this;
		SingleTreeNode next;
		while (!cur.state.isGameOver() && cur.nodeDepth < Agent.ROLLOUT_DEPTH)
		{
			next = cur.uct();
			// If we have expanded, return the new node for rollouts
			if(this.expanded)
			{
				// Also reset expanded to false
				this.expanded = false;
				return next;
			}
			// Else: continue with this node
			cur = next;
		}

		return cur;
	}


	public SingleTreeNode expand() 
	{
		Option nextOption;
		int bestOption = 0;

		// If there's no chosenOption, we'll have to choose a new one
		if(chosenOptionFinished)
		{
			double bestValue = -1;
			// Select random option with index that isn't taken yet.
			for (int i = 0; i < children.length; i++) 
			{
				double x = random.nextDouble();
				if (x > bestValue && children[i] == null) 
				{
					bestOption = i;
					bestValue = x;
				}
			}
			nextOption = this.possibleOptions.get(bestOption).copy();
		}
		// Else, this node will just expand the chosenOption into child 0 (its
		// only child) until it's done! 
		else
		{
			bestOption = 0;
			// FIXME: Should this be .copy()?
			nextOption = chosenOption;
		}

		SingleTreeNode tn = expandChild(bestOption, nextOption);
		return tn;
	}

	public SingleTreeNode expandChild(int id, Option nextOption)
	{
		this.expanded = true;
		StateObservation nextState = this.state.copy();
		Types.ACTIONS action = nextOption.act(nextState);

		// Step 1: Follow the option:
		nextState.advance(action);

		// Step 2: Update the option's values:
		//nextOption.addReward(Lib.simpleValue(nextState) - //Lib.simpleValue(state));
		nextOption.addReward(nextState.getGameScore() - state.getGameScore());

		// Step 3: get the new option set
		ArrayList<Option> newOptions = (ArrayList<Option>) this.possibleOptions.clone();
		HashSet<Integer> newOptionObsIDs = (HashSet<Integer>) this.optionObsIDs.clone();
		Lib.setOptions(nextState, newOptions, newOptionObsIDs);

		// Step 4: create a child node
		SingleTreeNode tn = new SingleTreeNode(nextState, this, nextOption, newOptions, newOptionObsIDs, this.random);
		children[id] = tn;

		// Step 5: Set the observation grid to the new grid:
		Agent.aStar.setLastObservationGrid(nextState.getObservationGrid());
		Agent.aStar.checkForWalls(state, action, nextState);
		return tn;
	}

	public SingleTreeNode uct() 
	{
		// For speeding up the situation where an option is being followed, and
		// just 1 child exists
		if(!chosenOptionFinished)
		{
			if(this.children[0] == null)
				// FIXME: This might need .copy() (but I don't think so);
				expandChild(0, chosenOption);
			return this.children[0];
		}

		int selectedId = -1;
		double bestValue = -Double.MAX_VALUE;
		// Defines if a child already exists in the array, or still has the
		// "null" value
		boolean expandChild;
		boolean bestExpandChild = false;
		double hvVal;
		int visits;
		int agentOptionIndex = -1;
		if(agentOption != null)
		{
			// TODO: Why would this result in -1?
			agentOptionIndex = possibleOptions.indexOf(agentOption);
		}
		// Expand the agentOption first, so that it's always expanded
		if(agentOptionIndex != -1 && children[possibleOptions.indexOf(agentOption)] == null)
		{
			visits = 0;
			selectedId = possibleOptions.indexOf(agentOption);
			bestExpandChild = true;
		}
		// Expand all the other stuff
		else
		{
			for (int i=0; i<this.children.length; i++)
			{
				expandChild = false;
				// Initialize hvVal with the optionRanking
				hvVal = Agent.optionRanking.get(this.possibleOptions.get(i).getType());
				if(children[i] == null)
				{
					expandChild = true;
					// set hvVal to the option's expected value
					visits = 0;
					// Keep hvVal as it is now: this encourages exploration towards
					// good options
				}
				else
				{
					// Count the optionRanking only ALPHA times
					hvVal = (1 - Agent.ALPHA) * this.children[i].totValue
								+ Agent.ALPHA * hvVal;
					visits = this.children[i].nVisits;
				}

				double childValue =  hvVal / (visits + this.epsilon);

				childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

				double uctValue = childValue +
						Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (visits + this.epsilon));

				// small sampleRandom numbers: break ties in unexpanded nodes
				uctValue = Utils.noise(uctValue, this.epsilon, this.random.nextDouble());	 //break ties randomly

				// small sampleRandom numbers: break ties in unexpanded nodes
				if (uctValue > bestValue) 
				{
					selectedId = i;
					bestValue = uctValue;
					bestExpandChild = expandChild;
				}
			}
		}

		if (selectedId == -1)
		{
			throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
		}

		SingleTreeNode selected;
		if(bestExpandChild)
		{
			Option nextOption = this.possibleOptions.get(selectedId).copy();
			selected = expandChild(selectedId, nextOption);
		}
		else
		{
			selected = children[selectedId];
		}

		return selected;
	}

	/** Perform a rollout with actions taken from chosenOption, and when that's
	 * finished random actions, on the current node of maximally
	 * Agent.ROLLOUT_DEPTH. 
	 * @return Delta is the "simpleValue" of the last state the rollOut
	 * arrives in. 
	 */
	public double rollOut()
	{
		StateObservation rollerState = state.copy();
		int thisDepth = this.nodeDepth;
		Option rollerOption = null;
		if(chosenOption != null)
			rollerOption = chosenOption.copy();
		// Instantiate "rollerOptionFinished" to whether it's null:
		boolean rollerOptionFinished = rollerOption == null;
		double lastScore = Lib.simpleValue(rollerState);
		//double lastScore = rollerState.getGameScore();
		while (!finishRollout(rollerState,thisDepth)) 
		{
			// System.out.println("Roller depth " + thisDepth);
			// if(this.parent != null)
			// 	System.out.println(this.parent);

			Types.ACTIONS action;
			//if(!rollerOptionFinished)
			//{
			//	// Set the lastScore for the next iteration 
			//	//lastScore = Lib.simpleValue(rollerState);
			//	lastScore = rollerState.getGameScore();

			//	// If the option is finished, update the Agent's option ranking
			//	if(rollerOption.isFinished(rollerState))
			//	{
			//		rollerOption.updateOptionRanking();
			//		rollerOptionFinished = true;
			//	}
			//}
			//// If possible follow this node's option, then follow a random policy
			//if(!rollerOptionFinished)
			//{
			//	action = rollerOption.act(rollerState);
			//	rollerState.advance(action);
			//	// Update the option's reward
			//	//rollerOption.addReward(Lib.simpleValue(rollerState) - lastScore);
			//	rollerOption.addReward(rollerState.getGameScore() - lastScore);
			//}
			//else
			//{
				action = Agent.actions[random.nextInt(Agent.actions.length)];
				rollerState.advance(action);
			//}
			thisDepth++;
		}

		// Update the ranking with how far this option has come if it hasn't
		// finished
		// if(!rollerOptionFinished)
		// {
		// 	// Update the ranking for the finished option
		// 	rollerOption.updateOptionRanking();
		// }
		
		double delta = Lib.simpleValue(rollerState);

		if(delta < bounds[0])
			bounds[0] = delta;

		if(delta > bounds[1])
			bounds[1] = delta;

		return delta;
	}

	public boolean finishRollout(StateObservation rollerState, int depth)
	{
		if(depth >= Agent.ROLLOUT_DEPTH)	  //rollout end condition.
			return true;

		if(rollerState.isGameOver())			   //end of game
			return true;

		return false;
	}

	public void backUp(SingleTreeNode node, double result)
	{
		SingleTreeNode n = node;
		while(n != null)
		{
			n.nVisits++;
			n.totValue += result;
			n = n.parent;
		}
	}

	public int mostVisitedAction() 
	{
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		boolean allEqual = true;
		double first = -1;

		for (int i=0; i<children.length; i++) {

			if(children[i] != null)
			{
				if(first == -1)
					first = children[i].nVisits;
				else if(first != children[i].nVisits)
				{
					allEqual = false;
				}

				double childValue = children[i].nVisits;
				childValue = Utils.noise(childValue, this.epsilon, this.random.nextDouble());	 //break ties randomly
				if (childValue > bestValue) {
					bestValue = childValue;
					selected = i;
				}
			}
		}

		if (selected == -1)
		{
			System.out.println("Unexpected selection!");
			selected = 0;
		}
		else if(allEqual)
		{
			//If all are equal, we opt to choose for the one with the best Q.
			selected = bestAction();
		}
		return selected;
	}

	public int bestAction()
	{
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;

		for (int i=0; i<children.length; i++) 
		{
			if(children[i] != null) 
			{
				double childValue = children[i].totValue / (children[i].nVisits + this.epsilon);
				childValue = Utils.noise(childValue, this.epsilon, this.random.nextDouble());	 //break ties randomly
				if(agentOption != null && agentOption.equals(children[i].chosenOption) && !agentOption.finished)
				{
					//System.out.println("Adding for option " + agentOption);
					childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
					childValue += AGENT_OPTION_EXTRA;
					//System.out.println(childValue);
				}
				else
				{
					//System.out.println("Not adding for child " + children[i].chosenOption + 
					//		" and option " + agentOption);
					childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
					//System.out.println(childValue);
				}
				if (childValue > bestValue)
				{
					bestValue = childValue;
					selected = i;
				}
			}
		}
		if (selected == -1)
		{
			System.out.println("Unexpected selection!");
			selected = 0;
		}
		return selected;
	}

	public boolean notFullyExpanded() 
	{
		for (SingleTreeNode tn : children) 
		{
			if (tn == null) 
			{
				return true;
			}
		}

		return false;
	}
	
	public String print(int depth)
	{
		String s = "";
		if(this.parent == null)
			s += "root";
		else
			s += String.format("%s (%f)", chosenOption, totValue);
		for(SingleTreeNode node : children)
		{
			if(node != null)
			{
				s += "\n";
				for(int i=0; i<depth; i++)
				{
					s += "|\t";
				}
				s += "\\- ";
				s += node.print(depth+1);
			}
		}
		return s;
	}

	public String toString()
	{
		return print(0);
	}

}
