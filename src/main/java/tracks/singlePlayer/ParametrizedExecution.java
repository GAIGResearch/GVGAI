package tracks.singlePlayer;

import static java.util.logging.Level.INFO;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import tools.Utils;
import tracks.ArcadeMachine;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

/**
 * author: Flora Branchi (florabranchi@gmail.com)
 */
public class ParametrizedExecution {


  private final static Logger LOGGER = Logger.getLogger("GVGAI_BOT");


  public static void main(String[] args) throws IOException {

    PropertyLoader propertyLoader = new PropertyLoader(args[0]);


    //Game settings
    LOGGER.setLevel(propertyLoader.LOGGER_LEVEL);
    LOGGER.log(INFO, "Starting game....");
    final boolean saveActions = false;

    //boolean visuals = false;
    int seed = new Random().nextInt();


    // Load available games
    String spGamesCollection = "examples/all_games_sp.csv";
    String[][] games = Utils.readGames(spGamesCollection);

    int gameIdx = propertyLoader.GAME;
    int levelIdx = propertyLoader.LEVEL;
    int episodes = propertyLoader.EPISODES;

    String selectedAgent = propertyLoader.AGENT;
    boolean visuals = propertyLoader.VISUALS;

    String gameName = games[gameIdx][1];
    String game = games[gameIdx][0];
    String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);


    // where to record the actions
    // executed. null if not to save.
    String recordActionsFile = null;
    if (saveActions) {
      recordActionsFile = "botLogs/actions_" + gameName + "_lvl" + levelIdx + "_" + seed + ".txt";
    }

    if (visuals) {
      ArcadeMachine.runOneGame(game, level1, visuals, selectedAgent, recordActionsFile, seed, 0);

    } else {
      String[] levelFiles;
      levelFiles = new String[1];
      levelFiles[0] = level1;
      ArcadeMachine.runGames(game, levelFiles, episodes, selectedAgent, null);
    }

  }
}
