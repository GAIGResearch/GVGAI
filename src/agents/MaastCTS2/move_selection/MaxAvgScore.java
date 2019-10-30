package agents.MaastCTS2.move_selection;

import java.util.ArrayList;

import agents.MaastCTS2.Agent;
import agents.MaastCTS2.Globals;
import agents.MaastCTS2.controller.MctsController;
import agents.MaastCTS2.model.MctNode;
import ontology.Types.ACTIONS;

/**
 * Strategy that recommends the agent to play the move corresponding to the node with
 * the highest average score.
 * 
 * <p> Ties are broken by selecting actions with the highest number of visits, because
 * we are more certain about those scores being correct, and it allows for better tree reuse.
 * 
 * <p> This class actually is no longer a clean implementation that only maximizes the average score,
 * but also has some other special cases.
 *
 * @author Dennis Soemers
 */
public class MaxAvgScore implements IMoveSelectionStrategy {

	@Override
	public ACTIONS selectMove(MctNode root){
		ArrayList<MctNode> children = root.getChildren();	
		MctsController controller = ((MctsController)Agent.controller);
		
		double minScore = controller.MIN_SCORE;
		double maxScore = controller.MAX_SCORE;
		
		ACTIONS bestAction = ACTIONS.ACTION_NIL;
		double bestAvgScore = Double.NEGATIVE_INFINITY;
		double bestActionNumVisits = Double.NEGATIVE_INFINITY;
		double bestActionRandomTieBreaker = Double.NEGATIVE_INFINITY;
		int numChildren = children.size();
		
		ACTIONS bestActionNonNovel = ACTIONS.ACTION_NIL;
		double bestAvgScoreNonNovel = Double.NEGATIVE_INFINITY;
		double bestActionNumVisitsNonNovel = Double.NEGATIVE_INFINITY;
		double bestActionRandomTieBreakerNonNovel = Double.NEGATIVE_INFINITY;
		
		/*NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		DecimalFormat scoreFormatter = (DecimalFormat)nf;
		scoreFormatter.applyPattern("#000.000000000");
		
		String scores = "Avg. Scores	= [";
		String visits = "Visits		= [";
		String novelties = "Novelties 	= [";
		for(int i = 0; i < numChildren; ++i){
			MctNode child = children.get(i);
			double numVisits = child.getNumVisits();
			double avgScore = child.getTotalScore() / numVisits;
			double novelty = child.isNovel() ? 1.0 : 0.0;

			double normalised = Globals.normalise(avgScore, minScore, maxScore);
			
			if(i < numChildren - 1){
				scores += scoreFormatter.format(normalised) + ", ";
				visits += scoreFormatter.format(numVisits) + ", ";
				novelties += scoreFormatter.format(novelty) + ", ";
			}
			else{
				scores += scoreFormatter.format(normalised);
				visits += scoreFormatter.format(numVisits);
				novelties += scoreFormatter.format(novelty);
			}
		}
		scores += "]";
		visits += "]";
		novelties += "]";*/
		
		for(int i = 0; i < numChildren; ++i){
			MctNode child = children.get(i);
			double numVisits = child.getNumVisits();
			double avgScore;
			
			//if(Globals.knowledgeBase.isGameDeterministic()){
			//	avgScore = child.getMaxScore();		// can simply go for max score ever observed in deterministic games
			//}
			//else{
				avgScore = child.getTotalScore() / numVisits;
			//}
			
			if(!child.isNovel() && !root.isInescapableLossFound()){
				if(numVisits >= 1.0){	// only gonna consider non-novel actions with >= 1.0 visits
					if(avgScore > bestAvgScoreNonNovel){	// new best score
						bestAvgScoreNonNovel = avgScore;
						bestActionNumVisitsNonNovel = numVisits;
						bestActionRandomTieBreakerNonNovel = Globals.smallNoise();
						bestActionNonNovel = child.getActionFromParent();
					}
					else if(avgScore >= bestAvgScoreNonNovel){
						if(numVisits > bestActionNumVisitsNonNovel){	// num visits as tie-breaker
							bestAvgScoreNonNovel = Math.max(avgScore, bestAvgScoreNonNovel);
							bestActionNumVisitsNonNovel = numVisits;
							bestActionRandomTieBreakerNonNovel = Globals.smallNoise();
							bestActionNonNovel = child.getActionFromParent();
						}
						else if(avgScore >= bestAvgScoreNonNovel && numVisits == bestActionNumVisitsNonNovel){
							double randomTieBreaker = Globals.smallNoise();
							if(randomTieBreaker > bestActionRandomTieBreakerNonNovel){	// randomly break ties
								bestAvgScoreNonNovel = Math.max(avgScore, bestAvgScoreNonNovel);
								bestActionNumVisitsNonNovel = numVisits;
								bestActionRandomTieBreakerNonNovel = randomTieBreaker;
								bestActionNonNovel = child.getActionFromParent();
							}
						}
					}
				}
				
				// give penalty to non-novel action
				avgScore -= Globals.HUGE_ENDGAME_SCORE;
			}
			
			if(avgScore > bestAvgScore){	// new best score
				//System.out.println("New best child because score: " + i + ". " + avgScore + " > " + (bestAvgScore));
				bestAvgScore = avgScore;
				bestActionNumVisits = numVisits;
				bestActionRandomTieBreaker = Globals.smallNoise();
				bestAction = child.getActionFromParent();
			}
			else if(avgScore >= bestAvgScore){
				if(numVisits > bestActionNumVisits){	// num visits as tie-breaker
					//System.out.println("New best child because visits: " + i + ". " + numVisits + " > " + (bestActionNumVisits));
					bestAvgScore = Math.max(avgScore, bestAvgScore);
					bestActionNumVisits = numVisits;
					bestActionRandomTieBreaker = Globals.smallNoise();
					bestAction = child.getActionFromParent();
				}
				else if(avgScore >= bestAvgScore && numVisits == bestActionNumVisits){
					double randomTieBreaker = Globals.smallNoise();
					if(randomTieBreaker > bestActionRandomTieBreaker){	// randomly break ties
						//System.out.println("New best child because random tiebreaker: " + i);
						bestAvgScore = Math.max(avgScore, bestAvgScore);
						bestActionNumVisits = numVisits;
						bestActionRandomTieBreaker = randomTieBreaker;
						bestAction = child.getActionFromParent();
					}
				}
			}
		}
		
		if(bestActionNonNovel != ACTIONS.ACTION_NIL &&
			Globals.normalise(bestAvgScore, minScore, maxScore) < 0.5 && bestAvgScoreNonNovel > bestAvgScore){
			//System.out.println("Playing non-novel move with avg score of " + bestAvgScore + " and num visits of " + bestActionNumVisitsNonNovel);
			bestAction = bestActionNonNovel;	// take the non-novel action if everything else is really bad
			
			/*for(int i = 0; i < numChildren; ++i){
				if(children.get(i).getActionFromParent() == bestAction){
					System.out.println();
					System.out.println(scores);
					System.out.println(visits);
					System.out.println(novelties);
					System.out.println("Best child: " + i);
				}
			}*/
		}
		
		/*for(int i = 0; i < numChildren; ++i){
			if(children.get(i).getActionFromParent() == bestAction){
				System.out.println();
				System.out.println(scores);
				System.out.println(visits);
				System.out.println(novelties);
				System.out.println("Best child: " + i);
			}
		}*/
		
		return bestAction;
	}
	
	@Override
	public String getName() {
		return "MaxAvgScore";
	}

	@Override
	public String getConfigDataString() {
		return "";
	}
}
