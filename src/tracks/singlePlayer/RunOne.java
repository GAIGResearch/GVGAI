package tracks.singlePlayer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Random;

import core.logging.Logger;
import tools.Utils;
import tracks.ArcadeMachine;
import video.constants.InteractionStaticData;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class RunOne {

    public static void main(String[] args) throws IOException {
    	
		//Load available games
		String spGamesCollection =  "examples/atDelfi_games.csv";
		String[][] games = Utils.readGames(spGamesCollection);

		//Load available agents
		String[] agents = RunOne.generateAgents();
		
		//Game settings
		boolean visuals = false;
		int seed = new Random().nextInt();

		// Game and level to play
		int gameIdx = Integer.parseInt(args[0]);
		int levelIdx = Integer.parseInt(args[1]); // level names from 0 to 4 (game_lvlN.txt).
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
						// + levelIdx + "_" + seed + ".txt";
						// where to record the actions
						// executed. null if not to save.
		
		String agent = agents[Integer.parseInt(args[2])];

		// set up InteractionStaticData
		InteractionStaticData.gameName = gameName;
		InteractionStaticData.agentName = args[2];
		InteractionStaticData.levelCount = args[1];
		// play 20 per level
		int playthroughTotal = Integer.parseInt(args[3]);
//		InteractionStaticData.playthroughCount = args[2];
		
		InteractionStaticData.createFolders();
		for(int i = 0; i < playthroughTotal; i++) {
			InteractionStaticData.playthroughCount = "" + i;
			ArcadeMachine.runOneGame(game, level1, visuals, agent, recordActionsFile, seed, 0);
		}

    }
    
	public static String[] generateAgents() {
		try {
			File agentsDirectory = new File("src/agents");
			String[] agents = agentsDirectory.list(new FilenameFilter() {
				  @Override
				  public boolean accept(File current, String name) {
				    return new File(current, name).isDirectory();
				  }
				});
			
			for(int i = 0; i < agents.length; i++) {
				agents[i] = "agents." + agents[i] + ".Agent";
			}
			
			return agents;
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
    
}
