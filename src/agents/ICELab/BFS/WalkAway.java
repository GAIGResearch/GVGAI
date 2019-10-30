package agents.ICELab.BFS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import core.game.Observation;
import core.game.StateObservation;

public class WalkAway {

	final Vector2d startPosition;
	final Random random;

	public WalkAway( StateObservation so) {
		this.startPosition = so.getAvatarPosition().copy();
		this.random = new Random();
	}

	public ACTIONS run(StateObservation so, ElapsedCpuTimer timer) {
		double dist;
		double bestDist = Double.POSITIVE_INFINITY;  //shortest distance between portal and nilMove
		ACTIONS bestAction = ACTIONS.ACTION_NIL;
		ArrayList<ACTIONS> actions = new ArrayList<ACTIONS>();
		StateObservation sim;

		for( ACTIONS action: so.getAvailableActions( false ) ) {
			if (advanceStateAndCheckIfIsNilMove(so.copy(), action)) {
				actions.add(action);
			}
		}
		if(actions.isEmpty()){
			return bestAction;
		}
		else if(so.getResourcesPositions()!=null){
			for(ACTIONS action : actions){
				sim = so.copy();
				sim.advance(action);
				List<Observation>[] allResources = sim.getResourcesPositions(sim.getAvatarPosition());
				if(allResources!=null&&allResources.length!=0&&allResources[0].size()!=0){
					dist = allResources[0].get(0).position.dist(sim.getAvatarPosition());
					if(dist < bestDist){
						bestDist = dist;
						bestAction = action;
					}
				}
			}
			return actions.get(0);
		}
		else if(so.getMovablePositions()!=null){
		//	Collections.shuffle(actions);
			for(ACTIONS action : actions){
				sim = so.copy();
				sim.advance(action);
				List<Observation>[] allMovables = sim.getMovablePositions(sim.getAvatarPosition());
				if(allMovables!=null){
					dist = allMovables[0].get(0).position.dist(sim.getAvatarPosition());
					if(dist < bestDist){
						bestDist = dist;
						bestAction = action;
					}
				}
			}
			return actions.get(0);
		}
		else if(so.getImmovablePositions()!=null){
			for(ACTIONS action : actions){
				sim = so.copy();
				sim.advance(action);
				List<Observation>[] allImmovables = sim.getImmovablePositions(sim.getAvatarPosition());
				if(allImmovables!=null){
					dist = allImmovables[0].get(0).position.dist(sim.getAvatarPosition());
					if(dist < bestDist){
						bestDist = dist;
						bestAction = action;
					}
				}
			}
			return actions.get(0);
		}
		return bestAction;
	}


    public static boolean advanceStateAndCheckIfIsNilMove( StateObservation so, Types.ACTIONS action ) {
        double scoreBefore = so.getGameScore();
        Vector2d avatarPositionBefore = so.getAvatarPosition();
        int movableObjectStateBefore = hash( so.getMovablePositions() );

        so.advance( action );
        Vector2d avatorPositionAfter = so.getAvatarPosition();

        if (so.isGameOver())
            return false;

        double scoreAfter = so.getGameScore();
        if (scoreAfter != scoreBefore)
            return false;

        if(avatorPositionAfter.x == avatarPositionBefore.x && avatorPositionAfter.y == avatarPositionBefore.y){
        	return false;
        }
        int movableObjectStateAfter = hash(so.getMovablePositions());
        if (movableObjectStateAfter != movableObjectStateBefore)
            return false;

        return true;
    }

    public static int hash(List<Observation>[] observations) {
        if (observations == null)
            return 0;

        int sum = 0;
        for (List<Observation> observationsOfSameType : observations) {
            for (Observation anObservations : observationsOfSameType) {
                sum += hash( anObservations );
            }
        }

        return sum;
    }

    public static int hash( Observation anObservation ) {
        return (int) (anObservation.obsID * (anObservation.position.x + anObservation.position.y));
    }
}
