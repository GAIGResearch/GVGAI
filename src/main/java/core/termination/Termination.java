package core.termination;

import java.util.ArrayList;

import core.vgdl.VGDLFactory;
import core.content.TerminationContent;
import core.game.Game;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 22/10/13
 * Time: 18:47
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public abstract class Termination {

    public String win;
    public int limit;
    public boolean count_score = false;
    public static boolean canEnd = true;

    public void parseParameters(TerminationContent content)
    {
        VGDLFactory.GetInstance().parseParameters(content, this);
    }

    public abstract boolean isDone(Game game);

    public boolean isFinished(Game game)
    {
        //It's finished if the player pressed ESCAPE or the game is over..
        return game.isGameOver();
    }

    /**
     * Determine win state of a specific player.
     * @param playerID - ID of the player to query.
     * @return - true if player won, false otherwise.
     */
    public boolean win (int playerID) {
        try {
            String[] winners = win.split(",");
            boolean win = Boolean.parseBoolean(winners[playerID]);
            return win;
        } catch (Exception e) {
            return false;
        }
    }

    public void countScore(Game game) {
        if (count_score) {
            double maxScore = game.getAvatar(0).getScore();
            boolean all0 = maxScore == 0;
            for (int i = 1; i < game.no_players; i++) {
                double score = game.getAvatar(i).getScore();
                if (score != 0) {
                    all0 = false;
                    if (score > maxScore) {
                        maxScore = score;
                    }
                }
            }

            // Give win to player/s with most number of points, rest lose
            win = "";
            for (int i = 0; i < game.no_players; i++) {
                double s = game.getAvatar(i).getScore();
                if (!all0 && s == maxScore) {
                    win += "True";
                } else {
                    win += "False";
                }
                if (i != game.no_players - 1) {
                    win += ",";
                }
            }
        }
    }

    /**
     * Get all sprites that are used to check the termination condition
     * @return all termination condition sprites
     */
    public ArrayList<String> getTerminationSprites(){
    	return new ArrayList<String>();
    }
}
