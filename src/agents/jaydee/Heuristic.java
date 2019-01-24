package agents.jaydee;

import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.awt.*;

public class Heuristic {
    public static final double LOST             = -1e7;
    public static final double WON              = 1e7;
    public static final double SCORE            = 1;
    public static final double VISITED_WEIGHT   = 1;
    public static final double OPPOSITE_ACTIONS = -0.1;
    public static final double BLOCKED_MOVEMENT = -100;

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
        score += SCORE * state.getGameScore();

        // visited weights
        score += -VISITED_WEIGHT * node.pheromones.grid[avatarTilePosition.x][avatarTilePosition.y];

        // avoid actions that are the opposite of the last action
        if (node.prev != null) {
            if ((node.action == Types.ACTIONS.ACTION_LEFT && node.prev.action == Types.ACTIONS.ACTION_RIGHT)
                    || (node.action == Types.ACTIONS.ACTION_RIGHT && node.prev.action == Types.ACTIONS.ACTION_LEFT)
                    || (node.action == Types.ACTIONS.ACTION_UP && node.prev.action == Types.ACTIONS.ACTION_DOWN)
                    || (node.action == Types.ACTIONS.ACTION_DOWN && node.prev.action == Types.ACTIONS.ACTION_UP)) {

                score += OPPOSITE_ACTIONS;
            }
        }

        // avoid actions that should move the avatar, but it didn't move - i.e. moves that run into a wall
        if ((node.action == Types.ACTIONS.ACTION_LEFT)
                || (node.action == Types.ACTIONS.ACTION_RIGHT)
                || (node.action == Types.ACTIONS.ACTION_UP)
                || (node.action == Types.ACTIONS.ACTION_DOWN)) {
            Vector2d prevPos = node.prev.avatarPos;
            if (avatarPosition.dist(prevPos) == 0) {
                score += BLOCKED_MOVEMENT;
            }
        }

        return score;
    }
}
