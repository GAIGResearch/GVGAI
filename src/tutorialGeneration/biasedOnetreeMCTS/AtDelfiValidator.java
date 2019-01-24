package tutorialGeneration.biasedOnetreeMCTS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import core.competition.CompetitionParameters;
import core.game.Game;
import core.game.StateObservation;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;
import ontology.Types;
import tools.Utils;
import tracks.ArcadeMachine;
import video.constants.InteractionStaticData;

public class AtDelfiValidator {

	public boolean VERBOSE = true;
	String atDelfiGamesCollection =  "examples/atDelfi_games.csv";
    String gamesPath = "examples/gridphysics/";
    String physicsGamesPath = "examples/contphysics/";
    String generateTutorialPath = gamesPath;

	String[][] games = Utils.readAtDelfiGames(atDelfiGamesCollection);
    int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
    int gameIdx = 47;
	
	public StateObservation startup(int randomSeed, String game_file, String level_file) throws IOException {
		VGDLFactory.GetInstance().init(); // This always first thing to do.
		VGDLRegistry.GetInstance().init();

		if (VERBOSE)
			System.out.println(" ** Playing game " + game_file + ", level " + level_file + " **");

		if (CompetitionParameters.OS_WIN)
		{
			System.out.println(" * WARNING: Time limitations based on WALL TIME on Windows * ");
		}

		// First, we create the game to be played..
		Game toPlay = new VGDLParser().parseGame(game_file);
		toPlay.buildLevel(level_file, randomSeed);

		if(InteractionStaticData.saveSpriteGroup) {
			InteractionStaticData.createJSONInfo(toPlay);
		}
		
		// Warm the game up.
		ArcadeMachine.warmUp(toPlay, CompetitionParameters.WARMUP_TIME);
		
		return toPlay.getObservation();
	}
	public static void main(String[] args) {
		
		int seed = 100;
		AtDelfiValidator validator = new AtDelfiValidator();
		validator.runXperiments(10, true, seed, false);
	}
	
	public String[] getGame(int id) {
		return games[id];
	}
	
	public void runXperiments(int x, boolean improved, int seed, boolean seeded) {
		
		String gameFile = generateTutorialPath + getGame(gameIdx)[1] + ".txt";
		String levelFile = gamesPath + getGame(gameIdx)[1] + "_lvl" + levelIdx + ".txt";
		try{
			// make experiments directory and main experiment file //
			File directory = new File(String.valueOf("experiments"));

			if(!directory.exists()){

				directory.mkdir();
			}
			File mainFile = new File("experiments/main_file.txt");
			
			mainFile.createNewFile();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		// run x amount of experiments //
		File mainFile = new File("experiments/main_file.txt");
		int winning = 0;
		for(int i = 0; i < x; i++) {
			try{
				File expFile = new File("experiments/experiment_"+ i + ".txt");
				expFile.createNewFile();
				
				// more setup stuff
				if(!seeded)
					seed = new Random().nextInt();
				StateObservation root = startup(seed, gameFile, levelFile);
		        ArrayList<Types.ACTIONS> act = root.getAvailableActions();
		        Types.ACTIONS[] actions = new Types.ACTIONS[act.size()];
		        
		        for(int k = 0; k < actions.length; ++k)
		        {
		            actions[k] = act.get(k);
		        }
		        
		        int num_actions = actions.length;
		        		        
		        // create the mcts player //
				if(!seeded)
					seed = new Random().nextInt();
		        SingleMCTSPlayer player = new SingleMCTSPlayer(new Random(seed), num_actions, actions, improved, expFile, mainFile);
		        
		        player.init(root);
		        winning += player.run(i);
		        System.out.println("Complete. Wins: " + winning + " / " + (i+1));
				
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
