package agents.ICELab.OpenLoopRLBiasMCTS;

import java.awt.Point;

import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import agents.ICELab.Agent;
import agents.ICELab.GameInfo;
public class Heuristic {
    public static final double LOST             = -1e4;
    public static final double WON              = 1e4;
    public static final double SCORE            = 50;
    public static final double VISITED_WEIGHT   = 1;
    public static final double DISTANCE_WEIGHT   = 0.1;

    static public double evaluate(Node node) {
        double score = 0;

        StateObservation state = node.state;

        Vector2d avatarPosition = node.avatarPos;
        Point avatarTilePosition = Utils.toTileCoord(avatarPosition);

        // first check win or lose
        boolean gameOver = state.isGameOver();
        Types.WINNER win = state.getGameWinner();

        if (gameOver && win == Types.WINNER.PLAYER_LOSES) {
            score += LOST;
        }

        if (gameOver && win == Types.WINNER.PLAYER_WINS) {
            score += WON;
        }

        // game score
        score += SCORE * (state.getGameScore() - ((node.prev == null)? 0:node.prev.state.getGameScore()));

        if (node.prev != null && !node.state.isGameOver() && GameInfo.width * GameInfo.height < 500){
        		double distance_score = Agent.memory.getDistanceChanges(node);
        		//System.out.println("distance_score: " + distance_score);
        		score += DISTANCE_WEIGHT * distance_score;
        }
        // visited weights
        score += -VISITED_WEIGHT * node.pheromones.grid[avatarTilePosition.x][avatarTilePosition.y];

        return score;
    }
}
