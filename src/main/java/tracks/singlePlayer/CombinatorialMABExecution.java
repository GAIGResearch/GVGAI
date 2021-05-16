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
import tracks.singlePlayer.florabranchi.agents.CombinatorialMABAgent;
import tracks.singlePlayer.florabranchi.persistence.AvailableGames;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

/**
 * author: Flora Branchi (florabranchi@gmail.com)
 */
public class CombinatorialMABExecution {


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

    String recordActionsFile = null;

    if (visuals) {
      final double[] doubles = ArcadeMachine.runOneGame(game, level1, visuals, selectedAgent,
          recordActionsFile, seed, 0);

      System.out.println(Arrays.toString(doubles));
    } else {

      ///List<String> gameList = Arrays.asList("camelRace", "crossfire", "thecitadel");
      List<String> gameList = Arrays.asList("jaws");

      int episodesPerLevel = 20;
      RunInstructions runInstructions = new RunInstructions();
      for (String gameInList : gameList) {
        gameIdx = Objects.requireNonNull(AvailableGames.fromName(gameInList)).getId();
        // Play given game 5 times each level
        for (int levelIt = 0; levelIt < 5; levelIt++) {
          String gamePath = games[gameIdx][0];
          String levelPath = gamePath.replace(gameInList, gameInList + "_lvl" + levelIt);
          runInstructions.addInstruction(new RunInstructions.RunInstruction(gamePath, gameInList, levelPath, levelIt, episodesPerLevel));
        }
      }
      CombinatorialMABAgent combinatorialMABAgent = null;

      for (RunInstructions.RunInstruction runInstruction : runInstructions.runInstructionList) {

        PropertyLoader.GAME_NAME = runInstruction.gameName;
        PropertyLoader.LOAD_RUN_INSTRUCTIONS = true;
        PropertyLoader.GAME = gameIdx;
        PropertyLoader.LEVEL = runInstruction.levelId;

        if (combinatorialMABAgent == null) {
          combinatorialMABAgent = new CombinatorialMABAgent();
        }

        String[] levelFiles;
        levelFiles = new String[1];
        levelFiles[0] = runInstruction.levelPath;
        episodes = runInstruction.episodes;
        System.out.printf("Running gameId %s level %s for %s episodes", runInstruction.gamePath, runInstruction.levelPath, episodes);

        int gamecount = 1;
        int totalWins = 0;
        for (int i = 0; i < episodes; i++) {


          // Setup Agent parameters
          final double[] doubles = ArcadeMachine.runOneGame(runInstruction.gamePath, runInstruction.levelPath, visuals, selectedAgent, recordActionsFile, seed, 0);
          System.out.println("Game Results " + Arrays.toString(doubles) + " -------------------------------");

          final boolean won = doubles[0] == 1;
          if (won) totalWins++;
          System.out.println("----------------- Win Record : games " + gamecount + " wins: " + totalWins);
          gamecount++;
          final int ticks = (int) doubles[2];
          final int score = (int) doubles[1];
          combinatorialMABAgent.result(score, won);
          combinatorialMABAgent.act(score, won);
        }
      }
    }
  }
}

