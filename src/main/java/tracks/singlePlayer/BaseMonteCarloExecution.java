package tracks.singlePlayer;

import static java.util.logging.Level.INFO;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

import tools.Utils;
import tracks.ArcadeMachine;
import tracks.singlePlayer.florabranchi.persistence.AvailableGames;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

/**
 * author: Flora Branchi (florabranchi@gmail.com)
 */
public class BaseMonteCarloExecution {


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

    int gameIdx = PropertyLoader.GAME;
    int levelIdx = PropertyLoader.LEVEL;
    int episodes = PropertyLoader.EPISODES;

    String selectedAgent = PropertyLoader.AGENT;
    boolean visuals = PropertyLoader.VISUALS;

    //String gameName = games[gameIdx][1];


    List<String> gameList = Arrays.asList("aliens");

    if (visuals) {
      String game = games[gameIdx][0];
      String level1 = game.replace(game, game + "_lvl" + levelIdx);
      final double[] doubles = ArcadeMachine.runOneGame(game, level1, visuals, selectedAgent,
          null, seed, 0);

      System.out.println(Arrays.toString(doubles));
    } else {

      RunInstructions runInstructions = new RunInstructions();
      for (String gameName : gameList) {
        gameIdx = Objects.requireNonNull(AvailableGames.fromName(gameName)).getId();
        // Play given game 5 times each level
        for (int levelIt = 0; levelIt < 5; levelIt++) {
          String game = games[gameIdx][0];
          String levelPath = game.replace(gameName, gameName + "_lvl" + levelIt);
          runInstructions.addInstruction(new RunInstructions.RunInstruction(game, gameName, levelPath, levelIt, 5));
        }
      }


      for (RunInstructions.RunInstruction runInstruction : runInstructions.runInstructionList) {

        PropertyLoader.GAME_NAME = runInstruction.gameName;
        PropertyLoader.GAME = gameIdx;
        PropertyLoader.LEVEL = runInstruction.levelId;
        String[] levelFiles;
        levelFiles = new String[1];
        levelFiles[0] = runInstruction.levelPath;
        episodes = runInstruction.episodes;
        System.out.printf("Running gameId %s level %s for %s episodes", runInstruction.gamePath, episodes, runInstruction.levelPath);
        ArcadeMachine.runGames(runInstruction.gamePath, levelFiles, episodes, selectedAgent, null);
      }


    }

  }
}
