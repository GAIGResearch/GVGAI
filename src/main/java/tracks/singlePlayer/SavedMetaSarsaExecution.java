package tracks.singlePlayer;

import static java.util.logging.Level.INFO;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

import tools.Utils;
import tracks.ArcadeMachine;
import tracks.singlePlayer.florabranchi.agents.meta.MabParameters;
import tracks.singlePlayer.florabranchi.agents.meta.MetaMCTSAgent;
import tracks.singlePlayer.florabranchi.agents.meta.RunOptions;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

/**
 * author: Flora Branchi (florabranchi@gmail.com)
 */
public class SavedMetaSarsaExecution {


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

    MabParameters gameOptions = new MabParameters();
    //gameOptions.addParameter() = gameIdx;
    //gameOptions.treeReuse = PropertyLoader.TREE_REUSE;
/*    gameOptions.lossAvoidance = PropertyLoader.LOSS_AVOIDANCE;
    gameOptions.expandAllNodes = PropertyLoader.EXPAND_ALL_CHILD_NODES;
    gameOptions.safetyPreprunning = PropertyLoader.SAFETY_PREPRUNNING;
    gameOptions.shallowRollout = PropertyLoader.SIMULATION_DEPTH <= 50;*/
    //gameOptions.rawGameScore = PropertyLoader.TREE_REUSE;

    //todo fix run options
    RunOptions runOptions = new RunOptions();

    // where to record the actions
    // executed. null if not to save.
    String recordActionsFile = null;
    if (saveActions) {
      recordActionsFile = "botLogs/actions_" + gameName + "_lvl" + levelIdx + "_" + seed + ".txt";
    }

    if (visuals) {
      final double[] doubles = ArcadeMachine.runOneGame(game, level1, visuals, selectedAgent,
          recordActionsFile, seed, 0);

      System.out.println(Arrays.toString(doubles));
    } else {
      String[] levelFiles;
      levelFiles = new String[1];
      levelFiles[0] = level1;

      MetaMCTSAgent agent = new MetaMCTSAgent();
      agent.initializeTrainingWeightVector();

      for (int i = 0; i < episodes; i++) {

        // Setup Agent parameters
        final double[] doubles = ArcadeMachine.runOneGame(game, level1, visuals, selectedAgent, recordActionsFile, seed, 0);
        System.out.println(Arrays.toString(doubles));

        final boolean won = doubles[0] == 1;
        final int score = (int) doubles[1];
        final int ticks = (int) doubles[2];
        final MabParameters result = agent.act(score);

        final MabParameters actions = agent.updateAndGetNewMab(won, score);

        System.out.println("Selected action: \n" + actions);
        //gameOptions.act(actions);
      }

      //ArcadeMachine.runGames(game, levelFiles, episodes, selectedAgent, null);
    }

  }
}
