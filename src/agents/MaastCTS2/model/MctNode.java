package agents.MaastCTS2.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import agents.MaastCTS2.Agent;
import agents.MaastCTS2.Globals;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.iw.NoveltyTester;
import agents.MaastCTS2.iw.NoveltyTester.NOVELTY_LEVELS;
import agents.MaastCTS2.iw.NoveltyTester.NOVELTY_TEST_MODES;
import agents.MaastCTS2.iw.StateMemory;
import core.game.StateObservation;
import ontology.Types.ACTIONS;

public class MctNode {
	/** The parent node of this node */
	private MctNode parent;
	/** The action that, when applied to the parent, results in this node */
	private final ACTIONS actionFromParent;

	/** The number of times this node has been visited in the MCTS algorithm (double instead of int to accomodate tree decay) */
	private double numVisits;
	/** The sum of all the scores that have been backpropagated through this node */
	private double totalScore;
	/** The depth of this node in the current MCTS tree */
	private int depth;
	/** The highest score that has been propagated through this node so far */
	private double maxScore;

	/** List of actions that have not yet been expanded into child nodes */
	private ArrayList<ACTIONS> unexpandedActions;
	/** List of child nodes of this node */
	private ArrayList<MctNode> children;
	/** Observation of the game state in this node. Can change during the search in Open Loop MCTS */
	private StateObservation stateObs;
	
	private ArrayList<StateObservation> cachedStateObservations = null;
	
	/** By default, every node is assumed to represent a novel state */
	private boolean novel = true;
	/** When set to true, it means we have performed novelty tests for this node's children */
	private boolean noveltyTestedChildren = false;
	
	private NoveltyTester cachedNoveltyTester = null;
	private StateMemory cachedStateMemory = null;

	/** A cached state observation that can be used for closed-loop-style traversal of tree */
	private StateObservation savedStateObs = null;
	
	/** An integer-representation of the cell in which the avatar was in the last state observation seen in this node */
	private int lastAvatarCell;
	
	/** 
	 * Set to true if Loss Avoidance ended up backpropagating a loss through this node,
	 * indicating that this node may lead to an inescapable loss.
	 * 
	 * If this is the case, Novelty-Based Pruning is disabled when selecting a successor
	 * for this node
	 */
	private boolean inescapableLossFound;
	
	/**
	 * Set to true if a state was observed in this node with an immediate loss
	 */
	private boolean immediateLossDetected;

	public MctNode() {
		this(null, ACTIONS.ACTION_NIL);
	}

	public MctNode(MctNode parent, ACTIONS action) {
		totalScore = 0.0;
		maxScore = 0.0;
		numVisits = 0.0;
		this.parent = parent;
		actionFromParent = action;
		children = new ArrayList<MctNode>(5);
		stateObs = null;
		
		if (parent != null) {
			depth = parent.getDepth() + 1;
		} 
		else {
			depth = 0;
		}
		
		unexpandedActions = null;
		
		lastAvatarCell = -1;
		
		inescapableLossFound = false;
	}
	
	/**
	 * Backpropagates the given score from a Monte-Carlo simulation through this node
	 * 
	 * @param score
	 */
	public void backpropagate(double score){
		backpropagate(score, 1.0);
	}
	
	/**
	 * Backpropagates the given score through this node with the given number of visits
	 * <br> Can be used to pretend that a score was obtained through more or less than
	 * exactly 1.0 simulation
	 * 
	 * @param score
	 * @param numVisits
	 */
	public void backpropagate(double score, double numVisits){
		totalScore += score * numVisits;
		maxScore = Math.max(maxScore, score);
		this.numVisits += numVisits;
	}
	
	/**
	 * Decays the results collected in this node by the given factor
	 * 
	 * @param decayFactor
	 */
	public void decay(double decayFactor){
		// decay visit count
		numVisits *= decayFactor;
		
		// now decay the total scores by the same amount
		totalScore *= decayFactor;
		
		//maxScore = (numVisits > 0.0) ? (totalScore / numVisits) : 0.0;
		
		// depth should be lowered by 1 because our new root is 1 level lower than the previous root
		decrementDepth();
		
		// for nondeterministic game, get rid of old state that may be incorrect now
		savedStateObs = null;
		
		immediateLossDetected = false;
	}
	
	public void decrementDepth(){
		--depth;
	}
	
	public void addChild(MctNode newChildNode){
		children.add(newChildNode);
	}
	
	public void cacheNoveltyTester(NoveltyTester noveltyTester){
		cachedNoveltyTester = noveltyTester;
	}
	
	public void cacheStateMemory(StateMemory stateMem){
		cachedStateMemory = stateMem;
	}
	
	public void cacheStateObservation(StateObservation stateObs){
		if(cachedStateObservations == null){
			cachedStateObservations = new ArrayList<StateObservation>(2);
		}
		
		cachedStateObservations.add(stateObs);
	}
	
	/**
	 * Caches the given list of state observations. Every cached state observation will be used once
	 * instead of generating a new state when traversing the MCTS tree.
	 * 
	 * @param stateObservations
	 */
	public void cacheStateObservations(ArrayList<StateObservation> stateObservations){
		if(cachedStateObservations == null){
			cachedStateObservations = new ArrayList<StateObservation>(stateObservations);
		}
		else{
			cachedStateObservations.addAll(stateObservations);
		}
	}
	
	public boolean canBeImmediateLoss(){
		return immediateLossDetected;
	}
	
	/**
	 * Checks if all children are not novel. If so, also sets this node to not novel
	 * and tells parent to check it's children
	 */
	public void checkAllChildrenNotNovel(){
		boolean allChildrenNotNovel = true;
		for(MctNode child : children){
			if(child.isNovel()){
				allChildrenNotNovel = false;
			}
		}
		
		if(allChildrenNotNovel && !cachedStateMemory.importantGameStateChange){
			markNotNovel();
			
			if(parent != null){
				parent.checkAllChildrenNotNovel();
			}
		}
	}
	
	/**
	 * Generates and sets a new State Observation for this node.
	 * <br> Calling this method will likely modify the ''previousState'' object
	 * 
	 * <p> Returns the generated state (which will be the same ''previousState'' object
	 * again if the method chose to modify it, or a different object otherwise).
	 * 
	 * @param previousState
	 * @param action
	 * @return 
	 */
	public StateObs generateNewStateObs(StateObs previousState, ACTIONS action){
		StateObs returnState;
		
		if(hasCachedState()){
			// we still have some unused cached state observations, so use one of them instead
			// of generating a new state
			setStateObs(getNextCachedState());
			
			// the previousState will no longer be used anywhere, so we can let our parent cache it
			if(!previousState.shouldCopy()){
				parent.cacheStateObservation(previousState.getStateObsNoCopy());
			}
			
			returnState = new StateObs(stateObs, false);
		}
		else if(savedStateObs != null){
			setStateObs(savedStateObs);
			returnState = new StateObs(savedStateObs, true);
		}
		else{
			StateObservation nextState = previousState.getStateObs();
			nextState.advance(action);
			setStateObs(nextState);
			MctsController.NUM_ADVANCE_OPS += 1;
			
			if(savedStateObs == null && Globals.knowledgeBase.isGameDeterministic() && 
				numVisits + 1.0 >= MctsController.DETERMINISTIC_STATE_CACHE_VISIT_THRESHOLD){
				// time to start saving this state
				savedStateObs = nextState;
				returnState = new StateObs(nextState, true);
			}
			else{
				// don't need to copy this
				returnState = new StateObs(nextState, false);
			}
		}
		
		return returnState;
	}
	
	public ACTIONS getActionFromParent() {
		return actionFromParent;
	}
	
	public ActionLocation getActionLocationFromParent(){
		return new ActionLocation(actionFromParent, parent.getLastAvatarCell());
	}
	
	public NoveltyTester getCachedNoveltyTester(){
		return cachedNoveltyTester;
	}

	public StateMemory getCachedStateMemory(){
		return cachedStateMemory;
	}
	
	public ArrayList<MctNode> getChildren() {
		return children;
	}

	public int getDepth(){
		return depth;
	}
	
	public MctNode getExpandedChildForAction(ACTIONS action){
		for(MctNode child : children){
			if(child.getActionFromParent() == action){
				return child;
			}
		}
		
		return null;
	}
	
	public int getLastAvatarCell(){
		return lastAvatarCell;
	}
	
	public double getMaxScore(){
		return maxScore;
	}
	
	public StateObservation getNextCachedState(){
		return cachedStateObservations.remove(cachedStateObservations.size() - 1);
	}
	
	public double getNumVisits(){
		return numVisits;
	}
	
	public MctNode getParent(){
		return parent;
	}
	
	public StateObservation getSavedStateObs(){
		return savedStateObs;
	}

	public StateObservation getStateObs(){
		return stateObs;
	}
	
	/**
	 * Returns the sum of all scores backpropagated through this node
	 * 
	 * @return
	 */
	public double getTotalScore() {
		return totalScore;
	}
	
	/**
	 * Returns the list of actions that have not yet been used to create a new node
	 * 
	 * @return
	 */
	public ArrayList<ACTIONS> getUnexpandedActions(){
		return unexpandedActions;
	}
	
	public boolean hasCachedState(){
		return (cachedStateObservations != null && !cachedStateObservations.isEmpty());
	}
	
	public boolean hasNonImmediateLossChildren(){
		if(unexpandedActions.size() > 0){
			return true;
		}
		
		for(MctNode child : children){
			if(!child.canBeImmediateLoss()){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns true if and only if the node is fully expanded (meaning that all legal
	 * actions in this node have been used to generate children)
	 * 
	 * @return
	 */
	public boolean isFullyExpanded(){
		return (unexpandedActions != null && unexpandedActions.isEmpty());
	}
	
	public boolean isInescapableLossFound(){
		return inescapableLossFound;
	}
	
	public boolean isNovel(){
		return novel;
	}
	
	public void markNotNovel(){
		novel = false;
	}
	
	/**
	 * Called right before MCTS selects one of the children of this node in the selection phase
	 * 
	 * @param stateObs The current state observation in this node
	 */
	public void preSelect(StateObservation stateObs){
		MctsController mcts = (MctsController)Agent.controller;
		
		// TODO this isFullyExpanded() check is currently redundant, but might become necessary if we ever
		// implement something where we can pass up on selecting some unexpanded nodes (urgency stuff?)
		if(isFullyExpanded() && !noveltyTestedChildren){			
			if(mcts.allowsNoveltyBasedPruning()){
				NoveltyTester noveltyTester = new NoveltyTester();
				noveltyTester.setNoveltyTestMode(NOVELTY_TEST_MODES.One);
				
				StateMemory stateMem = cachedStateMemory;
				if(stateMem == null){
					stateMem = new StateMemory(stateObs, (parent == null ? null : parent.getCachedStateMemory()));
					cacheStateMemory(stateMem);
				}
				
				if(cachedNoveltyTester == null){
					// don't already have a cached novelty tester that contains this state in memory, so need to add it to the new novelty tester
					noveltyTester.noveltyTest(stateMem, true);
				}
				
				optimizeChildOrdering();
				
				for(int i = 0; i < children.size(); ++i){
					MctNode child = children.get(i);
					ACTIONS action = child.getActionFromParent();
					StateObservation successorState = child.getSavedStateObs();
					
					if(successorState == null){
						successorState = stateObs.copy();
						successorState.advance(action);
						MctsController.NUM_ADVANCE_OPS += 1;
						child.cacheStateObservation(successorState);
					}
					
					StateMemory childStateMem = new StateMemory(successorState, stateMem);
					child.cacheStateMemory(childStateMem);
					if(noveltyTester.noveltyTest(childStateMem, true) == NOVELTY_LEVELS.NotNovel){
						// this child is not novel according to novelty test
						
						if(action == ACTIONS.ACTION_USE){
							// USE action is always allowed to be non-novel
							child.markNotNovel();
						}
						else if(stateObs.getAvailableActions().size() > 3 && stateObs.getAvatarSpeed() > 0.5){
							// TODO should probably change the check for > 3 actions to explicitly check for no vertical or no horizontal movement
							// movement actions are only allowed to be non-novel if we can move in all directions and at a sufficient speed
							child.markNotNovel();
						}
						
						//System.out.println("failing direct novelty test");
					}
					else{
						// so far the child seems to be novel; time to follow trail of parent nodes back up the tree
						// and test on their cached novelty testers
						MctNode parent = child.getParent();
						while(parent != null){
							NoveltyTester parentNoveltyTester = parent.getCachedNoveltyTester();
							
							if(parentNoveltyTester == null){
								break;
							}
							
							StateMemory parentStateMem = parent.getCachedStateMemory();
							if(parentStateMem != null && parentStateMem.importantGameStateChange){
								// our parent caused an important change in game state (for instance, score increase),
								// so we want to stop following the chain of parents upwards because we want to 
								// ''reset the memory'' after this parent
								break;
							}
							
							if(parentNoveltyTester.noveltyTest(childStateMem, false) == NOVELTY_LEVELS.NotNovel){
								// this child is not novel
								child.markNotNovel();
								//System.out.println("failing novelty test based on chain of parents");
								
								// no need to continue following chain of parents
								break;
							}
							
							parent = parent.getParent();
						}
					}
				}
				
				boolean allChildrenFailedTest = true;
				for(MctNode child : children){
					child.cacheNoveltyTester(noveltyTester);
					if(child.isNovel()){
						allChildrenFailedTest = false;
					}
				}
				
				if(allChildrenFailedTest && !cachedStateMemory.importantGameStateChange){
					// if all follow-up actions are not novel, we can assume this node itself is also not novel
					markNotNovel();
					
					if(parent != null){
						parent.checkAllChildrenNotNovel();
					}
				}
				
				noveltyTestedChildren = true;
			}
		}
	}
	
	public void optimizeChildOrdering(){
		// TODO can probably optimize this by hardcoding the sorting
		Collections.sort(children, new Comparator<MctNode>(){

			@Override
			public int compare(MctNode child1, MctNode child2) {
				ACTIONS action1 = child1.getActionFromParent();
				ACTIONS action2 = child2.getActionFromParent();
				
				// Rule 1: USE action always last, because it is most likely to be not novel in most games
				if(action1 == ACTIONS.ACTION_USE){
					return 1;
				}
				else if(action2 == ACTIONS.ACTION_USE){
					return -1;
				}
				
				// Rule 2: an ''opposite'' movement of the previous movement ordered second-to-last
				ACTIONS previousAction = MctNode.this.actionFromParent;
				if(Globals.isMovementAction(previousAction)){
					if(Globals.isOppositeMovement(action1, previousAction)){
						return 1;
					}
					else if(Globals.isOppositeMovement(action2, previousAction)){
						return -1;
					}
				}
				
				// Rule 3: a copy of the previous movement is ordered third-to-last, because it is slightly more likely
				// to run into an edge of the screen or a wall (if we already moved down in the previous turn, another
				// move down is slightly more likely to run into the bottom of the screen because we know we just moved closer
				// to it).
				if(action1 == previousAction){
					return 1;
				}
				else if(action2 == previousAction){
					return 1;
				}
				
				// don't care about the ordering if the above rules don't apply
				return 0;
			}
			
		});
		
		// we dont want the USE action first, because it is most likely to be an action that doesn't do
		// anything, and by putting it somewhere other than first we make sure we observe deterministic
		// behavior of non-avatar objects in other nodes already
		// TODO is this still necessary here? isn't this already done above?
		if(children.get(0).getActionFromParent() == ACTIONS.ACTION_USE){
			Collections.swap(children, 0, children.size() - 1);
		}
	}
	
	/**
	 * This method is called after the backup step of MCTS went through this node.
	 * 
	 * Currently used to clean up memory a little bit in some cases
	 */
	public void postBackup(){
		stateObs = null;
	}
	
	public void removeCachedStates(){
		cachedStateObservations = null;
	}
	
	public void resetNoveltyTestResults(){
		noveltyTestedChildren = false;
		novel = true;
		cachedNoveltyTester = null;
		cachedStateMemory = null;
	}
	
	public void resetParent(){
		parent = null;
	}
	
	public void setChildren(ArrayList<MctNode> children){
		this.children.addAll(children);
	}
	
	public void setImmediateLossDetected(){
		immediateLossDetected = true;
	}
	
	public void setInescapableLossFound(){
		inescapableLossFound = true;
	}

	public void setStateObs(StateObservation stateObs) {
		this.stateObs = stateObs;
		lastAvatarCell = Globals.knowledgeBase.positionToInt(stateObs.getAvatarPosition());
		
		if(unexpandedActions == null){		// we haven't generated the list of unexpanded actions or children for this node yet
			ArrayList<ACTIONS> availableActions = stateObs.getAvailableActions();
			
			if(availableActions.size() > 0){
				unexpandedActions = new ArrayList<ACTIONS>(availableActions);
			}
		}
	}
}
