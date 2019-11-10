package tutorialGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import org.graphstream.graph.Graph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import core.game.Game;
import core.game.GameDescription;
import core.game.SLDescription;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;
import tools.GameAnalyzer;
import tools.IO;
import tools.LevelAnalyzer;
import tutorialGeneration.criticalPathing.CriticalPather;
import tutorialGeneration.criticalPathing.BestFirstPather;
import video.basics.BunchOfGames;

public class AtDelfi {

	private boolean verbose;
	
	private AtDelfiGraph gameGraph;
	private SLDescription sl;
	private GameDescription gd;
	private LevelAnalyzer la;
	private GameAnalyzer ga;
	
	private Game game;
	private String[] lines; 
	
	private int seed;
	
	private String gameName;
	private String gameFile;
	private String levelFile;
	
	private VisualDemonstrationInterfacer vdi;
	
	public String path;
		
	public static String[] agents;
	//	= {"adrienctx.Agent", "NovelTS.Agent", "NovTea.Agent", "Number27.Agent", "YOLOBOT.Agent", "tracks.singlePlayer.simple.doNothing.Agent", "tracks.singlePlayer.simple.sampleonesteplookahead.Agent"};
	public static int levelCount = 5;
	public static int playthroughCount = 30;
	//	private String[] agents = {"adrienctx.Agent"};
	
	private boolean visualizeCriticalPath = false;

	public AtDelfi(String gameFile, String levelFile, String gameName, int seed, boolean verbose) {
		this.verbose = verbose;
		if (this.verbose) 
			System.out.println("Initializing AtDelfi...");
		this.seed = seed;
		
		this.gameFile = gameFile;
		this.levelFile = levelFile;
		this.gameName = gameName;
		
		// build game, game description, and slDescription
		this.game = getGame();
		this.lines = getLevelLines();
		
		this.gd = new GameDescription(this.game);
		
		try {
			this.sl = new SLDescription(this.game, this.lines, this.seed);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.la = new LevelAnalyzer(sl);
		this.ga = new GameAnalyzer(gd);
		
		try {
			this.vdi = new VisualDemonstrationInterfacer(this.gameName, false);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// read in all agents
		generateAgents();
//		this.buildGraph();
	}
	public void generateAgents() {
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
			
			this.agents = agents;
			
			System.out.println(agents);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void buildGraph(String agent, int level) {
		this.gameGraph = new AtDelfiGraph(gd, sl, ga, la, this.gameName);
		Graph graph = this.gameGraph.build();
		changeGraphTitle(graph);
//		this.gameGraph.insertFrameInformation(vdi);
		
		if(gameGraph.isMechanicVisualization()) {
			gameGraph.visualizeMechanicGraph(agent, level);
			if(visualizeCriticalPath) {
				CriticalPather cp = new BestFirstPather(gameGraph);
				criticalPath(cp, agent, true, level);
			}
		}
		if(gameGraph.isNodeVisualization()) {
			gameGraph.visualizeNodeGraph();
		}
	}
	
	public void insertFrameInfo() {
		this.gameGraph.insertFrameInformation(vdi);
	}

	public void testPlayGames() {
		ArrayList<BunchOfGames> bogs = new ArrayList<>();
		levelCount = 1;
		playthroughCount = 1;
		for(int i = 0; i <levelCount; i++) {
			for(int j = 0; j < playthroughCount; j++) {
				bogs.add(new BunchOfGames(gameFile, levelFile, "human", "" + i, "" + j));
			}
		}
		try {
			String[] agents = {"adrienctx.Agent"};
			vdi.runBunchOfGames(bogs, agents, levelCount, playthroughCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Mechanic> criticalPath(CriticalPather criticalPather, String agent, boolean isWin, int level) {
		List<Mechanic> criticalPath = criticalPather.findCriticalPath(agent, isWin, level);
		
		if(visualizeCriticalPath) {
			this.gameGraph.colorizeCriticalPath(criticalPath);
		}
		
		return criticalPath;
	}
	
	public void playGames() {
		if(verbose)
			System.out.println("** Playing Games **");
		ArrayList<BunchOfGames> bogs = new ArrayList<>();
		for(int i = 0; i < agents.length; i++) {
			for(int j = 0; j < levelCount; j++) {
				for (int k = 0; k < playthroughCount; k++) {
					BunchOfGames game = new BunchOfGames(gameFile, levelFile, "human", "" + j, "" + k);
					bogs.add(game);
				}
			}
		}
		try {
			vdi.runBunchOfGames(bogs, this.agents, levelCount, playthroughCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void playGames(int id) {
		if(verbose)
			System.out.println("** Playing Games **");
		ArrayList<BunchOfGames> bogs = new ArrayList<>();
		
		int agentId = id%29;

		for(int j = 0; j < levelCount; j++) {
			for (int k = 0; k < playthroughCount; k++) {
				String levelFile = this.path + j + ".txt";
				BunchOfGames game = new BunchOfGames(gameFile, levelFile, agents[agentId], "" + j, "" + k);
				bogs.add(game);
			}
		}
		
		try {
			vdi.runBunchOfGames(bogs, AtDelfi.agents, levelCount, playthroughCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void changeGraphTitle(Graph graph) {
		String title = this.gameName;
		Border border = BorderFactory.createTitledBorder(title);
		graph.display().getDefaultView().setBorder(border);
	}
	public Game getGame() {
        VGDLFactory.GetInstance().init(); // This always first thing to do.
        VGDLRegistry.GetInstance().init();
	
        System.out.println(
                "** Generating the game from " + this.gameName + " **");
        
        // First, we create the game to be played..
        Game toPlay = new VGDLParser().parseGame(this.gameFile);
        return toPlay;
	}
	
	public void saveGameMechanics(String filename, List<Mechanic> mechsToSave) {
		// gather all mechanics in format
		JSONArray jsonMechArray = new JSONArray(); 
		for(Mechanic mech : mechsToSave) {
			JSONObject jsonMech = new JSONObject();
			jsonMech.put("condition", mech.getConditions().get(0).getName());
			jsonMech.put("action", mech.getReadibleAction());
			
			
			for(int i = 0; i < mech.getSprites().size(); i++) {
				jsonMech.put("sprite" + (i+1), mech.getSprites().get(i).getName());
			}
			
			jsonMechArray.add(jsonMech);
		}
		
		try (FileWriter file = new FileWriter(filename)) {
			 
            file.write(jsonMechArray.toJSONString());
            file.flush();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
		// save to file
	}
	public String[] getLevelLines() {
		String[] lines = new IO().readFile(this.levelFile);
		return lines;
	}
	
	/**
	 * @return the gameGraph
	 */
	public AtDelfiGraph getGameGraph() {
		return gameGraph;
	}


	/**
	 * @param gameGraph the gameGraph to set
	 */
	public void setGameGraph(AtDelfiGraph gameGraph) {
		this.gameGraph = gameGraph;
	}
	
	
}	
