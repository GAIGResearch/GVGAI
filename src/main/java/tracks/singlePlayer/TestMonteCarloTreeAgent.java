package tracks.singlePlayer;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

import java.util.Random;
import java.util.logging.Level;

import tools.Utils;
import tracks.ArcadeMachine;

/**
 * author: Flora Branchi (florabranchi@gmail.com)
 */
public class TestMonteCarloTreeAgent {

  public static void main(String[] args) {

    LOGGER.setLevel(Level.CONFIG);

    //Game settings

    final boolean saveActions = false;
    final ETrainingType selectedTraining = ETrainingType.BOT;
    boolean visuals = true;
    int seed = new Random().nextInt();

    String floraController = "tracks.singlePlayer.florabranchi.agents.MonteCarloTreeAgent";

    // Load available games
    String spGamesCollection = "examples/all_games_sp.csv";
    String[][] games = Utils.readGames(spGamesCollection);


    // Game and level to play
    int gameIdx = 0;
    int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).

    String gameName = games[gameIdx][1];
    String game = games[gameIdx][0];
    String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

    // where to record the actions
    // executed. null if not to save.
    String recordActionsFile = null;
    if (saveActions) {
      recordActionsFile = "botLogs/actions_" + gameName + "_lvl" + levelIdx + "_" + seed + ".txt";
    }

    switch (selectedTraining) {

      case PLAYER:
        // 1. This starts a game, in a level, played by a human.
        ArcadeMachine.playOneGame(game, level1, recordActionsFile, seed);
        break;
      case BOT:
        // 2. This plays a game in a level by the controller.
        ArcadeMachine.runOneGame(game, level1, visuals, floraController, recordActionsFile, seed, 0);
        break;
      case REPLAY:
        // 3. This replays a game from an action file previously recorded
        ArcadeMachine.replayGame(game, level1, visuals, recordActionsFile);
    }


    // 4. This plays a single game, in N levels, M times :
//		String level2 = new String(game).replace(gameName, gameName + "_lvl" + 1);
//		int M = 10;
//		for(int i=0; i<games.length; i++){
//			game = games[i][0];
//			gameName = games[i][1];
//			level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
//			ArcadeMachine.runGames(game, new String[]{level1}, M, sampleMCTSController, null);
//		}

    //5. This plays N games, in the first L levels, M times each. Actions to file optional (set saveActions to true).
//		int N = games.length, L = 2, M = 1;
//		boolean saveActions = false;
//		String[] levels = new String[L];
//		String[] actionFiles = new String[L*M];
//		for(int i = 0; i < N; ++i)
//		{
//			int actionIdx = 0;
//			game = games[i][0];
//			gameName = games[i][1];
//			for(int j = 0; j < L; ++j){
//				levels[j] = game.replace(gameName, gameName + "_lvl" + j);
//				if(saveActions) for(int k = 0; k < M; ++k)
//				actionFiles[actionIdx++] = "actions_game_" + i + "_level_" + j + "_" + k + ".txt";
//			}
//			ArcadeMachine.runGames(game, levels, M, sampleRHEAController, saveActions? actionFiles:null);
//		}


  }

  public enum ETrainingType {
    PLAYER,
    BOT,
    REPLAY
  }
}
